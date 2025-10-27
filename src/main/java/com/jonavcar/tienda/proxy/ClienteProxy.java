package com.jonavcar.tienda.proxy;

import com.jonavcar.tienda.model.Cliente;
import com.jonavcar.tienda.proxy.factory.EndpointSpecificHeadersFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/clientes")
@RegisterRestClient(configKey = "cliente-api")
@RegisterClientHeaders(EndpointSpecificHeadersFactory.class)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ClienteProxy {

    @GET
    List<Cliente> listar();

    @POST
    Cliente create(Cliente cliente);

    @GET
    @Path("/{id}")
    Cliente getById(@PathParam("id") Long id);
}

