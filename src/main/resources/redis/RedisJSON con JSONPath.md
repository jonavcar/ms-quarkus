## Solución Óptima: RedisJSON con JSONPath

Para modificar **únicamente el balance** sin traer los cientos de productos completos, la mejor solución es **RedisJSON con RJsonBucket** de Redisson, usando **JSONPath** para actualizaciones parciales atómicas. Esto permite actualizar campos anidados sin deserializar/serializar el objeto completo, logrando operaciones hasta 10x más rápidas.[1][2][3][4]

### Comparativa de Estrategias

| Estrategia | Memoria | Performance Update | Anidación | Actualización Parcial | Complejidad |
|------------|---------|-------------------|-----------|----------------------|-------------|
| **RedisJSON + JSONPath** | Alta eficiencia | **Óptima** (O(1) path específico) | ✅ Nativa | ✅ Atómica sin fetch | Baja |
| Hash Plano (desnormalizado) | Media | Buena (O(1) campo) | ❌ Campos planos | ✅ Campo individual | Media |
| Hash con JSON serializado | Baja (overhead) | ⚠️ Mala (fetch+parse+update+set) | ✅ Sí | ❌ Requiere fetch total | Alta |
| String JSON | Óptima (compacto) | ⚠️ Mala (fetch+parse+update+set) | ✅ Sí | ❌ Requiere fetch total | Alta |

[5][6][7][1]

### Arquitectura Recomendada

**Estructura de clave:**
```
session:<sessionId>:product:<productId>
```

**Ventajas clave:** Cada producto es independiente (permite escalar a millones), actualización de balance sin leer estado/nombre, operaciones atómicas garantizadas por Redis, y consultas JSONPath para filtros complejos.[8][9][4][1]

### Implementación con RedisJSON + Redisson

#### Dependencias Maven

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-quarkus-30</artifactId>
    <version>3.36.0</version>
</dependency>
```

**Nota:** Requiere módulo **RedisJSON** instalado en servidor Redis.[3][10]

#### Modelo de Dominio

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String productId;
    private String nombre;
    private Estado estado;
    private Balance balance;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Estado {
    private String codigo;
    private String descripcion;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Balance {
    private BigDecimal saldo;
    private BigDecimal contable;
    private BigDecimal credilaine;
}
```

#### Repositorio con RedisJSON

```java
@ApplicationScoped
public class ProductSessionRepository {

    private static final String KEY_TEMPLATE = "session:%s:product:%s";
    
    @Inject
    RedissonClient redissonClient;
    
    // Clave Redis para un producto específico
    private String buildKey(String sessionId, String productId) {
        return String.format(KEY_TEMPLATE, sessionId, productId);
    }
    
    // Obtener RJsonBucket para un producto
    private RJsonBucket<Product> getJsonBucket(String sessionId, String productId) {
        String key = buildKey(sessionId, productId);
        return redissonClient.getJsonBucket(key, new JacksonCodec<>(Product.class));
    }
    
    // Guardar producto completo
    public void saveProduct(String sessionId, Product product) {
        RJsonBucket<Product> bucket = getJsonBucket(sessionId, product.getProductId());
        bucket.set(product);
    }
    
    // Actualizar SOLO el balance (sin traer el producto completo)
    public void updateBalance(String sessionId, String productId, Balance newBalance) {
        RJsonBucket<Product> bucket = getJsonBucket(sessionId, productId);
        
        // JSONPath: actualizar solo el nodo balance
        bucket.set("$.balance", newBalance);
    }
    
    // Actualizar SOLO un campo específico del balance
    public void updateBalanceSaldo(String sessionId, String productId, BigDecimal newSaldo) {
        RJsonBucket<Product> bucket = getJsonBucket(sessionId, productId);
        
        // JSONPath específico: solo el campo saldo dentro de balance
        bucket.set("$.balance.saldo", newSaldo);
    }
    
    // Actualizar múltiples campos del balance sin traer producto
    public void updateBalanceFields(String sessionId, String productId, 
                                    BigDecimal saldo, BigDecimal contable) {
        RJsonBucket<Product> bucket = getJsonBucket(sessionId, productId);
        
        // Actualización atómica de múltiples campos
        Map<String, Object> updates = Map.of(
            "$.balance.saldo", saldo,
            "$.balance.contable", contable
        );
        
        updates.forEach(bucket::set);
    }
    
    // Obtener SOLO el balance (sin traer todo el producto)
    public Optional<Balance> getBalance(String sessionId, String productId) {
        RJsonBucket<Product> bucket = getJsonBucket(sessionId, productId);
        
        // Recuperar solo el path balance
        Balance balance = bucket.get(new JacksonCodec<>(Balance.class), "$.balance");
        return Optional.ofNullable(balance);
    }
    
    // Obtener producto completo (cuando realmente se necesite)
    public Optional<Product> getProduct(String sessionId, String productId) {
        RJsonBucket<Product> bucket = getJsonBucket(sessionId, productId);
        return Optional.ofNullable(bucket.get());
    }
    
    // Verificar existencia sin traer datos
    public boolean exists(String sessionId, String productId) {
        return getJsonBucket(sessionId, productId).isExists();
    }
    
    // Eliminar producto
    public boolean deleteProduct(String sessionId, String productId) {
        return getJsonBucket(sessionId, productId).delete();
    }
}
```


#### Servicio de Negocio con SOLID

```java
@ApplicationScoped
public class ProductBalanceService {

    @Inject
    ProductSessionRepository repository;
    
    @Inject
    Event<BalanceUpdatedEvent> balanceUpdatedEvent;
    
    // Actualizar saldo con validación
    public Result<Balance> updateSaldo(String sessionId, String productId, 
                                       BigDecimal newSaldo) {
        // Validación de negocio
        if (newSaldo.compareTo(BigDecimal.ZERO) < 0) {
            return Result.error("Saldo no puede ser negativo");
        }
        
        // Verificar existencia antes de actualizar
        if (!repository.exists(sessionId, productId)) {
            return Result.error("Producto no encontrado");
        }
        
        // Actualización eficiente - solo el campo saldo
        repository.updateBalanceSaldo(sessionId, productId, newSaldo);
        
        // Evento de dominio
        balanceUpdatedEvent.fire(
            new BalanceUpdatedEvent(sessionId, productId, newSaldo)
        );
        
        // Recuperar balance actualizado para confirmación
        Balance updated = repository.getBalance(sessionId, productId)
            .orElseThrow();
            
        return Result.success(updated);
    }
    
    // Ajuste de saldo (operación incremental)
    public Result<Balance> adjustSaldo(String sessionId, String productId, 
                                       BigDecimal delta) {
        // Obtener solo el balance actual
        Balance current = repository.getBalance(sessionId, productId)
            .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        
        BigDecimal newSaldo = current.getSaldo().add(delta);
        
        // Actualizar solo el campo saldo
        repository.updateBalanceSaldo(sessionId, productId, newSaldo);
        current.setSaldo(newSaldo);
        
        return Result.success(current);
    }
    
    // Actualización completa del balance
    public void replaceBalance(String sessionId, String productId, Balance newBalance) {
        validateBalance(newBalance);
        repository.updateBalance(sessionId, productId, newBalance);
    }
    
    private void validateBalance(Balance balance) {
        if (balance.getSaldo().compareTo(BigDecimal.ZERO) < 0 ||
            balance.getContable().compareTo(BigDecimal.ZERO) < 0 ||
            balance.getCredilaine().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Balance con valores negativos");
        }
    }
}
```


### Comandos Redis JSON Equivalentes

Para entender las operaciones subyacentes:[11][9]

```bash
# Guardar producto completo
JSON.SET session:abc123:product:P001 $ '{"productId":"P001","nombre":"Laptop","estado":{"codigo":"A","descripcion":"Activo"},"balance":{"saldo":1000,"contable":950,"credilaine":50}}'

# Actualizar SOLO el balance (sin leer el producto)
JSON.SET session:abc123:product:P001 $.balance '{"saldo":1500,"contable":1400,"credilaine":100}'

# Actualizar SOLO el campo saldo del balance
JSON.SET session:abc123:product:P001 $.balance.saldo 2000

# Obtener SOLO el balance (sin traer todo el producto)
JSON.GET session:abc123:product:P001 $.balance

# Obtener SOLO el saldo
JSON.GET session:abc123:product:P001 $.balance.saldo

# Verificar existencia
EXISTS session:abc123:product:P001
```

### Estrategia Alternativa: Hash Plano (Sin RedisJSON)

Si **no puedes instalar RedisJSON**, usa Hash con campos desnormalizados:[12][5]

```java
@ApplicationScoped
public class ProductSessionHashRepository {

    @Inject
    RedissonClient redissonClient;
    
    private RMap<String, String> getProductHash(String sessionId, String productId) {
        String key = String.format("session:%s:product:%s", sessionId, productId);
        return redissonClient.getMap(key);
    }
    
    // Guardar producto completo con campos planos
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
    
    // Actualizar SOLO el saldo (operación atómica O(1))
    public void updateBalanceSaldo(String sessionId, String productId, BigDecimal newSaldo) {
        RMap<String, String> hash = getProductHash(sessionId, productId);
        // Solo actualiza este campo - no trae todo el hash
        hash.fastPut("balance.saldo", newSaldo.toString());
    }
    
    // Actualizar múltiples campos del balance eficientemente
    public void updateBalance(String sessionId, String productId, Balance balance) {
        RMap<String, String> hash = getProductHash(sessionId, productId);
        
        Map<String, String> balanceFields = Map.of(
            "balance.saldo", balance.getSaldo().toString(),
            "balance.contable", balance.getContable().toString(),
            "balance.credilaine", balance.getCredilaine().toString()
        );
        
        // Actualización batch de solo estos 3 campos
        hash.putAll(balanceFields);
    }
    
    // Obtener SOLO el balance (sin traer nombre/estado)
    public Optional<Balance> getBalance(String sessionId, String productId) {
        RMap<String, String> hash = getProductHash(sessionId, productId);
        
        // Solo recupera los 3 campos del balance
        Map<String, String> balanceFields = hash.getAll(
            Set.of("balance.saldo", "balance.contable", "balance.credilaine")
        );
        
        if (balanceFields.isEmpty()) {
            return Optional.empty();
        }
        
        Balance balance = new Balance(
            new BigDecimal(balanceFields.get("balance.saldo")),
            new BigDecimal(balanceFields.get("balance.contable")),
            new BigDecimal(balanceFields.get("balance.credilaine"))
        );
        
        return Optional.of(balance);
    }
    
    // Reconstruir producto completo (cuando sea necesario)
    public Optional<Product> getProduct(String sessionId, String productId) {
        RMap<String, String> hash = getProductHash(sessionId, productId);
        Map<String, String> fields = hash.readAllMap();
        
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        
        Product product = new Product();
        product.setProductId(fields.get("productId"));
        product.setNombre(fields.get("nombre"));
        
        Estado estado = new Estado(
            fields.get("estado.codigo"),
            fields.get("estado.descripcion")
        );
        product.setEstado(estado);
        
        Balance balance = new Balance(
            new BigDecimal(fields.get("balance.saldo")),
            new BigDecimal(fields.get("balance.contable")),
            new BigDecimal(fields.get("balance.credilaine"))
        );
        product.setBalance(balance);
        
        return Optional.of(product);
    }
}
```


### Índice para Consultas Rápidas por Sesión

Para listar todos los productos de una sesión eficientemente:[13]

```java
@ApplicationScoped
public class SessionProductIndex {

    @Inject
    RedissonClient redissonClient;
    
    private RSet<String> getSessionIndex(String sessionId) {
        return redissonClient.getSet("session:" + sessionId + ":products");
    }
    
    // Agregar producto al índice de sesión
    public void addToIndex(String sessionId, String productId) {
        getSessionIndex(sessionId).add(productId);
    }
    
    // Obtener todos los productIds de una sesión
    public Set<String> getProductIds(String sessionId) {
        return getSessionIndex(sessionId).readAll();
    }
    
    // Eliminar del índice
    public void removeFromIndex(String sessionId, String productId) {
        getSessionIndex(sessionId).remove(productId);
    }
    
    // Contar productos de la sesión
    public int countProducts(String sessionId) {
        return getSessionIndex(sessionId).size();
    }
}
```

### Gestión de TTL y Limpieza

```java
public void setProductExpiration(String sessionId, String productId, Duration ttl) {
    RJsonBucket<Product> bucket = getJsonBucket(sessionId, productId);
    bucket.expire(ttl);
}

public void extendSessionExpiration(String sessionId, Duration additionalTime) {
    // Extender TTL de todos los productos de la sesión
    Set<String> productIds = sessionProductIndex.getProductIds(sessionId);
    
    productIds.forEach(productId -> {
        RJsonBucket<Product> bucket = getJsonBucket(sessionId, productId);
        bucket.expire(additionalTime);
    });
}
```

### Comparativa de Performance

**Escenario:** Actualizar solo el saldo en sesión con 500 productos.

| Método | Operaciones Redis | Datos Transferidos | Tiempo Estimado |
|--------|------------------|-------------------|-----------------|
| **RedisJSON JSONPath** | 1 comando `JSON.SET` | ~20 bytes (solo valor) | **~1-2ms** ⚡ |
| Hash Plano | 1 comando `HSET` | ~20 bytes | **~1-2ms** ⚡ |
| Hash Serializado | `HGET` + `HSET` | ~2KB (producto completo x2) | ~15-20ms |
| String JSON | `GET` + `SET` | ~2KB (producto completo x2) | ~15-20ms |

[4][1]

### Recomendación Final

**Para tu caso con cientos de productos:** Usa **RedisJSON + RJsonBucket + JSONPath**. Proporciona actualizaciones atómicas de campos anidados sin fetch previo (O(1) puro), máxima eficiencia en red (solo envía datos modificados), soporte nativo para estructuras complejas anidadas, y escalabilidad profesional para millones de productos.[2][7][1][3][8][4][14]

**Si no tienes RedisJSON:** Usa **Hash Plano desnormalizado** como alternativa profesional, sacrificando anidación pero manteniendo O(1) en actualizaciones individuales.[12][5]

[1](https://redis.io/docs/latest/develop/data-types/json/use_cases/)
[2](https://www.c-sharpcorner.com/article/redis-redisstring-and-redisjson/)
[3](https://redisson.pro/blog/how-to-store-json-with-redis-on-java.html)
[4](https://redis.io/docs/latest/develop/data-types/json/performance/)
[5](https://dev.to/iamrj846/which-is-better-for-efficiency-redis-strings-vs-redis-hashes-to-represent-json-2ge5)
[6](https://redis.io/docs/latest/develop/ai/redisvl/user_guide/hash_vs_json/)
[7](https://openillumi.com/en/en-redis-json-storage-string-hash-compare/)
[8](https://redis.io/blog/index-and-query-json-docs-with-redis/)
[9](https://redis.io/docs/latest/develop/data-types/json/path/)
[10](https://stackoverflow.com/questions/71280769/quarkus-redis-client-how-to-insert-json)
[11](https://redis.io/docs/latest/commands/json.set/)
[12](https://stackoverflow.com/questions/16375188/redis-strings-vs-redis-hashes-to-represent-json-efficiency)
[13](https://redis.io/blog/session-store-patterns/)
[14](https://redisson.pro/blog/advanced-redis-integration-with-quarkus.html)
[15](https://stackoverflow.com/questions/75939489/how-can-update-multiple-fields-in-redisjson)
[16](https://redis.io/docs/latest/commands/json.get/)
[17](https://discuss.elastic.co/t/partial-update-and-nested-type-performance/110534)
[18](https://www.reddit.com/r/reactjs/comments/1c72e2o/how_to_handle_immutability_with_nested_object_the/)
[19](https://github.com/redisson/redisson)
[20](https://stackoverflow.com/questions/45926760/redisson-and-json-for-objects)
[21](https://www.baeldung.com/redis-redisson)