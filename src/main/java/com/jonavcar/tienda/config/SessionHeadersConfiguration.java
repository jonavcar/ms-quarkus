package com.jonavcar.tienda.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SessionHeadersConfiguration {

  private static final Logger LOG = Logger.getLogger(SessionHeadersConfiguration.class);
  private final Map<String, String> sessionHeaders;

  @Inject
  public SessionHeadersConfiguration(ObjectMapper objectMapper) {
    this.sessionHeaders = loadSessionHeaders(objectMapper);
  }

  private Map<String, String> loadSessionHeaders(ObjectMapper objectMapper) {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream("headers-static.json")) {
      if (is == null) {
        LOG.warn("headers-static.json not found, using empty headers");
        return Map.of();
      }
      Map<String, Object> config = objectMapper.readValue(is, Map.class);
      Object sessionHeadersObj = config.get("sessionHeaders");
      if (sessionHeadersObj instanceof Map) {
        return Map.copyOf((Map<String, String>) sessionHeadersObj);
      }
      return Map.of();
    } catch (Exception e) {
      LOG.error("Error loading headers-static.json", e);
      return Map.of();
    }
  }

  public Map<String, String> getSessionHeaders() {
    return new HashMap<>(sessionHeaders);
  }
}

