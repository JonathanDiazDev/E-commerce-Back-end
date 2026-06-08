package com.jonathan.ecommerce.repository;

import com.jonathan.ecommerce.entity.FailedEmail;
import com.jonathan.ecommerce.entity.enums.EmailStatus;
import com.jonathan.ecommerce.entity.enums.EmailType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedEmailRepository extends JpaRepository<FailedEmail, Long> {

  Optional<FailedEmail> findByRecipientAndEmailType(String recipient, EmailType emailType);

  List<FailedEmail> findTop50ByStatus(EmailStatus status);
}
