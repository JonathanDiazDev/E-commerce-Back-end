package com.jonathan.ecommerce.entity;

import com.jonathan.ecommerce.dto.enums.OrderOutboxStatus;
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
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "order_event")
public class OrderOutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "order_id")
  private Order order;

  @Column(columnDefinition = "TEXT")
  private String payload;

  @Enumerated(EnumType.STRING)
  private OrderOutboxStatus orderOutboxStatus;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;
}
