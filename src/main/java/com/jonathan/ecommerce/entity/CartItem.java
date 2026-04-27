package com.jonathan.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cart_item")
public class CartItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JoinColumn(name = "cart")
  @ManyToOne(fetch = FetchType.LAZY)
  private Cart cart;

  @JoinColumn(name = "product")
  @ManyToOne(fetch = FetchType.LAZY)
  private Product product;

  private Integer quantity;

  private BigDecimal unitPrice;

  private BigDecimal subTotal;
}
