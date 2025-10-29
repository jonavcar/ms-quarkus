package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.util.Optional;

@ConfigMapping(prefix = "application.product-query-params")
public interface ProductQueryParamsConfig {

  @WithName("familyCode")
  Optional<String> familyCode();

  @WithName("filterFields")
  @WithDefault("0210")
  String filterFields();

  @WithName("extraFields")
  @WithDefault("balanceInformation")
  String extraFields();
}
