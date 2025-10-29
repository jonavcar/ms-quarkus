package com.jonavcar.tienda.dao;

import com.jonavcar.tienda.context.EndpointHeaderContext;
import com.jonavcar.tienda.model.Usuario;
import com.jonavcar.tienda.proxy.UsuarioProxy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UsuarioDao {

  private static final Logger LOG = Logger.getLogger(UsuarioDao.class);

  @Inject
  @RestClient
  UsuarioProxy usuarioProxy;

  @Inject
  EndpointHeaderContext endpointHeaderContext;

  public List<Usuario> listar() {
    LOG.debugf("Listing usuarios without specific headers");
    return usuarioProxy.listar();
  }

  public Usuario create(Usuario usuario) {
    endpointHeaderContext.addHeader("X-Operation", "CREATE_USER");
    endpointHeaderContext.addHeader("X-Resource-Type", "usuario");
    endpointHeaderContext.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
    LOG.debugf("Creating usuario with specific headers");
    return usuarioProxy.create(usuario);
  }

  public Usuario update(Long id, Usuario usuario) {
    endpointHeaderContext.addHeader("X-Operation", "UPDATE_USER");
    endpointHeaderContext.addHeader("X-Resource-Type", "usuario");
    endpointHeaderContext.addHeader("X-Entity-Id", String.valueOf(id));
    endpointHeaderContext.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
    LOG.debugf("Updating usuario %d with specific headers", id);
    return usuarioProxy.update(id, usuario);
  }

  public void delete(Long id) {
    endpointHeaderContext.addHeader("X-Operation", "DELETE_USER");
    endpointHeaderContext.addHeader("X-Resource-Type", "usuario");
    endpointHeaderContext.addHeader("X-Entity-Id", String.valueOf(id));
    endpointHeaderContext.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
    LOG.debugf("Deleting usuario %d with specific headers", id);
    usuarioProxy.delete(id);
  }
}

