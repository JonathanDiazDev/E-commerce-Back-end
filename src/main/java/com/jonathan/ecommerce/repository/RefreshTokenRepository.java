package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.RefreshToken;
import com.jonathan.ecommerce.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  List<RefreshToken> findAllByUserAndRevokedFalse(User user);

  @Modifying
  @Transactional
  @Query(
      """
    UPDATE RefreshToken rt
    SET rt.revoked = true
    WHERE rt.familyId = :familyId
    """)
  void revokeAllByFamilyId(@Param("familyId") UUID familyId);

  @Modifying
  @Transactional
  @Query(
      """
    UPDATE RefreshToken rt
    SET rt.revoked = true
    WHERE rt.user.id = :userId
      AND rt.revoked = false
""")
  void revokeAllByUserId(@Param("userId") Long userId);

  List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);
}
