package com.jonavcar.dao;

import com.example.client.product.model.ProductSearchResponse;
import com.jonavcar.proxy.ProductProxy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ProductDaoImpl {
  @Inject
  @RestClient
  ProductProxy productProxy;

  public ProductSearchResponse getProducts() {
    return productProxy.searchProducts(null, null, null);
  }

}
