package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "application.product-query-params")
public interface ProductQueryParamsConfig {

    @WithName("familyCode")
    String familyCode();

    @WithName("filterFields")
    String filterFields();

    @WithName("extraFields")
    String extraFields();
}

