package com.jonavcar.tienda.service;

import com.jonavcar.tienda.config.SessionHeadersConfiguration;
import com.jonavcar.tienda.context.SessionPropagationContext;
import com.jonavcar.tienda.dao.SessionDao;
import com.jonavcar.tienda.dto.SessionDto;
import com.jonavcar.tienda.model.SessionValidationResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SessionValidationService {

  private static final Logger LOG = Logger.getLogger(SessionValidationService.class);

  @Inject
  SessionDao sessionDao;

  @Inject
  SessionPropagationContext sessionPropagationContext;

  @Inject
  SessionHeadersConfiguration sessionHeadersConfiguration;

  public SessionDto validateAndPropagateHeaders() {
    LOG.debug("Adding static session headers to propagation context");
    sessionPropagationContext.addHeaders(sessionHeadersConfiguration.getSessionHeaders());

    LOG.debug("Validating session token");
    SessionValidationResponse response = sessionDao.validateToken();

    if (!response.isValid()) {
      String message = response.getMessage() != null ? response.getMessage() : "Invalid session";
      LOG.warnf("Session validation failed: %s", message);
      throw new WebApplicationException(message, Response.Status.UNAUTHORIZED);
    }

    if (response.getSession() == null) {
      LOG.error("Session validation succeeded but session is null");
      throw new WebApplicationException("Session data missing",
          Response.Status.INTERNAL_SERVER_ERROR);
    }

    if (response.getPropagationHeaders() != null && !response.getPropagationHeaders().isEmpty()) {
      LOG.debugf("Adding %d dynamic propagation headers", response.getPropagationHeaders().size());
      sessionPropagationContext.addHeaders(response.getPropagationHeaders());
    }

    LOG.infof("Session validated successfully for user: %s", response.getSession().getUsername());
    return response.getSession();
  }
}

