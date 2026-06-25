package com.jonathan.ecommerce.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserDetailsServiceImpl userDetailsService;

  @Test
  void loadUserByUsername_Success() {
    User user = new User();
    user.setEmail("test@test.com");
    user.setPassword("password");

    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

    var result = userDetailsService.loadUserByUsername("test@test.com");

    assertEquals("test@test.com", result.getUsername());
    assertEquals("password", result.getPassword());
  }

  @Test
  void loadUserByUsername_NotFound() {
    when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

    assertThrows(
        UsernameNotFoundException.class,
        () -> userDetailsService.loadUserByUsername("notfound@test.com"));
  }
}
