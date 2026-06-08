package com.jonathan.ecommerce.entity;

import com.jonathan.ecommerce.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_attempt")
public class PaymentAttempt {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_id", nullable = false)
  private Payment payment;

  private int attemptNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Column(columnDefinition = "TEXT")
  private String errorMessage;

  private String transactionId;

  @CreationTimestamp
  @Column(updatable = false)
  private Instant attemptedAt;
}
