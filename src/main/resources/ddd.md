# Código Completo Corregido

Aquí está el repositorio **production-ready con el codec correcto** que deserializa a `List<Product>` y guarda JSON limpio sin `@class`:[3][6]

```java
package com.company.repository;

import com.company.domain.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository optimizado para gestión de productos por sesión en Redis.
 * Configurado para volúmenes de 1-100 productos por sesión.
 * 
 * JDK 21 Imperative | Production-Ready | Advanced Patterns
 * Codec: TypedJsonJacksonCodec - JSON limpio sin @class, deserialización automática
 */
@ApplicationScoped
public class ProductSessionRepository {

    private static final String SESSION_KEY_TEMPLATE = "session:%s:products";
    private static final int MAX_BATCH_SIZE = 100;
    private static final int MIN_SESSION_ID_LENGTH = 3;
    
    // ✅ CODEC CORRECTO: TypedJsonJacksonCodec sin CompositeCodec
    // String.class = tipo de KEY, Product.class = tipo de VALUE
    // Resultado: JSON limpio sin @class, deserialización a List<Product>
    private static final TypedJsonJacksonCodec CODEC = 
        new TypedJsonJacksonCodec(String.class, Product.class);

    @Inject
    RedissonClient redissonClient;

    @ConfigProperty(name = "app.redis.session.ttl-minutes", defaultValue = "30")
    int sessionTtlMinutes;

    /**
     * Obtiene el RMap de Redis con codec optimizado.
     * Reutiliza la misma instancia para la misma clave.
     */
    private RMap<String, Product> getSessionProductsMap(String sessionId) {
        return redissonClient.getMap(buildSessionKey(sessionId), CODEC);
    }

    private String buildSessionKey(String sessionId) {
        return String.format(SESSION_KEY_TEMPLATE, sessionId);
    }

    private void applyTTL(RMap<String, Product> map) {
        try {
            map.expire(Duration.ofMinutes(sessionTtlMinutes));
        } catch (Exception e) {
            Log.errorf(e, "Error al aplicar TTL a sesión");
            throw new RepositoryOperationException("Fallo aplicar TTL", e);
        }
    }

    // ==================== VALIDACIONES CONSOLIDADAS ====================

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.length() < MIN_SESSION_ID_LENGTH) {
            throw new IllegalArgumentException(
                String.format("sessionId inválido: requerido min %d caracteres", MIN_SESSION_ID_LENGTH)
            );
        }
    }

    private void validateProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId no puede ser nulo o vacío");
        }
    }

    private void validateProduct(Product product) {
        if (product == null || !product.isValid()) {
            throw new IllegalArgumentException("Producto inválido o null");
        }
    }

    private void validateBalance(BalanceInformation balance) {
        if (balance == null || !balance.isValid()) {
            throw new IllegalArgumentException("Balance nulo o inválido");
        }
    }

    private void validateBigDecimal(BigDecimal value, String fieldName) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(
                String.format("%s debe ser >= 0, recibido: %s", fieldName, value)
            );
        }
    }

    private void validateBatchSize(Map<?, ?> batch) {
        if (batch != null && batch.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                String.format("Tamaño de batch excede máximo: %d > %d", batch.size(), MAX_BATCH_SIZE)
            );
        }
    }

    // ==================== OPERACIONES DE ESCRITURA ====================

    /**
     * Guarda lista completa de productos (operación atómica).
     */
    public void saveAllProducts(String sessionId, List<Product> products) {
        validateSessionId(sessionId);
        
        if (products == null || products.isEmpty()) {
            Log.warnf("Intento guardar lista vacía, sessionId: %s", sessionId);
            return;
        }

        Map<String, Product> validProducts = products.stream()
            .filter(Product::isValid)
            .collect(Collectors.toMap(
                Product::getProductId,
                product -> product,
                (existing, replacement) -> {
                    Log.debugf("Producto duplicado sobrescrito: %s", existing.getProductId());
                    return replacement;
                }
            ));

        if (validProducts.isEmpty()) {
            Log.warnf("Sin productos válidos para guardar, sessionId: %s", sessionId);
            return;
        }

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        try {
            map.putAll(validProducts);
            applyTTL(map);
            Log.infof("Guardados %d productos, sessionId: %s", validProducts.size(), sessionId);
        } catch (Exception e) {
            Log.errorf(e, "Error guardando productos, sessionId: %s", sessionId);
            throw new RepositoryOperationException("Fallo guardar productos", e);
        }
    }

    /**
     * Guarda un único producto (O(1) atómica).
     */
    public void saveProduct(String sessionId, Product product) {
        validateSessionId(sessionId);
        validateProduct(product);

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        try {
            map.fastPut(product.getProductId(), product);
            applyTTL(map);
            Log.debugf("Producto guardado - sessionId: %s, productId: %s", 
                       sessionId, product.getProductId());
        } catch (Exception e) {
            Log.errorf(e, "Error guardando producto, sessionId: %s, productId: %s", 
                       sessionId, product.getProductId());
            throw new RepositoryOperationException("Fallo guardar producto", e);
        }
    }

    /**
     * Actualiza balance de un producto (FIXES BUG: ahora aplica TTL).
     */
    public boolean updateBalance(String sessionId, String productId, BalanceInformation balance) {
        validateSessionId(sessionId);
        validateProductId(productId);
        validateBalance(balance);

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        try {
            Product updated = map.compute(productId, (key, product) -> {
                if (product == null) return null;
                product.setBalanceInformation(balance);
                return product;
            });

            if (updated != null) {
                applyTTL(map);
                Log.debugf("Balance actualizado - sessionId: %s, productId: %s", 
                           sessionId, productId);
                return true;
            }

            Log.warnf("Producto no encontrado - sessionId: %s, productId: %s", 
                      sessionId, productId);
            return false;
        } catch (Exception e) {
            Log.errorf(e, "Error actualizando balance - sessionId: %s, productId: %s", 
                       sessionId, productId);
            throw new RepositoryOperationException("Fallo actualizar balance", e);
        }
    }

    /**
     * Actualiza balances en batch (FIXES: TTL al final, sin redundancia).
     */
    public int updateBalancesBatch(String sessionId, Map<String, BalanceInformation> balanceUpdates) {
        validateSessionId(sessionId);
        validateBatchSize(balanceUpdates);
        
        if (balanceUpdates == null || balanceUpdates.isEmpty()) {
            return 0;
        }

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        int updatedCount = 0;

        try {
            for (Map.Entry<String, BalanceInformation> entry : balanceUpdates.entrySet()) {
                String productId = entry.getKey();
                BalanceInformation balance = entry.getValue();

                if (!balance.isValid()) {
                    Log.warnf("Balance inválido ignorado - productId: %s", productId);
                    continue;
                }

                Product updated = map.compute(productId, (key, product) -> {
                    if (product != null) {
                        product.setBalanceInformation(balance);
                    }
                    return product;
                });

                if (updated != null) updatedCount++;
            }

            if (updatedCount > 0) {
                applyTTL(map);
            }

            Log.infof("Actualizados %d/%d balances - sessionId: %s", 
                      updatedCount, balanceUpdates.size(), sessionId);
            return updatedCount;
        } catch (Exception e) {
            Log.errorf(e, "Error batch update balances - sessionId: %s", sessionId);
            throw new RepositoryOperationException("Fallo batch update", e);
        }
    }

    /**
     * Actualiza campo específico de balance (FIXED: ahora con TTL).
     */
    public boolean updateBalanceField(String sessionId, String productId, 
                                      BalanceField field, BigDecimal newValue) {
        validateSessionId(sessionId);
        validateProductId(productId);
        validateBigDecimal(newValue, field.name());

        RMap<String, Product> map = getSessionProductsMap(sessionId);

        try {
            Product updated = map.compute(productId, (key, product) -> {
                if (product == null) return null;

                BalanceInformation balance = product.getBalanceInformation();
                if (balance == null) {
                    balance = new BalanceInformation();
                    product.setBalanceInformation(balance);
                }

                switch (field) {
                    case PRODUCT_BALANCE -> balance.setProductBalance(newValue);
                    case AVAILABLE_AMOUNT -> balance.setAvailableAmount(newValue);
                    case USED_AMOUNT -> balance.setUsedAmount(newValue);
                }

                return product;
            });

            if (updated != null) {
                applyTTL(map);
                return true;
            }

            return false;
        } catch (Exception e) {
            Log.errorf(e, "Error actualizando campo balance - sessionId: %s, productId: %s", 
                       sessionId, productId);
            throw new RepositoryOperationException("Fallo update field", e);
        }
    }

    // ==================== OPERACIONES DE LECTURA ====================

    /**
     * Obtiene todos los productos (O(1) en Redis).
     * ✅ CORRECCIÓN: Ahora retorna List<Product> correctamente, no LinkedHashMap
     */
    public List<Product> findAllBySessionId(String sessionId) {
        validateSessionId(sessionId);

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        try {
            Map<String, Product> productsMap = map.readAllMap();
            List<Product> products = new ArrayList<>(productsMap.values());
            
            if (products.isEmpty()) {
                Log.debugf("No hay productos - sessionId: %s", sessionId);
            } else {
                Log.debugf("Obtenidos %d productos - sessionId: %s", products.size(), sessionId);
            }
            
            return products;
        } catch (Exception e) {
            Log.errorf(e, "Error leyendo productos - sessionId: %s", sessionId);
            throw new RepositoryOperationException("Fallo findAll", e);
        }
    }

    /**
     * Obtiene un producto específico (O(1)).
     */
    public Optional<Product> findByProductId(String sessionId, String productId) {
        validateSessionId(sessionId);
        validateProductId(productId);

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        try {
            Product product = map.get(productId);
            return Optional.ofNullable(product);
        } catch (Exception e) {
            Log.errorf(e, "Error leyendo producto - sessionId: %s, productId: %s", 
                       sessionId, productId);
            throw new RepositoryOperationException("Fallo findByProductId", e);
        }
    }

    /**
     * Obtiene balance de un producto.
     */
    public Optional<BalanceInformation> findBalanceByProductId(String sessionId, String productId) {
        return findByProductId(sessionId, productId)
            .map(Product::getBalanceInformation)
            .filter(Objects::nonNull);
    }

    /**
     * Obtiene todos los balances (FIXED: con null filtering).
     */
    public Map<String, BalanceInformation> findAllBalances(String sessionId) {
        validateSessionId(sessionId);

        try {
            List<Product> products = findAllBySessionId(sessionId);
            
            return products.stream()
                .filter(p -> p.getBalanceInformation() != null)
                .collect(Collectors.toMap(
                    Product::getProductId,
                    Product::getBalanceInformation,
                    (existing, replacement) -> replacement
                ));
        } catch (Exception e) {
            Log.errorf(e, "Error leyendo balances - sessionId: %s", sessionId);
            throw new RepositoryOperationException("Fallo findAllBalances", e);
        }
    }

    /**
     * Obtiene IDs de productos (más eficiente si solo necesitas keys).
     */
    public Set<String> findAllProductIds(String sessionId) {
        validateSessionId(sessionId);

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        try {
            return map.readAllKeySet();
        } catch (Exception e) {
            Log.errorf(e, "Error leyendo productIds - sessionId: %s", sessionId);
            throw new RepositoryOperationException("Fallo findAllProductIds", e);
        }
    }

    // ==================== OPERACIONES DE VERIFICACIÓN ====================

    /**
     * Verifica existencia de producto.
     */
    public boolean existsByProductId(String sessionId, String productId) {
        validateSessionId(sessionId);
        validateProductId(productId);

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        try {
            return map.containsKey(productId);
        } catch (Exception e) {
            Log.errorf(e, "Error verificando producto - sessionId: %s, productId: %s", 
                       sessionId, productId);
            throw new RepositoryOperationException("Fallo exists", e);
        }
    }

    /**
     * Verifica existencia de sesión.
     */
    public boolean existsSession(String sessionId) {
        validateSessionId(sessionId);

        return getSessionProductsMap(sessionId).isExists();
    }

    /**
     * Cuenta productos en sesión.
     */
    public int countProducts(String sessionId) {
        validateSessionId(sessionId);

        try {
            return getSessionProductsMap(sessionId).size();
        } catch (Exception e) {
            Log.errorf(e, "Error contando productos - sessionId: %s", sessionId);
            throw new RepositoryOperationException("Fallo count", e);
        }
    }

    // ==================== OPERACIONES DE ELIMINACIÓN ====================

    /**
     * Elimina un producto.
     */
    public boolean deleteProduct(String sessionId, String productId) {
        validateSessionId(sessionId);
        validateProductId(productId);

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        try {
            Product removed = map.remove(productId);
            
            if (removed != null) {
                Log.infof("Producto eliminado - sessionId: %s, productId: %s", 
                          sessionId, productId);
                return true;
            }

            return false;
        } catch (Exception e) {
            Log.errorf(e, "Error eliminando producto - sessionId: %s, productId: %s", 
                       sessionId, productId);
            throw new RepositoryOperationException("Fallo delete", e);
        }
    }

    /**
     * Elimina todos los productos de una sesión.
     */
    public int deleteAllProducts(String sessionId) {
        validateSessionId(sessionId);

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        try {
            int count = map.size();
            
            if (count > 0) {
                map.delete();
                Log.infof("Eliminados %d productos - sessionId: %s", count, sessionId);
            }

            return count;
        } catch (Exception e) {
            Log.errorf(e, "Error eliminando productos - sessionId: %s", sessionId);
            throw new RepositoryOperationException("Fallo deleteAll", e);
        }
    }

    // ==================== OPERACIONES DE TTL ====================

    /**
     * Extiende TTL de sesión.
     */
    public boolean extendSessionTTL(String sessionId, int additionalMinutes) {
        validateSessionId(sessionId);
        
        if (additionalMinutes <= 0) {
            throw new IllegalArgumentException("additionalMinutes debe ser > 0");
        }

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        try {
            boolean extended = map.expire(Duration.ofMinutes(additionalMinutes));
            
            if (extended) {
                Log.infof("TTL extendido %d min - sessionId: %s", additionalMinutes, sessionId);
            }

            return extended;
        } catch (Exception e) {
            Log.errorf(e, "Error extendiendo TTL - sessionId: %s", sessionId);
            throw new RepositoryOperationException("Fallo extend TTL", e);
        }
    }

    /**
     * Obtiene TTL restante (en ms).
     */
    public long getRemainingTTL(String sessionId) {
        validateSessionId(sessionId);

        try {
            return getSessionProductsMap(sessionId).remainTimeToLive();
        } catch (Exception e) {
            Log.errorf(e, "Error obteniendo TTL - sessionId: %s", sessionId);
            throw new RepositoryOperationException("Fallo getRemainingTTL", e);
        }
    }

    // ==================== ENUM Y EXCEPCIONES ====================

    /**
     * Campos de balance para actualización selectiva.
     */
    public enum BalanceField {
        PRODUCT_BALANCE("productBalance"),
        AVAILABLE_AMOUNT("availableAmount"),
        USED_AMOUNT("usedAmount");

        private final String fieldName;

        BalanceField(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    /**
     * Excepción personalizada para operaciones de repositorio.
     */
    public static class RepositoryOperationException extends RuntimeException {
        public RepositoryOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

***

## Cambios Clave Implementados

**Línea 24-26**: Reemplacé el `CompositeCodec` problemático:

```java
// ❌ ANTES (causaba LinkedHashMap)
private static final CompositeCodec CODEC = new CompositeCodec(
    StringCodec.INSTANCE,
    new TypedJsonJacksonCodec(Product.class)
);

// ✅ AHORA (deserializa correctamente a List<Product>)
private static final TypedJsonJacksonCodec CODEC = 
    new TypedJsonJacksonCodec(String.class, Product.class);
```

**Resultado en Redis**: JSON limpio sin `@class` - optimizado para memoria.[6][3]

**Resultado en Java**: `List<Product>` correctamente tipado - sin LinkedHashMap.[9]

[1](https://github.com/redisson/redisson/issues/1865)
[2](https://stackoverflow.com/questions/78647290/when-i-use-compositecodecstringcodec-instance-new-typedjsonjacksoncodecmember)
[3](https://github.com/redisson/redisson/wiki/4.-Data-serialization/68868af1368b5333a1e14424818504d89666d010)
[4](https://javadoc.io/doc/org.redisson/redisson/2.15.1/org/redisson/codec/TypedJsonJacksonCodec.html)
[5](https://stackoverflow.com/questions/tagged/redisson)
[6](https://www.javadoc.io/doc/org.redisson/redisson/3.20.1/org/redisson/codec/TypedJsonJacksonCodec.html)
[7](https://gitee.com/Luke-Lu/redisson/blob/master/CHANGELOG.md)
[8](https://www.cisco.com/c/dam/en_us/about/doing_business/open_source/docs/CiscoUnifiedCommunicationManager-1401-SU3-1682073267.pdf)
[9](https://stackoverflow.com/questions/77069969/how-to-successfully-use-redisson-live-objects)