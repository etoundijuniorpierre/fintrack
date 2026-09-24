// Configuration Spring : declare les regles techniques liees a validation.

package com.fintrack.document.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

// Relie Bean Validation au MessageSource pour localiser les messages de contrainte.
@Configuration
// Regroupe la configuration Spring necessaire au module.
public class ValidationConfig {

  // Expose le validateur Bean Validation de l'application.
  @Bean
  public LocalValidatorFactoryBean defaultValidator(
    MessageSource messageSource
  ) {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setValidationMessageSource(messageSource);
    return factory;
  }
}
