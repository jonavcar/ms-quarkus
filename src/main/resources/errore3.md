Perfecto, la solución es usar **`.openapi-generator-ignore`** para excluir la generación del `ApiExceptionMapper`. Así el generador solo usará tu `ApiException` de la librería sin crear un mapper nuevo:[1]

## Configuración en pom.xml

```xml
<execution>
    <id>generate-products-client</id>
    <phase>generate-sources</phase>
    <goals>
        <goal>generate</goal>
    </goals>
    <configuration>
        <inputSpec>${project.basedir}/src/main/openapi/products-contract.yaml</inputSpec>
        <generatorName>java</generatorName>
        <output>${project.build.directory}/generated-sources/openapi</output>
        
        <!-- Mapear ApiException a tu librería -->
        <schemaMappings>ApiException=com.example.library.exception.ApiException</schemaMappings>
        
        <!-- Usar archivo ignore para no generar ApiExceptionMapper -->
        <ignoreFileOverride>.openapi-generator-ignore</ignoreFileOverride>
        
        <apiPackage>com.example.api.client.products</apiPackage>
        <modelPackage>com.example.api.model.products</modelPackage>
        
        <generateApiTests>false</generateApiTests>
        <generateModelTests>false</generateModelTests>
        
        <configOptions>
            <library>microprofile</library>
            <java8>true</java8>
            <serializationLibrary>jackson</serializationLibrary>
            <interfaceOnly>true</interfaceOnly>
        </configOptions>
    </configuration>
</execution>
```

## Archivo .openapi-generator-ignore

Crea este archivo en **la raíz del proyecto**:[1]

```bash
# .openapi-generator-ignore

# Excluir la generación de ApiExceptionMapper
**/ApiExceptionMapper.java
**/ApiException.java

# También excluir otros archivos de soporte si no los necesitas
README.md
.openapi-generator-metadata.yaml
```

## Resultado

Después de ejecutar `mvn clean compile`, el generador:

✅ Generará interfaces RestClient
✅ Usará tu `ApiException` de librería (mapeado)
❌ **NO generará un nuevo** `ApiExceptionMapper`
❌ **NO generará un nuevo** `ApiException`

Y al inyectar el `ProductsClient` en tu servicio, automáticamente usará el `ApiExceptionMapper` con `@Provider` de tu librería:[1]

```java
@ApplicationScoped
public class ProductService {
    
    @Inject
    @RestClient
    ProductsClient productsClient;  // Usa tu mapper de librería
    
    public List<Product> getAll() {
        return productsClient.getAll();  // Lanza tu ApiException
    }
}
```

Así tienes **un solo** `ApiException` y **un solo** `ApiExceptionMapper` global de la librería para todos los clientes.[2][1]

[1](https://stackoverflow.com/questions/76369664/how-openapi-generator-openapi-generator-ignore-works-on-openapi-generator-maven)
[2](https://openapi-generator.tech/docs/customization/)
[3](https://stackoverflow.com/questions/57464620/how-to-skip-generation-of-support-and-metadata-files-with-openapi-generator-and)
[4](https://github.com/OpenAPITools/openapi-generator/issues/3563)
[5](https://openapi-generator.tech/docs/generators/java)
[6](https://github.com/OpenAPITools/openapi-generator/issues/18897)
[7](https://docs.quarkiverse.io/quarkus-openapi-generator/dev/client.html)
[8](https://openapi-generator.tech/docs/faq-extending/)
[9](https://github.com/OpenAPITools/openapi-generator/issues/20135)
[10](https://openapi-generator.tech/docs/generators/java-microprofile/)