package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.AuthRequest;
import com.jonathan.ecommerce.dto.AuthResponse;
import com.jonathan.ecommerce.dto.UserRequest;
import com.jonathan.ecommerce.dto.UserResponse;
import com.jonathan.ecommerce.entity.RefreshToken;
import com.jonathan.ecommerce.entity.Role;
import com.jonathan.ecommerce.entity.Token;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.repository.RefreshTokenRepository;
import com.jonathan.ecommerce.repository.TokenRepository;
import com.jonathan.ecommerce.repository.UserRepository;
import com.jonathan.ecommerce.service.AuthService;
import com.jonathan.ecommerce.service.JwtService;
import com.jonathan.ecommerce.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

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
                user.getEmail(),
                user.getName(),
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
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));
        tokenRepository.revokeAllAccessTokensByUserId(user.getId());
        saveUserToken(user, accessToken);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setTokenHash(HashUtil.hashToken(refreshToken));
        newRefreshToken.setExpiresAt(Instant.now().plusMillis(jwtService.refreshTokenExpiration));
        newRefreshToken.setRevoked(false);
        refreshTokenRepository.save(newRefreshToken);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String hashedToken = HashUtil.hashToken(refreshToken);

        RefreshToken refreshToken1 = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new BadCredentialsException("Token no encontrado"));

        if (refreshToken1.isRevoked() || refreshToken1.getExpiresAt().isBefore(Instant.now()) ) {
            throw new BadCredentialsException("El Token ya no es valido");
        }

        String email = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtService.isRefreshTokenValid(refreshToken,  userDetails)) {
            throw new BadCredentialsException("Token invalido");
        }
        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        refreshToken1.setRevoked(true);
        refreshToken1.setReplacedByToken(HashUtil.hashToken(newAccessToken));
        refreshTokenRepository.save(refreshToken1);

        RefreshToken newRefreshTokenEntity = new RefreshToken();
        newRefreshTokenEntity.setUser(refreshToken1.getUser());
        newRefreshTokenEntity.setTokenHash(HashUtil.hashToken(newRefreshToken));
        newRefreshTokenEntity.setExpiresAt(Instant.now().plusMillis(jwtService.refreshTokenExpiration));
        newRefreshTokenEntity.setRevoked(false);
        refreshTokenRepository.save(newRefreshTokenEntity);

        User user = userRepository.findByEmail(email)
                .orElseThrow(()  -> new BadCredentialsException("Usuario no encontrado"));

        return  new AuthResponse(
                newAccessToken,
                newRefreshToken,
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );
    }

    @Override
    public void logout(String refreshToken, String  accessToken) {
        String hashedToken = HashUtil.hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(hashedToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
        tokenRepository.findByToken(accessToken).ifPresent(token -> {
            token.setRevoked(true);
            tokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void logoutAll(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        List<RefreshToken> refreshTokens =
                refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        refreshTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(refreshTokens);

        List<Token> accessTokens =
                tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(user.getId());
        accessTokens.forEach(token -> token.setRevoked(true));
        tokenRepository.saveAll(accessTokens);
    }

    private void saveUserToken(User user, String jwtToken) {
        Token token = new Token();
        token.setUser(user);
        token.setExpired(false);
        token.setRevoked(false);
        token.setToken(jwtToken);
        tokenRepository.save(token);
    }

}
