El problema es que `JsonJacksonCodec` está guardando metadatos de clase (`@class`) en Redis. Para JSON **limpio**, necesitas configurar un ObjectMapper personalizado.

## Cambios Puntuales

### 1. Crear Configuración de ObjectMapper

```java
package com.company.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class RedisObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // CRÍTICO: Deshabilitar type information
        objectMapper.deactivateDefaultTyping();
    }
}
```

### 2. Modificar Repository - Cambiar Codec

**Reemplaza esta línea:**
```java
return redissonClient.getMap(key, new JsonJacksonCodec(Product.class));
```

**Por:**
```java
private RMap<String, Product> getProductsMap(String sessionId) {
    String key = String.format(KEY_PATTERN, sessionId);
    
    // Usar codec personalizado sin metadatos de clase
    JsonJacksonCodec codec = new JsonJacksonCodec(objectMapper, Product.class);
    return redissonClient.getMap(key, codec);
}
```

### 3. Inyectar ObjectMapper en Repository

**Agregar al inicio del Repository:**
```java
@Inject
ObjectMapper objectMapper;
```

## Código Completo del Repository Modificado

```java
package com.company.repository;

import com.company.domain.BalanceInformation;
import com.company.domain.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@ApplicationScoped
public class ProductSessionRepository {

    private static final String KEY_PATTERN = "session:%s:products";

    @Inject
    RedissonClient redissonClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "app.redis.session.ttl-minutes", defaultValue = "30")
    int sessionTtlMinutes;

    /**
     * Obtiene el RMap de productos para una sesión con JSON limpio
     */
    private RMap<String, Product> getProductsMap(String sessionId) {
        String key = String.format(KEY_PATTERN, sessionId);
        JsonJacksonCodec codec = new JsonJacksonCodec(objectMapper, Product.class);
        return redissonClient.getMap(key, codec);
    }

    // ... resto del código sin cambios
}
```

## Resultado en Redis

**❌ ANTES (con metadatos):**
```json
{
  "@class": "com.company.domain.Product",
  "productId": "P001",
  "productNumber": "123",
  "balanceInformation": {
    "@class": "com.company.domain.BalanceInformation",
    "productBalance": 1000
  }
}
```

**✅ DESPUÉS (limpio):**
```json
{
  "productId": "P001",
  "productNumber": "123",
  "balanceInformation": {
    "productBalance": 1000
  }
}
```

Esos son los **únicos 3 cambios** necesarios para tener JSON limpio en Redis.