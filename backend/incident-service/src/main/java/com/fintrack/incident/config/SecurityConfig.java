// Configuration Spring : declare les regles techniques liees a security.

package com.fintrack.incident.config;

import com.fintrack.incident.security.InternalServiceAuthenticationFilter;
import com.fintrack.incident.security.JwtAuthenticationFilter;
import com.fintrack.incident.security.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Configuration de la securite web : sessions sans etat, filtre JWT, CORS et regles d'acces aux endpoints
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtUtils jwtUtils;
  private final InternalServiceAuthenticationFilter internalServiceAuthenticationFilter;

  @Value("${fintrack.allowed-origins}")
  private String allowedOrigins;

  // Declare le comportement technique attendu par l'infrastructure.

  @Bean
  public JwtAuthenticationFilter authenticationJwtTokenFilter() {
    return new JwtAuthenticationFilter(jwtUtils);
  }

  // Definit la chaine de filtres : Swagger et actuator publics, tout le reste authentifie
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      .exceptionHandling(exception ->
        exception.authenticationEntryPoint(
          (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response
              .getWriter()
              .write(
                "{\"error\": \"Unauthorized\", \"message\": \"" +
                  authException.getMessage() +
                  "\"}"
              );
          }
        )
      )
      .authorizeHttpRequests(auth ->
        auth
          .requestMatchers("/error")
          .permitAll()
          .requestMatchers(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
          )
          .permitAll()
          .requestMatchers("/actuator/shutdown")
          .hasRole("SUPER_ADMIN")
          .requestMatchers("/actuator/**")
          .permitAll()
          .anyRequest()
          .authenticated()
      );

    http.addFilterBefore(
      internalServiceAuthenticationFilter,
      UsernamePasswordAuthenticationFilter.class
    );
    http.addFilterBefore(
      authenticationJwtTokenFilter(),
      UsernamePasswordAuthenticationFilter.class
    );

    return http.build();
  }

  // Configure les origines, methodes et en-tetes autorises pour le CORS
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
    configuration.setAllowedMethods(
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    );
    configuration.setAllowedHeaders(
      List.of("Authorization", "Content-Type", "X-Requested-With")
    );
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source =
      new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
