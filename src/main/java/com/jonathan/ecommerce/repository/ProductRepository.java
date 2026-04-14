package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Category;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByName(String name);
    List<Product> findByCategory(Category category);
    List<Product> findByStatus(Status status);
    List<Product> findByCategoryAndStatus(Category category, Status status);
    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);
    List<Product> findByActiveTrue();
    List<Product> findByCategoryAndActiveTrue(Category category);
    List<Product> findByStatusAndActiveTrue(Status status);
    List<Product> findByCategoryAndStatusAndActiveTrue(Category category, Status status);
    List<Product> findByPriceBetweenAndActiveTrue(BigDecimal min, BigDecimal max);
}
