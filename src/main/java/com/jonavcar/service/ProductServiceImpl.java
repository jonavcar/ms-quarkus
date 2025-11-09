package com.jonavcar.service;

import com.example.client.product.model.ProductSearchResponse;
import com.jonavcar.dao.ProductDaoImpl;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductServiceImpl {
  private final ProductDaoImpl productDao;

  public ProductServiceImpl(ProductDaoImpl productDao) {
    this.productDao = productDao;
  }

  public ProductSearchResponse getProducts() {
    return productDao.getProducts();
  }

}
