No, mi respuesta anterior era para **Quarkus OpenAPI Generator** (quarkiverse), no para el `org.openapitools` estándar. Para tu caso con **openapi-generator-maven-plugin 7.13.0** la configuración es diferente.[1][2]

## Solución 1: schemaMappings en pom.xml (Recomendada)

Para `org.openapitools:openapi-generator-maven-plugin:7.13.0` debes usar **schemaMappings** en lugar de importMappings, ya que importMappings no funciona correctamente en Maven:[1]

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.13.0</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/openapi/api-contract.yaml</inputSpec>
                <generatorName>java</generatorName>
                <output>${project.build.directory}/generated-sources/openapi</output>
                
                <!-- Base packages -->
                <apiPackage>com.example.api</apiPackage>
                <modelPackage>com.example.model</modelPackage>
                <invokerPackage>com.example.client</invokerPackage>
                
                <!-- Schema Mapping: mapea ApiException a tu librería -->
                <schemaMappings>ApiException=com.example.library.exception.ApiException</schemaMappings>
                
                <!-- Desabilitar generación de tests -->
                <generateApiTests>false</generateApiTests>
                <generateModelTests>false</generateModelTests>
                <generateModelDocumentation>false</generateModelDocumentation>
                <generateApiDocumentation>false</generateApiDocumentation>
                
                <configOptions>
                    <!-- Quarkus + JDK 21 imperativo -->
                    <library>resteasy</library>
                    <openApiNullable>true</openApiNullable>
                    <dateLibrary>java8</dateLibrary>
                    <java8>true</java8>
                    <serializationLibrary>jackson</serializationLibrary>
                    <interfaceOnly>true</interfaceOnly>
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>org.openapitools</groupId>
            <artifactId>openapi-generator-cli</artifactId>
            <version>7.13.0</version>
        </dependency>
    </dependencies>
</plugin>
```

## Configuración completa para Quarkus JDK 21 Imperativo

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.13.0</version>
    <executions>
        <execution>
            <id>generate-api-client</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/openapi/api-contract.yaml</inputSpec>
                <generatorName>java</generatorName>
                <output>${project.build.directory}/generated-sources/openapi</output>
                
                <!-- Mapear ApiException de tu librería -->
                <schemaMappings>
                    ApiException=com.example.library.exception.ApiException,
                    ErrorResponse=com.example.library.dto.ErrorResponse
                </schemaMappings>
                
                <!-- Packages -->
                <apiPackage>com.example.api.client</apiPackage>
                <modelPackage>com.example.api.model</modelPackage>
                <invokerPackage>com.example.api.client.invoker</invokerPackage>
                
                <!-- No generar tests ni documentación -->
                <generateApiTests>false</generateApiTests>
                <generateModelTests>false</generateModelTests>
                <generateModelDocumentation>false</generateModelDocumentation>
                <generateApiDocumentation>false</generateApiDocumentation>
                
                <configOptions>
                    <!-- Para Quarkus con RESTEasy (es parte de Quarkus) -->
                    <library>resteasy</library>
                    
                    <!-- JDK 21 imperativo -->
                    <java8>true</java8>
                    
                    <!-- Serialización con Jackson -->
                    <serializationLibrary>jackson</serializationLibrary>
                    
                    <!-- Generar solo interfaces (client-side) -->
                    <interfaceOnly>true</interfaceOnly>
                    
                    <!-- Usar nullables de OpenAPI -->
                    <openApiNullable>true</openApiNullable>
                    
                    <!-- Fechas como LocalDateTime -->
                    <dateLibrary>java8</dateLibrary>
                    
                    <!-- Documentación y anotaciones -->
                    <documentation>false</documentation>
                    <useBeanValidation>true</useBeanValidation>
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>org.openapitools</groupId>
            <artifactId>openapi-generator-cli</artifactId>
            <version>7.13.0</version>
        </dependency>
    </dependencies>
</plugin>
```

## Múltiples esquemas mapeados

```xml
<schemaMappings>
    ApiException=com.example.library.exception.ApiException,
    ErrorResponse=com.example.library.dto.ErrorResponse,
    PagedResponse=com.example.library.dto.PagedResponse
</schemaMappings>
```

## Solución 2: Archivos a ignorar (.openapi-generator-ignore)

Complementario: crear archivo en raíz del proyecto:[3][4]

```bash
# .openapi-generator-ignore

# Excluir el modelo ApiException
**/model/ApiException.java

# Excluir cualquier modelo de error
**/model/*Error*.java
```

## Tu ResponseExceptionMapper en la librería

```java
package com.example.library.mapper;

import com.example.library.exception.ApiException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.logging.Logger;

@Provider
public class ApiExceptionMapper implements ResponseExceptionMapper<ApiException> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class);

    @Override
    public ApiException toThrowable(Response response) {
        int status = response.getStatus();
        String body = response.readEntity(String.class);
        
        LOG.errorf("Error en proxy: Status %d - %s", status, body);
        
        return new ApiException(status, "Error HTTP: " + status, body);
    }

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status >= 400;
    }
}
```

## Tu RestClient en el microservicio

```java
package com.example.service.client;

import com.example.api.client.ProductsApi;
import com.example.library.mapper.ApiExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "products-api")
@RegisterProvider(ApiExceptionMapper.class)
public interface ProductsClient extends ProductsApi {
    // Hereda todos los métodos del interface generado
}
```

## Verificar la generación

Después de `mvn clean compile`, el generador creará interfaces imperativos normales en `target/generated-sources/openapi/` usando tu `ApiException` de la librería:

```java
@Path("/api/products")
public interface ProductsApi {
    
    @GET
    @Produces("application/json")
    List<Product> getAllProducts() throws ApiException; // Tu excepción
    
    @GET
    @Path("/{id}")
    Product getById(@PathParam("id") Long id) throws ApiException;
}
```

**La configuración `schemaMappings` en pom.xml es la correcta para `org.openapitools:openapi-generator-maven-plugin:7.13.0`** y funcionará con Quarkus JDK 21 imperativo sin problemas.[2][3][1]

[1](https://stackoverflow.com/questions/75825152/openapi-generator-maven-plugin-doesnt-use-import-mappings)
[2](https://openapi-generator.tech/docs/usage/)
[3](https://openapi-generator.tech/docs/customization/)
[4](https://github.com/OpenAPITools/openapi-generator/issues/3563)
[5](https://github.com/OpenAPITools/openapi-generator)
[6](https://github.com/OpenAPITools/openapi-generator/issues/11506)
[7](https://stackoverflow.com/questions/76720790/how-to-set-type-mappings-for-openapi-generator-in-build-gradle-file)
[8](https://www.baeldung.com/spring-boot-openapi-generator-custom-templates)
[9](https://mvnrepository.com/artifact/org.openapitools/openapi-generator-maven-plugin/7.13.0)
[10](https://openapi-generator.tech/docs/generators/java)
[11](https://dzone.com/articles/openapi-extend-functionality-of-generator-plugin-u)