package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.InventoryMovement;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MovementRepository extends JpaRepository<InventoryMovement, Long> {

  @Query(
      "SELECT m FROM InventoryMovement m "
          + "JOIN FETCH m.inventory i "
          + "JOIN FETCH i.product "
          + "WHERE i.id = :inventoryId")
  Page<InventoryMovement> findByInventoryId(
      @Param("inventoryId") Long inventoryId, Pageable pageable);
}
