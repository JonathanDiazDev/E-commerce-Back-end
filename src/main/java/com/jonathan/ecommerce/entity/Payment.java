package com.jonathan.ecommerce.entity;

import com.jonathan.ecommerce.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment")
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", unique = true)
  private Order order;

  @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 20)
  private List<PaymentAttempt> attempts = new ArrayList<>();

  @Column(name = "stripe_payment_intent_id")
  private String stripePaymentIntentId;

  @Column(precision = 19, scale = 2, nullable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false)
  private PaymentStatus paymentStatus = PaymentStatus.PENDING;

  @UpdateTimestamp private Instant updatedAt;
}
