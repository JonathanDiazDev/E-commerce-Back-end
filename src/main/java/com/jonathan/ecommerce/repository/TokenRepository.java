package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByToken(String token);

    /**
     * Returns all active tokens (not expired and not revoked)
     * for a given user. Used during authentication to validate
     * that a JWT is still allowed.
     */
    List<Token> findAllByUserIdAndExpiredFalseAndRevokedFalse(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Token t SET t.revoked = true, t.expired = true WHERE t.user.id = :userId AND t.revoked = false")
    void revokeAllAccessTokensByUserId(@Param("userId") Long userId);
}
