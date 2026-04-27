package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

  @EntityGraph(attributePaths = {"products"})
  List<Category> findByActiveTrue();

    @Query("""
    SELECT c 
    FROM Category c 
    LEFT JOIN FETCH c.products 
    WHERE c.name = :name
    """)
    Optional<Category> findByName(@Param("name") String name);

  @Query(
      "SELECT c FROM Category c LEFT JOIN FETCH c.products "
          + "WHERE c.parentCategory IS NULL AND c.active = true")
  List<Category> findByParentCategoryIsNullAndActiveTrue();
}
