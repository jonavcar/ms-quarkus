package com.jonavcar.tienda.controller;

import com.jonavcar.tienda.config.AccountStatusTypesPropertiesConfig;
import com.jonavcar.tienda.config.ProductQueryParamsConfig;
import com.jonavcar.tienda.dto.SessionDto;
import com.jonavcar.tienda.model.Cliente;
import com.jonavcar.tienda.model.Usuario;
import com.jonavcar.tienda.model.Venta;
import com.jonavcar.tienda.service.ClienteService;
import com.jonavcar.tienda.service.SessionValidationService;
import com.jonavcar.tienda.service.UsuarioService;
import com.jonavcar.tienda.service.VentaService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TiendaController {

    private static final Logger LOG = Logger.getLogger(TiendaController.class);

    @Inject
    SessionValidationService sessionValidationService;

    @Inject
    ProductQueryParamsConfig productQueryParamsConfig;
    @Inject
    AccountStatusTypesPropertiesConfig accountStatusTypesPropertiesConfig;

    @Inject
    UsuarioService usuarioService;

    @Inject
    ClienteService clienteService;

    @Inject
    VentaService ventaService;

    @GET
    @Path("/usuarios")
    @RunOnVirtualThread
    public List<Usuario> listarUsuarios() {
        LOG.debug("GET /api/usuarios - start");
        SessionDto session = sessionValidationService.validateAndPropagateHeaders();
        LOG.infof("Session validated for user: %s, tenant: %s", session.getUsername(), session.getTenantId());
        return usuarioService.listar();
    }

    @POST
    @Path("/usuarios")
    @RunOnVirtualThread
    public Response createUsuario(@Valid Usuario usuario) {
        LOG.debug("POST /api/usuarios - start");
        SessionDto session = sessionValidationService.validateAndPropagateHeaders();
        LOG.infof("Session validated for user: %s", session.getUsername());
        Usuario created = usuarioService.create(usuario);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/usuarios/{id}")
    @RunOnVirtualThread
    public Usuario updateUsuario(@PathParam("id") Long id, @Valid Usuario usuario) {
        LOG.debugf("PUT /api/usuarios/%d - start", id);
        SessionDto session = sessionValidationService.validateAndPropagateHeaders();
        LOG.infof("Session validated for user: %s", session.getUsername());
        return usuarioService.update(id, usuario);
    }

    @DELETE
    @Path("/usuarios/{id}")
    @RunOnVirtualThread
    public Response deleteUsuario(@PathParam("id") Long id) {
        LOG.debugf("DELETE /api/usuarios/%d - start", id);
        SessionDto session = sessionValidationService.validateAndPropagateHeaders();
        LOG.infof("Session validated for user: %s", session.getUsername());
        usuarioService.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/clientes")
    @RunOnVirtualThread
    public List<Cliente> listarClientes() {
        LOG.debug("GET /api/clientes - start");
        SessionDto session = sessionValidationService.validateAndPropagateHeaders();
        LOG.infof("Session validated for user: %s, tenant: %s", session.getUsername(), session.getTenantId());
        return clienteService.listar();
    }

    @POST
    @Path("/clientes")
    @RunOnVirtualThread
    public Response createCliente(@Valid Cliente cliente) {
        LOG.debug("POST /api/clientes - start");
        SessionDto session = sessionValidationService.validateAndPropagateHeaders();
        LOG.infof("Session validated for user: %s", session.getUsername());
        Cliente created = clienteService.create(cliente);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Path("/clientes/{id}")
    @RunOnVirtualThread
    public Cliente getClienteById(@PathParam("id") Long id) {
        LOG.debugf("GET /api/clientes/%d - start", id);
        SessionDto session = sessionValidationService.validateAndPropagateHeaders();
        LOG.infof("Session validated for user: %s", session.getUsername());
        return clienteService.getById(id);
    }

    @GET
    @Path("/ventas")
    @RunOnVirtualThread
    public List<Venta> listarVentas() {
        LOG.debug("GET /api/ventas - start");
        SessionDto session = sessionValidationService.validateAndPropagateHeaders();
        LOG.infof("Session validated for user: %s, tenant: %s", session.getUsername(), session.getTenantId());
        return ventaService.listar();
    }

    @POST
    @Path("/ventas")
    @RunOnVirtualThread
    public Response createVenta(@Valid Venta venta) {
        LOG.debug("POST /api/ventas - start");
        SessionDto session = sessionValidationService.validateAndPropagateHeaders();
        LOG.infof("Session validated for user: %s", session.getUsername());
        Venta created = ventaService.create(venta);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }
}
