package com.jonavcar.tienda.dao;

import com.jonavcar.tienda.context.EndpointHeaderContext;
import com.jonavcar.tienda.model.Cliente;
import com.jonavcar.tienda.proxy.ClienteProxy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ClienteDao {

  private static final Logger LOG = Logger.getLogger(ClienteDao.class);

  @Inject
  @RestClient
  ClienteProxy clienteProxy;

  @Inject
  EndpointHeaderContext endpointHeaderContext;

  public List<Cliente> listar() {
    endpointHeaderContext.addHeader("X-Operation", "LIST_CLIENTES");
    endpointHeaderContext.addHeader("X-Query-Type", "LIST_ALL");
    LOG.debugf("Listing clientes with specific headers");
    return clienteProxy.listar();
  }

  public Cliente create(Cliente cliente) {
    endpointHeaderContext.addHeader("X-Operation", "CREATE_CLIENTE");
    endpointHeaderContext.addHeader("X-Resource-Type", "cliente");
    endpointHeaderContext.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
    LOG.debugf("Creating cliente with specific headers");
    return clienteProxy.create(cliente);
  }

  public Cliente getById(Long id) {
    LOG.debugf("Getting cliente %d without specific headers", id);
    return clienteProxy.getById(id);
  }
}

