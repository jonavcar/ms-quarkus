package com.jonavcar.tienda.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Operation {

  private String codigo;
  private String sessionId;
  private LocalDateTime fecha;
  private String descripcion;

  public Operation() {
  }

  public Operation(String codigo, String sessionId, LocalDateTime fecha, String descripcion) {
    this.codigo = codigo;
    this.sessionId = sessionId;
    this.fecha = fecha;
    this.descripcion = descripcion;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public void setFecha(LocalDateTime fecha) {
    this.fecha = fecha;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Operation operation = (Operation) o;
    return Objects.equals(codigo, operation.codigo) &&
        Objects.equals(sessionId, operation.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(codigo, sessionId);
  }

  @Override
  public String toString() {
    return "Operation{" +
        "codigo='" + codigo + '\'' +
        ", sessionId='" + sessionId + '\'' +
        ", fecha=" + fecha +
        ", descripcion='" + descripcion + '\'' +
        '}';
  }
}

