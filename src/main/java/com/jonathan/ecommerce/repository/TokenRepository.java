package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
