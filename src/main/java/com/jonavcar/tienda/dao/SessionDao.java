package com.jonavcar.tienda.dao;

import com.jonavcar.tienda.model.SessionValidationResponse;
import com.jonavcar.tienda.proxy.SessionProxy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SessionDao {

    private static final Logger LOG = Logger.getLogger(SessionDao.class);

    @Inject
    @RestClient
    SessionProxy sessionProxy;

    public SessionValidationResponse validateToken() {
        LOG.debug("Validating token via SessionProxy");
        return sessionProxy.validateToken();
    }
}

