Para tu caso tienes **dos soluciones** efectivas: usar **type-mappings** para reemplazar el modelo generado por el de tu librería, o usar **.openapi-generator-ignore** para excluir la generación del ApiException.[1][2]

## Solución 1: Type Mappings (Recomendada)

Esta solución mapea el `ApiException` del contrato al `ApiException` de tu librería automáticamente:[1]

### Configuración en application.properties

```properties
# Asumiendo que tu archivo OpenAPI se llama api-contract.yaml
# y está en src/main/openapi/

# Mapear el tipo ApiException al de tu librería
quarkus.openapi-generator.codegen.spec.api_contract_yaml.type-mappings.ApiException=com.example.library.exception.ApiException

# Importar la clase de tu librería
quarkus.openapi-generator.codegen.spec.api_contract_yaml.import-mappings.ApiException=com.example.library.exception.ApiException

# Schema mapping (si es necesario para schemas complejos)
quarkus.openapi-generator.codegen.spec.api_contract_yaml.schema-mappings.ApiException=com.example.library.exception.ApiException
```

### Ejemplo Completo

**Tu librería tiene:**
```java
package com.example.library.exception;

public class ApiException extends RuntimeException {
    private int code;
    private String message;
    
    public ApiException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    // getters y setters
}
```

**Tu contrato OpenAPI (api-contract.yaml) define:**
```yaml
components:
  schemas:
    ApiException:
      type: object
      properties:
        code:
          type: integer
        message:
          type: string
```

**Configuración completa:**
```properties
# Base package
quarkus.openapi-generator.codegen.spec.api_contract_yaml.base-package=com.example.api

# Type mappings - usa tu ApiException de la librería
quarkus.openapi-generator.codegen.spec.api_contract_yaml.type-mappings.ApiException=com.example.library.exception.ApiException
quarkus.openapi-generator.codegen.spec.api_contract_yaml.import-mappings.ApiException=com.example.library.exception.ApiException
```

**El generador creará:**
```java
package com.example.api.api;

import com.example.library.exception.ApiException; // Usa tu clase
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/products")
@RegisterRestClient(configKey = "products-api")
public interface ProductsApi {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Product> getAll() throws ApiException; // Usa tu ApiException
}
```

## Solución 2: Archivo .openapi-generator-ignore

Si prefieres **excluir completamente** la generación del ApiException:[2][3]

### Crear archivo .openapi-generator-ignore

En la raíz de tu proyecto (`src/main/openapi/` o donde configures `input-base-dir`), crea el archivo `.openapi-generator-ignore`:

```bash
# .openapi-generator-ignore

# Excluir el modelo ApiException
**/model/ApiException.java

# O excluir todos los modelos de excepciones si tienes varios
**/model/*Exception.java
```

### Sintaxis del archivo ignore

```bash
# Comentarios comienzan con #

# Excluir archivo específico en cualquier ubicación
ApiException.java

# Excluir usando rutas específicas
src/gen/java/com/example/model/ApiException.java

# Usar wildcards
**/model/Api*.java

# Excluir directorios completos
docs/

# Excluir archivos por extensión
*.md
```

### Configuración adicional

```properties
# Configurar el directorio de entrada
quarkus.openapi-generator.codegen.input-base-dir=src/main/openapi

# Asegurarte de que skip-overwrite está en false para que el ignore funcione
quarkus.openapi-generator.codegen.spec.api_contract_yaml.skip-overwrite=false
```

## Solución 3: Combinación (Más robusta)

Para máxima compatibilidad, combina ambas soluciones:[4][1]

**application.properties:**
```properties
# Type mappings
quarkus.openapi-generator.codegen.spec.api_contract_yaml.type-mappings.ApiException=com.example.library.exception.ApiException
quarkus.openapi-generator.codegen.spec.api_contract_yaml.import-mappings.ApiException=com.example.library.exception.ApiException

# Si usas sufijos en los modelos generados
quarkus.openapi-generator.codegen.spec.api_contract_yaml.model-name-suffix=DTO

# En este caso también mapear con el sufijo
quarkus.openapi-generator.codegen.spec.api_contract_yaml.type-mappings.ApiExceptionDTO=com.example.library.exception.ApiException
```

**.openapi-generator-ignore:**
```bash
# Backup: excluir por si acaso
**/model/ApiException.java
**/model/ApiExceptionDTO.java
```

## ResponseExceptionMapper con tu ApiException

Tu `ResponseExceptionMapper` en la librería funcionará directamente:[5]

```java
package com.example.library.mapper;

import com.example.library.exception.ApiException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

@Provider
public class ApiExceptionMapper implements ResponseExceptionMapper<ApiException> {

    @Override
    public ApiException toThrowable(Response response) {
        int status = response.getStatus();
        String body = response.readEntity(String.class);
        
        return new ApiException(status, body);
    }

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status >= 400;
    }
}
```

## Registro del Mapper en tu RestClient

**En tu microservicio:**
```java
package com.example.service.client;

import com.example.api.api.ProductsApi;
import com.example.library.mapper.ApiExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "products-api")
@RegisterProvider(ApiExceptionMapper.class) // Registra tu mapper
public interface ProductsClient extends ProductsApi {
    // Hereda los métodos del interface generado
}
```

## Configuración Maven

Asegúrate de tener la dependencia correcta:[1]

```xml
<dependency>
    <groupId>io.quarkiverse.openapi.generator</groupId>
    <artifactId>quarkus-openapi-generator</artifactId>
    <version>3.0.0</version>
</dependency>

<!-- Tu librería con el ApiException -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>tu-libreria</artifactId>
    <version>1.0.0</version>
</dependency>
```

La **solución 1 (Type Mappings)** es la más elegante porque permite que el generador entienda que debe usar tu clase en lugar de crear una nueva. La **solución 2 (.openapi-generator-ignore)** es útil como complemento o cuando necesitas control más fino sobre archivos específicos.[3][2][1]

[1](https://docs.quarkiverse.io/quarkus-openapi-generator/dev/client.html)
[2](https://github.com/OpenAPITools/openapi-generator/issues/3563)
[3](https://openapi-generator.tech/docs/customization/)
[4](https://github.com/OpenAPITools/openapi-generator/issues/11478)
[5](https://es.quarkus.io/guides/rest-client)
[6](https://es.quarkus.io/guides/openapi-swaggerui)
[7](https://github.com/Backbase/openapi-with-quarkus)
[8](https://docs.quarkiverse.io/quarkus-openapi-generator/dev/server.html)
[9](https://stackoverflow.com/questions/67632442/how-can-i-generate-quarkus-rest-jx-rs-service-based-on-given-openapi-3-0-3-yaml)
[10](https://www.baeldung.com/spring-boot-openapi-generator-custom-templates)
[11](https://github.com/OpenAPITools/openapi-generator/issues/11506)