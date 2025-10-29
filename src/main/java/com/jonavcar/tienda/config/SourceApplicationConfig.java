package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;
import java.util.Map;

@ConfigMapping(prefix = "application.event-parameters.audit-parameters.sourceApplication")
public interface SourceApplicationConfig {

  @WithParentName
  Map<String, String> applications();
}

