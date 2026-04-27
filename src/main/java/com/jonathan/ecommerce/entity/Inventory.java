package com.jonathan.ecommerce.entity;

import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory")
public class Inventory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(nullable = false)
  private Integer quantity = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InventoryStatus inventoryStatus;

  private boolean manualDisabled;

  @PrePersist
  @PreUpdate
  private void syncStatusWithQuantity() {
    if (manualDisabled || (this.quantity != null && this.quantity <= 0)) {
      this.inventoryStatus = InventoryStatus.OUT_OF_STOCK;
    } else {
      this.inventoryStatus = InventoryStatus.IN_STOCK;
    }
  }
}
