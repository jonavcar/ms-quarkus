package com.jonavcar.controller;

import com.example.server.api.SalesApi;
import com.example.server.model.ClientSearchRequest;
import com.example.server.model.ClientSearchResponse;
import com.example.server.model.CreateSaleRequest;
import com.example.server.model.CreateSaleResponse;
import com.example.server.model.ProductSearchRequest;
import com.example.server.model.ProductSearchResponse;
import com.example.server.model.SaleSearchRequest;
import com.example.server.model.SaleSearchResponse;
import com.jonavcar.service.ProductServiceImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;


@ApplicationScoped
public class SalesApiImpl implements SalesApi {

  @Inject
  ProductServiceImpl productServiceImpl;

  @Override
  public ClientSearchResponse searchClients(UUID requestId, UUID sessionUuid, String appCode,
                                            ClientSearchRequest clientSearchRequest) {
    return null;
  }

  @Override
  public ProductSearchResponse searchProducts(UUID requestId, UUID sessionUuid, String appCode,
                                              ProductSearchRequest productSearchRequest) {
    var x = productServiceImpl.getProducts();
    return null;
  }

  @Override
  public CreateSaleResponse createSale(UUID requestId, UUID sessionUuid, String appCode,
                                       CreateSaleRequest createSaleRequest) {
    return null;
  }

  @Override
  public SaleSearchResponse searchSales(UUID requestId, UUID sessionUuid, String appCode,
                                        SaleSearchRequest saleSearchRequest) {
    return null;
  }
}
