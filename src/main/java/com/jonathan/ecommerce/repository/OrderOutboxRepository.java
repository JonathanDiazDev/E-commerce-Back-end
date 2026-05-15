package com.jonathan.ecommerce.repository;
import com.jonathan.ecommerce.dto.enums.OrderOutboxStatus;
import com.jonathan.ecommerce.entity.OrderOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderOutboxRepository extends JpaRepository <OrderOutboxEvent, Long> {
    List<OrderOutboxEvent> findByOrderOutboxStatus(OrderOutboxStatus status);
}
