package com.jonathan.ecommerce.entity;

import static jakarta.persistence.FetchType.LAZY;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jonathan.ecommerce.entity.enums.CartStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cart")
public class Cart {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = LAZY)
  @JoinColumn(name = "user_id", unique = true, nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CartStatus cartStatus;

  @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CartItem> items = new ArrayList<>();

  public void addItem(CartItem item) {
    this.items.add(item);
    item.setCart(this);
  }

  @JsonProperty("total") // Esto obliga a Jackson a llamar a la propiedad 'total' en el JSON
  public BigDecimal getTotal() {
    if (items == null || items.isEmpty()) {
      return BigDecimal.ZERO;
    }
    return items.stream()
        .filter(item -> item.getProduct() != null && item.getUnitPrice() != null)
        .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
