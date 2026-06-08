package com.jonathan.ecommerce.service.helper;

import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityHelper {
  private final UserRepository userRepository;

  public String getCurrentUserEmail() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }

  public User getCurrentUser() {
    String email = getCurrentUserEmail();
    return userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new UsernameNotFoundException("User with email: " + email + "Not Found"));
  }
}
