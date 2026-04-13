package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrue();
    Optional<Category> findByName(String name);
    List<Category> findByParentCategoryIsNullAndActiveTrue();

}
