package com.company.repository;

import com.company.domain.BalanceInformation;
import com.company.domain.Product;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

/**
* Repository para gestión de productos en Redis por sesión
* Usa un Hash único por sesión: session:{sessionId}:products
* Optimizado para 1-100 productos por sesión
  */
  @ApplicationScoped
  public class ProductSessionRepository {

  private static final String KEY_PATTERN = "session:%s:products";

  @Inject
  RedissonClient redissonClient;

  @ConfigProperty(name = "app.redis.session.ttl-minutes", defaultValue = "30")
  int sessionTtlMinutes;

  /**
    * Obtiene el RMap de productos para una sesión
      */
      private RMap<String, Product> getProductsMap(String sessionId) {
      String key = String.format(KEY_PATTERN, sessionId);
      return redissonClient.getMap(key, new JsonJacksonCodec(Product.class));
      }

  /**
    * Aplica TTL al mapa de productos
      */
      private void applyTTL(RMap<String, Product> map) {
      map.expire(Duration.ofMinutes(sessionTtlMinutes));
      }

  // ==================== WRITE OPERATIONS ====================

  /**
    * Guarda múltiples productos. Operación atómica en 1 comando
      */
      public void saveAll(String sessionId, List<Product> products) {
      if (products == null || products.isEmpty()) {
      return;
      }

      RMap<String, Product> map = getProductsMap(sessionId);

      Map<String, Product> productsMap = new HashMap<>();
      for (Product product : products) {
      productsMap.put(product.getProductId(), product);
      }

      map.putAll(productsMap);
      applyTTL(map);

      Log.debugf("Saved %d products for session: %s", products.size(), sessionId);
      }

  /**
    * Guarda un producto individual. Operación O(1)
      */
      public void save(String sessionId, Product product) {
      RMap<String, Product> map = getProductsMap(sessionId);
      map.fastPut(product.getProductId(), product);
      applyTTL(map);
      }

  /**
    * Actualiza solo el balance de un producto. Operación atómica
      */
      public boolean updateBalance(String sessionId, String productId, BalanceInformation balance) {
      RMap<String, Product> map = getProductsMap(sessionId);

      Product updated = map.compute(productId, (key, product) -> {
      if (product == null) {
      return null;
      }
      product.setBalanceInformation(balance);
      return product;
      });

      return updated != null;
      }

  /**
    * Actualiza múltiples balances
      */
      public int updateBalances(String sessionId, Map<String, BalanceInformation> balances) {
      if (balances == null || balances.isEmpty()) {
      return 0;
      }

      RMap<String, Product> map = getProductsMap(sessionId);
      int updated = 0;

      for (Map.Entry<String, BalanceInformation> entry : balances.entrySet()) {
      Product result = map.compute(entry.getKey(), (key, product) -> {
      if (product != null) {
      product.setBalanceInformation(entry.getValue());
      }
      return product;
      });

           if (result != null) {
               updated++;
           }
      }

      return updated;
      }

  /**
    * Actualiza un campo específico del balance
      */
      public boolean updateBalanceField(String sessionId, String productId,
      String field, BigDecimal value) {
      RMap<String, Product> map = getProductsMap(sessionId);

      Product updated = map.compute(productId, (key, product) -> {
      if (product == null) {
      return null;
      }

           BalanceInformation balance = product.getBalanceInformation();
           if (balance == null) {
               balance = new BalanceInformation();
               product.setBalanceInformation(balance);
           }
           
           switch (field) {
               case "productBalance" -> balance.setProductBalance(value);
               case "availableAmount" -> balance.setAvailableAmount(value);
               case "usedAmount" -> balance.setUsedAmount(value);
               default -> throw new IllegalArgumentException("Unknown field: " + field);
           }
           
           return product;
      });

      return updated != null;
      }

  // ==================== READ OPERATIONS ====================

  /**
    * Obtiene todos los productos de una sesión
      */
      public List<Product> findAll(String sessionId) {
      RMap<String, Product> map = getProductsMap(sessionId);
      Map<String, Product> products = map.readAllMap();
      return new ArrayList<>(products.values());
      }

  /**
    * Busca un producto por ID
      */
      public Optional<Product> findById(String sessionId, String productId) {
      RMap<String, Product> map = getProductsMap(sessionId);
      Product product = map.get(productId);
      return Optional.ofNullable(product);
      }

  /**
    * Obtiene el balance de un producto
      */
      public Optional<BalanceInformation> findBalance(String sessionId, String productId) {
      return findById(sessionId, productId)
      .map(Product::getBalanceInformation);
      }

  /**
    * Obtiene todos los balances
      */
      public Map<String, BalanceInformation> findAllBalances(String sessionId) {
      List<Product> products = findAll(sessionId);
      Map<String, BalanceInformation> balances = new HashMap<>();

      for (Product product : products) {
      if (product.getBalanceInformation() != null) {
      balances.put(product.getProductId(), product.getBalanceInformation());
      }
      }

      return balances;
      }

  /**
    * Obtiene todos los IDs de productos
      */
      public Set<String> findAllIds(String sessionId) {
      RMap<String, Product> map = getProductsMap(sessionId);
      return map.readAllKeySet();
      }

  // ==================== CHECK OPERATIONS ====================

  /**
    * Verifica si existe un producto
      */
      public boolean exists(String sessionId, String productId) {
      RMap<String, Product> map = getProductsMap(sessionId);
      return map.containsKey(productId);
      }

  /**
    * Verifica si existe una sesión
      */
      public boolean existsSession(String sessionId) {
      RMap<String, Product> map = getProductsMap(sessionId);
      return map.isExists();
      }

  /**
    * Cuenta productos en una sesión
      */
      public int count(String sessionId) {
      RMap<String, Product> map = getProductsMap(sessionId);
      return map.size();
      }

  // ==================== DELETE OPERATIONS ====================

  /**
    * Elimina un producto
      */
      public boolean delete(String sessionId, String productId) {
      RMap<String, Product> map = getProductsMap(sessionId);
      return map.fastRemove(productId) > 0;
      }

  /**
    * Elimina todos los productos de una sesión
      */
      public int deleteAll(String sessionId) {
      RMap<String, Product> map = getProductsMap(sessionId);
      int count = map.size();

      if (count > 0) {
      map.delete();
      }

      return count;
      }

  // ==================== TTL OPERATIONS ====================

  /**
    * Extiende el TTL de la sesión
      */
      public boolean extendTTL(String sessionId, int additionalMinutes) {
      RMap<String, Product> map = getProductsMap(sessionId);
      return map.expire(Duration.ofMinutes(additionalMinutes));
      }

  /**
    * Obtiene el TTL restante en milisegundos
    *
    * @return milisegundos restantes, -1 si no tiene TTL, -2 si no existe
      */
      public long getRemainingTTL(String sessionId) {
      RMap<String, Product> map = getProductsMap(sessionId);
      return map.remainTimeToLive();
      }
      }
