package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  void deleteByOrderId(Long orderId);
}
