package com.jonavcar.tienda.proxy.factory;

import com.jonavcar.tienda.context.SessionPropagationContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class SessionFlowHeadersFactory implements ClientHeadersFactory {

    private static final Logger LOG = Logger.getLogger(SessionFlowHeadersFactory.class);

    @Inject
    SessionPropagationContext sessionPropagationContext;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                   MultivaluedMap<String, String> clientOutgoingHeaders) {
        LinkedHashMap<String, String> mergedHeaders = new LinkedHashMap<>();

        if (incomingHeaders != null) {
            incomingHeaders.forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    mergedHeaders.put(key, values.get(0));
                }
            });
        }

        mergedHeaders.putAll(sessionPropagationContext.getAllHeaders());

        LOG.debugf("SessionFlowHeadersFactory: merged %d headers", mergedHeaders.size());

        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        mergedHeaders.forEach(result::putSingle);
        return result;
    }
}

