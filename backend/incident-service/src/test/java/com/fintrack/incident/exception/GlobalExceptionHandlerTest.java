// Tests d'integration legers : verifient la localisation des erreurs metier exposees par l'API.

package com.fintrack.incident.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;

// Valide que le gestionnaire traduit une meme cle selon la langue de la requete.
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;
  private MockHttpServletRequest request;

  // Configure le catalogue de messages reel utilise en production.
  @BeforeEach
  void setUp() {
    ResourceBundleMessageSource messageSource =
      new ResourceBundleMessageSource();
    messageSource.setBasename("i18n/messages");
    messageSource.setDefaultEncoding("UTF-8");
    handler = new GlobalExceptionHandler(messageSource);
    request = new MockHttpServletRequest("GET", "/api/v1/incidents/dashboard");
  }

  // Nettoie la locale attachee au thread entre deux tests.
  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
  }

  // Retourne le message francais lorsque la requete utilise le francais.
  @Test
  void handleFinTrackException_FrenchLocale_ReturnsFrenchMessage() {
    LocaleContextHolder.setLocale(Locale.FRENCH);

    var response = handler.handleFinTrackException(
      new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "dashboard.error.scope_filters_mutually_exclusive"
      ),
      request
    );

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo(
      "Les filtres agence et service ne peuvent pas être utilisés simultanément."
    );
  }

  // Retourne le message anglais lorsque la requete utilise l'anglais.
  @Test
  void handleFinTrackException_EnglishLocale_ReturnsEnglishMessage() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    var response = handler.handleFinTrackException(
      new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "dashboard.error.scope_filters_mutually_exclusive"
      ),
      request
    );

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo(
      "Agency and service filters cannot be used at the same time."
    );
  }
}
