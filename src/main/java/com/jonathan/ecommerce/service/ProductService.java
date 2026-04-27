package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.request.ProductRequest;
import com.jonathan.ecommerce.dto.response.ProductResponse;
import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

  ProductResponse addProduct(ProductRequest request);

  ProductResponse updateProduct(Long id, ProductRequest request);

  List<ProductResponse> getAllProducts();

  ProductResponse getProductById(Long id);

  List<ProductResponse> getProductsByCategory(String category);

  ProductResponse getProductsByName(String name);

  List<ProductResponse> getProductsByPrice(BigDecimal min, BigDecimal max);

  void deactivateProduct(Long id);
}
