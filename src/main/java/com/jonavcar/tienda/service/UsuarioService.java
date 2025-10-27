package com.jonavcar.tienda.service;

import com.jonavcar.tienda.dao.UsuarioDao;
import com.jonavcar.tienda.model.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class UsuarioService {

    private static final Logger LOG = Logger.getLogger(UsuarioService.class);

    @Inject
    UsuarioDao usuarioDao;

    public List<Usuario> listar() {
        LOG.debug("Listing all usuarios");
        return usuarioDao.listar();
    }

    public Usuario create(Usuario usuario) {
        LOG.debug("Creating new usuario");
        validateUsuario(usuario);
        return usuarioDao.create(usuario);
    }

    public Usuario update(Long id, Usuario usuario) {
        LOG.debugf("Updating usuario %d", id);
        validateUsuario(usuario);
        return usuarioDao.update(id, usuario);
    }

    public void delete(Long id) {
        LOG.debugf("Deleting usuario %d", id);
        usuarioDao.delete(id);
    }

    private void validateUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario cannot be null");
        }
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
    }
}

