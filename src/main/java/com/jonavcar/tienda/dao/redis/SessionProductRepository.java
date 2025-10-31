package com.jonavcar.tienda.dao.redis;

import com.jonavcar.tienda.entity.Balance;
import com.jonavcar.tienda.entity.Product;
import com.jonavcar.tienda.entity.ProductWithoutBalance;
import jakarta.enterprise.context.ApplicationScoped;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class SessionProductRepository {

    private final RedissonClient redisson;
    private static final String SESSION_PREFIX = "session:";

    public SessionProductRepository(RedissonClient redisson) {
        this.redisson = redisson;
    }

    // ============================================
    // SAVE & UPDATE OPERATIONS
    // ============================================

    public void saveSession(String sessionId, List<Product> products) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        Map<String, Product> productsToSave = products.stream()
            .collect(Collectors.toMap(Product::id, Function.identity()));

        productsMap.clear();
        productsMap.putAll(productsToSave);
    }

    public void updateProduct(String sessionId, Product product) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        productsMap.fastPut(product.id(), product);
    }

    public void updateBalance(String sessionId, String productId, Balance balance) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        Product currentProduct = productsMap.get(productId);
        if (currentProduct == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        Product updatedProduct = new Product(
            currentProduct.id(),
            currentProduct.number(),
            currentProduct.status(),
            balance
        );

        productsMap.fastPut(productId, updatedProduct);
    }

    public void updateBalancesBatch(String sessionId, Map<String, Balance> balances) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        Map<String, Product> updatedProducts = new HashMap<>();

        for (Map.Entry<String, Balance> entry : balances.entrySet()) {
            String productId = entry.getKey();
            Balance newBalance = entry.getValue();

            Product currentProduct = productsMap.get(productId);
            if (currentProduct != null) {
                Product updatedProduct = new Product(
                    currentProduct.id(),
                    currentProduct.number(),
                    currentProduct.status(),
                    newBalance
                );
                updatedProducts.put(productId, updatedProduct);
            }
        }

        productsMap.putAll(updatedProducts);
    }

    public void updateProductsBatch(String sessionId, List<Product> products) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        Map<String, Product> productsToUpdate = products.stream()
            .collect(Collectors.toMap(Product::id, Function.identity()));

        productsMap.putAll(productsToUpdate);
    }

    // ============================================
    // RETRIEVE SINGLE PRODUCT - WITH BALANCE
    // ============================================

    public Product getProductWithBalance(String sessionId, String productId) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        return productsMap.get(productId);
    }

    // ============================================
    // RETRIEVE SINGLE PRODUCT - WITHOUT BALANCE
    // ============================================

    public ProductWithoutBalance getProductWithoutBalance(String sessionId, String productId) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        Product product = productsMap.get(productId);

        if (product == null) {
            return null;
        }

        return new ProductWithoutBalance(
            product.id(),
            product.number(),
            product.status()
        );
    }

    // ============================================
    // RETRIEVE ALL PRODUCTS - WITH BALANCE
    // ============================================

    public List<Product> getAllProductsWithBalance(String sessionId) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        return new ArrayList<>(productsMap.values());
    }

    // ============================================
    // RETRIEVE ALL PRODUCTS - WITHOUT BALANCE
    // ============================================

    public List<ProductWithoutBalance> getAllProductsWithoutBalance(String sessionId) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        return productsMap.values().stream()
            .map(product -> new ProductWithoutBalance(
                product.id(),
                product.number(),
                product.status()
            ))
            .toList();
    }

    // ============================================
    // RETRIEVE ONLY BALANCE
    // ============================================

    public Balance getBalance(String sessionId, String productId) {
        Product product = getProductWithBalance(sessionId, productId);
        return product != null ? product.balance() : null;
    }

    // ============================================
    // UTILITY OPERATIONS
    // ============================================

    public boolean productExists(String sessionId, String productId) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        return productsMap.containsKey(productId);
    }

    public void deleteProduct(String sessionId, String productId) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        productsMap.fastRemove(productId);
    }

    public void addProduct(String sessionId, Product product) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        productsMap.fastPut(product.id(), product);
    }

    public int countProducts(String sessionId) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        return productsMap.size();
    }

    public void clearSession(String sessionId) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        productsMap.delete();
    }

    public void setExpiration(String sessionId, long timeout, TimeUnit timeUnit) {
        String mapKey = buildSessionKey(sessionId);
        RMap<String, Product> productsMap = redisson.getMap(mapKey);

        productsMap.expire(timeout, timeUnit);
    }

    private String buildSessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }
}

