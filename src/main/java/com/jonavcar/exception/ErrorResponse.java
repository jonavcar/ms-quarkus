package com.jonavcar.exception;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Modelo para la respuesta JSON de error enviada al frontend.
 */
public class ErrorResponse {

  private String error;              // Código de error interno (ej. MC001)
  private String message;            // Mensaje legible para el usuario
  private OffsetDateTime timestamp;  // Timestamp cuando ocurrió el error
  private String traceId;            // UUID para trazabilidad
  private Map<String, Object> details;  // Detalles adicionales del error

  public ErrorResponse() {
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public Map<String, Object> getDetails() {
    return details;
  }

  public void setDetails(Map<String, Object> details) {
    this.details = details;
  }
}