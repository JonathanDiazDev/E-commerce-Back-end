package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

  @EntityGraph(value = "Cart.withUser")
  Optional<Cart> findByUserId(@Param("userId") Long id);
}
