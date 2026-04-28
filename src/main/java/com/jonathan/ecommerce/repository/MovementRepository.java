package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.InventoryMovement;
import com.jonathan.ecommerce.entity.enums.MovementType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MovementRepository extends JpaRepository<InventoryMovement, Long> {

  @Query(
      value =
          "SELECT m FROM InventoryMovement m "
              + "JOIN FETCH m.inventory i "
              + "JOIN FETCH i.product p "
              + "JOIN FETCH p.category c "
              + "WHERE i.id = :inventoryId",
      countQuery = "SELECT COUNT(m) FROM InventoryMovement m WHERE m.inventory.id = :inventoryId")
  Page<InventoryMovement> findByInventoryId(
      @Param("inventoryId") Long inventoryId, Pageable pageable);

  @Query(
      value =
          "SELECT m FROM InventoryMovement m "
              + "JOIN FETCH m.inventory i "
              + "JOIN FETCH i.product p "
              + "JOIN FETCH p.category c "
              + "WHERE i.id = :inventoryId AND m.type = :type",
      countQuery =
          "SELECT COUNT(m) FROM InventoryMovement m WHERE m.inventory.id = :inventoryId AND m.type = :type")
  Page<InventoryMovement> findByInventoryIdAndType(
      @Param("inventoryId") Long inventoryId, @Param("type") MovementType type, Pageable pageable);
}
