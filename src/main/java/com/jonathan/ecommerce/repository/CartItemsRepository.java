package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemsRepository extends JpaRepository<CartItem, Long> {

  @Query(
      """
    SELECT ci
    FROM CartItem ci
    WHERE ci.cart.id = :cartId
      AND ci.product.id = :productId
""")
  Optional<CartItem> findByCartIdAndProductId(
      @Param("cartId") Long cartId, @Param("productId") Long productId);
}
