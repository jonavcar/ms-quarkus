package com.jonavcar;

import com.example.server.api.SalesApi;
import com.example.server.model.ClientSearchRequest;
import com.example.server.model.ClientSearchResponse;
import com.example.server.model.CreateSaleRequest;
import com.example.server.model.CreateSaleResponse;
import com.example.server.model.ProductSearchRequest;
import com.example.server.model.ProductSearchResponse;
import com.example.server.model.SaleSearchRequest;
import com.example.server.model.SaleSearchResponse;
import com.jonavcar.proxy.ClientProxy;
import jakarta.inject.Inject;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;


public class SalesApiImpl implements SalesApi {

  @Inject
  @RestClient
  ClientProxy clientProxy;

  @Override
  public ClientSearchResponse searchClients(UUID requestId, UUID sessionUuid, String appCode,
                                            ClientSearchRequest clientSearchRequest) {
    com.example.client.clientservice.model.ClientSearchRequest clientSearchReques1t =
        new com.example.client.clientservice.model.ClientSearchRequest();
    clientSearchReques1t.setDni(clientSearchReques1t.getDni());
    clientSearchReques1t.setNames(clientSearchRequest.getNames());
    var x = clientProxy.searchClients(requestId, sessionUuid, "mipoi", clientSearchReques1t);
    return null;
  }

  @Override
  public ProductSearchResponse searchProducts(UUID requestId, UUID sessionUuid, String appCode,
                                              ProductSearchRequest productSearchRequest) {
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
