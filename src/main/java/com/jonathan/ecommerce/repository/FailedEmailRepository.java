package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.FailedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FailedEmailRepository extends JpaRepository<FailedEmail, Long> {
    Optional<FailedEmail> findByRecipientAndProductName(String recipient, String productName);
}
