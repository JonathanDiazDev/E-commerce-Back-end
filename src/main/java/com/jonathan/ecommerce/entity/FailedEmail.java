package com.jonathan.ecommerce.entity;

import com.jonathan.ecommerce.entity.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "failed_emails")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "product_name", nullable = false)
    private String productName; // Antes era 'pro'

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt; // El momento exacto del fallo

    @Column(name = "retry_count")
    private int retryCount;

    @Enumerated(EnumType.STRING)
    private EmailStatus status; // PENDING, SENT, FAILED
}