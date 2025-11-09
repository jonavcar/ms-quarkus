package com.jonavcar.proxy;


import com.example.client.product.api.ProductsApi;
import com.jonavcar.exception.UnifiedApiExceptionMapper;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


@RegisterRestClient
@RegisterProvider(UnifiedApiExceptionMapper.class)
public interface ProductProxy extends ProductsApi {
}