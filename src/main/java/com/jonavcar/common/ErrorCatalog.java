package com.jonavcar.common;


/**
 * Enum type-safe para claves de error definidas en YAML.
 */
public enum ErrorCatalog {
  UNAUTHORIZED,
  FORBIDDEN,
  UNEXPECTED,
  NOT_FOUND,
  SERVICE_UNAVAILABLE,
  BAD_REQUEST,
  INVALID_AMOUNT,
  INVALID_DISCOUNT,
  INSUFFICIENT_STOCK,
  CACHE_ERROR,
  CACHE_TIMEOUT,
  VALIDATION_ERROR,
  DUPLICATE_SALE;

  public String key() {
    return name();
  }
}