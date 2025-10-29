package com.jonavcar.tienda.service;

import com.jonavcar.tienda.dao.ClienteDao;
import com.jonavcar.tienda.model.Cliente;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ClienteService {

  private static final Logger LOG = Logger.getLogger(ClienteService.class);

  @Inject
  ClienteDao clienteDao;

  public List<Cliente> listar() {
    LOG.debug("Listing all clientes");
    return clienteDao.listar();
  }

  public Cliente create(Cliente cliente) {
    LOG.debug("Creating new cliente");
    return clienteDao.create(cliente);
  }

  public Cliente getById(Long id) {
    LOG.debugf("Getting cliente %d", id);
    return clienteDao.getById(id);
  }
}

