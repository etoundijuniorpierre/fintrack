// Tests du gestionnaire global : verifie les reponses HTTP localisees du service documentaire.

package com.fintrack.document.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.document.model.dto.response.ErrorResponse;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Verifie que les erreurs documentaires restent explicites sans exposer de details techniques.
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;
  private MockHttpServletRequest request;

  // Prepare les traductions necessaires aux scenarios de taille maximale.
  @BeforeEach
  void setUp() {
    StaticMessageSource messageSource = new StaticMessageSource();
    messageSource.addMessage(
      ErrorCode.FILE_SIZE_EXCEEDED.getMessageKey(),
      Locale.FRENCH,
      "La pièce jointe dépasse la limite autorisée de 150 Mo."
    );
    messageSource.addMessage(
      ErrorCode.FILE_SIZE_EXCEEDED.getMessageKey(),
      Locale.ENGLISH,
      "The attachment exceeds the allowed 150 MB limit."
    );
    handler = new GlobalExceptionHandler(messageSource);
    request = new MockHttpServletRequest(
      "POST",
      "/api/v1/documentService/attachments/upload"
    );
  }

  // Nettoie la langue de test pour ne pas contaminer les scenarios suivants.
  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
  }

  // Confirme la reponse francaise et le statut 413 pour un fichier trop lourd.
  @Test
  @DisplayName("oversized upload - Returns a localized French 413 response")
  void oversizedUpload_FrenchLocale_ReturnsLocalizedPayloadTooLarge() {
    LocaleContextHolder.setLocale(Locale.FRENCH);

    ResponseEntity<ErrorResponse> response = oversizedUploadResponse();

    assertThat(response.getStatusCode().value()).isEqualTo(413);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("FILE_SIZE_EXCEEDED");
    assertThat(response.getBody().getMessage()).isEqualTo(
      "La pièce jointe dépasse la limite autorisée de 150 Mo."
    );
  }

  // Confirme que la meme erreur suit la langue anglaise de l'interface.
  @Test
  @DisplayName("oversized upload - Returns a localized English 413 response")
  void oversizedUpload_EnglishLocale_ReturnsLocalizedPayloadTooLarge() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    ResponseEntity<ErrorResponse> response = oversizedUploadResponse();

    assertThat(response.getStatusCode().value()).isEqualTo(413);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo(
      "The attachment exceeds the allowed 150 MB limit."
    );
  }

  // Simule l'exception levee par Spring avant l'entree dans le controleur.
  private ResponseEntity<ErrorResponse> oversizedUploadResponse() {
    return handler.handleMaxUploadSizeExceededException(
      new MaxUploadSizeExceededException(150L * 1024 * 1024),
      request
    );
  }
}
