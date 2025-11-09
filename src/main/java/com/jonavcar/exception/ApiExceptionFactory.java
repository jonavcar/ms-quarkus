package com.jonavcar.exception;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Map;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ApiExceptionFactory {
  private static final Logger LOG = Logger.getLogger(ApiExceptionFactory.class);
  // Implementación por defecto para ErrorConfig cuando no se encuentra la clave
  private static final ApplicationErrorConfig.ErrorConfig DEFAULT_ERROR_CONFIG =
      new ApplicationErrorConfig.ErrorConfig() {
        @Override
        public String code() {
          return "MC003";
        }

        @Override
        public String description() {
          return "Unexpected error";
        }

        @Override
        public int httpStatus() {
          return 500;
        }
      };
  @Inject
  ApplicationErrorConfig config;

  public StandardException buildException(ErrorCatalog catalog) {
    return buildException(catalog, null, null);
  }

  public StandardException buildException(ErrorCatalog catalog, String message) {
    return buildException(catalog, message, null);
  }

  public StandardException buildException(ErrorCatalog catalog, String message,
                                          Map<String, Object> details) {
    ApplicationErrorConfig.ErrorConfig ec = getConfig(catalog.key());
    String desc = message != null ? message : ec.description();
    Map<String, Object> safeDetails = details == null ? Collections.emptyMap() : details;
    return new StandardException(ec.code(), desc, ec.httpStatus(), null, null, safeDetails,
        Collections.emptyList());
  }

  public StandardException buildException(ErrorCatalog catalog, Throwable cause) {
    ApplicationErrorConfig.ErrorConfig ec = getConfig(catalog.key());
    return new StandardException(ec.code(), ec.description(), ec.httpStatus(),
        cause.getMessage(), cause, Collections.emptyMap(), Collections.emptyList());
  }

  public StandardException buildExceptionFromExternal(ErrorResponseDto ext, int httpStatus) {
    String svc = ext.getComponent();
    String extCode = ext.getCode();
    String internalKey = config.resolver()
        .getOrDefault(svc, Collections.emptyMap())
        .getOrDefault(extCode, "UNEXPECTED");
    ApplicationErrorConfig.ErrorConfig ec = getConfig(internalKey);

    return new StandardException(ec.code(), ec.description(), ec.httpStatus(),
        ext.getError(), null,
        Collections.singletonMap("externalService", svc), ext.getDetails());
  }

  public StandardException buildExceptionFromHttpStatus(int httpStatus) {
    String key = config.resolver()
        .getOrDefault("http-status", Collections.emptyMap())
        .getOrDefault(String.valueOf(httpStatus), getDefaultKey(httpStatus));
    ApplicationErrorConfig.ErrorConfig ec = getConfig(key);
    return new StandardException(ec.code(), ec.description(), ec.httpStatus());
  }

  private ApplicationErrorConfig.ErrorConfig getConfig(String key) {
    ApplicationErrorConfig.ErrorConfig ec = config.errors().get(key);
    if (ec == null) {
      LOG.warn("Missing error config key: " + key + ", using UNEXPECTED");
      ec = config.errors().get("UNEXPECTED");
      if (ec == null) {
        ec = DEFAULT_ERROR_CONFIG;
      }
    }
    return ec;
  }

  private String getDefaultKey(int status) {
    return switch (status) {
      case 401 -> "UNAUTHORIZED";
      case 403 -> "FORBIDDEN";
      case 404 -> "NOT_FOUND";
      case 503 -> "SERVICE_UNAVAILABLE";
      default -> status >= 500 ? "UNEXPECTED" : "BAD_REQUEST";
    };
  }
}
