package com.jonavcar.exception;


import io.quarkus.arc.Unremovable;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import java.util.Map;

/**
 * Interface para mapear configuración YAML de errores.
 */
@ConfigMapping(prefix = "application")
@Unremovable
public interface ApplicationErrorConfig {
  @WithName("errors")
  Map<String, ErrorConfig> errors();

  @WithName("resolver")
  Map<String, Map<String, String>> resolver();

  interface ErrorConfig {
    String code();

    String description();

    @WithName("http-status")
    int httpStatus();
  }
}