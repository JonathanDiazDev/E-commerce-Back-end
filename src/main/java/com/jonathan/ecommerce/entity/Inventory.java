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

  @Version
  @Column(name = "version")
  private Long version;

  @PrePersist
  @PreUpdate
  private void syncStatusWithQuantity() {
    if (manualDisabled || (this.quantity != null && this.quantity <= 0)) {
      this.inventoryStatus = InventoryStatus.OUT_OF_STOCK;
    } else {
      this.inventoryStatus = InventoryStatus.IN_STOCK;
    }
  }

  public boolean hasStock(Integer amount) {
    return this.quantity >= amount && !this.manualDisabled;
  }

  public void decrementBy(Integer amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Cannot decrement by negative amount");
    }
    this.quantity -= amount;
  }

  public void incrementBy(Integer amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("Cannot increment by negative amount");
    }
    this.quantity += amount;
  }
}
