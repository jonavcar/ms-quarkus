package com.jonavcar.tienda.entity;

import java.util.List;

public record SessionProducts(
    String sessionId,
    List<Product> products
) {}

