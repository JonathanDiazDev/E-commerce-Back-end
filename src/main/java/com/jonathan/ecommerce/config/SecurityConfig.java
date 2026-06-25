package com.jonathan.ecommerce.config;

import com.jonathan.ecommerce.exception.CustomAccessDeniedHandler;
import com.jonathan.ecommerce.exception.CustomAuthenticationEntryPoint;
import com.jonathan.ecommerce.ratelimiting.filter.RateLimitingFilter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final UserDetailsService userDetailsService;
  private final JwtAuthenticationFilter jwtAuthFilter;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final Optional<RateLimitingFilter> rateLimitingFilter;

  private static final String[] PUBLIC_URLS = {
    "/api/v1/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**"
  };

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(PUBLIC_URLS)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/products/**")
                    .permitAll()
                    .requestMatchers("/api/v1/webhooks/**")
                    .permitAll()
                    // Permisos específicos para rutas de POST
                    .requestMatchers(HttpMethod.POST, "/api/v1/order/checkout")
                    .hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/cart/**")
                    .hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/cart/**")
                    .hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/cart/**")
                    .hasAnyRole("USER", "ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    // Conditionally add RateLimitingFilter only if it's available
    rateLimitingFilter.ifPresent(
        filter -> http.addFilterBefore(filter, JwtAuthenticationFilter.class));

    return http.build();
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
    configuration.setAllowCredentials(true);
    configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
