package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      """
    UPDATE PasswordResetToken prt
    SET prt.used = true
    WHERE prt.user.id = :userId
      AND prt.used = false
""")
  void invalidateAllByUserId(@Param("userId") Long userId);
}
