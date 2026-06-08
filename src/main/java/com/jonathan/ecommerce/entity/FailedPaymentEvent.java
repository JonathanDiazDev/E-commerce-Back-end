package com.jonathan.ecommerce.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "failed_payment_events")
public class FailedPaymentEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long orderId;

  @Column(nullable = false)
  private int attemptNumber;

  @Column(columnDefinition = "TEXT")
  private String errorMessage;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant failedAt;

  @Column(nullable = false)
  private boolean notificationSent = false;
}
