package com.jonavcar.tienda.proxy.factory;

import com.jonavcar.tienda.context.SessionPropagationContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.logging.Logger;

@ApplicationScoped
public class FrameworkAndSessionHeadersFactory implements ClientHeadersFactory {

  private static final Logger LOG = Logger.getLogger(FrameworkAndSessionHeadersFactory.class);

  @Inject
  SessionPropagationContext sessionPropagationContext;

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> incomingHeaders,
      MultivaluedMap<String, String> clientOutgoingHeaders) {

    Map<String, String> mergedHeaders = new LinkedHashMap<>();

    incomingHeaders.forEach((key, values) -> {
      if (values != null && !values.isEmpty()) {
        mergedHeaders.put(key, values.get(0));
      }
    });

    mergedHeaders.putAll(sessionPropagationContext.getAllHeaders());

    MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
    mergedHeaders.forEach(result::add);

    LOG.debugf("Framework + Session headers: %d", result.size());
    return result;
  }
}


