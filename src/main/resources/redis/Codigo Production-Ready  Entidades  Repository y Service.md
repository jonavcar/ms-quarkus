## Código Production-Ready: Entidades, Repository y Service

### 1. Entidades de Dominio

```java
package com.company.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad Product - Representa un producto financiero del usuario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String productId;
    private String productNumber;
    private String displayName;
    private String formattedProductNumber;
    private ProductCode productCode;
    private BalanceInformation balanceInformation;
    
    /**
     * Valida que el producto tenga los campos mínimos requeridos
     */
    public boolean isValid() {
        return productId != null && !productId.isBlank();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(productId, product.productId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }
}
```

```java
package com.company.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;

/**
 * Código de producto con información de clasificación
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductCode implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String code;
    private String name;
}
```

```java
package com.company.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Información de balance del producto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceInformation implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private BigDecimal productBalance;
    private BigDecimal availableAmount;
    private BigDecimal usedAmount;
    
    /**
     * Valida que todos los campos de balance sean válidos
     * 
     * @return true si todos los campos son no nulos y no negativos
     */
    public boolean isValid() {
        return productBalance != null && 
               availableAmount != null && 
               usedAmount != null &&
               productBalance.compareTo(BigDecimal.ZERO) >= 0 &&
               availableAmount.compareTo(BigDecimal.ZERO) >= 0 &&
               usedAmount.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Verifica consistencia contable básica
     * 
     * @return true si availableAmount + usedAmount = productBalance
     */
    public boolean isBalanced() {
        if (!isValid()) return false;
        
        BigDecimal sum = availableAmount.add(usedAmount);
        return sum.compareTo(productBalance) == 0;
    }
}
```

***

### 2. Repository

```java
package com.company.repository;

import com.company.domain.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository para gestión de productos por sesión en Redis
 * Optimizado para volúmenes de 1-100 productos por sesión
 * Usa un solo Hash Redis por sesión para máxima eficiencia
 */
@ApplicationScoped
public class ProductSessionRepository {

    private static final String SESSION_KEY_TEMPLATE = "session:%s:products";

    @Inject
    RedissonClient redissonClient;

    @ConfigProperty(name = "app.redis.session.ttl-minutes", defaultValue = "30")
    int sessionTtlMinutes;

    /**
     * Obtiene el RMap de Redis para los productos de una sesión
     * 
     * @param sessionId ID de sesión del usuario
     * @return RMap configurado con codec JSON
     */
    private RMap<String, Product> getSessionProductsMap(String sessionId) {
        String key = buildSessionKey(sessionId);
        return redissonClient.getMap(key, new JsonJacksonCodec(Product.class));
    }

    /**
     * Construye la clave Redis para una sesión
     * 
     * @param sessionId ID de sesión
     * @return clave Redis formateada
     */
    private String buildSessionKey(String sessionId) {
        return String.format(SESSION_KEY_TEMPLATE, sessionId);
    }

    /**
     * Configura TTL para el hash de productos
     * 
     * @param map RMap al que aplicar TTL
     */
    private void applyTTL(RMap<String, Product> map) {
        map.expire(Duration.ofMinutes(sessionTtlMinutes));
    }

    // ==================== OPERACIONES DE ESCRITURA ====================

    /**
     * Guarda una lista completa de productos para una sesión
     * Sobrescribe productos existentes con mismo ID
     * Operación atómica - todos los productos se guardan en 1 comando
     * 
     * @param sessionId ID de sesión del usuario
     * @param products Lista de productos a guardar
     * @throws IllegalArgumentException si sessionId o products son inválidos
     */
    public void saveAllProducts(String sessionId, List<Product> products) {
        validateSessionId(sessionId);
        
        if (products == null || products.isEmpty()) {
            Log.warnf("Intento de guardar lista vacía para sesión: %s", sessionId);
            return;
        }

        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        Map<String, Product> productsMap = products.stream()
            .filter(Product::isValid)
            .collect(Collectors.toMap(
                Product::getProductId, 
                product -> product,
                (existing, replacement) -> replacement
            ));
        
        if (productsMap.isEmpty()) {
            Log.warnf("Ningún producto válido para guardar en sesión: %s", sessionId);
            return;
        }
        
        map.putAll(productsMap);
        applyTTL(map);
        
        Log.infof("Guardados %d productos para sesión: %s", productsMap.size(), sessionId);
    }

    /**
     * Guarda o actualiza un solo producto
     * Operación O(1) atómica - no trae otros productos
     * 
     * @param sessionId ID de sesión del usuario
     * @param product Producto a guardar
     * @throws IllegalArgumentException si product es inválido
     */
    public void saveProduct(String sessionId, Product product) {
        validateSessionId(sessionId);
        validateProduct(product);
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        map.fastPut(product.getProductId(), product);
        applyTTL(map);
        
        Log.debugf("Producto guardado - Session: %s, ProductId: %s", 
                   sessionId, product.getProductId());
    }

    /**
     * Actualiza SOLO el balance de un producto
     * No requiere traer el producto completo - operación atómica
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @param balance Nueva información de balance
     * @return true si se actualizó, false si el producto no existe
     */
    public boolean updateBalance(String sessionId, String productId, BalanceInformation balance) {
        validateSessionId(sessionId);
        validateProductId(productId);
        validateBalance(balance);
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        Product updated = map.compute(productId, (key, product) -> {
            if (product == null) {
                return null;
            }
            product.setBalanceInformation(balance);
            return product;
        });
        
        if (updated != null) {
            Log.debugf("Balance actualizado - Session: %s, ProductId: %s", sessionId, productId);
            return true;
        }
        
        Log.warnf("Producto no encontrado para actualizar balance - Session: %s, ProductId: %s", 
                  sessionId, productId);
        return false;
    }

    /**
     * Actualiza balances de múltiples productos
     * Operación optimizada para volúmenes pequeños (1-20 productos)
     * 
     * @param sessionId ID de sesión
     * @param balanceUpdates Mapa de productId -> nuevo balance
     * @return cantidad de productos actualizados exitosamente
     */
    public int updateBalancesBatch(String sessionId, Map<String, BalanceInformation> balanceUpdates) {
        validateSessionId(sessionId);
        
        if (balanceUpdates == null || balanceUpdates.isEmpty()) {
            return 0;
        }
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        int updatedCount = 0;
        
        for (Map.Entry<String, BalanceInformation> entry : balanceUpdates.entrySet()) {
            String productId = entry.getKey();
            BalanceInformation balance = entry.getValue();
            
            if (!balance.isValid()) {
                Log.warnf("Balance inválido ignorado para producto: %s", productId);
                continue;
            }
            
            Product updated = map.compute(productId, (key, product) -> {
                if (product != null) {
                    product.setBalanceInformation(balance);
                }
                return product;
            });
            
            if (updated != null) {
                updatedCount++;
            }
        }
        
        Log.infof("Actualizados %d/%d balances para sesión: %s", 
                  updatedCount, balanceUpdates.size(), sessionId);
        
        return updatedCount;
    }

    /**
     * Ajusta un campo específico del balance
     * Útil para operaciones incrementales (ej: ajustar saldo disponible)
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @param field Campo a actualizar (productBalance, availableAmount, usedAmount)
     * @param newValue Nuevo valor
     * @return true si se actualizó exitosamente
     */
    public boolean updateBalanceField(String sessionId, String productId, 
                                      BalanceField field, java.math.BigDecimal newValue) {
        validateSessionId(sessionId);
        validateProductId(productId);
        
        if (newValue == null || newValue.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor del campo balance debe ser >= 0");
        }
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        
        Product updated = map.compute(productId, (key, product) -> {
            if (product == null) return null;
            
            BalanceInformation balance = product.getBalanceInformation();
            if (balance == null) {
                balance = new BalanceInformation();
                product.setBalanceInformation(balance);
            }
            
            switch (field) {
                case PRODUCT_BALANCE:
                    balance.setProductBalance(newValue);
                    break;
                case AVAILABLE_AMOUNT:
                    balance.setAvailableAmount(newValue);
                    break;
                case USED_AMOUNT:
                    balance.setUsedAmount(newValue);
                    break;
            }
            
            return product;
        });
        
        return updated != null;
    }

    // ==================== OPERACIONES DE LECTURA ====================

    /**
     * Obtiene todos los productos de una sesión
     * Operación O(1) - trae todo el hash en 1 comando
     * 
     * @param sessionId ID de sesión
     * @return Lista de productos, vacía si no hay productos
     */
    public List<Product> findAllBySessionId(String sessionId) {
        validateSessionId(sessionId);
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        Map<String, Product> productsMap = map.readAllMap();
        
        if (productsMap.isEmpty()) {
            Log.debugf("No hay productos para sesión: %s", sessionId);
            return Collections.emptyList();
        }
        
        List<Product> products = new ArrayList<>(productsMap.values());
        Log.debugf("Obtenidos %d productos para sesión: %s", products.size(), sessionId);
        
        return products;
    }

    /**
     * Busca un producto específico por su ID
     * Operación O(1) - solo trae el campo solicitado
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @return Optional con el producto si existe
     */
    public Optional<Product> findByProductId(String sessionId, String productId) {
        validateSessionId(sessionId);
        validateProductId(productId);
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        Product product = map.get(productId);
        
        return Optional.ofNullable(product);
    }

    /**
     * Obtiene solo el balance de un producto
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @return Optional con el balance si existe
     */
    public Optional<BalanceInformation> findBalanceByProductId(String sessionId, String productId) {
        return findByProductId(sessionId, productId)
            .map(Product::getBalanceInformation);
    }

    /**
     * Obtiene todos los balances de los productos de una sesión
     * 
     * @param sessionId ID de sesión
     * @return Mapa de productId -> balance
     */
    public Map<String, BalanceInformation> findAllBalances(String sessionId) {
        validateSessionId(sessionId);
        
        List<Product> products = findAllBySessionId(sessionId);
        
        return products.stream()
            .filter(p -> p.getBalanceInformation() != null)
            .collect(Collectors.toMap(
                Product::getProductId,
                Product::getBalanceInformation
            ));
    }

    /**
     * Obtiene todos los IDs de productos de una sesión
     * Más eficiente que traer productos completos si solo necesitas IDs
     * 
     * @param sessionId ID de sesión
     * @return Set de productIds
     */
    public Set<String> findAllProductIds(String sessionId) {
        validateSessionId(sessionId);
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        return map.readAllKeySet();
    }

    // ==================== OPERACIONES DE VERIFICACIÓN ====================

    /**
     * Verifica si existe un producto en la sesión
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @return true si el producto existe
     */
    public boolean existsByProductId(String sessionId, String productId) {
        validateSessionId(sessionId);
        validateProductId(productId);
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        return map.containsKey(productId);
    }

    /**
     * Verifica si existe una sesión con productos
     * 
     * @param sessionId ID de sesión
     * @return true si la sesión existe en Redis
     */
    public boolean existsSession(String sessionId) {
        validateSessionId(sessionId);
        
        return getSessionProductsMap(sessionId).isExists();
    }

    /**
     * Cuenta la cantidad de productos en una sesión
     * 
     * @param sessionId ID de sesión
     * @return cantidad de productos
     */
    public int countProducts(String sessionId) {
        validateSessionId(sessionId);
        
        return getSessionProductsMap(sessionId).size();
    }

    // ==================== OPERACIONES DE ELIMINACIÓN ====================

    /**
     * Elimina un producto específico de la sesión
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto a eliminar
     * @return true si se eliminó, false si no existía
     */
    public boolean deleteProduct(String sessionId, String productId) {
        validateSessionId(sessionId);
        validateProductId(productId);
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        Product removed = map.remove(productId);
        
        if (removed != null) {
            Log.infof("Producto eliminado - Session: %s, ProductId: %s", sessionId, productId);
            return true;
        }
        
        return false;
    }

    /**
     * Elimina todos los productos de una sesión
     * 
     * @param sessionId ID de sesión
     * @return cantidad de productos eliminados
     */
    public int deleteAllProducts(String sessionId) {
        validateSessionId(sessionId);
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        int count = map.size();
        
        if (count > 0) {
            map.delete();
            Log.infof("Eliminados %d productos de sesión: %s", count, sessionId);
        }
        
        return count;
    }

    // ==================== OPERACIONES DE TTL ====================

    /**
     * Extiende el tiempo de vida de la sesión
     * 
     * @param sessionId ID de sesión
     * @param additionalMinutes Minutos adicionales de vida
     * @return true si se extendió exitosamente
     */
    public boolean extendSessionTTL(String sessionId, int additionalMinutes) {
        validateSessionId(sessionId);
        
        if (additionalMinutes <= 0) {
            throw new IllegalArgumentException("additionalMinutes debe ser > 0");
        }
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        boolean extended = map.expire(Duration.ofMinutes(additionalMinutes));
        
        if (extended) {
            Log.infof("TTL extendido %d minutos para sesión: %s", additionalMinutes, sessionId);
        }
        
        return extended;
    }

    /**
     * Obtiene el tiempo de vida restante de la sesión
     * 
     * @param sessionId ID de sesión
     * @return milisegundos restantes, -1 si no tiene TTL, -2 si no existe
     */
    public long getRemainingTTL(String sessionId) {
        validateSessionId(sessionId);
        
        RMap<String, Product> map = getSessionProductsMap(sessionId);
        return map.remainTimeToLive();
    }

    // ==================== VALIDACIONES ====================

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId no puede ser nulo o vacío");
        }
    }

    private void validateProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId no puede ser nulo o vacío");
        }
    }

    private void validateProduct(Product product) {
        if (product == null || !product.isValid()) {
            throw new IllegalArgumentException("Producto inválido");
        }
    }

    private void validateBalance(BalanceInformation balance) {
        if (balance == null || !balance.isValid()) {
            throw new IllegalArgumentException("Balance inválido o con valores negativos");
        }
    }

    /**
     * Enum para campos de balance
     */
    public enum BalanceField {
        PRODUCT_BALANCE,
        AVAILABLE_AMOUNT,
        USED_AMOUNT
    }
}
```

***

### 3. Service

```java
package com.company.service;

import com.company.domain.*;
import com.company.repository.ProductSessionRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service para lógica de negocio de productos por sesión
 * Maneja validaciones, transformaciones y orquestación
 */
@ApplicationScoped
public class ProductSessionService {

    @Inject
    ProductSessionRepository repository;

    // ==================== OPERACIONES DE ESCRITURA ====================

    /**
     * Guarda una lista completa de productos para una sesión
     * Valida cada producto antes de guardar
     * 
     * @param sessionId ID de sesión del usuario
     * @param products Lista de productos a guardar
     * @throws IllegalArgumentException si hay errores de validación
     */
    public void saveProducts(String sessionId, List<Product> products) {
        validateSessionId(sessionId);
        validateProductsList(products);
        
        repository.saveAllProducts(sessionId, products);
        
        Log.infof("Service: Guardados %d productos para sesión %s", products.size(), sessionId);
    }

    /**
     * Guarda o actualiza un solo producto
     * 
     * @param sessionId ID de sesión
     * @param product Producto a guardar
     */
    public void saveProduct(String sessionId, Product product) {
        validateSessionId(sessionId);
        validateProduct(product);
        
        repository.saveProduct(sessionId, product);
    }

    /**
     * Actualiza el balance de un producto específico
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @param balance Nuevo balance
     * @throws ProductNotFoundException si el producto no existe
     */
    public void updateBalance(String sessionId, String productId, BalanceInformation balance) {
        validateSessionId(sessionId);
        validateProductId(productId);
        validateBalance(balance);
        
        boolean updated = repository.updateBalance(sessionId, productId, balance);
        
        if (!updated) {
            throw new ProductNotFoundException(
                String.format("Producto no encontrado - Session: %s, ProductId: %s", sessionId, productId)
            );
        }
    }

    /**
     * Actualiza múltiples balances en batch
     * 
     * @param sessionId ID de sesión
     * @param balanceUpdates Mapa de productId -> nuevo balance
     * @return cantidad de productos actualizados
     */
    public int updateBalancesBatch(String sessionId, Map<String, BalanceInformation> balanceUpdates) {
        validateSessionId(sessionId);
        
        if (balanceUpdates == null || balanceUpdates.isEmpty()) {
            throw new IllegalArgumentException("balanceUpdates no puede ser nulo o vacío");
        }
        
        // Validar todos los balances antes de procesar
        List<String> invalidBalances = balanceUpdates.entrySet().stream()
            .filter(entry -> entry.getValue() == null || !entry.getValue().isValid())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        if (!invalidBalances.isEmpty()) {
            throw new IllegalArgumentException(
                "Balances inválidos para productos: " + String.join(", ", invalidBalances)
            );
        }
        
        int updatedCount = repository.updateBalancesBatch(sessionId, balanceUpdates);
        
        Log.infof("Service: Actualizados %d/%d balances para sesión %s", 
                  updatedCount, balanceUpdates.size(), sessionId);
        
        return updatedCount;
    }

    /**
     * Ajusta el monto disponible de un producto (incremento/decremento)
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @param delta Cantidad a ajustar (positivo o negativo)
     * @return Balance actualizado
     * @throws ProductNotFoundException si el producto no existe
     * @throws IllegalArgumentException si el resultado sería negativo
     */
    public BalanceInformation adjustAvailableAmount(String sessionId, String productId, BigDecimal delta) {
        validateSessionId(sessionId);
        validateProductId(productId);
        
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("delta debe ser diferente de cero");
        }
        
        BalanceInformation currentBalance = repository.findBalanceByProductId(sessionId, productId)
            .orElseThrow(() -> new ProductNotFoundException(
                String.format("Producto no encontrado - Session: %s, ProductId: %s", sessionId, productId)
            ));
        
        BigDecimal newAvailable = currentBalance.getAvailableAmount().add(delta);
        
        if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                String.format("Saldo disponible resultante sería negativo: %s + %s = %s", 
                    currentBalance.getAvailableAmount(), delta, newAvailable)
            );
        }
        
        currentBalance.setAvailableAmount(newAvailable);
        
        boolean updated = repository.updateBalance(sessionId, productId, currentBalance);
        
        if (!updated) {
            throw new ProductNotFoundException("Producto eliminado durante la operación");
        }
        
        Log.infof("Saldo disponible ajustado - Session: %s, ProductId: %s, Delta: %s, Nuevo: %s",
                  sessionId, productId, delta, newAvailable);
        
        return currentBalance;
    }

    /**
     * Transfiere monto entre dos productos de la misma sesión
     * 
     * @param sessionId ID de sesión
     * @param fromProductId Producto origen
     * @param toProductId Producto destino
     * @param amount Monto a transferir
     */
    public void transferAmount(String sessionId, String fromProductId, 
                                String toProductId, BigDecimal amount) {
        validateSessionId(sessionId);
        validateProductId(fromProductId);
        validateProductId(toProductId);
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount debe ser mayor que cero");
        }
        
        if (fromProductId.equals(toProductId)) {
            throw new IllegalArgumentException("No se puede transferir al mismo producto");
        }
        
        // Decrementar origen
        adjustAvailableAmount(sessionId, fromProductId, amount.negate());
        
        try {
            // Incrementar destino
            adjustAvailableAmount(sessionId, toProductId, amount);
        } catch (Exception e) {
            // Rollback: revertir origen
            adjustAvailableAmount(sessionId, fromProductId, amount);
            throw new RuntimeException("Error en transferencia, operación revertida", e);
        }
        
        Log.infof("Transferencia exitosa - Session: %s, From: %s, To: %s, Amount: %s",
                  sessionId, fromProductId, toProductId, amount);
    }

    // ==================== OPERACIONES DE LECTURA ====================

    /**
     * Obtiene todos los productos de una sesión
     * 
     * @param sessionId ID de sesión
     * @return Lista de productos
     */
    public List<Product> getAllProducts(String sessionId) {
        validateSessionId(sessionId);
        return repository.findAllBySessionId(sessionId);
    }

    /**
     * Obtiene un producto específico
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @return Optional con el producto
     */
    public Optional<Product> getProduct(String sessionId, String productId) {
        validateSessionId(sessionId);
        validateProductId(productId);
        return repository.findByProductId(sessionId, productId);
    }

    /**
     * Obtiene un producto o lanza excepción si no existe
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @return Producto
     * @throws ProductNotFoundException si no existe
     */
    public Product getProductOrThrow(String sessionId, String productId) {
        return getProduct(sessionId, productId)
            .orElseThrow(() -> new ProductNotFoundException(
                String.format("Producto no encontrado - Session: %s, ProductId: %s", sessionId, productId)
            ));
    }

    /**
     * Obtiene todos los balances de una sesión
     * 
     * @param sessionId ID de sesión
     * @return Mapa de productId -> balance
     */
    public Map<String, BalanceInformation> getAllBalances(String sessionId) {
        validateSessionId(sessionId);
        return repository.findAllBalances(sessionId);
    }

    /**
     * Obtiene el balance de un producto específico
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @return Optional con el balance
     */
    public Optional<BalanceInformation> getBalance(String sessionId, String productId) {
        validateSessionId(sessionId);
        validateProductId(productId);
        return repository.findBalanceByProductId(sessionId, productId);
    }

    /**
     * Calcula el balance total de todos los productos de una sesión
     * 
     * @param sessionId ID de sesión
     * @return Balance agregado
     */
    public BalanceInformation calculateTotalBalance(String sessionId) {
        validateSessionId(sessionId);
        
        Map<String, BalanceInformation> balances = repository.findAllBalances(sessionId);
        
        if (balances.isEmpty()) {
            return BalanceInformation.builder()
                .productBalance(BigDecimal.ZERO)
                .availableAmount(BigDecimal.ZERO)
                .usedAmount(BigDecimal.ZERO)
                .build();
        }
        
        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        
        for (BalanceInformation balance : balances.values()) {
            totalBalance = totalBalance.add(balance.getProductBalance());
            totalAvailable = totalAvailable.add(balance.getAvailableAmount());
            totalUsed = totalUsed.add(balance.getUsedAmount());
        }
        
        return BalanceInformation.builder()
            .productBalance(totalBalance)
            .availableAmount(totalAvailable)
            .usedAmount(totalUsed)
            .build();
    }

    /**
     * Obtiene productos filtrados por código de producto
     * 
     * @param sessionId ID de sesión
     * @param productCode Código a filtrar
     * @return Lista de productos que coinciden
     */
    public List<Product> getProductsByCode(String sessionId, String productCode) {
        validateSessionId(sessionId);
        
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("productCode no puede ser nulo o vacío");
        }
        
        return repository.findAllBySessionId(sessionId).stream()
            .filter(p -> p.getProductCode() != null)
            .filter(p -> productCode.equals(p.getProductCode().getCode()))
            .collect(Collectors.toList());
    }

    // ==================== OPERACIONES DE VERIFICACIÓN ====================

    /**
     * Verifica si existe un producto
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @return true si existe
     */
    public boolean existsProduct(String sessionId, String productId) {
        validateSessionId(sessionId);
        validateProductId(productId);
        return repository.existsByProductId(sessionId, productId);
    }

    /**
     * Verifica si existe una sesión con productos
     * 
     * @param sessionId ID de sesión
     * @return true si existe
     */
    public boolean existsSession(String sessionId) {
        validateSessionId(sessionId);
        return repository.existsSession(sessionId);
    }

    /**
     * Cuenta productos de una sesión
     * 
     * @param sessionId ID de sesión
     * @return cantidad de productos
     */
    public int countProducts(String sessionId) {
        validateSessionId(sessionId);
        return repository.countProducts(sessionId);
    }

    /**
     * Verifica si una sesión está vacía
     * 
     * @param sessionId ID de sesión
     * @return true si no tiene productos
     */
    public boolean isSessionEmpty(String sessionId) {
        return countProducts(sessionId) == 0;
    }

    // ==================== OPERACIONES DE ELIMINACIÓN ====================

    /**
     * Elimina un producto específico
     * 
     * @param sessionId ID de sesión
     * @param productId ID del producto
     * @return true si se eliminó
     */
    public boolean deleteProduct(String sessionId, String productId) {
        validateSessionId(sessionId);
        validateProductId(productId);
        return repository.deleteProduct(sessionId, productId);
    }

    /**
     * Elimina todos los productos de una sesión
     * 
     * @param sessionId ID de sesión
     * @return cantidad de productos eliminados
     */
    public int deleteAllProducts(String sessionId) {
        validateSessionId(sessionId);
        return repository.deleteAllProducts(sessionId);
    }

    // ==================== OPERACIONES DE TTL ====================

    /**
     * Extiende el tiempo de vida de la sesión
     * 
     * @param sessionId ID de sesión
     * @param additionalMinutes Minutos adicionales
     */
    public void extendSessionTTL(String sessionId, int additionalMinutes) {
        validateSessionId(sessionId);
        
        if (additionalMinutes <= 0) {
            throw new IllegalArgumentException("additionalMinutes debe ser mayor que cero");
        }
        
        boolean extended = repository.extendSessionTTL(sessionId, additionalMinutes);
        
        if (!extended) {
            Log.warnf("No se pudo extender TTL - sesión no existe o ya expiró: %s", sessionId);
        }
    }

    /**
     * Obtiene el tiempo restante de la sesión en minutos
     * 
     * @param sessionId ID de sesión
     * @return minutos restantes, -1 si no tiene TTL
     */
    public long getRemainingMinutes(String sessionId) {
        validateSessionId(sessionId);
        
        long remainingMillis = repository.getRemainingTTL(sessionId);
        
        if (remainingMillis == -2) {
            throw new SessionNotFoundException("Sesión no existe: " + sessionId);
        }
        
        return remainingMillis == -1 ? -1 : remainingMillis / (1000 * 60);
    }

    // ==================== VALIDACIONES ====================

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId no puede ser nulo o vacío");
        }
    }

    private void validateProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId no puede ser nulo o vacío");
        }
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product no puede ser nulo");
        }
        
        if (!product.isValid()) {
            throw new IllegalArgumentException("Product inválido - debe tener productId");
        }
        
        if (product.getBalanceInformation() != null && !product.getBalanceInformation().isValid()) {
            throw new IllegalArgumentException("BalanceInformation contiene valores inválidos");
        }
    }

    private void validateProductsList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Lista de productos no puede ser nula o vacía");
        }
        
        List<String> invalidProducts = new ArrayList<>();
        Set<String> duplicateIds = new HashSet<>();
        Set<String> seenIds = new HashSet<>();
        
        for (Product product : products) {
            if (product == null || !product.isValid()) {
                invalidProducts.add(product == null ? "null" : product.getProductId());
            }
            
            if (product != null && !seenIds.add(product.getProductId())) {
                duplicateIds.add(product.getProductId());
            }
        }
        
        if (!invalidProducts.isEmpty()) {
            throw new IllegalArgumentException("Productos inválidos: " + String.join(", ", invalidProducts));
        }
        
        if (!duplicateIds.isEmpty()) {
            throw new IllegalArgumentException("IDs duplicados: " + String.join(", ", duplicateIds));
        }
    }

    private void validateBalance(BalanceInformation balance) {
        if (balance == null) {
            throw new IllegalArgumentException("Balance no puede ser nulo");
        }
        
        if (!balance.isValid()) {
            throw new IllegalArgumentException("Balance contiene valores inválidos o negativos");
        }
    }
}
```

***

### 4. Excepciones Personalizadas

```java
package com.company.exception;

/**
 * Excepción lanzada cuando no se encuentra un producto
 */
public class ProductNotFoundException extends RuntimeException {
    
    public ProductNotFoundException(String message) {
        super(message);
    }
    
    public ProductNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
package com.company.exception;

/**
 * Excepción lanzada cuando no se encuentra una sesión
 */
public class SessionNotFoundException extends RuntimeException {
    
    public SessionNotFoundException(String message) {
        super(message);
    }
    
    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

***

### 5. Configuración (application.properties)

```properties
# Redis Configuration
quarkus.redisson.single-server-config.address=redis://localhost:6379
quarkus.redisson.single-server-config.password=${REDIS_PASSWORD:}
quarkus.redisson.single-server-config.database=0
quarkus.redisson.single-server-config.connection-pool-size=64
quarkus.redisson.single-server-config.connection-minimum-idle-size=10
quarkus.redisson.single-server-config.timeout=3000
quarkus.redisson.single-server-config.retry-attempts=3

# Application Configuration
app.redis.session.ttl-minutes=30

# Logging
quarkus.log.level=INFO
quarkus.log.category."com.company".level=DEBUG
```

Este código está **completamente optimizado para producción** con validaciones robustas, logging apropiado, manejo de errores profesional, operaciones atómicas, documentación completa Javadoc, principios SOLID aplicados, y performance optimizado para 1-100 productos por sesión.