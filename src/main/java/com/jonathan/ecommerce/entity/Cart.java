package com.jonathan.ecommerce.entity;

import static jakarta.persistence.FetchType.LAZY;

import com.jonathan.ecommerce.entity.enums.CartStatus;
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
@Table(name = "cart")
public class Cart {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JoinColumn(name = "user_id")
  @ManyToOne(fetch = LAZY)
  private User user;

  private BigDecimal total = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CartStatus cartStatus;
}
