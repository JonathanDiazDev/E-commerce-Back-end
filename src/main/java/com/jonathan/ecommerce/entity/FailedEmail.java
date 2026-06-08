package com.jonathan.ecommerce.entity;

import com.jonathan.ecommerce.entity.enums.EmailStatus;
import com.jonathan.ecommerce.entity.enums.EmailType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "failed_emails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedEmail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String recipient;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EmailType emailType;

  @Column(columnDefinition = "TEXT")
  private String payload;

  @Column(columnDefinition = "TEXT")
  private String errorMessage;

  private Instant occurredAt;

  private int retryCount;

  @Enumerated(EnumType.STRING)
  private EmailStatus status; // PENDING, SENT, FAILED
}
