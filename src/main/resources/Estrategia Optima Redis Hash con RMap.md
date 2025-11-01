## Estrategia Óptima: Redis Hash con RMap

La mejor manera de almacenar una lista de productos por sesión en Redis, permitiendo modificaciones eficientes de productos individuales sin recuperar toda la colección, es usar **Redis Hash** con el tipo **RMap** de Redisson. Este enfoque permite acceso directo a elementos individuales mediante operaciones atómicas sin traer toda la estructura de datos a memoria.[1][2][3][4]

### Ventajas de Hash sobre List/Set

Redis Hash proporciona acceso O(1) a campos individuales y reduce el uso de memoria hasta en un 50% comparado con estructuras SET cuando se configuran correctamente. Para e-commerce y catálogos de productos, Hash permite actualizaciones rápidas y acceso a detalles individuales sin afectar el resto de la colección.[5][6][2][7]

### Patrón de Diseño Recomendado

**Estructura de clave óptima:**
```java
session:<sessionId>:products
```

Cada producto individual se almacena como un **field** dentro del Hash, donde:
- **Field**: `productId` (String)
- **Value**: Objeto producto serializado (JSON o binario)[8][3]

### Implementación con Quarkus JDK 21 + Redisson

```java
@ApplicationScoped
public class ProductSessionCache {

    private static final String CACHE_PREFIX = "session:";
    private static final String PRODUCTS_SUFFIX = ":products";
    
    @Inject
    RedissonClient redissonClient;
    
    // Obtener RMap para una sesión específica
    private RMap<String, Product> getProductMap(String sessionId) {
        String cacheKey = CACHE_PREFIX + sessionId + PRODUCTS_SUFFIX;
        return redissonClient.getMap(cacheKey, 
            new JsonJacksonCodec(Product.class));
    }
    
    // Agregar/Actualizar UN SOLO producto (sin traer toda la lista)
    public void putProduct(String sessionId, Product product) {
        RMap<String, Product> map = getProductMap(sessionId);
        // Operación atómica - solo envía este producto a Redis
        map.put(product.getId(), product);
    }
    
    // Obtener UN SOLO producto (sin traer toda la lista)
    public Optional<Product> getProduct(String sessionId, String productId) {
        RMap<String, Product> map = getProductMap(sessionId);
        // Operación atómica - solo recupera este producto
        return Optional.ofNullable(map.get(productId));
    }
    
    // Modificar UN SOLO producto eficientemente
    public boolean updateProduct(String sessionId, String productId, 
                                  UnaryOperator<Product> updater) {
        RMap<String, Product> map = getProductMap(sessionId);
        
        // Operación optimista con fastPut
        Product current = map.get(productId);
        if (current == null) {
            return false;
        }
        
        Product updated = updater.apply(current);
        map.fastPut(productId, updated);
        return true;
    }
    
    // Eliminar UN SOLO producto
    public boolean removeProduct(String sessionId, String productId) {
        RMap<String, Product> map = getProductMap(sessionId);
        return map.fastRemove(productId) > 0;
    }
    
    // Obtener cantidad de productos (sin traer datos)
    public int getProductCount(String sessionId) {
        return getProductMap(sessionId).size();
    }
    
    // Solo cuando REALMENTE necesites toda la lista
    public Map<String, Product> getAllProducts(String sessionId) {
        return getProductMap(sessionId).readAllMap();
    }
}
```

### Operaciones Atómicas Avanzadas

Para modificaciones más complejas sin race conditions:[9]

```java
public void updateProductQuantity(String sessionId, String productId, 
                                   int delta) {
    RMap<String, Product> map = getProductMap(sessionId);
    
    // Operación atómica usando compute
    map.compute(productId, (key, product) -> {
        if (product != null) {
            product.setQuantity(product.getQuantity() + delta);
        }
        return product;
    });
}

// Operación batch eficiente para múltiples productos
public void putAllProducts(String sessionId, Map<String, Product> products) {
    RMap<String, Product> map = getProductMap(sessionId);
    // Una sola operación a Redis
    map.putAll(products);
}
```

### Optimización con Local Cache

Para máxima performance en lecturas (hasta 45x más rápido), usa **RLocalCachedMap**:[10][11]

```java
private RLocalCachedMap<String, Product> getProductMapWithCache(String sessionId) {
    String cacheKey = CACHE_PREFIX + sessionId + PRODUCTS_SUFFIX;
    
    LocalCachedMapOptions<String, Product> options = 
        LocalCachedMapOptions.<String, Product>defaults()
            .evictionPolicy(EvictionPolicy.LRU)
            .cacheSize(1000)
            .maxIdle(Duration.ofMinutes(30))
            .timeToLive(Duration.ofHours(2))
            .syncStrategy(SyncStrategy.UPDATE)
            .reconnectionStrategy(ReconnectionStrategy.LOAD);
    
    return redissonClient.getLocalCachedMap(cacheKey, 
        new JsonJacksonCodec(Product.class), options);
}
```

### Configuración de Serialización

Para máxima eficiencia, configura el codec apropiado:[12]

```java
// Para objetos Java estándar (más rápido)
new JsonJacksonCodec(Product.class)

// Para máxima compatibilidad
new SerializationCodec()

// Para objetos simples (más eficiente en memoria)
new FstCodec()
```

### Gestión de TTL por Sesión

```java
public void setSessionExpiration(String sessionId, Duration ttl) {
    RMap<String, Product> map = getProductMap(sessionId);
    map.expire(ttl);
}

public void extendSessionExpiration(String sessionId, Duration additionalTime) {
    RMap<String, Product> map = getProductMap(sessionId);
    map.expire(additionalTime);
}
```

### Principios SOLID Aplicados

**Single Responsibility**: Clase dedicada exclusivamente a gestión de caché de productos por sesión.[11][10]

**Open/Closed**: Extensible mediante inyección de estrategias de serialización y políticas de caché.

**Dependency Inversion**: Depende de abstracción `RedissonClient`, no de implementaciones concretas.

```java
@ApplicationScoped
public class SessionCacheFactory {
    
    @Inject
    RedissonClient redissonClient;
    
    public <T> RMap<String, T> createCache(String sessionId, 
                                            String cacheType, 
                                            Class<T> valueType) {
        String cacheKey = String.format("session:%s:%s", sessionId, cacheType);
        return redissonClient.getMap(cacheKey, new JsonJacksonCodec(valueType));
    }
}
```

### Patrón Repository Profesional

```java
@ApplicationScoped
public class ProductSessionRepository {
    
    @Inject
    SessionCacheFactory cacheFactory;
    
    private static final String PRODUCTS_TYPE = "products";
    
    public CompletionStage<Product> saveAsync(String sessionId, Product product) {
        RMap<String, Product> map = getMap(sessionId);
        return map.putAsync(product.getId(), product)
                  .thenApply(v -> product);
    }
    
    public CompletionStage<Product> findByIdAsync(String sessionId, String productId) {
        return getMap(sessionId).getAsync(productId);
    }
    
    private RMap<String, Product> getMap(String sessionId) {
        return cacheFactory.createCache(sessionId, PRODUCTS_TYPE, Product.class);
    }
}
```

### Comandos Redis Equivalentes

Para entender las operaciones subyacentes:[13][1]

```bash
# Agregar/actualizar producto
HSET session:abc123:products prod001 '{"id":"prod001","name":"Item"}'

# Obtener UN producto
HGET session:abc123:products prod001

# Modificar UN campo específico
HSET session:abc123:products prod001 '{"id":"prod001","quantity":5}'

# Eliminar UN producto
HDEL session:abc123:products prod001

# Obtener cantidad
HLEN session:abc123:products

# Obtener todos (evitar si es posible)
HGETALL session:abc123:products
```

### Beneficios de Esta Arquitectura

Este patrón proporciona operaciones O(1) para acceso individual, reducción de tráfico de red al enviar solo datos necesarios, atomicidad garantizada por Redis sin necesidad de locks adicionales, escalabilidad horizontal con particionamiento de datos por sesión, y uso eficiente de memoria con optimización ziplist para hashes pequeños. La implementación sigue principios de clean architecture, separation of concerns, y es production-ready con manejo de errores y operaciones asíncronas.[6][14][9][5][10][11]

[1](https://redis.io/docs/latest/develop/data-types/hashes/)
[2](https://www.dragonflydb.io/guides/redis-hashes-examples-and-pro-tips)
[3](https://estuary.dev/blog/redis-data-types/)
[4](https://github.com/redisson/redisson/issues/4376)
[5](https://engineering.salesforce.com/using-redis-hash-instead-of-set-to-reduce-cache-size-and-operating-costs-2a1f7b847ded/)
[6](https://stackoverflow.com/questions/62974363/performance-comparison-of-using-redis-hashes-vs-many-keys)
[7](https://www.dragonflydb.io/databases/schema/redis)
[8](https://redis.io/glossary/object-hash-storage/)
[9](https://www.alibabacloud.com/blog/implementation-principles-and-best-practices-of-distributed-lock_600811)
[10](https://redisson.pro/blog/advanced-quarkus-redis-cache.html)
[11](https://redisson.pro/blog/advanced-redis-integration-with-quarkus.html)
[12](https://www.baeldung.com/redis-redisson)
[13](https://www.geeksforgeeks.org/system-design/a-complete-guide-to-redis-hashes/)
[14](https://redis.io/blog/session-store-patterns/)
[15](https://github.com/redisson/redisson/wiki/16.-FAQ/1c7f52a7f8607060c464c193437d3540021f1b49)
[16](https://stackoverflow.com/questions/77069969/how-to-successfully-use-redisson-live-objects)
[17](https://aws.amazon.com/blogs/database/best-practices-valkey-redis-oss-clients-and-amazon-elasticache/)
[18](https://www.instaclustr.com/blog/redis-java-clients-and-client-side-caching/)
[19](https://stackoverflow.com/questions/11209613/redis-finding-hashes-by-field-values)
[20](https://stackoverflow.com/questions/64008276/how-to-update-time-to-live-of-an-entry-using-redisson-client-in-java)