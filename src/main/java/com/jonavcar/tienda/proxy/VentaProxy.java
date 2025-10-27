package com.jonavcar.tienda.proxy;

import com.jonavcar.tienda.model.Venta;
import com.jonavcar.tienda.proxy.factory.EndpointSpecificHeadersFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/ventas")
@RegisterRestClient(configKey = "venta-api")
@RegisterClientHeaders(EndpointSpecificHeadersFactory.class)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VentaProxy {

    @GET
    List<Venta> listar();

    @POST
    Venta create(Venta venta);
}
