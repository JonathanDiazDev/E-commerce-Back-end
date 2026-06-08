package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.FailedPaymentEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedPaymentEventRepository extends JpaRepository<FailedPaymentEvent, Long> {
  List<FailedPaymentEvent> findByOrderId(long orderId);
}
