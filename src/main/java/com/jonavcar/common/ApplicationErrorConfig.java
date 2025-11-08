package com.jonavcar.common;


import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import java.util.Map;

/**
 * Interface para mapear configuración YAML de errores.
 */
@ConfigMapping(prefix = "application")
public interface ApplicationErrorConfig {
  @WithName("errors")
  Map<String, ErrorConfig> errors();

  @WithName("resolver")
  Map<String, Map<String, String>> resolver();
}