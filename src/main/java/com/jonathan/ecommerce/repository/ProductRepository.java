package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Category;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.enums.Status;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

  @EntityGraph(attributePaths = {"category"})
  Optional<Product> findByName(String name);

  @EntityGraph(attributePaths = {"category"})
  List<Product> findByCategory(Category category);

  @EntityGraph(attributePaths = {"category"})
  List<Product> findByStatus(Status status);

  @EntityGraph(attributePaths = {"category"})
  List<Product> findByCategoryAndStatus(Category category, Status status);

  List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

  List<Product> findByActiveTrue();

  List<Product> findByCategoryAndActiveTrue(Category category);

  List<Product> findByStatusAndActiveTrue(Status status);

  List<Product> findByCategoryAndStatusAndActiveTrue(Category category, Status status);

  List<Product> findByPriceBetweenAndActiveTrue(BigDecimal min, BigDecimal max);
}
