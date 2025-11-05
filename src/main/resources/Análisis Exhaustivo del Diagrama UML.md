<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

## Análisis Exhaustivo del Diagrama UML

Este diagrama representa un **sistema robusto de manejo de excepciones** para APIs, siguiendo patrones de diseño como Builder y utilizando una jerarquía de categorías de excepción.[^1_1]

### Arquitectura General

El sistema está compuesto por cinco componentes principales que trabajan conjuntamente para proporcionar un manejo estructurado y flexible de excepciones en aplicaciones web (web-server y web-client).[^1_1]

### Componente ExceptionCategory

Es una interfaz (<<interface>>) que define el contrato base para todas las categorías de excepciones, con un único método `propertyName(): String`. Esta interfaz actúa como el padre en la jerarquía, permitiendo extensibilidad mediante dos implementaciones:[^1_1]

- **ExceptionCategoryTypes**: Enumeración que define 13 tipos estándar de categorías de error incluyendo INVALID_REQUEST, ARGUMENT_MISMATCH, UNAUTHORIZED, FORBIDDEN, RESOURCE_NOT_FOUND, CONFLICT, PRECONDITION_FAILED, EXTERNAL_ERROR, HOST_NOT_FOUND, UNEXPECTED, NOT_IMPLEMENTED, SERVICE_UNAVAILABLE y EXTERNAL_TIMEOUT[^1_1]
- **HttpExceptionCategory**: Implementación alternativa para categorías basadas en códigos HTTP[^1_1]


### Componente ApiException

Es la clase principal de excepción con una estructura compleja que incluye código, descripción, tipo de error, detalles de excepción, propiedades, headers y categoría. Los atributos clave son:[^1_1]

- `code`: Identificador único del error
- `description` y `errorType`: Información descriptiva
- `exceptionDetails`: Set de objetos ApiExceptionDetail para contexto adicional
- `properties` y `headers`: Mapas para datos adicionales y cabeceras HTTP
- `category`: Referencia a ExceptionCategory para clasificación[^1_1]


### Componente ApiExceptionDetails

Proporciona información granular sobre cada error específico dentro de una excepción, con tres campos: `code` (identificador), `component` (componente afectado) y `description` (descripción detallada). Tiene una relación 0..n con ApiException, permitiendo múltiples detalles por excepción.[^1_1]

### Patrón Builder

El diagrama implementa el patrón Builder a través de dos clases constructoras:[^1_1]

**ApiExceptionBuilder** ofrece métodos fluidos para construir instancias de ApiException, incluyendo `code()`, `description()`, `errorType()`, `exceptionDetails()`, `properties()`, `headers()`, `category()`, `suppressDetails()`, `isMutated()`, `isResolved()`, `cause()`, `addDetail()` y `build()`. Los flags `isResolved` e `isMutated` controlan el estado de la excepción.[^1_1]

**ApiExceptionDetailBuilder** construye instancias de ApiExceptionDetails con métodos `code()`, `description()`, `component()`, `resolved()` y `push()`. El método `push()` retorna un ApiExceptionBuilder, permitiendo encadenamiento fluido.[^1_1]

### Relaciones y Patrones de Uso

La relación de composición entre ApiExceptionBuilder y ApiExceptionDetailBuilder facilita la construcción jerárquica de excepciones con múltiples detalles. Ambos componentes son utilizados por `web-server` y `web-client`, indicando que este es un modelo compartido entre cliente y servidor para manejo consistente de errores.[^1_1]

### Características Avanzadas

El sistema soporta propagación de headers HTTP, manejo de causas encadenadas (Throwable), y flags de estado como `suppressDetails` para controlar la exposición de información sensible. La arquitectura permite resolver errores dinámicamente y rastrear mutaciones, características útiles para logging y debugging.[^1_1]

<div align="center">⁂</div>

[^1_1]: WhatsApp-Image-2025-11-04-at-12.28.16-AM.jpg


---

# crealo para quarkus jdk 21

Aquí está la implementación completa del sistema de excepciones para Quarkus JDK 21, utilizando características modernas como records, sealed classes y el estilo imperativo:[^2_1][^2_2][^2_3]

## ExceptionCategory.java

```java
package com.example.exception;

public sealed interface ExceptionCategory 
    permits ExceptionCategoryTypes, HttpExceptionCategory {
    String propertyName();
}
```


## ExceptionCategoryTypes.java

```java
package com.example.exception;

public enum ExceptionCategoryTypes implements ExceptionCategory {
    INVALID_REQUEST("invalid_request"),
    ARGUMENT_MISMATCH("argument_mismatch"),
    UNAUTHORIZED("unauthorized"),
    FORBIDDEN("forbidden"),
    RESOURCE_NOT_FOUND("resource_not_found"),
    CONFLICT("conflict"),
    PRECONDITION_FAILED("precondition_failed"),
    EXTERNAL_ERROR("external_error"),
    HOST_NOT_FOUND("host_not_found"),
    UNEXPECTED("unexpected"),
    NOT_IMPLEMENTED("not_implemented"),
    SERVICE_UNAVAILABLE("service_unavailable"),
    EXTERNAL_TIMEOUT("external_timeout");

    private final String property;

    ExceptionCategoryTypes(String property) {
        this.property = property;
    }

    @Override
    public String propertyName() {
        return property;
    }
}
```


## HttpExceptionCategory.java

```java
package com.example.exception;

public final class HttpExceptionCategory implements ExceptionCategory {
    private final String propertyName;

    public HttpExceptionCategory(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public String propertyName() {
        return propertyName;
    }
}
```


## ApiExceptionDetails.java

```java
package com.example.exception;

public record ApiExceptionDetails(
    String code,
    String component,
    String description
) {}
```


## ApiException.java

```java
package com.example.exception;

import java.util.Map;
import java.util.Set;

public class ApiException extends RuntimeException {
    private final String code;
    private final String description;
    private final String errorType;
    private final Set<ApiExceptionDetails> exceptionDetails;
    private final Map<String, Object> properties;
    private final Map<String, String> headers;
    private final ExceptionCategory category;

    public ApiException(
            String code,
            String description,
            String errorType,
            Set<ApiExceptionDetails> exceptionDetails,
            Map<String, Object> properties,
            Map<String, String> headers,
            ExceptionCategory category,
            Throwable cause) {
        super(description, cause);
        this.code = code;
        this.description = description;
        this.errorType = errorType;
        this.exceptionDetails = exceptionDetails != null ? Set.copyOf(exceptionDetails) : Set.of();
        this.properties = properties != null ? Map.copyOf(properties) : Map.of();
        this.headers = headers != null ? Map.copyOf(headers) : Map.of();
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getErrorType() {
        return errorType;
    }

    public Set<ApiExceptionDetails> getExceptionDetails() {
        return exceptionDetails;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public ExceptionCategory getCategory() {
        return category;
    }
}
```


## ApiExceptionDetailBuilder.java

```java
package com.example.exception;

public class ApiExceptionDetailBuilder {
    private String code;
    private String component;
    private String description;
    private boolean resolved;
    private final ApiExceptionBuilder parentBuilder;

    public ApiExceptionDetailBuilder(ApiExceptionBuilder parentBuilder) {
        this.parentBuilder = parentBuilder;
    }

    public ApiExceptionDetailBuilder code(String code) {
        this.code = code;
        return this;
    }

    public ApiExceptionDetailBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ApiExceptionDetailBuilder component(String component) {
        this.component = component;
        return this;
    }

    public ApiExceptionDetailBuilder resolved(boolean resolved) {
        this.resolved = resolved;
        return this;
    }

    public ApiExceptionBuilder push() {
        ApiExceptionDetails detail = new ApiExceptionDetails(code, component, description);
        return parentBuilder.addDetail(detail);
    }
}
```


## ApiExceptionBuilder.java

```java
package com.example.exception;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApiExceptionBuilder {
    private String code;
    private String description;
    private String errorType;
    private Set<ApiExceptionDetails> exceptionDetails = new HashSet<>();
    private Map<String, Object> properties = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private ExceptionCategory category;
    private boolean isResolved;
    private boolean isMutated;
    private boolean suppressDetails;
    private Throwable cause;

    public ApiExceptionBuilder code(String code) {
        this.code = code;
        return this;
    }

    public ApiExceptionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ApiExceptionBuilder errorType(String errorType) {
        this.errorType = errorType;
        return this;
    }

    public ApiExceptionBuilder exceptionDetails(Set<ApiExceptionDetails> exceptionDetails) {
        this.exceptionDetails = new HashSet<>(exceptionDetails);
        return this;
    }

    public ApiExceptionBuilder category(ExceptionCategory category) {
        this.category = category;
        return this;
    }

    public ApiExceptionBuilder isResolved(boolean isResolved) {
        this.isResolved = isResolved;
        return this;
    }

    public ApiExceptionBuilder isMutated(boolean isMutated) {
        this.isMutated = isMutated;
        return this;
    }

    public ApiExceptionBuilder suppressDetails(boolean suppressDetails) {
        this.suppressDetails = suppressDetails;
        return this;
    }

    public ApiExceptionBuilder properties(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    public ApiExceptionBuilder properties(Map<String, Object> properties) {
        this.properties.putAll(properties);
        return this;
    }

    public ApiExceptionBuilder headers(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public ApiExceptionBuilder headers(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public ApiExceptionBuilder cause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    public ApiExceptionBuilder addDetail(ApiExceptionDetails detail) {
        this.exceptionDetails.add(detail);
        return this;
    }

    public ApiExceptionDetailBuilder addDetail() {
        return new ApiExceptionDetailBuilder(this);
    }

    public ApiException build() {
        Set<ApiExceptionDetails> details = suppressDetails ? Set.of() : exceptionDetails;
        return new ApiException(
            code,
            description,
            errorType,
            details,
            properties,
            headers,
            category,
            cause
        );
    }
}
```


## ApiExceptionMapper.java

```java
package com.example.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {
    
    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class);

    @Override
    public Response toResponse(ApiException exception) {
        String errorId = UUID.randomUUID().toString();
        
        LOG.errorf(exception, "ApiException [%s] - Code: %s, Category: %s", 
            errorId, exception.getCode(), exception.getCategory().propertyName());

        Map<String, Object> errorResponse = buildErrorResponse(exception, errorId);
        
        int statusCode = mapCategoryToHttpStatus(exception.getCategory());
        
        Response.ResponseBuilder responseBuilder = Response.status(statusCode)
            .entity(errorResponse);
        
        exception.getHeaders().forEach(responseBuilder::header);
        
        return responseBuilder.build();
    }

    private Map<String, Object> buildErrorResponse(ApiException exception, String errorId) {
        Map<String, Object> response = new HashMap<>();
        response.put("errorId", errorId);
        response.put("code", exception.getCode());
        response.put("description", exception.getDescription());
        response.put("errorType", exception.getErrorType());
        response.put("category", exception.getCategory().propertyName());
        
        if (!exception.getExceptionDetails().isEmpty()) {
            Set<Map<String, String>> details = exception.getExceptionDetails().stream()
                .map(detail -> Map.of(
                    "code", detail.code(),
                    "component", detail.component(),
                    "description", detail.description()
                ))
                .collect(Collectors.toSet());
            response.put("details", details);
        }
        
        if (!exception.getProperties().isEmpty()) {
            response.put("properties", exception.getProperties());
        }
        
        return response;
    }

    private int mapCategoryToHttpStatus(ExceptionCategory category) {
        if (category instanceof ExceptionCategoryTypes type) {
            return switch (type) {
                case INVALID_REQUEST, ARGUMENT_MISMATCH -> 400;
                case UNAUTHORIZED -> 401;
                case FORBIDDEN -> 403;
                case RESOURCE_NOT_FOUND, HOST_NOT_FOUND -> 404;
                case CONFLICT -> 409;
                case PRECONDITION_FAILED -> 412;
                case NOT_IMPLEMENTED -> 501;
                case SERVICE_UNAVAILABLE -> 503;
                case EXTERNAL_TIMEOUT -> 504;
                case EXTERNAL_ERROR, UNEXPECTED -> 500;
            };
        }
        return 500;
    }
}
```


## Ejemplo de Uso

```java
package com.example.resource;

import com.example.exception.ApiException;
import com.example.exception.ApiExceptionBuilder;
import com.example.exception.ExceptionCategoryTypes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/customers")
public class CustomerResource {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getCustomer(@PathParam("id") String id) {
        if (id.isBlank()) {
            throw new ApiExceptionBuilder()
                .code("CUSTOMER_001")
                .description("Customer ID cannot be blank")
                .errorType("ValidationError")
                .category(ExceptionCategoryTypes.INVALID_REQUEST)
                .properties("field", "id")
                .addDetail()
                    .code("FIELD_001")
                    .component("CustomerService")
                    .description("ID parameter is required")
                    .resolved(false)
                    .push()
                .build();
        }
        
        return "Customer: " + id;
    }
}
```

Esta implementación utiliza características modernas de JDK 21 como **sealed interfaces**, **records**, **pattern matching en switch expressions** y un estilo imperativo limpio siguiendo las convenciones de Quarkus.[^2_2][^2_3][^2_4][^2_1]
<span style="display:none">[^2_10][^2_11][^2_5][^2_6][^2_7][^2_8][^2_9]</span>

<div align="center">⁂</div>

[^2_1]: https://developers.redhat.com/articles/2022/03/03/rest-api-error-modeling-quarkus-20

[^2_2]: https://www.the-main-thread.com/p/quarkus-custom-error-pages-rest-qute

[^2_3]: https://www.infoq.com/articles/java-sealed-classes/

[^2_4]: WhatsApp-Image-2025-11-04-at-12.28.16-AM.jpg

[^2_5]: https://es.quarkus.io/guides/

[^2_6]: https://es.quarkus.io/blog/virtual-thread-1/

[^2_7]: https://www.the-main-thread.com/p/quarkus-rfc9457-api-error-handling

[^2_8]: https://stackoverflow.com/questions/71437324/quarkus-resilience-best-practice

[^2_9]: https://es.quarkus.io/guides/native-reference

[^2_10]: https://github.com/quarkusio/quarkus/discussions/26033

[^2_11]: https://stackoverflow.com/questions/77203891/annotation-processor-based-builder-pattern-with-java-14-records


---

# dame todas las formas de uso

Aquí están **todas las formas de uso** del sistema de excepciones ApiException en diferentes contextos de Quarkus JDK 21:

## Uso Básico en REST Endpoints

```java
package com.example.resource;

import com.example.exception.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/products")
public class ProductResource {

    // Forma 1: Excepción simple con categoría
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProduct(@PathParam("id") String id) {
        if (id == null || id.isBlank()) {
            throw new ApiExceptionBuilder()
                .code("PROD_001")
                .description("Product ID is required")
                .errorType("ValidationError")
                .category(ExceptionCategoryTypes.INVALID_REQUEST)
                .build();
        }
        return "Product: " + id;
    }

    // Forma 2: Excepción con propiedades adicionales
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String createProduct(Product product) {
        if (product.getPrice() < 0) {
            throw new ApiExceptionBuilder()
                .code("PROD_002")
                .description("Product price must be positive")
                .errorType("BusinessValidationError")
                .category(ExceptionCategoryTypes.ARGUMENT_MISMATCH)
                .properties("field", "price")
                .properties("value", product.getPrice())
                .properties("constraint", "min:0")
                .build();
        }
        return "Created";
    }

    // Forma 3: Excepción con headers personalizados
    @PUT
    @Path("/{id}")
    public String updateProduct(@PathParam("id") String id) {
        throw new ApiExceptionBuilder()
            .code("PROD_003")
            .description("Product update not allowed")
            .errorType("AuthorizationError")
            .category(ExceptionCategoryTypes.FORBIDDEN)
            .headers("X-Required-Permission", "product:write")
            .headers("X-User-Role", "viewer")
            .build();
    }
}
```


## Uso con Detalles Múltiples

```java
package com.example.resource;

import com.example.exception.*;
import jakarta.ws.rs.*;

@Path("/api/orders")
public class OrderResource {

    // Forma 4: Múltiples detalles con builder fluido
    @POST
    public String createOrder(Order order) {
        throw new ApiExceptionBuilder()
            .code("ORD_001")
            .description("Order validation failed")
            .errorType("ValidationError")
            .category(ExceptionCategoryTypes.INVALID_REQUEST)
            .addDetail()
                .code("FIELD_001")
                .component("OrderValidator")
                .description("Customer ID is required")
                .resolved(false)
                .push()
            .addDetail()
                .code("FIELD_002")
                .component("OrderValidator")
                .description("Order items cannot be empty")
                .resolved(false)
                .push()
            .addDetail()
                .code("FIELD_003")
                .component("PriceCalculator")
                .description("Total amount exceeds credit limit")
                .resolved(false)
                .push()
            .build();
    }

    // Forma 5: Detalles creados previamente
    @GET
    @Path("/{id}/status")
    public String getOrderStatus(@PathParam("id") String id) {
        Set<ApiExceptionDetails> details = Set.of(
            new ApiExceptionDetails("STATUS_001", "OrderService", "Order not found in database"),
            new ApiExceptionDetails("STATUS_002", "CacheService", "Order not found in cache")
        );

        throw new ApiExceptionBuilder()
            .code("ORD_002")
            .description("Order not found")
            .errorType("NotFoundError")
            .category(ExceptionCategoryTypes.RESOURCE_NOT_FOUND)
            .exceptionDetails(details)
            .properties("orderId", id)
            .build();
    }
}
```


## Uso en Capa de Servicio

```java
package com.example.service;

import com.example.exception.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PaymentService {
    
    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    // Forma 6: Excepción con causa raíz (wrapping)
    public void processPayment(String paymentId, double amount) {
        try {
            // Simulación de llamada a servicio externo
            callExternalPaymentGateway(paymentId, amount);
        } catch (Exception e) {
            throw new ApiExceptionBuilder()
                .code("PAY_001")
                .description("Payment gateway communication failed")
                .errorType("ExternalServiceError")
                .category(ExceptionCategoryTypes.EXTERNAL_ERROR)
                .cause(e)
                .properties("paymentId", paymentId)
                .properties("amount", amount)
                .properties("gateway", "stripe")
                .addDetail()
                    .code("COMM_001")
                    .component("StripeClient")
                    .description("Connection timeout after 30 seconds")
                    .resolved(false)
                    .push()
                .build();
        }
    }

    // Forma 7: Excepción de negocio con múltiples propiedades
    public void refundPayment(String paymentId) {
        double availableBalance = 100.0;
        double refundAmount = 150.0;
        
        if (refundAmount > availableBalance) {
            throw new ApiExceptionBuilder()
                .code("PAY_002")
                .description("Insufficient funds for refund")
                .errorType("BusinessRuleViolation")
                .category(ExceptionCategoryTypes.PRECONDITION_FAILED)
                .properties("paymentId", paymentId)
                .properties("requestedAmount", refundAmount)
                .properties("availableBalance", availableBalance)
                .properties("shortfall", refundAmount - availableBalance)
                .build();
        }
    }

    // Forma 8: HttpExceptionCategory personalizada
    public void cancelPayment(String paymentId) {
        throw new ApiExceptionBuilder()
            .code("PAY_003")
            .description("Payment already processed")
            .errorType("StateConflict")
            .category(new HttpExceptionCategory("payment_already_processed"))
            .properties("paymentId", paymentId)
            .properties("currentState", "COMPLETED")
            .build();
    }

    private void callExternalPaymentGateway(String id, double amount) throws Exception {
        throw new Exception("Gateway timeout");
    }
}
```


## Uso con REST Client

```java
package com.example.client;

import com.example.exception.*;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/external/api")
@RegisterRestClient(configKey = "external-api")
@RegisterProvider(ExternalApiExceptionMapper.class)
public interface ExternalApiClient {
    @GET
    @Path("/data")
    String getData();
}

// Mapper para convertir errores del cliente REST
package com.example.client;

import com.example.exception.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class ExternalApiExceptionMapper implements ResponseExceptionMapper<ApiException> {

    @Override
    public ApiException toThrowable(Response response) {
        // Forma 9: Mapear respuestas HTTP a ApiException
        if (response.getStatus() == 404) {
            return new ApiExceptionBuilder()
                .code("EXT_001")
                .description("External resource not found")
                .errorType("ExternalNotFound")
                .category(ExceptionCategoryTypes.RESOURCE_NOT_FOUND)
                .properties("statusCode", response.getStatus())
                .properties("url", response.getLocation())
                .build();
        }
        
        if (response.getStatus() == 503) {
            return new ApiExceptionBuilder()
                .code("EXT_002")
                .description("External service unavailable")
                .errorType("ExternalUnavailable")
                .category(ExceptionCategoryTypes.SERVICE_UNAVAILABLE)
                .properties("statusCode", response.getStatus())
                .headers("Retry-After", "60")
                .build();
        }
        
        if (response.getStatus() >= 500) {
            return new ApiExceptionBuilder()
                .code("EXT_003")
                .description("External service error")
                .errorType("ExternalError")
                .category(ExceptionCategoryTypes.EXTERNAL_ERROR)
                .properties("statusCode", response.getStatus())
                .build();
        }
        
        return null;
    }

    @Override
    public int getPriority() {
        return 100;
    }
}
```


## Uso en Interceptores y Filtros

```java
package com.example.filter;

import com.example.exception.*;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
public class AuthenticationFilter implements ContainerRequestFilter {

    // Forma 10: Excepción en filtros de seguridad
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String authHeader = requestContext.getHeaderString("Authorization");
        
        if (authHeader == null || authHeader.isBlank()) {
            throw new ApiExceptionBuilder()
                .code("AUTH_001")
                .description("Missing authentication credentials")
                .errorType("AuthenticationError")
                .category(ExceptionCategoryTypes.UNAUTHORIZED)
                .headers("WWW-Authenticate", "Bearer realm=\"api\"")
                .addDetail()
                    .code("HDR_001")
                    .component("AuthenticationFilter")
                    .description("Authorization header is required")
                    .resolved(false)
                    .push()
                .build();
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            throw new ApiExceptionBuilder()
                .code("AUTH_002")
                .description("Invalid authentication scheme")
                .errorType("AuthenticationError")
                .category(ExceptionCategoryTypes.UNAUTHORIZED)
                .properties("providedScheme", authHeader.split(" ")[^3_0])
                .properties("expectedScheme", "Bearer")
                .build();
        }
    }
}
```


## Uso con Validación de Bean

```java
package com.example.validator;

import com.example.exception.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.*;
import java.util.Set;

@ApplicationScoped
public class CustomValidator {

    private final Validator validator;

    public CustomValidator(Validator validator) {
        this.validator = validator;
    }

    // Forma 11: Conversión de errores de validación a ApiException
    public <T> void validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        
        if (!violations.isEmpty()) {
            ApiExceptionBuilder builder = new ApiExceptionBuilder()
                .code("VAL_001")
                .description("Bean validation failed")
                .errorType("ValidationError")
                .category(ExceptionCategoryTypes.INVALID_REQUEST);
            
            for (ConstraintViolation<T> violation : violations) {
                builder.addDetail()
                    .code("VAL_FIELD")
                    .component("BeanValidator")
                    .description(violation.getMessage())
                    .resolved(false)
                    .push();
                
                builder.properties(
                    violation.getPropertyPath().toString(),
                    violation.getInvalidValue()
                );
            }
            
            throw builder.build();
        }
    }
}
```


## Uso con Circuit Breaker

```java
package com.example.service;

import com.example.exception.*;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.*;

@ApplicationScoped
public class ResilientService {

    // Forma 12: Con Circuit Breaker y fallback
    @CircuitBreaker(
        requestVolumeThreshold = 4,
        failureRatio = 0.5,
        delay = 5000
    )
    @CircuitBreakerName("external-service")
    @Fallback(fallbackMethod = "fallbackMethod")
    @Timeout(value = 3000)
    @Retry(
        maxRetries = 3,
        delay = 1000,
        retryOn = {ApiException.class}
    )
    public String callExternalService() {
        throw new ApiExceptionBuilder()
            .code("CB_001")
            .description("External service call failed")
            .errorType("CircuitBreakerError")
            .category(ExceptionCategoryTypes.EXTERNAL_TIMEOUT)
            .properties("retryAttempt", 1)
            .build();
    }

    public String fallbackMethod() {
        return "Fallback response";
    }

    // Forma 13: Excepción cuando el circuit breaker está abierto
    @CircuitBreaker(failOn = ApiException.class)
    public String riskyOperation() {
        throw new ApiExceptionBuilder()
            .code("CB_002")
            .description("Service temporarily unavailable")
            .errorType("CircuitOpen")
            .category(ExceptionCategoryTypes.SERVICE_UNAVAILABLE)
            .properties("circuitState", "OPEN")
            .headers("Retry-After", "5")
            .suppressDetails(true) // No exponer detalles internos
            .build();
    }
}
```


## Uso con Repository/DAO

```java
package com.example.repository;

import com.example.exception.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;

@ApplicationScoped
public class UserRepository {

    @PersistenceContext
    EntityManager em;

    // Forma 14: Wrapping de excepciones de persistencia
    public User findById(Long id) {
        try {
            User user = em.find(User.class, id);
            if (user == null) {
                throw new ApiExceptionBuilder()
                    .code("DB_001")
                    .description("User not found")
                    .errorType("EntityNotFound")
                    .category(ExceptionCategoryTypes.RESOURCE_NOT_FOUND)
                    .properties("userId", id)
                    .properties("entityType", "User")
                    .build();
            }
            return user;
        } catch (PersistenceException e) {
            throw new ApiExceptionBuilder()
                .code("DB_002")
                .description("Database query failed")
                .errorType("PersistenceError")
                .category(ExceptionCategoryTypes.EXTERNAL_ERROR)
                .cause(e)
                .properties("userId", id)
                .properties("operation", "findById")
                .suppressDetails(true)
                .build();
        }
    }

    // Forma 15: Excepción de conflicto por duplicados
    public void save(User user) {
        try {
            em.persist(user);
        } catch (EntityExistsException e) {
            throw new ApiExceptionBuilder()
                .code("DB_003")
                .description("User already exists")
                .errorType("DuplicateEntity")
                .category(ExceptionCategoryTypes.CONFLICT)
                .cause(e)
                .properties("username", user.getUsername())
                .properties("email", user.getEmail())
                .addDetail()
                    .code("UNIQ_001")
                    .component("UserRepository")
                    .description("Username must be unique")
                    .resolved(false)
                    .push()
                .build();
        }
    }
}
```


## Uso con Kafka/Messaging

```java
package com.example.messaging;

import com.example.exception.*;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderConsumer {
    
    private static final Logger LOG = Logger.getLogger(OrderConsumer.class);

    // Forma 16: Excepción en procesamiento de mensajes
    @Incoming("orders")
    @Blocking
    public void processOrder(String orderJson) {
        try {
            // Procesar mensaje
            processOrderInternal(orderJson);
        } catch (Exception e) {
            throw new ApiExceptionBuilder()
                .code("MSG_001")
                .description("Failed to process order message")
                .errorType("MessageProcessingError")
                .category(ExceptionCategoryTypes.UNEXPECTED)
                .cause(e)
                .properties("payload", orderJson)
                .properties("topic", "orders")
                .addDetail()
                    .code("PARSE_001")
                    .component("OrderConsumer")
                    .description("Invalid JSON format in message")
                    .resolved(false)
                    .push()
                .build();
        }
    }

    private void processOrderInternal(String json) throws Exception {
        throw new Exception("Parse error");
    }
}
```


## Uso con Transacciones

```java
package com.example.service;

import com.example.exception.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TransactionalService {

    // Forma 17: Rollback con excepción personalizada
    @Transactional(rollbackOn = ApiException.class)
    public void performBusinessOperation() {
        // Operación 1
        updateInventory();
        
        // Operación 2
        createOrder();
        
        // Si algo falla, hacer rollback
        throw new ApiExceptionBuilder()
            .code("TXN_001")
            .description("Business operation failed")
            .errorType("TransactionError")
            .category(ExceptionCategoryTypes.PRECONDITION_FAILED)
            .properties("operation", "performBusinessOperation")
            .properties("rollback", true)
            .isMutated(true) // Indicar que hubo cambios
            .build();
    }

    private void updateInventory() {}
    private void createOrder() {}
}
```


## Uso con Flags de Control

```java
package com.example.service;

import com.example.exception.*;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SecurityService {

    // Forma 18: Suprimir detalles sensibles
    public void validateApiKey(String apiKey) {
        throw new ApiExceptionBuilder()
            .code("SEC_001")
            .description("Invalid API key")
            .errorType("SecurityError")
            .category(ExceptionCategoryTypes.UNAUTHORIZED)
            .suppressDetails(true) // No exponer detalles de seguridad
            .addDetail()
                .code("KEY_001")
                .component("SecurityService")
                .description("API key validation failed")
                .push()
            .build();
    }

    // Forma 19: Marcar error como resuelto
    public void logResolvedError() {
        throw new ApiExceptionBuilder()
            .code("LOG_001")
            .description("Logging error occurred but was handled")
            .errorType("LoggingError")
            .category(ExceptionCategoryTypes.UNEXPECTED)
            .isResolved(true) // Indicar que el error fue manejado
            .properties("handled", true)
            .build();
    }

    // Forma 20: Múltiples headers y propiedades complejas
    public void complexError() {
        throw new ApiExceptionBuilder()
            .code("COMPLEX_001")
            .description("Complex error scenario")
            .errorType("ComplexError")
            .category(ExceptionCategoryTypes.EXTERNAL_ERROR)
            .headers("X-Error-Id", "abc123")
            .headers("X-Correlation-Id", "xyz789")
            .headers("X-Request-Id", "req-456")
            .properties("timestamp", System.currentTimeMillis())
            .properties("userId", "user123")
            .properties("sessionId", "session456")
            .properties(Map.of(
                "metadata", Map.of("key1", "value1", "key2", "value2"),
                "context", "production"
            ))
            .isMutated(true)
            .build();
    }
}
```

Estas 20 formas de uso cubren prácticamente todos los escenarios comunes en aplicaciones Quarkus JDK 21, desde casos simples hasta situaciones complejas con múltiples detalles, propiedades, headers y control de estado.[^3_1][^3_2][^3_3][^3_4][^3_5]
<span style="display:none">[^3_10][^3_11][^3_6][^3_7][^3_8][^3_9]</span>

<div align="center">⁂</div>

[^3_1]: https://es.quarkus.io/guides/rest

[^3_2]: https://developers.redhat.com/articles/2022/03/03/rest-api-error-modeling-quarkus-20

[^3_3]: https://es.quarkus.io/guides/rest-client

[^3_4]: https://www.the-main-thread.com/p/quarkus-rfc7807-error-handling-java

[^3_5]: WhatsApp-Image-2025-11-04-at-12.28.16-AM.jpg

[^3_6]: https://stackoverflow.com/questions/79104491/handling-multiple-exceptions-with-mutiny-in-quarkus-service-layer

[^3_7]: https://www.youtube.com/watch?v=e0UVVCPwabk

[^3_8]: https://stackoverflow.com/questions/79722622/error-handling-with-quarkus-rest-client-and-clientexceptionmapper

[^3_9]: https://github.com/quarkusio/quarkus/discussions/26033

[^3_10]: https://github.com/quarkusio/quarkus/issues/22936

[^3_11]: https://es.quarkus.io/blog/mutiny-failure-handling/


---

# ahora creame un  factory para centralizar el buil y psarle solo el ExceptionCategoryTypes e internamente debe keer el codigo y descriocpion personalizada del error desde el yamel properties

Aquí está la implementación completa de un **Factory centralizado** que lee códigos y descripciones desde YAML/properties para construir ApiException de forma automática:[^4_1][^4_2][^4_3]

## application.yaml

```yaml
# Configuración de excepciones por categoría
api:
  exceptions:
    invalid-request:
      code: "ERR_400_001"
      description: "Invalid request parameters"
      error-type: "ValidationError"
    argument-mismatch:
      code: "ERR_400_002"
      description: "Argument type mismatch"
      error-type: "ValidationError"
    unauthorized:
      code: "ERR_401_001"
      description: "Authentication required"
      error-type: "AuthenticationError"
    forbidden:
      code: "ERR_403_001"
      description: "Access denied"
      error-type: "AuthorizationError"
    resource-not-found:
      code: "ERR_404_001"
      description: "Resource not found"
      error-type: "NotFoundError"
    conflict:
      code: "ERR_409_001"
      description: "Resource conflict detected"
      error-type: "ConflictError"
    precondition-failed:
      code: "ERR_412_001"
      description: "Precondition failed"
      error-type: "PreconditionError"
    external-error:
      code: "ERR_500_001"
      description: "External service error"
      error-type: "ExternalError"
    host-not-found:
      code: "ERR_500_002"
      description: "Host not found"
      error-type: "NetworkError"
    unexpected:
      code: "ERR_500_003"
      description: "Unexpected error occurred"
      error-type: "UnexpectedError"
    not-implemented:
      code: "ERR_501_001"
      description: "Feature not implemented"
      error-type: "NotImplementedError"
    service-unavailable:
      code: "ERR_503_001"
      description: "Service temporarily unavailable"
      error-type: "UnavailabilityError"
    external-timeout:
      code: "ERR_504_001"
      description: "External service timeout"
      error-type: "TimeoutError"
```


## ExceptionConfig.java

```java
package com.example.exception.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import java.util.Map;

@ConfigMapping(prefix = "api.exceptions")
public interface ExceptionConfig {
    
    @WithName("invalid-request")
    ExceptionDetails invalidRequest();
    
    @WithName("argument-mismatch")
    ExceptionDetails argumentMismatch();
    
    @WithName("unauthorized")
    ExceptionDetails unauthorized();
    
    @WithName("forbidden")
    ExceptionDetails forbidden();
    
    @WithName("resource-not-found")
    ExceptionDetails resourceNotFound();
    
    @WithName("conflict")
    ExceptionDetails conflict();
    
    @WithName("precondition-failed")
    ExceptionDetails preconditionFailed();
    
    @WithName("external-error")
    ExceptionDetails externalError();
    
    @WithName("host-not-found")
    ExceptionDetails hostNotFound();
    
    @WithName("unexpected")
    ExceptionDetails unexpected();
    
    @WithName("not-implemented")
    ExceptionDetails notImplemented();
    
    @WithName("service-unavailable")
    ExceptionDetails serviceUnavailable();
    
    @WithName("external-timeout")
    ExceptionDetails externalTimeout();
    
    interface ExceptionDetails {
        String code();
        String description();
        
        @WithName("error-type")
        String errorType();
    }
}
```


## ApiExceptionFactory.java

```java
package com.example.exception.factory;

import com.example.exception.*;
import com.example.exception.config.ExceptionConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ApiExceptionFactory {

    private final ExceptionConfig config;

    @Inject
    public ApiExceptionFactory(ExceptionConfig config) {
        this.config = config;
    }

    /**
     * Crea una ApiException usando solo la categoría
     */
    public ApiException create(ExceptionCategoryTypes category) {
        ExceptionConfig.ExceptionDetails details = getDetailsForCategory(category);
        
        return new ApiExceptionBuilder()
            .code(details.code())
            .description(details.description())
            .errorType(details.errorType())
            .category(category)
            .build();
    }

    /**
     * Crea una ApiException con descripción personalizada
     */
    public ApiException create(ExceptionCategoryTypes category, String customDescription) {
        ExceptionConfig.ExceptionDetails details = getDetailsForCategory(category);
        
        return new ApiExceptionBuilder()
            .code(details.code())
            .description(customDescription)
            .errorType(details.errorType())
            .category(category)
            .build();
    }

    /**
     * Crea una ApiException con propiedades adicionales
     */
    public ApiException create(ExceptionCategoryTypes category, Map<String, Object> properties) {
        ExceptionConfig.ExceptionDetails details = getDetailsForCategory(category);
        
        return new ApiExceptionBuilder()
            .code(details.code())
            .description(details.description())
            .errorType(details.errorType())
            .category(category)
            .properties(properties)
            .build();
    }

    /**
     * Crea una ApiException con descripción personalizada y propiedades
     */
    public ApiException create(ExceptionCategoryTypes category, 
                               String customDescription, 
                               Map<String, Object> properties) {
        ExceptionConfig.ExceptionDetails details = getDetailsForCategory(category);
        
        return new ApiExceptionBuilder()
            .code(details.code())
            .description(customDescription)
            .errorType(details.errorType())
            .category(category)
            .properties(properties)
            .build();
    }

    /**
     * Crea una ApiException con causa raíz
     */
    public ApiException create(ExceptionCategoryTypes category, Throwable cause) {
        ExceptionConfig.ExceptionDetails details = getDetailsForCategory(category);
        
        return new ApiExceptionBuilder()
            .code(details.code())
            .description(details.description())
            .errorType(details.errorType())
            .category(category)
            .cause(cause)
            .build();
    }

    /**
     * Crea una ApiException con descripción personalizada y causa
     */
    public ApiException create(ExceptionCategoryTypes category, 
                               String customDescription, 
                               Throwable cause) {
        ExceptionConfig.ExceptionDetails details = getDetailsForCategory(category);
        
        return new ApiExceptionBuilder()
            .code(details.code())
            .description(customDescription)
            .errorType(details.errorType())
            .category(category)
            .cause(cause)
            .build();
    }

    /**
     * Crea una ApiException con detalles adicionales
     */
    public ApiException create(ExceptionCategoryTypes category, 
                               Set<ApiExceptionDetails> exceptionDetails) {
        ExceptionConfig.ExceptionDetails details = getDetailsForCategory(category);
        
        return new ApiExceptionBuilder()
            .code(details.code())
            .description(details.description())
            .errorType(details.errorType())
            .category(category)
            .exceptionDetails(exceptionDetails)
            .build();
    }

    /**
     * Crea una ApiException completa con todos los parámetros
     */
    public ApiException create(ExceptionCategoryTypes category,
                               String customDescription,
                               Map<String, Object> properties,
                               Map<String, String> headers,
                               Set<ApiExceptionDetails> exceptionDetails,
                               Throwable cause) {
        ExceptionConfig.ExceptionDetails details = getDetailsForCategory(category);
        
        return new ApiExceptionBuilder()
            .code(details.code())
            .description(customDescription != null ? customDescription : details.description())
            .errorType(details.errorType())
            .category(category)
            .properties(properties)
            .headers(headers)
            .exceptionDetails(exceptionDetails)
            .cause(cause)
            .build();
    }

    /**
     * Retorna un builder pre-configurado para construcción fluida
     */
    public ApiExceptionBuilder builder(ExceptionCategoryTypes category) {
        ExceptionConfig.ExceptionDetails details = getDetailsForCategory(category);
        
        return new ApiExceptionBuilder()
            .code(details.code())
            .description(details.description())
            .errorType(details.errorType())
            .category(category);
    }

    /**
     * Mapea ExceptionCategoryTypes a los detalles configurados en YAML
     */
    private ExceptionConfig.ExceptionDetails getDetailsForCategory(ExceptionCategoryTypes category) {
        return switch (category) {
            case INVALID_REQUEST -> config.invalidRequest();
            case ARGUMENT_MISMATCH -> config.argumentMismatch();
            case UNAUTHORIZED -> config.unauthorized();
            case FORBIDDEN -> config.forbidden();
            case RESOURCE_NOT_FOUND -> config.resourceNotFound();
            case CONFLICT -> config.conflict();
            case PRECONDITION_FAILED -> config.preconditionFailed();
            case EXTERNAL_ERROR -> config.externalError();
            case HOST_NOT_FOUND -> config.hostNotFound();
            case UNEXPECTED -> config.unexpected();
            case NOT_IMPLEMENTED -> config.notImplemented();
            case SERVICE_UNAVAILABLE -> config.serviceUnavailable();
            case EXTERNAL_TIMEOUT -> config.externalTimeout();
        };
    }
}
```


## Ejemplos de Uso del Factory

```java
package com.example.resource;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;

@Path("/api/products")
public class ProductResource {

    @Inject
    ApiExceptionFactory exceptionFactory;

    // Uso 1: Excepción simple usando solo la categoría
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProduct(@PathParam("id") String id) {
        if (id == null || id.isBlank()) {
            throw exceptionFactory.create(ExceptionCategoryTypes.INVALID_REQUEST);
        }
        return "Product: " + id;
    }

    // Uso 2: Excepción con descripción personalizada
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String createProduct(Product product) {
        if (product.getName() == null) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.INVALID_REQUEST,
                "Product name is required and cannot be null"
            );
        }
        return "Created";
    }

    // Uso 3: Excepción con propiedades adicionales
    @PUT
    @Path("/{id}")
    public String updateProduct(@PathParam("id") String id, Product product) {
        throw exceptionFactory.create(
            ExceptionCategoryTypes.FORBIDDEN,
            Map.of(
                "productId", id,
                "requiredPermission", "product:write",
                "userRole", "viewer"
            )
        );
    }

    // Uso 4: Excepción con descripción y propiedades
    @DELETE
    @Path("/{id}")
    public String deleteProduct(@PathParam("id") String id) {
        throw exceptionFactory.create(
            ExceptionCategoryTypes.CONFLICT,
            "Cannot delete product with active orders",
            Map.of(
                "productId", id,
                "activeOrders", 5
            )
        );
    }

    // Uso 5: Excepción con causa raíz
    @GET
    @Path("/external/{id}")
    public String getExternalProduct(@PathParam("id") String id) {
        try {
            callExternalService(id);
            return "Success";
        } catch (Exception e) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.EXTERNAL_ERROR,
                e
            );
        }
    }

    // Uso 6: Excepción con descripción personalizada y causa
    @POST
    @Path("/sync")
    public String syncProducts() {
        try {
            syncWithExternalSystem();
            return "Synced";
        } catch (Exception e) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.EXTERNAL_TIMEOUT,
                "Failed to sync products - external system timeout after 30 seconds",
                e
            );
        }
    }

    // Uso 7: Excepción con detalles adicionales
    @POST
    @Path("/validate")
    public String validateProduct(Product product) {
        Set<ApiExceptionDetails> details = Set.of(
            new ApiExceptionDetails("VAL_001", "NameValidator", "Product name is too short"),
            new ApiExceptionDetails("VAL_002", "PriceValidator", "Price must be positive"),
            new ApiExceptionDetails("VAL_003", "StockValidator", "Stock cannot be negative")
        );

        throw exceptionFactory.create(
            ExceptionCategoryTypes.ARGUMENT_MISMATCH,
            details
        );
    }

    // Uso 8: Builder pre-configurado para construcción fluida
    @GET
    @Path("/check/{id}")
    public String checkProduct(@PathParam("id") String id) {
        throw exceptionFactory.builder(ExceptionCategoryTypes.RESOURCE_NOT_FOUND)
            .properties("productId", id)
            .properties("searchedIn", "database")
            .addDetail()
                .code("DB_001")
                .component("ProductRepository")
                .description("Product not found in primary database")
                .push()
            .addDetail()
                .code("CACHE_001")
                .component("CacheService")
                .description("Product not found in cache")
                .push()
            .build();
    }

    // Uso 9: Excepción completa con todos los parámetros
    @POST
    @Path("/process")
    public String processProduct(Product product) {
        try {
            complexOperation(product);
            return "Processed";
        } catch (Exception e) {
            Set<ApiExceptionDetails> details = Set.of(
                new ApiExceptionDetails("PROC_001", "Validator", "Validation failed"),
                new ApiExceptionDetails("PROC_002", "Processor", "Processing error")
            );

            throw exceptionFactory.create(
                ExceptionCategoryTypes.UNEXPECTED,
                "Product processing failed due to multiple errors",
                Map.of("productId", product.getId(), "step", "validation"),
                Map.of("X-Request-Id", "req-123", "X-Trace-Id", "trace-456"),
                details,
                e
            );
        }
    }

    private void callExternalService(String id) throws Exception {
        throw new Exception("Service unavailable");
    }

    private void syncWithExternalSystem() throws Exception {
        throw new Exception("Timeout");
    }

    private void complexOperation(Product product) throws Exception {
        throw new Exception("Complex error");
    }
}
```


## Uso en Servicios

```java
package com.example.service;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;

@ApplicationScoped
public class OrderService {

    @Inject
    ApiExceptionFactory exceptionFactory;

    public void createOrder(Order order) {
        // Validación simple
        if (order.getCustomerId() == null) {
            throw exceptionFactory.create(ExceptionCategoryTypes.INVALID_REQUEST);
        }

        // Validación con mensaje personalizado
        if (order.getItems().isEmpty()) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.ARGUMENT_MISMATCH,
                "Order must contain at least one item"
            );
        }

        // Error de negocio con contexto
        if (order.getTotalAmount() > 10000.0) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.PRECONDITION_FAILED,
                "Order amount exceeds customer credit limit",
                Map.of(
                    "orderId", order.getId(),
                    "totalAmount", order.getTotalAmount(),
                    "creditLimit", 10000.0
                )
            );
        }
    }

    public void processPayment(String orderId) {
        try {
            callPaymentGateway(orderId);
        } catch (Exception e) {
            // Wrapping de excepción externa
            throw exceptionFactory.create(
                ExceptionCategoryTypes.EXTERNAL_ERROR,
                "Payment gateway communication failed",
                e
            );
        }
    }

    private void callPaymentGateway(String orderId) throws Exception {
        throw new Exception("Gateway error");
    }
}
```


## Uso en Filtros

```java
package com.example.filter;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
@PreMatching
public class SecurityFilter implements ContainerRequestFilter {

    @Inject
    ApiExceptionFactory exceptionFactory;

    @Override
    public void filter(ContainerRequestContext context) {
        String token = context.getHeaderString("Authorization");
        
        if (token == null) {
            throw exceptionFactory.builder(ExceptionCategoryTypes.UNAUTHORIZED)
                .headers("WWW-Authenticate", "Bearer realm=\"api\"")
                .addDetail()
                    .code("AUTH_001")
                    .component("SecurityFilter")
                    .description("Authorization header is required")
                    .push()
                .build();
        }
        
        if (!token.startsWith("Bearer ")) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.UNAUTHORIZED,
                "Invalid authentication scheme",
                Map.of(
                    "provided", token.split(" ")[^4_0],
                    "expected", "Bearer"
                )
            );
        }
    }
}
```


## pom.xml (Dependencia YAML)

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-config-yaml</artifactId>
</dependency>
```

Este factory centraliza completamente la construcción de excepciones, leyendo código, descripción y tipo de error desde YAML, permitiendo múltiples formas de uso según el contexto.[^4_2][^4_3][^4_4][^4_1]
<span style="display:none">[^4_10][^4_11][^4_5][^4_6][^4_7][^4_8][^4_9]</span>

<div align="center">⁂</div>

[^4_1]: https://es.quarkus.io/guides/config-yaml

[^4_2]: https://es.quarkus.io/guides/config-reference

[^4_3]: https://es.quarkus.io/guides/config-mappings

[^4_4]: WhatsApp-Image-2025-11-04-at-12.28.16-AM.jpg

[^4_5]: https://docs.redhat.com/es/documentation/red_hat_build_of_quarkus/2.13/html-single/configuring_your_quarkus_applications_by_using_a_yaml_file/index

[^4_6]: https://stackoverflow.com/questions/59662044/quarkus-how-to-define-and-read-properties-file-or-application-properties-outs

[^4_7]: https://jorney.srbala.com/guides/config-yaml

[^4_8]: https://stackoverflow.com/questions/61872993/how-to-use-configuration-maps-for-custom-quarkus-configproperties

[^4_9]: https://www.the-main-thread.com/p/quarkus-dependency-injection-cdi-basics

[^4_10]: https://www.youtube.com/watch?v=TwpmISkKnSc

[^4_11]: https://dev.to/yanev/exploring-synthetic-beans-in-quarkus-a-powerful-extension-mechanism-fbd


---

# como capturo los errores de los proxys microprofiles que tienen apiexception propio del generador que crea para las interfaces restclient

Aquí está la solución completa para **capturar y mapear las ApiException generadas por OpenAPI Generator** en los proxies MicroProfile REST Client de Quarkus:[^5_1][^5_2][^5_3]

## RestClientExceptionMapper.java

```java
package com.example.client.mapper;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper global que convierte ApiException del cliente generado por OpenAPI
 * a nuestra ApiException personalizada
 */
public class RestClientExceptionMapper implements ResponseExceptionMapper<ApiException> {

    private static final Logger LOG = Logger.getLogger(RestClientExceptionMapper.class);

    @Inject
    ApiExceptionFactory exceptionFactory;

    @Override
    public ApiException toThrowable(Response response) {
        // Solo procesar errores HTTP (status >= 400)
        if (response.getStatus() < 400) {
            return null;
        }

        try {
            // Intentar leer el body como ApiException del generador
            if (response.hasEntity()) {
                Map<String, Object> errorBody = response.readEntity(Map.class);
                return mapFromErrorBody(errorBody, response.getStatus());
            }
        } catch (Exception e) {
            LOG.warnf("Failed to parse error response body: %s", e.getMessage());
        }

        // Fallback: crear ApiException basada solo en el status code
        return createFromHttpStatus(response);
    }

    @Override
    public int getPriority() {
        return 100; // Menor valor = mayor prioridad
    }

    /**
     * Mapea el body JSON de error a nuestra ApiException
     */
    private ApiException mapFromErrorBody(Map<String, Object> errorBody, int statusCode) {
        String code = (String) errorBody.get("code");
        String description = (String) errorBody.get("description");
        String errorType = (String) errorBody.get("errorType");
        String categoryName = (String) errorBody.get("category");

        // Mapear categoría
        ExceptionCategoryTypes category = mapCategoryFromString(categoryName, statusCode);

        // Construir excepción usando el factory
        ApiExceptionBuilder builder = exceptionFactory.builder(category);

        // Si el error remoto tiene código personalizado, usarlo
        if (code != null) {
            builder.code(code);
        }

        // Si tiene descripción personalizada, usarla
        if (description != null) {
            builder.description(description);
        }

        if (errorType != null) {
            builder.errorType(errorType);
        }

        // Mapear propiedades adicionales
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) errorBody.get("properties");
        if (properties != null) {
            builder.properties(properties);
        }

        // Mapear headers
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) errorBody.get("headers");
        if (headers != null) {
            builder.headers(headers);
        }

        // Mapear detalles de excepción
        @SuppressWarnings("unchecked")
        Set<Map<String, String>> detailsList = (Set<Map<String, String>>) errorBody.get("details");
        if (detailsList != null && !detailsList.isEmpty()) {
            Set<ApiExceptionDetails> details = detailsList.stream()
                .map(detail -> new ApiExceptionDetails(
                    detail.get("code"),
                    detail.get("component"),
                    detail.get("description")
                ))
                .collect(Collectors.toSet());
            builder.exceptionDetails(details);
        }

        return builder.build();
    }

    /**
     * Crea ApiException basándose solo en el HTTP status code
     */
    private ApiException createFromHttpStatus(Response response) {
        int status = response.getStatus();
        ExceptionCategoryTypes category = mapHttpStatusToCategory(status);

        return exceptionFactory.builder(category)
            .properties("httpStatus", status)
            .properties("statusInfo", response.getStatusInfo().getReasonPhrase())
            .addDetail()
                .code("HTTP_" + status)
                .component("RestClient")
                .description("Remote service returned HTTP " + status)
                .push()
            .build();
    }

    /**
     * Mapea el nombre de categoría del string a ExceptionCategoryTypes
     */
    private ExceptionCategoryTypes mapCategoryFromString(String categoryName, int statusCode) {
        if (categoryName == null) {
            return mapHttpStatusToCategory(statusCode);
        }

        return switch (categoryName.toLowerCase().replace("_", "-")) {
            case "invalid-request", "invalid_request" -> ExceptionCategoryTypes.INVALID_REQUEST;
            case "argument-mismatch", "argument_mismatch" -> ExceptionCategoryTypes.ARGUMENT_MISMATCH;
            case "unauthorized" -> ExceptionCategoryTypes.UNAUTHORIZED;
            case "forbidden" -> ExceptionCategoryTypes.FORBIDDEN;
            case "resource-not-found", "resource_not_found" -> ExceptionCategoryTypes.RESOURCE_NOT_FOUND;
            case "conflict" -> ExceptionCategoryTypes.CONFLICT;
            case "precondition-failed", "precondition_failed" -> ExceptionCategoryTypes.PRECONDITION_FAILED;
            case "external-error", "external_error" -> ExceptionCategoryTypes.EXTERNAL_ERROR;
            case "host-not-found", "host_not_found" -> ExceptionCategoryTypes.HOST_NOT_FOUND;
            case "unexpected" -> ExceptionCategoryTypes.UNEXPECTED;
            case "not-implemented", "not_implemented" -> ExceptionCategoryTypes.NOT_IMPLEMENTED;
            case "service-unavailable", "service_unavailable" -> ExceptionCategoryTypes.SERVICE_UNAVAILABLE;
            case "external-timeout", "external_timeout" -> ExceptionCategoryTypes.EXTERNAL_TIMEOUT;
            default -> mapHttpStatusToCategory(statusCode);
        };
    }

    /**
     * Mapea HTTP status code a ExceptionCategoryTypes
     */
    private ExceptionCategoryTypes mapHttpStatusToCategory(int status) {
        return switch (status) {
            case 400 -> ExceptionCategoryTypes.INVALID_REQUEST;
            case 401 -> ExceptionCategoryTypes.UNAUTHORIZED;
            case 403 -> ExceptionCategoryTypes.FORBIDDEN;
            case 404 -> ExceptionCategoryTypes.RESOURCE_NOT_FOUND;
            case 409 -> ExceptionCategoryTypes.CONFLICT;
            case 412 -> ExceptionCategoryTypes.PRECONDITION_FAILED;
            case 422 -> ExceptionCategoryTypes.ARGUMENT_MISMATCH;
            case 500 -> ExceptionCategoryTypes.UNEXPECTED;
            case 501 -> ExceptionCategoryTypes.NOT_IMPLEMENTED;
            case 502 -> ExceptionCategoryTypes.EXTERNAL_ERROR;
            case 503 -> ExceptionCategoryTypes.SERVICE_UNAVAILABLE;
            case 504 -> ExceptionCategoryTypes.EXTERNAL_TIMEOUT;
            default -> ExceptionCategoryTypes.UNEXPECTED;
        };
    }
}
```


## Opción 1: Registro Global (Todos los REST Clients)

### Crear archivo de proveedor SPI

Crear archivo: `src/main/resources/META-INF/services/org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper`

Contenido:

```
com.example.client.mapper.RestClientExceptionMapper
```


## Opción 2: Registro por Interface (REST Clients Específicos)

```java
package com.example.client;

import com.example.client.mapper.RestClientExceptionMapper;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/products")
@RegisterRestClient(configKey = "product-api")
@RegisterProvider(RestClientExceptionMapper.class)
public interface ProductApiClient {
    
    @GET
    @Path("/{id}")
    ProductDTO getProduct(@PathParam("id") String id);
    
    @POST
    ProductDTO createProduct(ProductDTO product);
}
```


## Opción 3: Usando @ClientExceptionMapper (Más Simple)

```java
package com.example.client;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/api/orders")
@RegisterRestClient(configKey = "order-api")
@ApplicationScoped
public interface OrderApiClient {
    
    @Inject
    ApiExceptionFactory exceptionFactory;
    
    @GET
    @Path("/{id}")
    OrderDTO getOrder(@PathParam("id") String id);
    
    @POST
    OrderDTO createOrder(OrderDTO order);
    
    /**
     * Mapper de excepciones específico para este cliente
     */
    @ClientExceptionMapper
    static ApiException toException(Response response) {
        if (response.getStatus() < 400) {
            return null;
        }
        
        try {
            if (response.hasEntity()) {
                Map<String, Object> errorBody = response.readEntity(Map.class);
                return mapErrorFromBody(errorBody, response.getStatus());
            }
        } catch (Exception e) {
            // Ignorar y usar fallback
        }
        
        return mapFromHttpStatus(response.getStatus());
    }
    
    private static ApiException mapErrorFromBody(Map<String, Object> errorBody, int status) {
        String code = (String) errorBody.get("code");
        String description = (String) errorBody.get("description");
        
        ExceptionCategoryTypes category = mapStatus(status);
        
        return new ApiExceptionBuilder()
            .code(code != null ? code : "ERR_" + status)
            .description(description != null ? description : "Remote error")
            .errorType("RemoteError")
            .category(category)
            .properties(errorBody)
            .build();
    }
    
    private static ApiException mapFromHttpStatus(int status) {
        ExceptionCategoryTypes category = mapStatus(status);
        
        return new ApiExceptionBuilder()
            .code("HTTP_" + status)
            .description("HTTP error " + status)
            .errorType("HttpError")
            .category(category)
            .properties("statusCode", status)
            .build();
    }
    
    private static ExceptionCategoryTypes mapStatus(int status) {
        return switch (status) {
            case 400 -> ExceptionCategoryTypes.INVALID_REQUEST;
            case 401 -> ExceptionCategoryTypes.UNAUTHORIZED;
            case 403 -> ExceptionCategoryTypes.FORBIDDEN;
            case 404 -> ExceptionCategoryTypes.RESOURCE_NOT_FOUND;
            case 409 -> ExceptionCategoryTypes.CONFLICT;
            case 503 -> ExceptionCategoryTypes.SERVICE_UNAVAILABLE;
            case 504 -> ExceptionCategoryTypes.EXTERNAL_TIMEOUT;
            default -> ExceptionCategoryTypes.EXTERNAL_ERROR;
        };
    }
}
```


## Captura de ApiException Generada por OpenAPI

Si el generador OpenAPI crea su propia clase `ApiException`, puedes convertirla:

```java
package com.example.client.mapper;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.logging.Logger;

// Importar la ApiException generada por OpenAPI
import com.example.generated.client.ApiException as GeneratedApiException;

/**
 * Convierte la ApiException del OpenAPI Generator a nuestra ApiException
 */
public class OpenApiExceptionMapper implements ResponseExceptionMapper<ApiException> {

    private static final Logger LOG = Logger.getLogger(OpenApiExceptionMapper.class);

    @Inject
    ApiExceptionFactory exceptionFactory;

    @Override
    public ApiException toThrowable(Response response) {
        if (response.getStatus() < 400) {
            return null;
        }

        try {
            // El OpenAPI generator típicamente incluye información del error
            String responseBody = null;
            if (response.hasEntity()) {
                responseBody = response.readEntity(String.class);
            }

            ExceptionCategoryTypes category = mapHttpStatusToCategory(response.getStatus());

            return exceptionFactory.builder(category)
                .properties("statusCode", response.getStatus())
                .properties("responseBody", responseBody)
                .properties("headers", response.getStringHeaders())
                .addDetail()
                    .code("REMOTE_ERROR")
                    .component("OpenAPIClient")
                    .description("Error from OpenAPI generated client: " + responseBody)
                    .push()
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "Failed to map OpenAPI exception");
            return exceptionFactory.create(
                ExceptionCategoryTypes.EXTERNAL_ERROR,
                "Failed to process remote error",
                e
            );
        }
    }

    @Override
    public int getPriority() {
        return 50; // Alta prioridad
    }

    private ExceptionCategoryTypes mapHttpStatusToCategory(int status) {
        return switch (status) {
            case 400, 422 -> ExceptionCategoryTypes.INVALID_REQUEST;
            case 401 -> ExceptionCategoryTypes.UNAUTHORIZED;
            case 403 -> ExceptionCategoryTypes.FORBIDDEN;
            case 404 -> ExceptionCategoryTypes.RESOURCE_NOT_FOUND;
            case 409 -> ExceptionCategoryTypes.CONFLICT;
            case 412 -> ExceptionCategoryTypes.PRECONDITION_FAILED;
            case 500 -> ExceptionCategoryTypes.UNEXPECTED;
            case 501 -> ExceptionCategoryTypes.NOT_IMPLEMENTED;
            case 502 -> ExceptionCategoryTypes.EXTERNAL_ERROR;
            case 503 -> ExceptionCategoryTypes.SERVICE_UNAVAILABLE;
            case 504 -> ExceptionCategoryTypes.EXTERNAL_TIMEOUT;
            default -> ExceptionCategoryTypes.EXTERNAL_ERROR;
        };
    }
}
```


## Wrapper para ApiException del OpenAPI Generator

Si necesitas interceptar la `ApiException` generada y convertirla:

```java
package com.example.client.wrapper;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import com.example.generated.client.ApiClient;
import com.example.generated.client.api.ProductApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Wrapper que envuelve el cliente generado y convierte excepciones
 */
@ApplicationScoped
public class ProductApiClientWrapper {

    private static final Logger LOG = Logger.getLogger(ProductApiClientWrapper.class);

    @Inject
    ApiExceptionFactory exceptionFactory;

    private final ProductApi productApi;

    public ProductApiClientWrapper() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:8081");
        this.productApi = new ProductApi(apiClient);
    }

    public ProductDTO getProduct(String id) {
        try {
            return productApi.getProduct(id);
        } catch (com.example.generated.client.ApiException e) {
            throw convertGeneratedException(e);
        }
    }

    public ProductDTO createProduct(ProductDTO product) {
        try {
            return productApi.createProduct(product);
        } catch (com.example.generated.client.ApiException e) {
            throw convertGeneratedException(e);
        }
    }

    /**
     * Convierte ApiException generada a nuestra ApiException personalizada
     */
    private ApiException convertGeneratedException(com.example.generated.client.ApiException generatedException) {
        ExceptionCategoryTypes category = mapCodeToCategory(generatedException.getCode());

        return exceptionFactory.builder(category)
            .properties("remoteCode", generatedException.getCode())
            .properties("remoteMessage", generatedException.getMessage())
            .properties("responseBody", generatedException.getResponseBody())
            .properties("responseHeaders", generatedException.getResponseHeaders())
            .cause(generatedException)
            .addDetail()
                .code("OPENAPI_" + generatedException.getCode())
                .component("GeneratedClient")
                .description(generatedException.getMessage())
                .push()
            .build();
    }

    private ExceptionCategoryTypes mapCodeToCategory(int code) {
        return switch (code) {
            case 400 -> ExceptionCategoryTypes.INVALID_REQUEST;
            case 401 -> ExceptionCategoryTypes.UNAUTHORIZED;
            case 403 -> ExceptionCategoryTypes.FORBIDDEN;
            case 404 -> ExceptionCategoryTypes.RESOURCE_NOT_FOUND;
            case 409 -> ExceptionCategoryTypes.CONFLICT;
            case 503 -> ExceptionCategoryTypes.SERVICE_UNAVAILABLE;
            case 504 -> ExceptionCategoryTypes.EXTERNAL_TIMEOUT;
            default -> ExceptionCategoryTypes.EXTERNAL_ERROR;
        };
    }
}
```


## Uso en Servicios

```java
package com.example.service;

import com.example.exception.*;
import com.example.client.ProductApiClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);

    @RestClient
    ProductApiClient productApiClient;

    public ProductDTO getProduct(String id) {
        try {
            return productApiClient.getProduct(id);
        } catch (ApiException e) {
            // La excepción ya viene mapeada por RestClientExceptionMapper
            LOG.errorf("Error getting product: code=%s, category=%s", 
                e.getCode(), e.getCategory().propertyName());
            
            // Re-lanzar o manejar según lógica de negocio
            throw e;
        }
    }
}
```


## application.yaml - Configuración

```yaml
# Configuración del REST Client
quarkus:
  rest-client:
    product-api:
      url: http://localhost:8081
      scope: jakarta.enterprise.context.ApplicationScoped
    order-api:
      url: http://localhost:8082
      scope: jakarta.enterprise.context.ApplicationScoped
    # Auto-discovery de providers (incluye ResponseExceptionMapper)
    provider-autodiscovery: true

# Deshabilitar el mapper por defecto de MicroProfile si es necesario
microprofile:
  rest:
    client:
      disable:
        default:
          mapper: false
```

Con estas implementaciones, todas las excepciones de los REST Clients generados por OpenAPI o definidos manualmente serán automáticamente convertidas a tu `ApiException` personalizada, leyendo la configuración desde YAML y manteniendo consistencia en toda la aplicación.[^5_2][^5_3][^5_4][^5_1]
<span style="display:none">[^5_10][^5_11][^5_5][^5_6][^5_7][^5_8][^5_9]</span>

<div align="center">⁂</div>

[^5_1]: https://es.quarkus.io/guides/rest-client

[^5_2]: https://stackoverflow.com/questions/64213483/quarkus-microprofile-rest-client-responseexceptionmapper-doesnt-catch-errors

[^5_3]: https://download.eclipse.org/microprofile/microprofile-rest-client-1.0/microprofile-rest-client.pdf

[^5_4]: WhatsApp-Image-2025-11-04-at-12.28.16-AM.jpg

[^5_5]: https://github.com/quarkusio/quarkus/discussions/33322

[^5_6]: https://es.quarkus.io/guides/rest

[^5_7]: https://community.developer.atlassian.com/t/jira-rest-client-generation-fails-swagger-open-api-generator/36398

[^5_8]: https://github.com/quarkusio/quarkus/issues/37029

[^5_9]: https://javadoc.io/doc/io.quarkus/quarkus-rest-client-reactive/3.0.0.CR1/io/quarkus/rest/client/reactive/ClientExceptionMapper.html

[^5_10]: https://ademico-software.com/how-to-generate-the-client-api-for-an-openapi-rest-service/

[^5_11]: https://github.com/quarkusio/quarkus/issues/21175


---

# tengo un microservicio contract first que tiene contractos openapi para el servidor y para los clientes. se usa microprofiles restclient. como hago para que las interfaces generadas de los clientes los capture mi apiesception, ademas como hago para que mi apiexception lance los serrores de mi servidor

Aquí está la solución completa para un **microservicio contract-first con OpenAPI**, donde capturas errores de clientes REST generados Y lanzas tu ApiException desde el servidor:[^6_1][^6_2][^6_3][^6_4]

## Estructura del Proyecto

```
microservicio/
├── src/main/resources/
│   └── META-INF/
│       └── openapi/
│           ├── server-api.yaml          # Contrato del servidor
│           ├── client-products-api.yaml # Contrato cliente productos
│           └── client-orders-api.yaml   # Contrato cliente pedidos
└── src/main/java/
    └── com/example/
        ├── exception/              # Sistema de excepciones
        ├── server/                # Implementación del servidor
        └── client/                # Wrappers para clientes
```


## 1. Configuración pom.xml

```xml
<dependencies>
    <!-- Quarkus REST -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-rest</artifactId>
    </dependency>
    
    <!-- REST Client -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-rest-client</artifactId>
    </dependency>
    
    <!-- OpenAPI y Swagger UI -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-openapi</artifactId>
    </dependency>
    
    <!-- OpenAPI Generator -->
    <dependency>
        <groupId>io.quarkiverse.openapi.generator</groupId>
        <artifactId>quarkus-openapi-generator</artifactId>
        <version>2.4.3</version>
    </dependency>
    
    <!-- YAML Config -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-config-yaml</artifactId>
    </dependency>
</dependencies>
```


## 2. application.yaml - Configuración

```yaml
# Configuración del servidor OpenAPI
quarkus:
  smallrye-openapi:
    path: /openapi
    info-title: My Microservice API
    info-version: 1.0.0
    info-description: Contract-first microservice
    
  swagger-ui:
    path: /swagger-ui
    always-include: true

  # Configuración de generación OpenAPI
  openapi-generator:
    codegen:
      # Generación del servidor
      spec:
        server-api_yaml:
          base-package: com.example.generated.server
          config-key: server-api
          enable-security-generation: false
          generate-part: "model" # Solo generar modelos, las APIs las implementamos
          
      # Generación de cliente productos
      spec:
        client-products-api_yaml:
          base-package: com.example.generated.client.products
          config-key: products-api
          enable-security-generation: false
          
      # Generación de cliente pedidos  
      spec:
        client-orders-api_yaml:
          base-package: com.example.generated.client.orders
          config-key: orders-api
          enable-security-generation: false

  # Configuración de REST Clients
  rest-client:
    products-api:
      url: ${PRODUCTS_API_URL:http://localhost:8081}
      scope: jakarta.enterprise.context.ApplicationScoped
      
    orders-api:
      url: ${ORDERS_API_URL:http://localhost:8082}
      scope: jakarta.enterprise.context.ApplicationScoped
    
    # Auto-discovery de providers (ResponseExceptionMapper)
    provider-autodiscovery: true

# Configuración de excepciones (del factory anterior)
api:
  exceptions:
    invalid-request:
      code: "ERR_400_001"
      description: "Invalid request parameters"
      error-type: "ValidationError"
    unauthorized:
      code: "ERR_401_001"
      description: "Authentication required"
      error-type: "AuthenticationError"
    forbidden:
      code: "ERR_403_001"
      description: "Access denied"
      error-type: "AuthorizationError"
    resource-not-found:
      code: "ERR_404_001"
      description: "Resource not found"
      error-type: "NotFoundError"
    conflict:
      code: "ERR_409_001"
      description: "Resource conflict"
      error-type: "ConflictError"
    external-error:
      code: "ERR_500_001"
      description: "External service error"
      error-type: "ExternalError"
    service-unavailable:
      code: "ERR_503_001"
      description: "Service unavailable"
      error-type: "UnavailabilityError"
```


## 3. server-api.yaml - Contrato del Servidor

```yaml
openapi: 3.0.3
info:
  title: My Microservice API
  version: 1.0.0
paths:
  /api/customers/{id}:
    get:
      operationId: getCustomer
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Customer'
        '404':
          description: Not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiError'
        '500':
          description: Internal error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiError'
                
  /api/customers:
    post:
      operationId: createCustomer
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CustomerRequest'
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Customer'
        '400':
          description: Bad request
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiError'

components:
  schemas:
    Customer:
      type: object
      properties:
        id:
          type: string
        name:
          type: string
        email:
          type: string
          
    CustomerRequest:
      type: object
      required:
        - name
        - email
      properties:
        name:
          type: string
        email:
          type: string
          
    ApiError:
      type: object
      properties:
        errorId:
          type: string
        code:
          type: string
        description:
          type: string
        errorType:
          type: string
        category:
          type: string
        properties:
          type: object
          additionalProperties: true
        details:
          type: array
          items:
            $ref: '#/components/schemas/ApiErrorDetail'
            
    ApiErrorDetail:
      type: object
      properties:
        code:
          type: string
        component:
          type: string
        description:
          type: string
```


## 4. ApiErrorResponseDTO.java - DTO para respuestas de error

```java
package com.example.dto;

import com.example.exception.ApiException;
import com.example.exception.ApiExceptionDetails;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO que representa el error en el formato del contrato OpenAPI
 */
public record ApiErrorResponseDTO(
    String errorId,
    String code,
    String description,
    String errorType,
    String category,
    Map<String, Object> properties,
    Set<ApiErrorDetailDTO> details
) {
    
    public record ApiErrorDetailDTO(
        String code,
        String component,
        String description
    ) {}
    
    /**
     * Convierte ApiException a DTO para serialización
     */
    public static ApiErrorResponseDTO fromException(ApiException exception) {
        String errorId = UUID.randomUUID().toString();
        
        Set<ApiErrorDetailDTO> detailDTOs = exception.getExceptionDetails().stream()
            .map(detail -> new ApiErrorDetailDTO(
                detail.code(),
                detail.component(),
                detail.description()
            ))
            .collect(Collectors.toSet());
        
        return new ApiErrorResponseDTO(
            errorId,
            exception.getCode(),
            exception.getDescription(),
            exception.getErrorType(),
            exception.getCategory().propertyName(),
            exception.getProperties(),
            detailDTOs
        );
    }
}
```


## 5. GlobalExceptionMapper.java - Mapper para el Servidor

```java
package com.example.exception.mapper;

import com.example.dto.ApiErrorResponseDTO;
import com.example.exception.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Mapper que convierte ApiException a respuestas HTTP del servidor
 * Implementa el contrato OpenAPI del servidor
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<ApiException> {
    
    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(ApiException exception) {
        // Convertir a DTO según el contrato OpenAPI
        ApiErrorResponseDTO errorDTO = ApiErrorResponseDTO.fromException(exception);
        
        // Log del error
        LOG.errorf(exception, "ApiException [%s] - Code: %s, Category: %s", 
            errorDTO.errorId(), 
            errorDTO.code(), 
            errorDTO.category());
        
        // Mapear categoría a HTTP status
        int statusCode = mapCategoryToHttpStatus(exception.getCategory());
        
        // Construir respuesta con headers personalizados
        Response.ResponseBuilder responseBuilder = Response
            .status(statusCode)
            .entity(errorDTO);
        
        // Agregar headers de la excepción
        exception.getHeaders().forEach(responseBuilder::header);
        
        return responseBuilder.build();
    }

    private int mapCategoryToHttpStatus(ExceptionCategory category) {
        if (category instanceof ExceptionCategoryTypes type) {
            return switch (type) {
                case INVALID_REQUEST, ARGUMENT_MISMATCH -> 400;
                case UNAUTHORIZED -> 401;
                case FORBIDDEN -> 403;
                case RESOURCE_NOT_FOUND, HOST_NOT_FOUND -> 404;
                case CONFLICT -> 409;
                case PRECONDITION_FAILED -> 412;
                case NOT_IMPLEMENTED -> 501;
                case SERVICE_UNAVAILABLE -> 503;
                case EXTERNAL_TIMEOUT -> 504;
                case EXTERNAL_ERROR, UNEXPECTED -> 500;
            };
        }
        return 500;
    }
}
```


## 6. ThrowableMapper.java - Mapper para excepciones no controladas

```java
package com.example.exception.mapper;

import com.example.dto.ApiErrorResponseDTO;
import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Captura todas las excepciones no manejadas (Throwable)
 * y las convierte a ApiException
 */
@Provider
public class ThrowableMapper implements ExceptionMapper<Throwable> {
    
    private static final Logger LOG = Logger.getLogger(ThrowableMapper.class);

    @Inject
    ApiExceptionFactory exceptionFactory;

    @Override
    public Response toResponse(Throwable throwable) {
        // Si ya es ApiException, delegar al GlobalExceptionMapper
        if (throwable instanceof ApiException apiException) {
            return new GlobalExceptionMapper().toResponse(apiException);
        }
        
        // Convertir cualquier otra excepción a ApiException
        ApiException apiException = exceptionFactory.create(
            ExceptionCategoryTypes.UNEXPECTED,
            "An unexpected error occurred",
            throwable
        );
        
        // Usar el mapper global
        return new GlobalExceptionMapper().toResponse(apiException);
    }
}
```


## 7. ClientExceptionMapper.java - Mapper para Clientes REST

```java
package com.example.client.mapper;

import com.example.dto.ApiErrorResponseDTO;
import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper que captura errores de los clientes REST generados
 * y los convierte a nuestro ApiException
 */
public class ClientExceptionMapper implements ResponseExceptionMapper<ApiException> {

    private static final Logger LOG = Logger.getLogger(ClientExceptionMapper.class);

    @Inject
    ApiExceptionFactory exceptionFactory;

    @Override
    public ApiException toThrowable(Response response) {
        // Solo procesar errores HTTP
        if (response.getStatus() < 400) {
            return null;
        }

        try {
            // Intentar parsear el error según nuestro contrato
            if (response.hasEntity()) {
                ApiErrorResponseDTO errorDTO = response.readEntity(ApiErrorResponseDTO.class);
                return convertFromDTO(errorDTO);
            }
        } catch (Exception e) {
            LOG.warnf("Could not parse error response as ApiErrorResponseDTO: %s", e.getMessage());
        }

        // Fallback: crear ApiException basada en status code
        return createFromHttpStatus(response);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    /**
     * Convierte el DTO de error a ApiException
     */
    private ApiException convertFromDTO(ApiErrorResponseDTO errorDTO) {
        // Mapear categoría desde string
        ExceptionCategoryTypes category = mapCategoryFromString(errorDTO.category());
        
        // Construir detalles
        Set<ApiExceptionDetails> details = null;
        if (errorDTO.details() != null) {
            details = errorDTO.details().stream()
                .map(dto -> new ApiExceptionDetails(
                    dto.code(),
                    dto.component(),
                    dto.description()
                ))
                .collect(Collectors.toSet());
        }
        
        // Usar el builder pre-configurado del factory
        ApiExceptionBuilder builder = exceptionFactory.builder(category);
        
        // Sobrescribir con datos del servidor remoto
        if (errorDTO.code() != null) {
            builder.code(errorDTO.code());
        }
        if (errorDTO.description() != null) {
            builder.description(errorDTO.description());
        }
        if (errorDTO.errorType() != null) {
            builder.errorType(errorDTO.errorType());
        }
        if (errorDTO.properties() != null) {
            builder.properties(errorDTO.properties());
        }
        if (details != null) {
            builder.exceptionDetails(details);
        }
        
        // Agregar contexto de que viene de un cliente remoto
        builder.properties("source", "remote-service");
        builder.properties("remoteErrorId", errorDTO.errorId());
        
        return builder.build();
    }

    /**
     * Crea ApiException basándose solo en el status code
     */
    private ApiException createFromHttpStatus(Response response) {
        ExceptionCategoryTypes category = mapHttpStatusToCategory(response.getStatus());
        
        return exceptionFactory.builder(category)
            .properties("httpStatus", response.getStatus())
            .properties("statusInfo", response.getStatusInfo().getReasonPhrase())
            .properties("source", "remote-service")
            .addDetail()
                .code("HTTP_" + response.getStatus())
                .component("RestClient")
                .description("Remote service returned HTTP " + response.getStatus())
                .push()
            .build();
    }

    private ExceptionCategoryTypes mapCategoryFromString(String categoryName) {
        if (categoryName == null) {
            return ExceptionCategoryTypes.EXTERNAL_ERROR;
        }

        return switch (categoryName.toLowerCase().replace("_", "-")) {
            case "invalid-request" -> ExceptionCategoryTypes.INVALID_REQUEST;
            case "argument-mismatch" -> ExceptionCategoryTypes.ARGUMENT_MISMATCH;
            case "unauthorized" -> ExceptionCategoryTypes.UNAUTHORIZED;
            case "forbidden" -> ExceptionCategoryTypes.FORBIDDEN;
            case "resource-not-found" -> ExceptionCategoryTypes.RESOURCE_NOT_FOUND;
            case "conflict" -> ExceptionCategoryTypes.CONFLICT;
            case "precondition-failed" -> ExceptionCategoryTypes.PRECONDITION_FAILED;
            case "external-error" -> ExceptionCategoryTypes.EXTERNAL_ERROR;
            case "service-unavailable" -> ExceptionCategoryTypes.SERVICE_UNAVAILABLE;
            case "external-timeout" -> ExceptionCategoryTypes.EXTERNAL_TIMEOUT;
            default -> ExceptionCategoryTypes.EXTERNAL_ERROR;
        };
    }

    private ExceptionCategoryTypes mapHttpStatusToCategory(int status) {
        return switch (status) {
            case 400 -> ExceptionCategoryTypes.INVALID_REQUEST;
            case 401 -> ExceptionCategoryTypes.UNAUTHORIZED;
            case 403 -> ExceptionCategoryTypes.FORBIDDEN;
            case 404 -> ExceptionCategoryTypes.RESOURCE_NOT_FOUND;
            case 409 -> ExceptionCategoryTypes.CONFLICT;
            case 412 -> ExceptionCategoryTypes.PRECONDITION_FAILED;
            case 422 -> ExceptionCategoryTypes.ARGUMENT_MISMATCH;
            case 501 -> ExceptionCategoryTypes.NOT_IMPLEMENTED;
            case 503 -> ExceptionCategoryTypes.SERVICE_UNAVAILABLE;
            case 504 -> ExceptionCategoryTypes.EXTERNAL_TIMEOUT;
            default -> ExceptionCategoryTypes.EXTERNAL_ERROR;
        };
    }
}
```


## 8. Registro del ClientExceptionMapper

Crear archivo: `src/main/resources/META-INF/services/org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper`

Contenido:

```
com.example.client.mapper.ClientExceptionMapper
```


## 9. CustomerResource.java - Implementación del Servidor

```java
package com.example.server.resource;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import com.example.generated.server.model.Customer;
import com.example.generated.server.model.CustomerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Implementación del servidor según contrato OpenAPI
 * Lanza ApiException que será capturada por GlobalExceptionMapper
 */
@Path("/api/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {

    private static final Logger LOG = Logger.getLogger(CustomerResource.class);

    @Inject
    ApiExceptionFactory exceptionFactory;

    @GET
    @Path("/{id}")
    public Customer getCustomer(@PathParam("id") String id) {
        LOG.infof("Getting customer: %s", id);
        
        // Validación simple
        if (id == null || id.isBlank()) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.INVALID_REQUEST,
                "Customer ID cannot be blank",
                Map.of("field", "id")
            );
        }
        
        // Simulación: customer no encontrado
        if ("999".equals(id)) {
            throw exceptionFactory.builder(ExceptionCategoryTypes.RESOURCE_NOT_FOUND)
                .properties("customerId", id)
                .properties("searchedIn", "database")
                .addDetail()
                    .code("DB_001")
                    .component("CustomerRepository")
                    .description("Customer not found in database")
                    .push()
                .build();
        }
        
        // Retornar customer (usando modelos generados por OpenAPI)
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        
        return customer;
    }

    @POST
    public Response createCustomer(CustomerRequest request) {
        LOG.infof("Creating customer: %s", request.getName());
        
        // Validación de negocio
        if (request.getName() == null || request.getName().length() < 3) {
            throw exceptionFactory.builder(ExceptionCategoryTypes.ARGUMENT_MISMATCH)
                .addDetail()
                    .code("VAL_001")
                    .component("CustomerValidator")
                    .description("Name must be at least 3 characters")
                    .push()
                .properties("field", "name")
                .properties("providedLength", request.getName() != null ? request.getName().length() : 0)
                .properties("minimumLength", 3)
                .build();
        }
        
        if (!request.getEmail().contains("@")) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.INVALID_REQUEST,
                "Invalid email format",
                Map.of("field", "email", "value", request.getEmail())
            );
        }
        
        // Simulación: email duplicado
        if ("duplicate@example.com".equals(request.getEmail())) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.CONFLICT,
                "A customer with this email already exists",
                Map.of("email", request.getEmail())
            );
        }
        
        // Crear customer
        Customer customer = new Customer();
        customer.setId("123");
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        
        return Response.status(Response.Status.CREATED).entity(customer).build();
    }
}
```


## 10. ProductsServiceClient.java - Uso del Cliente REST

```java
package com.example.client.service;

import com.example.exception.*;
import com.example.generated.client.products.api.ProductsApi;
import com.example.generated.client.products.model.Product;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Service que usa el cliente REST generado
 * Los errores son automáticamente convertidos por ClientExceptionMapper
 */
@ApplicationScoped
public class ProductsServiceClient {

    private static final Logger LOG = Logger.getLogger(ProductsServiceClient.class);

    @RestClient
    ProductsApi productsApi;

    public Product getProduct(String productId) {
        try {
            LOG.infof("Fetching product from remote service: %s", productId);
            return productsApi.getProduct(productId);
            
        } catch (ApiException e) {
            // La excepción ya viene mapeada por ClientExceptionMapper
            LOG.errorf("Error fetching product: code=%s, category=%s, description=%s",
                e.getCode(),
                e.getCategory().propertyName(),
                e.getDescription());
            
            // Puedes agregar contexto adicional o re-lanzar
            throw e;
        }
    }

    public Product createProduct(Product product) {
        try {
            LOG.infof("Creating product in remote service: %s", product.getName());
            return productsApi.createProduct(product);
            
        } catch (ApiException e) {
            // Manejar error específico y agregar contexto local
            LOG.errorf("Error creating product: %s", e.getDescription());
            
            // Opción: envolver con más contexto
            if (e.getCategory() == ExceptionCategoryTypes.CONFLICT) {
                throw new ApiExceptionBuilder()
                    .code(e.getCode())
                    .description("Product creation failed: " + e.getDescription())
                    .errorType(e.getErrorType())
                    .category(e.getCategory())
                    .properties(e.getProperties())
                    .properties("localContext", "ProductsServiceClient")
                    .properties("attemptedProduct", product.getName())
                    .exceptionDetails(e.getExceptionDetails())
                    .cause(e)
                    .build();
            }
            
            throw e;
        }
    }
}
```


## 11. OrchestrationService.java - Orquestación con múltiples clientes

```java
package com.example.service;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import com.example.client.service.ProductsServiceClient;
import com.example.generated.client.orders.api.OrdersApi;
import com.example.generated.client.orders.model.Order;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Service que orquesta múltiples servicios remotos
 * Maneja errores de múltiples clientes REST
 */
@ApplicationScoped
public class OrchestrationService {

    private static final Logger LOG = Logger.getLogger(OrchestrationService.class);

    @Inject
    ProductsServiceClient productsClient;

    @RestClient
    OrdersApi ordersApi;

    @Inject
    ApiExceptionFactory exceptionFactory;

    public Order createOrderWithProduct(String productId, int quantity) {
        try {
            // 1. Verificar que el producto existe
            var product = productsClient.getProduct(productId);
            LOG.infof("Product found: %s", product.getName());
            
            // 2. Crear la orden
            Order order = new Order();
            order.setProductId(productId);
            order.setQuantity(quantity);
            
            var createdOrder = ordersApi.createOrder(order);
            LOG.infof("Order created: %s", createdOrder.getId());
            
            return createdOrder;
            
        } catch (ApiException e) {
            // Los errores de ambos clientes ya vienen como ApiException
            LOG.errorf("Error in orchestration: %s", e.getDescription());
            
            // Agregar contexto de orquestación
            throw exceptionFactory.builder(e.getCategory())
                .code(e.getCode())
                .description("Order creation failed: " + e.getDescription())
                .errorType(e.getErrorType())
                .properties(e.getProperties())
                .properties("orchestrationStep", determineStep(e))
                .properties("productId", productId)
                .properties("quantity", quantity)
                .exceptionDetails(e.getExceptionDetails())
                .cause(e)
                .build();
        }
    }

    private String determineStep(ApiException e) {
        var source = e.getProperties().get("source");
        if (source != null && source.toString().contains("products")) {
            return "product-validation";
        }
        return "order-creation";
    }
}
```


## 12. Ejemplo de Prueba

```java
package com.example.server.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
class CustomerResourceTest {

    @Test
    void testGetCustomer_NotFound() {
        given()
            .when().get("/api/customers/999")
            .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", equalTo("ERR_404_001"))
                .body("category", equalTo("resource-not-found"))
                .body("description", notNullValue())
                .body("errorId", notNullValue())
                .body("details", notNullValue());
    }

    @Test
    void testCreateCustomer_ValidationError() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "name": "Jo",
                    "email": "invalid-email"
                }
                """)
            .when().post("/api/customers")
            .then()
                .statusCode(400)
                .body("code", equalTo("ERR_400_002"))
                .body("category", equalTo("argument-mismatch"));
    }
}
```

Esta implementación completa te permite:

1. **Servidor**: Lanzar `ApiException` que se convierte automáticamente a respuestas JSON según el contrato OpenAPI[^6_4][^6_1]
2. **Clientes**: Capturar errores de servicios remotos y convertirlos a tu `ApiException`[^6_2][^6_3]
3. **Consistencia**: Usar el mismo modelo de excepciones en todo el microservicio
4. **Contract-First**: Mantener los contratos OpenAPI como fuente de verdad[^6_2]
5. **Factory**: Centralizar la creación de excepciones con configuración YAML

El sistema es completamente transparente: lanzas `ApiException` en el servidor y recibes `ApiException` de los clientes.[^6_5]
<span style="display:none">[^6_10][^6_11][^6_6][^6_7][^6_8][^6_9]</span>

<div align="center">⁂</div>

[^6_1]: https://es.quarkus.io/guides/openapi-swaggerui

[^6_2]: https://www.the-main-thread.com/p/spec-first-or-code-first-choosing

[^6_3]: https://es.quarkus.io/guides/rest-client

[^6_4]: https://developers.redhat.com/articles/2022/03/03/rest-api-error-modeling-quarkus-20

[^6_5]: WhatsApp-Image-2025-11-04-at-12.28.16-AM.jpg

[^6_6]: https://groups.google.com/g/quarkus-dev/c/f8hJRm8oWbQ

[^6_7]: https://stackoverflow.com/questions/75504213/how-to-use-a-contract-first-approach-with-graphql-and-quarkus

[^6_8]: https://github.com/OpenAPITools/openapi-generator

[^6_9]: https://github.com/quarkiverse/quarkus-openapi-generator/issues/1113

[^6_10]: https://es.quarkus.io/blog/openapi-for-everyone/

[^6_11]: https://stackoverflow.com/questions/73275301/quarkus-generate-rest-clients-from-openapi


---

# en el ProductsServiceClient en el catch a veces llema mi ApiException otras veces llega el ApiException que genera el generador y les pone a las apis clientes a las interfaces. como hago un capturador que capture ambos y los unifique

Aquí está la solución completa para **capturar y unificar ambos tipos de ApiException** (la tuya y la generada por OpenAPI) usando un patrón de captura unificado:[^7_1][^7_2][^7_3]

## 1. ExceptionUnifier.java - Unificador de Excepciones

```java
package com.example.exception.unifier;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Set;

/**
 * Unifica las excepciones:
 * - Nuestra ApiException personalizada
 * - ApiException generada por OpenAPI Generator
 */
@ApplicationScoped
public class ExceptionUnifier {

    private static final Logger LOG = Logger.getLogger(ExceptionUnifier.class);
    
    // Nombre completo de la clase ApiException generada por OpenAPI
    private static final String GENERATED_API_EXCEPTION_CLASS = 
        "org.openapitools.client.ApiException";

    @Inject
    ApiExceptionFactory exceptionFactory;

    /**
     * Captura cualquier Exception y la unifica a nuestra ApiException
     */
    public ApiException unify(Exception exception) {
        // Caso 1: Ya es nuestra ApiException
        if (exception instanceof com.example.exception.ApiException apiException) {
            LOG.debugf("Exception is already our ApiException: %s", apiException.getCode());
            return apiException;
        }

        // Caso 2: Es la ApiException generada por OpenAPI
        if (isGeneratedApiException(exception)) {
            LOG.debugf("Converting generated ApiException to our ApiException");
            return convertGeneratedApiException(exception);
        }

        // Caso 3: Cualquier otra excepción
        LOG.debugf("Converting generic exception to ApiException: %s", exception.getClass().getName());
        return convertGenericException(exception);
    }

    /**
     * Verifica si es la ApiException generada
     */
    private boolean isGeneratedApiException(Exception exception) {
        return exception.getClass().getName().equals(GENERATED_API_EXCEPTION_CLASS)
            || exception.getClass().getSimpleName().equals("ApiException");
    }

    /**
     * Convierte ApiException generada por OpenAPI a nuestra ApiException
     * Usa reflexión para extraer los datos
     */
    private ApiException convertGeneratedApiException(Exception generatedException) {
        try {
            // Extraer datos usando reflexión
            int code = getFieldValue(generatedException, "code", Integer.class, 500);
            String message = generatedException.getMessage();
            String responseBody = getFieldValue(generatedException, "responseBody", String.class, null);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseHeaders = getFieldValue(
                generatedException, 
                "responseHeaders", 
                Map.class, 
                Map.of()
            );

            // Mapear código HTTP a categoría
            ExceptionCategoryTypes category = mapHttpStatusToCategory(code);

            return exceptionFactory.builder(category)
                .properties("generatedCode", code)
                .properties("generatedMessage", message)
                .properties("responseBody", responseBody)
                .properties("responseHeaders", responseHeaders)
                .properties("source", "openapi-generated-client")
                .cause(generatedException)
                .addDetail()
                    .code("GENERATED_" + code)
                    .component("OpenAPIGeneratedClient")
                    .description(message != null ? message : "Error from generated client")
                    .push()
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "Failed to extract fields from generated ApiException");
            return createFallbackException(generatedException);
        }
    }

    /**
     * Convierte cualquier excepción genérica a nuestra ApiException
     */
    private ApiException convertGenericException(Exception exception) {
        return exceptionFactory.create(
            ExceptionCategoryTypes.EXTERNAL_ERROR,
            "External service call failed: " + exception.getMessage(),
            exception
        );
    }

    /**
     * Crea excepción fallback cuando no se puede convertir
     */
    private ApiException createFallbackException(Exception exception) {
        return exceptionFactory.create(
            ExceptionCategoryTypes.EXTERNAL_ERROR,
            "Failed to process external service error",
            Map.of(
                "exceptionType", exception.getClass().getName(),
                "exceptionMessage", exception.getMessage()
            )
        );
    }

    /**
     * Obtiene el valor de un campo usando reflexión
     */
    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object obj, String fieldName, Class<T> type, T defaultValue) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            return value != null ? (T) value : defaultValue;
        } catch (Exception e) {
            LOG.debugf("Could not extract field '%s': %s", fieldName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Mapea HTTP status code a categoría
     */
    private ExceptionCategoryTypes mapHttpStatusToCategory(int status) {
        return switch (status) {
            case 400 -> ExceptionCategoryTypes.INVALID_REQUEST;
            case 401 -> ExceptionCategoryTypes.UNAUTHORIZED;
            case 403 -> ExceptionCategoryTypes.FORBIDDEN;
            case 404 -> ExceptionCategoryTypes.RESOURCE_NOT_FOUND;
            case 409 -> ExceptionCategoryTypes.CONFLICT;
            case 412 -> ExceptionCategoryTypes.PRECONDITION_FAILED;
            case 422 -> ExceptionCategoryTypes.ARGUMENT_MISMATCH;
            case 500 -> ExceptionCategoryTypes.UNEXPECTED;
            case 501 -> ExceptionCategoryTypes.NOT_IMPLEMENTED;
            case 502, 503 -> ExceptionCategoryTypes.SERVICE_UNAVAILABLE;
            case 504 -> ExceptionCategoryTypes.EXTERNAL_TIMEOUT;
            default -> ExceptionCategoryTypes.EXTERNAL_ERROR;
        };
    }
}
```


## 2. RestClientCaller.java - Wrapper Genérico para Llamadas

```java
package com.example.client.wrapper;

import com.example.exception.ApiException;
import com.example.exception.unifier.ExceptionUnifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.function.Supplier;

/**
 * Wrapper que captura y unifica excepciones de REST Clients
 */
@ApplicationScoped
public class RestClientCaller {

    private static final Logger LOG = Logger.getLogger(RestClientCaller.class);

    @Inject
    ExceptionUnifier exceptionUnifier;

    /**
     * Ejecuta una llamada a REST Client y unifica cualquier excepción
     */
    public <T> T call(Supplier<T> clientCall) {
        try {
            return clientCall.get();
        } catch (com.example.exception.ApiException e) {
            // Ya es nuestra ApiException, re-lanzar directamente
            LOG.debugf("Caught our ApiException: %s", e.getCode());
            throw e;
        } catch (Exception e) {
            // Convertir cualquier otra excepción (incluida la generada)
            LOG.debugf("Caught exception, unifying: %s", e.getClass().getName());
            throw exceptionUnifier.unify(e);
        }
    }

    /**
     * Versión void para operaciones sin retorno
     */
    public void callVoid(Runnable clientCall) {
        try {
            clientCall.run();
        } catch (com.example.exception.ApiException e) {
            throw e;
        } catch (Exception e) {
            throw exceptionUnifier.unify(e);
        }
    }
}
```


## 3. ProductsServiceClient.java - Uso del Unificador

```java
package com.example.client.service;

import com.example.client.wrapper.RestClientCaller;
import com.example.exception.ApiException;
import com.example.generated.client.products.api.ProductsApi;
import com.example.generated.client.products.model.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Service que usa el cliente REST con captura unificada
 */
@ApplicationScoped
public class ProductsServiceClient {

    private static final Logger LOG = Logger.getLogger(ProductsServiceClient.class);

    @RestClient
    ProductsApi productsApi;

    @Inject
    RestClientCaller restClientCaller;

    /**
     * Obtener producto - captura unificada
     */
    public Product getProduct(String productId) {
        return restClientCaller.call(() -> {
            LOG.infof("Fetching product: %s", productId);
            return productsApi.getProduct(productId);
        });
    }

    /**
     * Crear producto - captura unificada
     */
    public Product createProduct(Product product) {
        return restClientCaller.call(() -> {
            LOG.infof("Creating product: %s", product.getName());
            return productsApi.createProduct(product);
        });
    }

    /**
     * Eliminar producto - operación void
     */
    public void deleteProduct(String productId) {
        restClientCaller.callVoid(() -> {
            LOG.infof("Deleting product: %s", productId);
            productsApi.deleteProduct(productId);
        });
    }
}
```


## 4. Alternativa: Try-Catch Unificado Manual

```java
package com.example.client.service;

import com.example.exception.ApiException;
import com.example.exception.unifier.ExceptionUnifier;
import com.example.generated.client.orders.api.OrdersApi;
import com.example.generated.client.orders.model.Order;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrdersServiceClient {

    private static final Logger LOG = Logger.getLogger(OrdersServiceClient.class);

    @RestClient
    OrdersApi ordersApi;

    @Inject
    ExceptionUnifier exceptionUnifier;

    /**
     * Captura manual con multi-catch
     */
    public Order getOrder(String orderId) {
        try {
            LOG.infof("Fetching order: %s", orderId);
            return ordersApi.getOrder(orderId);
            
        } catch (com.example.exception.ApiException e) {
            // Ya es nuestra ApiException
            LOG.errorf("API error: %s - %s", e.getCode(), e.getDescription());
            throw e;
            
        } catch (Exception e) {
            // Convertir ApiException generada u otra excepción
            LOG.errorf("Converting exception: %s", e.getClass().getName());
            throw exceptionUnifier.unify(e);
        }
    }

    /**
     * Multi-catch alternativo (Java 7+)
     */
    public Order createOrder(Order order) {
        try {
            return ordersApi.createOrder(order);
            
        } catch (com.example.exception.ApiException 
               | org.openapitools.client.ApiException e) {
            // Captura ambas en un solo catch
            throw exceptionUnifier.unify((Exception) e);
        } catch (Exception e) {
            throw exceptionUnifier.unify(e);
        }
    }
}
```


## 5. Versión con Aspect (AOP) - Automático

```java
package com.example.aspect;

import com.example.exception.ApiException;
import com.example.exception.unifier.ExceptionUnifier;
import io.quarkus.arc.ArcInvocationContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para marcar métodos que necesitan unificación de excepciones
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface UnifyExceptions {
}

/**
 * Interceptor que unifica excepciones automáticamente
 */
@UnifyExceptions
@Interceptor
@Priority(1000)
class ExceptionUnifierInterceptor {

    private static final Logger LOG = Logger.getLogger(ExceptionUnifierInterceptor.class);

    @Inject
    ExceptionUnifier exceptionUnifier;

    @AroundInvoke
    public Object unifyExceptions(InvocationContext context) throws Exception {
        try {
            return context.proceed();
        } catch (com.example.exception.ApiException e) {
            // Ya es nuestra ApiException
            throw e;
        } catch (Exception e) {
            // Unificar cualquier otra excepción
            LOG.debugf("Intercepted exception in %s.%s: %s",
                context.getMethod().getDeclaringClass().getSimpleName(),
                context.getMethod().getName(),
                e.getClass().getName());
            throw exceptionUnifier.unify(e);
        }
    }
}
```


### Uso del Interceptor:

```java
package com.example.client.service;

import com.example.aspect.UnifyExceptions;
import com.example.generated.client.products.api.ProductsApi;
import com.example.generated.client.products.model.Product;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
@UnifyExceptions  // Aplicar a toda la clase
public class ProductsServiceClientWithAOP {

    @RestClient
    ProductsApi productsApi;

    // No necesita try-catch, el interceptor lo maneja
    public Product getProduct(String productId) {
        return productsApi.getProduct(productId);
    }

    public Product createProduct(Product product) {
        return productsApi.createProduct(product);
    }
}
```


## 6. ClientExceptionMapperUnified.java - Versión Mejorada del Mapper

```java
package com.example.client.mapper;

import com.example.dto.ApiErrorResponseDTO;
import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Mapper mejorado que siempre retorna nuestra ApiException
 * Ya no necesitas capturar dos tipos diferentes
 */
public class ClientExceptionMapperUnified implements ResponseExceptionMapper<ApiException> {

    private static final Logger LOG = Logger.getLogger(ClientExceptionMapperUnified.class);

    @Inject
    ApiExceptionFactory exceptionFactory;

    @Override
    public ApiException toThrowable(Response response) {
        if (response.getStatus() < 400) {
            return null;
        }

        try {
            // Intentar parsear como nuestro formato
            if (response.hasEntity()) {
                // Primero intentar como ApiErrorResponseDTO
                try {
                    ApiErrorResponseDTO errorDTO = response.readEntity(ApiErrorResponseDTO.class);
                    return convertFromDTO(errorDTO);
                } catch (Exception e) {
                    // Si falla, intentar como Map genérico
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> errorMap = response.readEntity(Map.class);
                        return convertFromMap(errorMap, response.getStatus());
                    } catch (Exception ex) {
                        LOG.warnf("Could not parse error response: %s", ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error processing response");
        }

        // Fallback: crear desde HTTP status
        return createFromHttpStatus(response);
    }

    @Override
    public int getPriority() {
        return 50; // Alta prioridad
    }

    private ApiException convertFromDTO(ApiErrorResponseDTO errorDTO) {
        ExceptionCategoryTypes category = mapCategoryFromString(errorDTO.category());
        
        ApiExceptionBuilder builder = exceptionFactory.builder(category);
        
        if (errorDTO.code() != null) {
            builder.code(errorDTO.code());
        }
        if (errorDTO.description() != null) {
            builder.description(errorDTO.description());
        }
        if (errorDTO.errorType() != null) {
            builder.errorType(errorDTO.errorType());
        }
        if (errorDTO.properties() != null) {
            builder.properties(errorDTO.properties());
        }
        
        builder.properties("remoteErrorId", errorDTO.errorId());
        builder.properties("source", "unified-rest-client");
        
        return builder.build();
    }

    private ApiException convertFromMap(Map<String, Object> errorMap, int statusCode) {
        String code = (String) errorMap.get("code");
        String description = (String) errorMap.get("message");
        
        ExceptionCategoryTypes category = mapHttpStatusToCategory(statusCode);
        
        return exceptionFactory.builder(category)
            .code(code != null ? code : "ERR_" + statusCode)
            .description(description != null ? description : "Remote error")
            .properties(errorMap)
            .build();
    }

    private ApiException createFromHttpStatus(Response response) {
        ExceptionCategoryTypes category = mapHttpStatusToCategory(response.getStatus());
        
        return exceptionFactory.builder(category)
            .properties("httpStatus", response.getStatus())
            .addDetail()
                .code("HTTP_" + response.getStatus())
                .component("UnifiedRestClient")
                .description("Remote service returned HTTP " + response.getStatus())
                .push()
            .build();
    }

    private ExceptionCategoryTypes mapCategoryFromString(String categoryName) {
        if (categoryName == null) {
            return ExceptionCategoryTypes.EXTERNAL_ERROR;
        }
        return switch (categoryName.toLowerCase().replace("_", "-")) {
            case "invalid-request" -> ExceptionCategoryTypes.INVALID_REQUEST;
            case "unauthorized" -> ExceptionCategoryTypes.UNAUTHORIZED;
            case "forbidden" -> ExceptionCategoryTypes.FORBIDDEN;
            case "resource-not-found" -> ExceptionCategoryTypes.RESOURCE_NOT_FOUND;
            case "conflict" -> ExceptionCategoryTypes.CONFLICT;
            case "external-error" -> ExceptionCategoryTypes.EXTERNAL_ERROR;
            case "service-unavailable" -> ExceptionCategoryTypes.SERVICE_UNAVAILABLE;
            default -> ExceptionCategoryTypes.EXTERNAL_ERROR;
        };
    }

    private ExceptionCategoryTypes mapHttpStatusToCategory(int status) {
        return switch (status) {
            case 400 -> ExceptionCategoryTypes.INVALID_REQUEST;
            case 401 -> ExceptionCategoryTypes.UNAUTHORIZED;
            case 403 -> ExceptionCategoryTypes.FORBIDDEN;
            case 404 -> ExceptionCategoryTypes.RESOURCE_NOT_FOUND;
            case 409 -> ExceptionCategoryTypes.CONFLICT;
            case 503 -> ExceptionCategoryTypes.SERVICE_UNAVAILABLE;
            case 504 -> ExceptionCategoryTypes.EXTERNAL_TIMEOUT;
            default -> ExceptionCategoryTypes.EXTERNAL_ERROR;
        };
    }
}
```


## 7. Uso Final Simplificado

```java
package com.example.client.service;

import com.example.client.wrapper.RestClientCaller;
import com.example.exception.ApiException;
import com.example.exception.ExceptionCategoryTypes;
import com.example.generated.client.products.api.ProductsApi;
import com.example.generated.client.products.model.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProductsServiceClientFinal {

    private static final Logger LOG = Logger.getLogger(ProductsServiceClientFinal.class);

    @RestClient
    ProductsApi productsApi;

    @Inject
    RestClientCaller caller;

    public Product getProduct(String productId) {
        // Solo captura ApiException - siempre será la tuya
        try {
            return caller.call(() -> productsApi.getProduct(productId));
        } catch (ApiException e) {
            // Aquí SIEMPRE llega tu ApiException unificada
            LOG.errorf("Error: code=%s, category=%s, desc=%s",
                e.getCode(),
                e.getCategory().propertyName(),
                e.getDescription());
            
            // Manejar según categoría
            if (e.getCategory() == ExceptionCategoryTypes.RESOURCE_NOT_FOUND) {
                LOG.warn("Product not found, returning null");
                return null;
            }
            
            throw e;
        }
    }
}
```

Con estas implementaciones, **siempre capturarás un solo tipo de ApiException** (la tuya), sin importar si el error viene de:

1. Tu `ApiException` personalizada
2. La `ApiException` generada por OpenAPI Generator
3. Cualquier otra excepción del REST Client

El `ExceptionUnifier` + `RestClientCaller` o el `Interceptor` se encargan de unificar todo automáticamente.[^7_2][^7_3][^7_4][^7_1]
<span style="display:none">[^7_10][^7_11][^7_5][^7_6][^7_7][^7_8][^7_9]</span>

<div align="center">⁂</div>

[^7_1]: https://rollbar.com/blog/how-to-catch-multiple-exceptions-in-java/

[^7_2]: https://es.quarkus.io/guides/rest-client

[^7_3]: https://www.centron.de/en/tutorial/java-catch-multiple-exceptions-rethrow-exception-tutorial/

[^7_4]: WhatsApp-Image-2025-11-04-at-12.28.16-AM.jpg

[^7_5]: https://openapi-generator.tech/docs/generators/java/

[^7_6]: https://stackoverflow.com/questions/76068635/how-to-name-classes-created-by-openapi-generator-and-get-map-in-response

[^7_7]: https://openapi-generator.tech/docs/templating

[^7_8]: https://github.com/OpenAPITools/openapi-generator/issues/16403

[^7_9]: https://www.baeldung.com/java-openapi-generator-server

[^7_10]: https://ademico-software.com/how-to-generate-the-client-api-for-an-openapi-rest-service/

[^7_11]: https://github.com/quarkusio/quarkus/issues/22936


---

# en mi rpoyecto tengo el controller que es el resource donde llamo a los servicios, en los servicios tengo logica y llamo a daos (redis, restclients microproifles proxys generados que tienes un apiexception del generador que automatico les pone)

Perfecto, entiendo tu arquitectura en capas. Te voy a dar la solución completa para manejar excepciones en toda tu arquitectura **Resource → Service → DAO (Redis/REST Clients)**:[^8_1][^8_2][^8_3]

## Arquitectura de Capas con Manejo de Excepciones

```
┌─────────────────────────────────────────────────┐
│  Resource (Controller)                          │
│  - Punto de entrada REST                        │
│  - Captura ApiException del Service             │
│  - GlobalExceptionMapper convierte a Response   │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Service (Lógica de Negocio)                    │
│  - Validaciones                                 │
│  - Orquestación                                 │
│  - Lanza ApiException (factory)                 │
│  - Captura excepciones de DAOs y las unifica    │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  DAO Layer                                      │
│  - Redis DAO → RedisException                   │
│  - REST Client Proxy → ApiException generada    │
│  - Unificador convierte todo a ApiException     │
└─────────────────────────────────────────────────┘
```


## 1. RedisDAO.java - Capa de Acceso a Redis

```java
package com.example.dao;

import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;

@ApplicationScoped
public class ProductRedisDAO {

    private static final Logger LOG = Logger.getLogger(ProductRedisDAO.class);

    @Inject
    ApiExceptionFactory exceptionFactory;

    private final ValueCommands<String, String> commands;

    public ProductRedisDAO(RedisDataSource dataSource) {
        this.commands = dataSource.value(String.class, String.class);
    }

    /**
     * Obtener producto desde Redis
     * Convierte excepciones de Redis a ApiException
     */
    public String getProduct(String productId) {
        try {
            LOG.infof("Getting product from Redis: %s", productId);
            String product = commands.get("product:" + productId);
            
            if (product == null) {
                throw exceptionFactory.builder(ExceptionCategoryTypes.RESOURCE_NOT_FOUND)
                    .properties("productId", productId)
                    .properties("source", "redis")
                    .addDetail()
                        .code("REDIS_001")
                        .component("ProductRedisDAO")
                        .description("Product not found in Redis cache")
                        .push()
                    .build();
            }
            
            return product;
            
        } catch (ApiException e) {
            // Ya es nuestra ApiException, re-lanzar
            throw e;
            
        } catch (Exception e) {
            // Convertir excepciones de Redis a ApiException
            LOG.errorf(e, "Redis error getting product: %s", productId);
            throw exceptionFactory.create(
                ExceptionCategoryTypes.EXTERNAL_ERROR,
                "Failed to retrieve product from cache",
                Map.of(
                    "productId", productId,
                    "source", "redis",
                    "redisError", e.getMessage()
                )
            );
        }
    }

    /**
     * Guardar producto en Redis
     */
    public void saveProduct(String productId, String productData) {
        try {
            LOG.infof("Saving product to Redis: %s", productId);
            commands.set("product:" + productId, productData);
            
        } catch (Exception e) {
            LOG.errorf(e, "Redis error saving product: %s", productId);
            throw exceptionFactory.create(
                ExceptionCategoryTypes.EXTERNAL_ERROR,
                "Failed to save product to cache",
                e
            );
        }
    }

    /**
     * Eliminar producto de Redis
     */
    public void deleteProduct(String productId) {
        try {
            LOG.infof("Deleting product from Redis: %s", productId);
            Long deleted = commands.del("product:" + productId);
            
            if (deleted == 0) {
                throw exceptionFactory.create(
                    ExceptionCategoryTypes.RESOURCE_NOT_FOUND,
                    "Product not found in cache for deletion",
                    Map.of("productId", productId, "source", "redis")
                );
            }
            
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, "Redis error deleting product: %s", productId);
            throw exceptionFactory.create(
                ExceptionCategoryTypes.EXTERNAL_ERROR,
                "Failed to delete product from cache",
                e
            );
        }
    }
}
```


## 2. RestClientDAO.java - Wrapper para REST Clients Generados

```java
package com.example.dao;

import com.example.exception.*;
import com.example.exception.unifier.ExceptionUnifier;
import com.example.generated.client.external.api.ExternalProductsApi;
import com.example.generated.client.external.model.ExternalProduct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * DAO que encapsula las llamadas a REST Clients generados por OpenAPI
 * Captura tanto ApiException generada como la nuestra
 */
@ApplicationScoped
public class ExternalProductDAO {

    private static final Logger LOG = Logger.getLogger(ExternalProductDAO.class);

    @RestClient
    ExternalProductsApi externalProductsApi;

    @Inject
    ExceptionUnifier exceptionUnifier;

    /**
     * Obtener producto externo
     * Unifica excepciones del cliente generado
     */
    public ExternalProduct getExternalProduct(String productId) {
        try {
            LOG.infof("Calling external API for product: %s", productId);
            return externalProductsApi.getProduct(productId);
            
        } catch (com.example.exception.ApiException e) {
            // Ya es nuestra ApiException (del ResponseExceptionMapper)
            LOG.debugf("Caught our ApiException from REST client: %s", e.getCode());
            throw e;
            
        } catch (Exception e) {
            // Convertir ApiException generada u otra excepción
            LOG.errorf("Caught exception from REST client, unifying: %s", e.getClass().getName());
            throw exceptionUnifier.unify(e);
        }
    }

    /**
     * Crear producto externo
     */
    public ExternalProduct createExternalProduct(ExternalProduct product) {
        try {
            LOG.infof("Creating product in external API: %s", product.getName());
            return externalProductsApi.createProduct(product);
            
        } catch (com.example.exception.ApiException e) {
            throw e;
        } catch (Exception e) {
            throw exceptionUnifier.unify(e);
        }
    }
}
```


## 3. ProductService.java - Capa de Servicio con Lógica de Negocio

```java
package com.example.service;

import com.example.dao.*;
import com.example.dto.*;
import com.example.exception.*;
import com.example.exception.factory.ApiExceptionFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Servicio con lógica de negocio
 * Orquesta llamadas a DAOs (Redis + REST Clients)
 * Lanza ApiException que será capturada por el Resource
 */
@ApplicationScoped
public class ProductService {

    private static final Logger LOG = Logger.getLogger(ProductService.class);

    @Inject
    ApiExceptionFactory exceptionFactory;

    @Inject
    ProductRedisDAO redisDAO;

    @Inject
    ExternalProductDAO externalProductDAO;

    /**
     * Obtener producto - intenta Redis primero, luego API externa
     */
    public ProductDTO getProduct(String productId) {
        LOG.infof("Getting product: %s", productId);
        
        // Validación de entrada
        if (productId == null || productId.isBlank()) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.INVALID_REQUEST,
                "Product ID cannot be blank",
                Map.of("field", "productId")
            );
        }

        try {
            // 1. Intentar obtener de Redis (cache)
            String cachedProduct = redisDAO.getProduct(productId);
            LOG.infof("Product found in cache: %s", productId);
            return parseProduct(cachedProduct);
            
        } catch (ApiException e) {
            // Si no está en cache, buscar en API externa
            if (e.getCategory() == ExceptionCategoryTypes.RESOURCE_NOT_FOUND) {
                LOG.infof("Product not in cache, fetching from external API: %s", productId);
                return getProductFromExternalAPI(productId);
            }
            
            // Otro error de Redis, re-lanzar
            throw e;
        }
    }

    /**
     * Obtener producto de API externa y guardarlo en cache
     */
    private ProductDTO getProductFromExternalAPI(String productId) {
        try {
            var externalProduct = externalProductDAO.getExternalProduct(productId);
            
            // Guardar en cache para próximas consultas
            try {
                redisDAO.saveProduct(productId, externalProduct.toString());
            } catch (ApiException cacheError) {
                // Log pero no fallar si el cache falla
                LOG.warnf("Failed to cache product, continuing: %s", cacheError.getMessage());
            }
            
            // Mapear a nuestro DTO
            return mapToDTO(externalProduct);
            
        } catch (ApiException e) {
            // La excepción ya viene unificada del DAO
            LOG.errorf("Failed to get product from external API: %s", e.getDescription());
            
            // Agregar contexto de servicio
            throw exceptionFactory.builder(e.getCategory())
                .code(e.getCode())
                .description(e.getDescription())
                .errorType(e.getErrorType())
                .properties(e.getProperties())
                .properties("serviceLayer", "ProductService")
                .properties("operation", "getProductFromExternalAPI")
                .properties("productId", productId)
                .exceptionDetails(e.getExceptionDetails())
                .cause(e)
                .build();
        }
    }

    /**
     * Crear producto - validaciones de negocio + persistencia
     */
    @Transactional
    public ProductDTO createProduct(CreateProductRequest request) {
        LOG.infof("Creating product: %s", request.name());
        
        // Validaciones de negocio
        validateProductRequest(request);
        
        // Verificar duplicados en cache
        try {
            redisDAO.getProduct(request.sku());
            // Si encuentra, ya existe
            throw exceptionFactory.create(
                ExceptionCategoryTypes.CONFLICT,
                "Product with this SKU already exists",
                Map.of(
                    "sku", request.sku(),
                    "name", request.name()
                )
            );
        } catch (ApiException e) {
            if (e.getCategory() != ExceptionCategoryTypes.RESOURCE_NOT_FOUND) {
                // Error diferente a "no encontrado", re-lanzar
                throw e;
            }
            // No existe, continuar
        }

        try {
            // Crear en API externa
            var externalProduct = externalProductDAO.createExternalProduct(
                mapToExternalProduct(request)
            );
            
            // Guardar en cache
            redisDAO.saveProduct(request.sku(), externalProduct.toString());
            
            return mapToDTO(externalProduct);
            
        } catch (ApiException e) {
            LOG.errorf("Failed to create product: %s", e.getDescription());
            throw e;
        }
    }

    /**
     * Actualizar producto - orquestación de múltiples DAOs
     */
    @Transactional
    public ProductDTO updateProduct(String productId, UpdateProductRequest request) {
        LOG.infof("Updating product: %s", productId);
        
        // Validaciones
        if (productId == null || productId.isBlank()) {
            throw exceptionFactory.create(ExceptionCategoryTypes.INVALID_REQUEST);
        }
        
        if (request.price() != null && request.price() < 0) {
            throw exceptionFactory.builder(ExceptionCategoryTypes.ARGUMENT_MISMATCH)
                .addDetail()
                    .code("VAL_001")
                    .component("ProductService")
                    .description("Price must be positive")
                    .push()
                .properties("field", "price")
                .properties("value", request.price())
                .build();
        }

        try {
            // 1. Verificar que existe
            getProduct(productId);
            
            // 2. Actualizar en API externa (lógica simulada)
            // var updated = externalProductDAO.updateProduct(productId, ...);
            
            // 3. Invalidar cache
            try {
                redisDAO.deleteProduct(productId);
            } catch (ApiException e) {
                LOG.warnf("Failed to invalidate cache: %s", e.getMessage());
            }
            
            // 4. Retornar actualizado
            return getProduct(productId);
            
        } catch (ApiException e) {
            throw e;
        }
    }

    /**
     * Eliminar producto - orquestación con rollback manual
     */
    @Transactional
    public void deleteProduct(String productId) {
        LOG.infof("Deleting product: %s", productId);
        
        boolean cacheDeleted = false;
        
        try {
            // 1. Eliminar de cache
            redisDAO.deleteProduct(productId);
            cacheDeleted = true;
            
            // 2. Eliminar de API externa
            // externalProductDAO.deleteProduct(productId);
            
            LOG.infof("Product deleted successfully: %s", productId);
            
        } catch (ApiException e) {
            // Si ya eliminamos el cache, intentar rollback
            if (cacheDeleted) {
                LOG.warnf("Attempting to rollback cache deletion for: %s", productId);
                // Lógica de compensación
            }
            throw e;
        }
    }

    // ============ Métodos de Validación ============

    private void validateProductRequest(CreateProductRequest request) {
        if (request.name() == null || request.name().length() < 3) {
            throw exceptionFactory.builder(ExceptionCategoryTypes.ARGUMENT_MISMATCH)
                .addDetail()
                    .code("VAL_001")
                    .component("ProductService")
                    .description("Product name must be at least 3 characters")
                    .push()
                .properties("field", "name")
                .properties("providedLength", request.name() != null ? request.name().length() : 0)
                .properties("minimumLength", 3)
                .build();
        }

        if (request.price() < 0) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.INVALID_REQUEST,
                "Price must be positive",
                Map.of("field", "price", "value", request.price())
            );
        }

        if (request.sku() == null || !request.sku().matches("^[A-Z0-9-]+$")) {
            throw exceptionFactory.create(
                ExceptionCategoryTypes.ARGUMENT_MISMATCH,
                "Invalid SKU format. Must contain only uppercase letters, numbers and dashes",
                Map.of("field", "sku", "value", request.sku())
            );
        }
    }

    // ============ Métodos de Mapeo ============

    private ProductDTO parseProduct(String json) {
        // Lógica de parseo
        return new ProductDTO("id", "name", 100.0, "SKU");
    }

    private ProductDTO mapToDTO(Object externalProduct) {
        // Mapeo
        return new ProductDTO("id", "name", 100.0, "SKU");
    }

    private Object mapToExternalProduct(CreateProductRequest request) {
        // Mapeo
        return new Object();
    }
}
```


## 4. ProductResource.java - Capa de Controller/Resource

```java
package com.example.resource;

import com.example.dto.*;
import com.example.exception.*;
import com.example.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * Resource (Controller) que expone endpoints REST
 * Solo captura ApiException si necesita agregar contexto HTTP
 * GlobalExceptionMapper convierte ApiException a Response
 */
@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    private static final Logger LOG = Logger.getLogger(ProductResource.class);

    @Inject
    ProductService productService;

    /**
     * Obtener producto
     * No captura ApiException - deja que GlobalExceptionMapper lo maneje
     */
    @GET
    @Path("/{id}")
    public ProductDTO getProduct(@PathParam("id") String id) {
        LOG.infof("GET /api/products/%s", id);
        // El service lanza ApiException si hay error
        // GlobalExceptionMapper la convierte a Response
        return productService.getProduct(id);
    }

    /**
     * Crear producto
     */
    @POST
    public Response createProduct(CreateProductRequest request) {
        LOG.infof("POST /api/products - %s", request.name());
        ProductDTO created = productService.createProduct(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * Actualizar producto
     */
    @PUT
    @Path("/{id}")
    public ProductDTO updateProduct(
            @PathParam("id") String id,
            UpdateProductRequest request) {
        LOG.infof("PUT /api/products/%s", id);
        return productService.updateProduct(id, request);
    }

    /**
     * Eliminar producto
     */
    @DELETE
    @Path("/{id}")
    public Response deleteProduct(@PathParam("id") String id) {
        LOG.infof("DELETE /api/products/%s", id);
        productService.deleteProduct(id);
        return Response.noContent().build();
    }

    /**
     * Ejemplo: captura manual si necesitas agregar contexto HTTP específico
     */
    @POST
    @Path("/batch")
    public Response createBatchProducts(BatchCreateRequest request) {
        try {
            LOG.infof("POST /api/products/batch - %d products", request.products().size());
            // Lógica de batch
            return Response.ok().build();
            
        } catch (ApiException e) {
            // Agregar headers HTTP específicos antes de re-lanzar
            LOG.errorf("Batch creation failed: %s", e.getDescription());
            
            // Re-lanzar con headers adicionales
            throw new ApiExceptionBuilder()
                .code(e.getCode())
                .description(e.getDescription())
                .errorType(e.getErrorType())
                .category(e.getCategory())
                .properties(e.getProperties())
                .properties("batchSize", request.products().size())
                .exceptionDetails(e.getExceptionDetails())
                .headers("X-Batch-Failed", "true")
                .headers("X-Retry-After", "60")
                .cause(e)
                .build();
        }
    }
}
```


## 5. DTOs

```java
package com.example.dto;

public record ProductDTO(
    String id,
    String name,
    Double price,
    String sku
) {}

public record CreateProductRequest(
    String name,
    Double price,
    String sku
) {}

public record UpdateProductRequest(
    String name,
    Double price
) {}

public record BatchCreateRequest(
    java.util.List<CreateProductRequest> products
) {}
```


## 6. application.yaml - Configuración Completa

```yaml
quarkus:
  # Redis
  redis:
    hosts: ${REDIS_HOST:redis://localhost:6379}
    timeout: 5s
    
  # REST Clients
  rest-client:
    external-products-api:
      url: ${EXTERNAL_PRODUCTS_API_URL:http://localhost:8081}
      scope: jakarta.enterprise.context.ApplicationScoped
      
    provider-autodiscovery: true

  # OpenAPI
  smallrye-openapi:
    path: /openapi
    
  # Logging
  log:
    category:
      "com.example":
        level: DEBUG
      "com.example.dao":
        level: DEBUG
      "com.example.service":
        level: INFO

# Configuración de excepciones
api:
  exceptions:
    invalid-request:
      code: "ERR_400_001"
      description: "Invalid request parameters"
      error-type: "ValidationError"
    resource-not-found:
      code: "ERR_404_001"
      description: "Resource not found"
      error-type: "NotFoundError"
    conflict:
      code: "ERR_409_001"
      description: "Resource conflict"
      error-type: "ConflictError"
    external-error:
      code: "ERR_500_001"
      description: "External service error"
      error-type: "ExternalError"
```


## Flujo de Excepciones

```
1. DAO Layer (Redis/REST Client)
   ↓ ApiException generada o Redis Exception
   
2. ExceptionUnifier convierte a ApiException
   ↓ ApiException unificada
   
3. Service Layer captura y agrega contexto
   ↓ ApiException con contexto de negocio
   
4. Resource deja pasar (o agrega contexto HTTP)
   ↓ ApiException
   
5. GlobalExceptionMapper convierte a Response
   ↓ Response HTTP con JSON según contrato OpenAPI
```

Con esta arquitectura:

- **DAOs**: Convierten sus excepciones específicas (Redis, ApiException generada) a tu `ApiException`[^8_4][^8_3]
- **Services**: Agregan lógica de negocio y contexto[^8_1]
- **Resources**: Dejan pasar o agregan contexto HTTP[^8_5][^8_2]
- **Mappers**: Convierten `ApiException` a respuestas HTTP automáticamente[^8_2][^8_6][^8_1]
<span style="display:none">[^8_10][^8_11][^8_7][^8_8][^8_9]</span>

<div align="center">⁂</div>

[^8_1]: https://www.the-main-thread.com/p/quarkus-rfc7807-error-handling-java

[^8_2]: https://developers.redhat.com/articles/2022/03/03/rest-api-error-modeling-quarkus-20

[^8_3]: https://stackoverflow.com/questions/56976311/error-propagation-in-microservices-architecture

[^8_4]: https://es.quarkus.io/guides/redis-reference

[^8_5]: https://es.quarkus.io/guides/rest

[^8_6]: WhatsApp-Image-2025-11-04-at-12.28.16-AM.jpg

[^8_7]: https://stackoverflow.com/questions/79104491/handling-multiple-exceptions-with-mutiny-in-quarkus-service-layer

[^8_8]: https://es.quarkus.io/guides/

[^8_9]: https://www.youtube.com/watch?v=e0UVVCPwabk

[^8_10]: https://stackoverflow.com/questions/75228448/quarkus-native-build-random-splittablerandom-exception-with-vert-x-redis-client

[^8_11]: https://www.youtube.com/watch?v=zqUiOBng_5M

