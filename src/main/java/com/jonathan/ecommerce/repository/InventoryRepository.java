package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Inventory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = {"product"})
  Optional<Inventory> findByProductId(Long id);

  @EntityGraph(attributePaths = {"product"})
  List<Inventory> findByQuantityLessThan(Integer threshold);

  @Modifying
  @Transactional
  @Query(
      "UPDATE Inventory i "
          + "SET i.quantity = i.quantity - :amount "
          + "WHERE i.product.id = :productId "
          + "AND i.quantity >= :amount "
          + "AND i.manualDisabled = false")
  int decrementStockAtomic(@Param("productId") Long productId, @Param("amount") Integer amount);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Inventory> findWithLockByProductId(Long productId);

  @Modifying
  @Transactional
  @Query(
      "UPDATE Inventory i "
          + "SET i.quantity = i.quantity + :amount "
          + "WHERE i.product.id = :productId")
  int incrementStockAtomic(@Param("productId") Long productId, @Param("amount") Integer amount);
}
