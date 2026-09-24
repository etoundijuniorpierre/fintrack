// Configuration Spring : declare les regles techniques liees a password.

package com.fintrack.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Configuration de l'encodeur de mots de passe utilise pour hacher et verifier les mots de passe.
@Configuration
public class PasswordConfig {

  // Fournit un encodeur BCrypt comme bean d'encodage des mots de passe.
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
