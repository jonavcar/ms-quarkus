package com.jonavcar.ecommerce.resource.web;


import com.jonavcar.ecommerce.sales.model.api.*;

import java.util.UUID;

public class HelloResource implements SalesApi {

    @Override
    public CreateSaleResponse createSale(UUID requestId, UUID sessionUuid, String appCode,
                                         CreateSaleRequest createSaleRequest) {
        return null;
    }

    @Override
    public ClientSearchResponse searchClients(UUID requestId, UUID sessionUuid, String appCode,
                                              ClientSearchRequest clientSearchRequest) {
        return null;
    }

    @Override
    public ProductSearchResponse searchProducts(UUID requestId, UUID sessionUuid, String appCode,
                                                ProductSearchRequest productSearchRequest) {
        return null;
    }

    @Override
    public SaleSearchResponse searchSales(UUID requestId, UUID sessionUuid, String appCode,
                                          SaleSearchRequest saleSearchRequest) {
        return null;
    }
}
