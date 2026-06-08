package com.jonathan.ecommerce.entity;

import com.jonathan.ecommerce.entity.enums.EventType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "processed_event")
public class ProcessedEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String email;

  @Enumerated(EnumType.STRING)
  private EventType eventType;

  @Column(unique = true)
  private String idemKey;

  private Instant processedAt;
}
