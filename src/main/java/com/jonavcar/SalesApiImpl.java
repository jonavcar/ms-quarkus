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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;


@ApplicationScoped
public class SalesApiImpl implements SalesApi {

  @Inject
  @RestClient
  ClientProxy clientProxy;

  @Override
  public ClientSearchResponse searchClients(UUID requestId, UUID sessionUuid, String appCode,
                                            ClientSearchRequest clientSearchRequest) {
    com.example.client.clientservice.model.ClientSearchRequest clientSearchRequest1 =
        new com.example.client.clientservice.model.ClientSearchRequest();
    clientSearchRequest1.setDni(clientSearchRequest.getDni());
    clientSearchRequest1.setNames(clientSearchRequest.getNames());
    var x = clientProxy.searchClients(null, null, null, clientSearchRequest1);
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
