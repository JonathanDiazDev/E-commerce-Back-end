package com.jonathan.ecommerce.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_token")
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String tokenHash;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  private User user;

  private Instant expiresAt;

  private boolean revoked;

  private String replacedByTokenHash;

  @Column(nullable = false)
  private UUID familyId;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  private String userAgent;

  private String ipAddress;
}
