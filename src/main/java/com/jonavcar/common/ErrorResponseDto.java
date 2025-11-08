package com.jonavcar.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DTO para parsear errores recibidos de servicios externos.
 */
public final class ErrorResponseDto {
  @JsonProperty("details")
  private final List<Map<String, Object>> details;
  @JsonProperty("code")
  private String code;
  @JsonProperty("componente")
  private String component;
  @JsonProperty("error")
  private String error;
  @JsonProperty("httpcode")
  private String httpCode;

  public ErrorResponseDto() {
    this.details = Collections.emptyList();
  }

  public ErrorResponseDto(String code, String component, String error, String httpCode) {
    this.code = code;
    this.component = component;
    this.error = error;
    this.httpCode = httpCode;
    this.details = Collections.emptyList();
  }

  public String getCode() {
    return code;
  }

  public String getComponent() {
    return component;
  }

  public String getError() {
    return error;
  }

  public String getHttpCode() {
    return httpCode;
  }

  public List<Map<String, Object>> getDetails() {
    return details == null ? Collections.emptyList() : Collections.unmodifiableList(details);
  }
}