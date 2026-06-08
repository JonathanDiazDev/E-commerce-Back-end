package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.dto.enums.OutboxStatus;
import com.jonathan.ecommerce.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
  List<OutboxEvent> findByOutboxStatus(OutboxStatus status);

  List<OutboxEvent> findTop50ByOutboxStatusOrderByCreatedAtAsc(OutboxStatus status);
}
