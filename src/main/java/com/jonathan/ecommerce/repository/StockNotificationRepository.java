package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.StockNotification;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StockNotificationRepository extends JpaRepository<StockNotification, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT sn FROM StockNotification sn WHERE sn.product.id = :productId AND sn.isNotified = false")
  List<StockNotification> findByProductIdAndIsNotifiedFalse(Long productId);

  boolean existsByUserIdAndProductIdAndIsNotifiedFalse(Long userId, Long productId);
}
