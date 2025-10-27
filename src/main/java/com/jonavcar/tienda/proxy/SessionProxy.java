package com.jonavcar.tienda.proxy;

import com.jonavcar.tienda.model.SessionValidationResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/session")
@RegisterRestClient(configKey = "session-api")
@RegisterClientHeaders
public interface SessionProxy {

    @POST
    @Path("/validate")
    @Produces(MediaType.APPLICATION_JSON)
    SessionValidationResponse validateToken();
}

