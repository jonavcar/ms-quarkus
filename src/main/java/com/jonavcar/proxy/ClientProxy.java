package com.jonavcar.proxy;


import com.example.client.clientservice.api.ClientsApi;
import com.jonavcar.common.UnifiedApiExceptionMapper;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


@RegisterRestClient
@RegisterProvider(UnifiedApiExceptionMapper.class)
public interface ClientProxy extends ClientsApi {
}