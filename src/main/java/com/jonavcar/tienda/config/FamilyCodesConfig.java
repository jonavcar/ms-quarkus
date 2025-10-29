package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "application.account-status-types-properties.family-codes")
public interface FamilyCodesConfig {

  @WithName("invalid-code-origin")
  Optional<List<String>> invalidCodeOrigin();

  @WithName("invalid-code-destination")
  @WithDefault("009")
  List<String> invalidCodeDestination();
}

