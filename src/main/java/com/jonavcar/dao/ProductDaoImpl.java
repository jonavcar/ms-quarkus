package com.jonavcar.dao;

import com.example.client.product.api.ApiException;
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
    try {
      var x = productProxy.searchProducts(null, null, null);
      return x;
    } catch (ApiException e) {
      throw e;
    }
  }

}
