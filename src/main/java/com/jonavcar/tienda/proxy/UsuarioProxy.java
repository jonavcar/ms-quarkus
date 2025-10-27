package com.jonavcar.tienda.proxy;

import com.jonavcar.tienda.model.Usuario;
import com.jonavcar.tienda.proxy.factory.EndpointSpecificHeadersFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/usuarios")
@RegisterRestClient(configKey = "usuario-api")
@RegisterClientHeaders(EndpointSpecificHeadersFactory.class)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface UsuarioProxy {

    @GET
    List<Usuario> listar();

    @POST
    Usuario create(Usuario usuario);

    @PUT
    @Path("/{id}")
    Usuario update(@PathParam("id") Long id, Usuario usuario);

    @DELETE
    @Path("/{id}")
    void delete(@PathParam("id") Long id);
}

