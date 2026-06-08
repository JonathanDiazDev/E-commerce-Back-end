package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByStripePaymentIntentId(String id);

  @EntityGraph(value = "Payment.withOrder")
  Optional<Payment> findByOrderId(@Param("orderId") Long id);
}
