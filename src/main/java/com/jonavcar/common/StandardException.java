package com.jonavcar.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excepción estándar para errores internos y externos enriquecida con detalles.
 */
public final class StandardException extends RuntimeException {
  private final String code;
  private final String description;
  private final int httpStatus;
  private final String originalMessage;
  private final Map<String, Object> details;
  private final List<Map<String, Object>> externalDetails;

  public StandardException(String code, String description, int httpStatus) {
    this(code, description, httpStatus, null, null, Collections.emptyMap(),
        Collections.emptyList());
  }

  public StandardException(String code, String description, int httpStatus, String originalMessage,
                           Throwable cause) {
    this(code, description, httpStatus, originalMessage, cause, Collections.emptyMap(),
        Collections.emptyList());
  }

  public StandardException(String code, String description, int httpStatus,
                           Map<String, Object> details) {
    this(code, description, httpStatus, null, null, details, Collections.emptyList());
  }

  public StandardException(String code, String description, int httpStatus, String originalMessage,
                           Throwable cause,
                           Map<String, Object> details, List<Map<String, Object>> externalDetails) {
    super(description, cause);
    this.code = code;
    this.description = description;
    this.httpStatus = httpStatus;
    this.originalMessage = originalMessage;
    this.details = details == null ? Collections.emptyMap() :
        Collections.unmodifiableMap(new LinkedHashMap<>(details));
    this.externalDetails = externalDetails == null ? Collections.emptyList() :
        Collections.unmodifiableList(new ArrayList<>(externalDetails));
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public String getOriginalMessage() {
    return originalMessage;
  }

  public Map<String, Object> getDetails() {
    return details;
  }

  public List<Map<String, Object>> getExternalDetails() {
    return externalDetails;
  }

  public Map<String, Object> getAllDetails() {
    if (externalDetails.isEmpty()) {
      return details;
    }
    Map<String, Object> result = new LinkedHashMap<>(details);
    result.put("externalDetails", externalDetails);
    return Collections.unmodifiableMap(result);
  }
}