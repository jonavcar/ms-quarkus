## ¿Por Qué Podrías No Tener RedisJSON?

RedisJSON **NO es un problema de Quarkus** - Quarkus y Redisson lo soportan perfectamente. El problema es que **requiere Redis Stack o Redis 8+ en el servidor**, no en tu aplicación Java.[1][2][3][4][5]

### Razones Comunes para No Tenerlo

**Servicios cloud administrados tradicionales:** AWS ElastiCache, Azure Cache for Redis, Google Cloud Memorystore históricamente **NO incluían módulos Redis** por conflictos de licencia. Solo desde 2021-2024 algunos proveedores comenzaron a soportar RedisJSON nativamente.[6]

**Redis OSS básico:** Si usas Redis Community Edition estándar (versiones pre-8.0) sin Redis Stack, solo tienes tipos de datos básicos: String, Hash, List, Set, Sorted Set.[4][5]

**Restricciones empresariales:** Algunos entornos corporativos restringen la instalación de módulos adicionales por políticas de seguridad o complejidad operacional.[7]

**Redis 8.0 cambia todo:** Desde Redis 8.0 (mayo 2025), RedisJSON está integrado nativamente bajo licencia AGPLv3 (open source). Ya no necesitas instalación separada, pero muchos entornos production aún usan Redis 6.x/7.x.[8][9][10][11][1]

### RedisJSON NO Es Pagado

RedisJSON es **completamente gratuito y open source**:[12][1][8]

**Redis 8.0+:** Incluido nativamente bajo AGPLv3 (licencia OSI-approved open source).[9][1][8]

**Redis 7.x y anteriores:** Disponible como módulo en Redis Stack (también open source bajo RSALv2/SSPLv1/AGPLv3).[2][5][12]

**Instalación gratuita:**
```bash
# Docker (más simple)
docker run -d -p 6379:6379 redis/redis-stack-server:latest

# Homebrew (Mac)
brew tap redis-stack/redis-stack
brew install redis-stack

# Linux (compilar módulo)
git clone --recursive https://github.com/RedisJSON/RedisJSON.git
cd RedisJSON && make build
redis-server --loadmodule ./target/release/librejson.so
```


### Compatibilidad con Quarkus

Quarkus + Redisson **soporta completamente RedisJSON**. No hay incompatibilidad:[3][13]

```java
// Funciona perfectamente en Quarkus
@Inject
RedissonClient redissonClient;

RJsonBucket<Product> bucket = redissonClient.getJsonBucket("key", 
    new JacksonCodec<>(Product.class));
```

El único requisito es que tu **servidor Redis** tenga RedisJSON habilitado.[13][4]

## Desventajas Críticas de NO Tener RedisJSON

### Performance Degradada

Sin RedisJSON, para modificar solo el balance debes:[14][15]

```java
// ❌ Sin RedisJSON (ineficiente)
public void updateBalance(String sessionId, String productId, Balance newBalance) {
    // 1. FETCH: Traer producto completo desde Redis (~2KB)
    String json = redissonClient.getBucket("key").get();
    
    // 2. PARSE: Deserializar JSON completo a objeto Java
    Product product = objectMapper.readValue(json, Product.class);
    
    // 3. UPDATE: Modificar en memoria
    product.setBalance(newBalance);
    
    // 4. SERIALIZE: Serializar objeto completo a JSON
    String updatedJson = objectMapper.writeValueAsString(product);
    
    // 5. SET: Enviar producto completo de vuelta a Redis (~2KB)
    redissonClient.getBucket("key").set(updatedJson);
}

// ✅ Con RedisJSON (óptimo)
public void updateBalance(String sessionId, String productId, Balance newBalance) {
    // 1 operación atómica: actualiza solo el path específico (~50 bytes)
    redissonClient.getJsonBucket("key")
        .set("$.balance", newBalance);
}
```

**Impacto con 500 productos:**

| Operación | Sin RedisJSON | Con RedisJSON | Diferencia |
|-----------|---------------|---------------|------------|
| Comandos Redis | 2 (GET + SET) | 1 (JSON.SET path) | **50% menos** |
| Red por producto | ~4KB (fetch + send completo) | ~50 bytes (solo balance) | **98% menos** |
| Latencia | 15-25ms (parse/serialize) | 1-3ms (atómico) | **8x más rápido** |
| Tráfico total (500 prod) | ~2MB por actualización | ~25KB | **99% menos** |

[15][16][14]

### Sin Atomicidad Real

Sin RedisJSON, las actualizaciones tienen **race conditions**:[14]

```java
// ❌ Problema sin RedisJSON
Thread A: GET product  → balance.saldo = 1000
Thread B: GET product  → balance.saldo = 1000
Thread A: SET balance.saldo = 1500  (actualiza)
Thread B: SET balance.saldo = 1200  (sobreescribe A - dato perdido!)

// ✅ Con RedisJSON - operación atómica del servidor
JSON.SET key $.balance.saldo 1500  (atómico en Redis)
```

Para resolver esto sin RedisJSON necesitas:
- Lua scripts complejos[17]
- Locks distribuidos (overhead adicional)[17]
- Versioning optimista (más complejidad)

### No Puedes Consultar Estructuras Anidadas

Sin RedisJSON no puedes hacer queries eficientes:[18][19]

```bash
# ❌ Sin RedisJSON
# Para buscar productos con balance.saldo > 5000:
# Debes traer TODOS los productos y filtrar en aplicación (ineficiente)

# ✅ Con RedisJSON + RediSearch
FT.SEARCH products "@balance.saldo:[5000 +inf]"
```

### Complejidad de Código Mayor

**Sin RedisJSON necesitas:**
- Manejo manual de serialización/deserialización[20][21]
- Lógica de merge para actualizaciones parciales
- Gestión de race conditions
- Mayor testing de concurrencia

**Con RedisJSON:**
- Redis maneja todo internamente[14]
- API declarativa simple
- Atomicidad garantizada

## Alternativas Profesionales Sin RedisJSON

### Opción 1: Hash Plano Desnormalizado (Recomendada)

Mejor alternativa sin RedisJSON para tu caso:[21][22][20]

```java
@ApplicationScoped
public class ProductHashRepository {

    @Inject
    RedissonClient redissonClient;
    
    // Estructura: session:<sessionId>:product:<productId>
    private RMap<String, String> getProductHash(String sessionId, String productId) {
        String key = String.format("session:%s:product:%s", sessionId, productId);
        return redissonClient.getMap(key);
    }
    
    // ✅ Actualizar SOLO balance.saldo - O(1) atómico
    public void updateBalanceSaldo(String sessionId, String productId, 
                                    BigDecimal newSaldo) {
        RMap<String, String> hash = getProductHash(sessionId, productId);
        // Solo actualiza este campo - no trae todo el hash
        hash.fastPut("balance.saldo", newSaldo.toString());
    }
    
    // ✅ Actualizar múltiples campos del balance - 1 operación
    public void updateBalance(String sessionId, String productId, Balance balance) {
        RMap<String, String> hash = getProductHash(sessionId, productId);
        
        Map<String, String> balanceFields = Map.of(
            "balance.saldo", balance.getSaldo().toString(),
            "balance.contable", balance.getContable().toString(),
            "balance.credilaine", balance.getCredilaine().toString()
        );
        
        // Batch update - solo estos 3 campos
        hash.putAll(balanceFields);
    }
    
    // ✅ Obtener SOLO balance - sin traer nombre/estado
    public Optional<Balance> getBalance(String sessionId, String productId) {
        RMap<String, String> hash = getProductHash(sessionId, productId);
        
        // Solo recupera los 3 campos necesarios
        Map<String, String> fields = hash.getAll(
            Set.of("balance.saldo", "balance.contable", "balance.credilaine")
        );
        
        if (fields.isEmpty()) return Optional.empty();
        
        return Optional.of(new Balance(
            new BigDecimal(fields.get("balance.saldo")),
            new BigDecimal(fields.get("balance.contable")),
            new BigDecimal(fields.get("balance.credilaine"))
        ));
    }
    
    // Guardar producto completo
    public void saveProduct(String sessionId, Product product) {
        RMap<String, String> hash = getProductHash(sessionId, product.getProductId());
        
        Map<String, String> flatMap = new HashMap<>();
        flatMap.put("productId", product.getProductId());
        flatMap.put("nombre", product.getNombre());
        flatMap.put("estado.codigo", product.getEstado().getCodigo());
        flatMap.put("estado.descripcion", product.getEstado().getDescripcion());
        flatMap.put("balance.saldo", product.getBalance().getSaldo().toString());
        flatMap.put("balance.contable", product.getBalance().getContable().toString());
        flatMap.put("balance.credilaine", product.getBalance().getCredilaine().toString());
        
        hash.putAll(flatMap);
    }
}
```

**Ventajas Hash Plano:**
- ✅ Actualizaciones O(1) atómicas de campos individuales[23][24]
- ✅ Sin fetch completo para updates
- ✅ 100% compatible con Redis básico (cualquier versión)
- ✅ Funciona en ElastiCache/Azure Cache sin problemas[6]
- ✅ Ahorro de memoria con ziplist optimization (~50% menos)[25][26]

**Desventajas:**
- ❌ Pierdes estructura anidada visual
- ❌ Campos planos con notación punto (`balance.saldo`)
- ❌ Sin queries JSONPath complejas

### Opción 2: Objetos Separados

Almacenar balance como entidad independiente:

```java
// Clave para balance: session:<sessionId>:balance:<productId>
// Clave para producto: session:<sessionId>:product:<productId>

public void updateBalance(String sessionId, String productId, Balance balance) {
    String balanceKey = String.format("session:%s:balance:%s", sessionId, productId);
    redissonClient.getBucket(balanceKey, new JsonJacksonCodec(Balance.class))
        .set(balance);
}
```

**Ventajas:**
- ✅ Balance aislado (máxima eficiencia de updates)
- ✅ Mantiene tipos complejos

**Desventajas:**
- ❌ Mayor complejidad operacional (múltiples claves)
- ❌ Sin transacciones multi-key en cluster mode

### Opción 3: String JSON + Versionado Optimista

```java
@Data
public class VersionedProduct {
    private long version;
    private Product product;
}

public boolean updateBalance(String sessionId, String productId, Balance newBalance) {
    RBucket<VersionedProduct> bucket = redissonClient.getBucket(key, 
        new JsonJacksonCodec(VersionedProduct.class));
    
    // Retry optimista
    for (int attempt = 0; attempt < 3; attempt++) {
        VersionedProduct current = bucket.get();
        if (current == null) return false;
        
        current.getProduct().setBalance(newBalance);
        current.setVersion(current.getVersion() + 1);
        
        // CAS (Compare-And-Set)
        if (bucket.compareAndSet(current, current)) {
            return true;
        }
    }
    return false;
}
```

**Desventajas:**
- ❌ Requiere fetch completo
- ❌ Más complejo
- ❌ Puede requerir reintentos

## Comparativa Final

| Característica | RedisJSON | Hash Plano | String JSON |
|---------------|-----------|------------|-------------|
| **Instalación** | Redis Stack/8.0 | Redis básico | Redis básico |
| **Cloud managed** | ⚠️ Limitado | ✅ Universal | ✅ Universal |
| **Update parcial** | ✅ Nativo (O(1)) | ✅ Campo a campo (O(1)) | ❌ Fetch completo |
| **Atomicidad** | ✅ Garantizada | ✅ Por campo | ⚠️ Requiere locks |
| **Red por update** | ~50 bytes | ~50 bytes | ~2-4KB |
| **Anidación** | ✅ Nativa | ⚠️ Plano | ✅ Nativa |
| **Queries complejas** | ✅ JSONPath | ❌ No | ❌ No |
| **Complejidad código** | ⭐ Baja | ⭐⭐ Media | ⭐⭐⭐ Alta |

[16][22][27][28][20]

## Recomendación para Tu Caso

**Si puedes instalar Redis Stack/8.0:** Usa RedisJSON. Es la solución profesional ideal para cientos de productos con estructuras anidadas.[1][2]

**Si usas cloud managed (ElastiCache, etc.) sin RedisJSON:** Usa **Hash Plano desnormalizado**. Proporciona el 90% de los beneficios de RedisJSON con 100% de compatibilidad, operaciones O(1) atómicas, y es production-ready sin dependencias externas.[24][20][21][23]

[1](https://github.com/RedisJSON/RedisJSON)
[2](https://redis-stack.io/docs/data-types/json/)
[3](https://redisson.pro/blog/advanced-redis-integration-with-quarkus.html)
[4](https://stackoverflow.com/questions/71280769/quarkus-redis-client-how-to-insert-json)
[5](https://redis.io/about/about-stack/)
[6](https://stackoverflow.com/questions/41547073/aws-elasticache-is-it-possible-redis-compatible-elasticache-to-store-json-file)
[7](https://redis.io/technology/advantages/)
[8](https://redis.io/blog/agplv3/)
[9](https://www.infoq.com/news/2025/05/redis-agpl-license/)
[10](https://gigazine.net/gsc_news/en/20250502-redis-returns-to-open-source/)
[11](https://redis.io/compare/open-source/)
[12](https://redis.io/legal/licenses/)
[13](https://redisson.pro/blog/how-to-store-json-with-redis-on-java.html)
[14](https://redis.io/glossary/json-storage/)
[15](https://www.c-sharpcorner.com/article/redis-redisstring-and-redisjson/)
[16](https://redis.io/docs/latest/develop/data-types/json/performance/)
[17](https://www.alibabacloud.com/blog/implementation-principles-and-best-practices-of-distributed-lock_600811)
[18](https://redis.io/blog/index-and-query-json-docs-with-redis/)
[19](https://redis.io/docs/latest/develop/data-types/json/path/)
[20](https://dev.to/iamrj846/which-is-better-for-efficiency-redis-strings-vs-redis-hashes-to-represent-json-2ge5)
[21](https://stackoverflow.com/questions/16375188/redis-strings-vs-redis-hashes-to-represent-json-efficiency)
[22](https://openillumi.com/en/en-redis-json-storage-string-hash-compare/)
[23](https://redis.io/docs/latest/develop/data-types/hashes/)
[24](https://www.geeksforgeeks.org/system-design/a-complete-guide-to-redis-hashes/)
[25](https://engineering.salesforce.com/using-redis-hash-instead-of-set-to-reduce-cache-size-and-operating-costs-2a1f7b847ded/)
[26](https://stackoverflow.com/questions/62974363/performance-comparison-of-using-redis-hashes-vs-many-keys)
[27](https://redis.io/docs/latest/develop/data-types/json/use_cases/)
[28](https://redis.io/docs/latest/develop/ai/redisvl/user_guide/hash_vs_json/)
[29](https://www.reddit.com/r/linux/comments/1kcdc2j/redis_is_open_source_again/)
[30](https://stackoverflow.com/questions/74619797/installing-redis-json-on-macosx-with-brew)
[31](https://redis.io/about/redis-stack/)
[32](https://dev.to/redis/getting-started-with-redis-stack-3eb9)
[33](https://redis.io/learn/migration/elasticache-to-redis-cloud)
[34](https://www.reddit.com/r/redis/comments/1d8s80b/does_redis_and_redis_stack_share_the_same/)
[35](https://www.reddit.com/r/node/comments/1nhvhcb/looking_for_a_json_cache_store_like_redis_but_not/)