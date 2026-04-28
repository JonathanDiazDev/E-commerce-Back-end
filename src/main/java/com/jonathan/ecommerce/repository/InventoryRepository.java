package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Inventory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

  @EntityGraph(attributePaths = {"product"})
  Optional<Inventory> findByProductId(Long id);

  @EntityGraph(attributePaths = {"product"})
  List<Inventory> findByQuantityLessThan(Integer threshold);
}
