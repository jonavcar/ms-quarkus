package com.jonavcar.common;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Map;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ApiExceptionFactory {
  private static final Logger LOG = Logger.getLogger(ApiExceptionFactory.class);

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
    ErrorConfig ec = getConfig(catalog.key());
    String desc = message != null ? message : ec.getDescription();
    Map<String, Object> safeDetails = details == null ? Collections.emptyMap() : details;
    return new StandardException(ec.getCode(), desc, ec.getHttpStatus(), null, null, safeDetails,
        Collections.emptyList());
  }

  public StandardException buildException(ErrorCatalog catalog, Throwable cause) {
    ErrorConfig ec = getConfig(catalog.key());
    return new StandardException(ec.getCode(), ec.getDescription(), ec.getHttpStatus(),
        cause.getMessage(), cause, Collections.emptyMap(), Collections.emptyList());
  }

  public StandardException buildExceptionFromExternal(ErrorResponseDto ext, int httpStatus) {
    String svc = ext.getComponent();
    String extCode = ext.getCode();
    String internalKey = config.resolver()
        .getOrDefault(svc, Collections.emptyMap())
        .getOrDefault(extCode, "UNEXPECTED");
    ErrorConfig ec = getConfig(internalKey);

    return new StandardException(ec.getCode(), ec.getDescription(), ec.getHttpStatus(),
        ext.getError(), null,
        Collections.singletonMap("externalService", svc), ext.getDetails());
  }

  public StandardException buildExceptionFromHttpStatus(int httpStatus) {
    String key = config.resolver()
        .getOrDefault("http-status", Collections.emptyMap())
        .getOrDefault(String.valueOf(httpStatus), getDefaultKey(httpStatus));
    ErrorConfig ec = getConfig(key);
    return new StandardException(ec.getCode(), ec.getDescription(), ec.getHttpStatus());
  }

  private ErrorConfig getConfig(String key) {
    ErrorConfig ec = config.errors().get(key);
    if (ec == null) {
      LOG.warn("Missing error config key: " + key + ", using UNEXPECTED");
      ec = config.errors().get("UNEXPECTED");
      if (ec == null) {
        ec = new ErrorConfig();
        ec.setCode("MC003");
        ec.setDescription("Unexpected error");
        ec.setHttpStatus(500);
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