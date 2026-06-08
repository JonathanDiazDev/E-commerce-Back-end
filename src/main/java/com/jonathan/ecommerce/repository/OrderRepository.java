package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  @EntityGraph(attributePaths = {"payment", "items"})
  List<Order> findByUserId(Long userId);

  boolean existsByStripePaymentIdAndOrderStatus(String stripePaymentId, OrderStatus status);

  Optional<Order> findByStripePaymentId(String stripePaymentId);

  @Query(
      "SELECT o FROM Order o WHERE o.orderStatus = 'PENDING_PAYMENT' AND o.createdAt < :cutoffTime")
  List<Order> findExpiredPendingPaymentOrders(@Param("cutoffTime") Instant cutoffTime);
}
