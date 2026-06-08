package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
  boolean existsByIdemKey(String idemKey);
}
