mvn io.quarkus.platform:quarkus-maven-plugin:3.19.1:create ^
  -DprojectGroupId=com.jonavcar ^
  -DprojectArtifactId=ms-quarkus ^
  -DclassName="com.jonavcar.HelloResource" ^
  -Dpath="/hello" ^
  -Dextensions="resteasy-jackson"