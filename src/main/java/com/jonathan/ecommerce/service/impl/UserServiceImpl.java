package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.AuthRequest;
import com.jonathan.ecommerce.dto.AuthResponse;
import com.jonathan.ecommerce.dto.UserRequest;
import com.jonathan.ecommerce.dto.UserResponse;
import com.jonathan.ecommerce.entity.Role;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.repository.UserRepository;
import com.jonathan.ecommerce.service.JwtService;
import com.jonathan.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public UserResponse register(UserRequest request){
        if (userRepository.existsByEmail(request.email())){
            throw new IllegalArgumentException("El email ya está registrado");
        }
        User user  = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String accessToken = jwtService.generateToken(userDetails);
        return new AuthResponse(accessToken);
    }

}
