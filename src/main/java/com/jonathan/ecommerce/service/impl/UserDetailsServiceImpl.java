package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    com.jonathan.ecommerce.entity.User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException("Usuario no encontrado con el email: " + email));

    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

    return new User(user.getEmail(), user.getPassword(), List.of(authority));
  }
}
