package com.jonavcar.tienda.context;

import jakarta.enterprise.context.RequestScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequestScoped
public class SessionPropagationContext {

  private final ConcurrentHashMap<String, String> headers = new ConcurrentHashMap<>();

  public void addHeader(String key, String value) {
    if (key != null && value != null) {
      headers.put(key, value);
    }
  }

  public void addHeaders(Map<String, String> headersToAdd) {
    if (headersToAdd != null) {
      headers.putAll(headersToAdd);
    }
  }

  public String getHeader(String key) {
    return headers.get(key);
  }

  public Map<String, String> getAllHeaders() {
    return Map.copyOf(headers);
  }

  public boolean hasHeader(String key) {
    return headers.containsKey(key);
  }

  public int size() {
    return headers.size();
  }
}
