package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
  Optional<Inventory> findByProductId(Long id);

  List<Inventory> findByInventoryStatus(InventoryStatus status);

  List<Inventory> findByQuantityLessThan(Integer threshold);

  Optional<Inventory> findByProduct(Product product);
}
