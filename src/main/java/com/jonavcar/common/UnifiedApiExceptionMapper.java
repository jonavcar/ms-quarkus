package com.jonavcar.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.logging.Logger;

public class UnifiedApiExceptionMapper implements ResponseExceptionMapper<StandardException> {

  private static final Logger LOG = Logger.getLogger(UnifiedApiExceptionMapper.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Inject
  ApiExceptionFactory factory;

  @Override
  public StandardException toThrowable(Response response) {
    int status = response.getStatus();
    String contentType = response.getHeaderString("Content-Type");
    String body = null;

    try {
      if (response.hasEntity()) {
        body = response.readEntity(String.class);
      }
    } catch (Exception e) {
      LOG.warnf("Error reading response body: %s", e.getMessage());
    }

    if (body != null && !body.isBlank() && isJsonContent(contentType, body)) {
      try {
        ErrorResponseDto ext = MAPPER.readValue(body, ErrorResponseDto.class);
        return factory.buildExceptionFromExternal(ext, status);
      } catch (Exception ex) {
        LOG.warn("Failed to parse error response JSON: " + ex.getMessage());
      }
    }

    if (body != null && !body.isBlank()) {
      ErrorResponseDto fallback = new ErrorResponseDto(
          "HTTP_" + status,
          "external-service",
          body,
          String.valueOf(status));
      return factory.buildExceptionFromExternal(fallback, status);
    }

    return factory.buildExceptionFromHttpStatus(status);
  }

  @Override
  public boolean handles(int status, jakarta.ws.rs.core.MultivaluedMap<String, Object> headers) {
    return status >= 400;
  }

  private boolean isJsonContent(String contentType, String body) {
    if (contentType != null && (contentType.contains("application/json") ||
        contentType.contains("application/problem+json"))) {
      return true;
    }
    return body.trim().startsWith("{");
  }
}