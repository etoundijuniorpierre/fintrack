// Configuration Spring : declare les regles techniques liees a validation.

package com.fintrack.incident.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Câble le validateur Bean Validation sur le MessageSource de l'application :
 * les clés de contrainte (ex. {validation.not_blank}) sont ainsi résolues
 * et localisées depuis i18n/messages_*.properties.
 */
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
