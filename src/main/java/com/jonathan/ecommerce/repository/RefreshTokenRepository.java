package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.RefreshToken;
import com.jonathan.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);
    Optional<RefreshToken> findByTokenHashAndUser(String tokenHash, User user);

    void deleteByUser(User user);
}
