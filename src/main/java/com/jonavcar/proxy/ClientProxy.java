package com.jonavcar.proxy;


import com.example.client.clientservice.api.ClientsApi;
import com.jonavcar.common.UnifiedApiExceptionMapper;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


@RegisterRestClient(configKey = "client-service")
@RegisterProvider(UnifiedApiExceptionMapper.class)
public interface ClientProxy extends ClientsApi {
}