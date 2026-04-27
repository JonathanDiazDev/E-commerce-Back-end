package com.jonathan.ecommerce.entity;

import com.jonathan.ecommerce.entity.enums.MovementType;
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
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "inventory_movement")
public class InventoryMovement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JoinColumn(name = "inventory_id")
  @ManyToOne(fetch = FetchType.LAZY)
  private Inventory inventory;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MovementType movementType;

  @Column(nullable = false)
  private Integer quantity;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant date;

  private String reason;
}
