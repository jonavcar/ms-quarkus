package com.jonavcar.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {
  @Override
  public Response toResponse(Exception ex) {
    if (ex instanceof StandardException se) {
      return buildResponse(se);
    }
    StandardException se = new StandardException(
        "MC003",
        "Unexpected error",
        500,
        ex.getMessage(),
        ex,
        null,
        null
    );
    return buildResponse(se);
  }

  private Response buildResponse(StandardException se) {
    ErrorResponse error = new ErrorResponse();
    error.setError(se.getCode());
    error.setMessage(se.getDescription());
    error.setTimestamp(OffsetDateTime.now());
    error.setTraceId(UUID.randomUUID().toString());

    Map<String, Object> details = new HashMap<>(se.getDetails());
    if (se.getOriginalMessage() != null) {
      details.put("originalError", se.getOriginalMessage());
    }
    if (!se.getExternalDetails().isEmpty()) {
      details.put("externalDetails", se.getExternalDetails());
    }
    if (!details.isEmpty()) {
      error.setDetails(details);
    }
    return Response.status(se.getHttpStatus()).entity(error).build();
  }
}