// Gestion d'erreur : definit le comportement d'erreur lie a global exception handler.

package com.fintrack.incident.exception;

import com.fintrack.incident.model.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Gestionnaire global d'exceptions : transforme les erreurs incident en reponses HTTP localisees.
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  // Traite les exceptions metier selon leur code fonctionnel.
  @ExceptionHandler(FinTrackException.class)
  public ResponseEntity<ErrorResponse> handleFinTrackException(
    FinTrackException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    ErrorCode errorCode = ex.getErrorCode();

    String exceptionMessage = ex.getMessage();
    String localizedMessage = isMessageKey(exceptionMessage)
      ? localizedMessage(
        exceptionMessage,
        localizedMessage(errorCode.getMessageKey(), exceptionMessage)
      )
      : exceptionMessage;
    if (localizedMessage == null || localizedMessage.isBlank()) {
      localizedMessage = localizedMessage(errorCode.getMessageKey());
    }

    log.error(
      "[{}] Exception métier: {} - {} (origine : {}) | precisions: {}",
      correlationId,
      errorCode.name(),
      localizedMessage,
      ex.getMessage(),
      ex.getDetails()
    );

    return ResponseEntity.status(errorCode.getHttpStatus()).body(
      ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(errorCode.getHttpStatus().value())
        .error(errorCode.getHttpStatus().getReasonPhrase())
        .code(errorCode.name())
        .message(localizedMessage)
        .path(request.getRequestURI())
        .correlationId(correlationId)
        .details(ex.getDetails())
        .build()
    );
  }

  // Retourne les erreurs de validation de corps de requete avec le detail par champ.
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
    MethodArgumentNotValidException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    Map<String, Object> details = new HashMap<>();
    ex.getBindingResult()
      .getFieldErrors()
      .forEach(error ->
        details.put(error.getField(), error.getDefaultMessage())
      );

    String localizedMessage = localizedMessage("error.validation_failed");

    log.warn(
      "[{}] Erreur de validation a {}: {}",
      correlationId,
      request.getRequestURI(),
      details
    );

    return ResponseEntity.badRequest().body(
      ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .code(ErrorCode.INVALID_INPUT.name())
        .message(localizedMessage)
        .path(request.getRequestURI())
        .correlationId(correlationId)
        .details(details)
        .build()
    );
  }

  // Retourne les violations de contraintes de parametres avec le detail par propriete.
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
    ConstraintViolationException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    Map<String, Object> details = new HashMap<>();
    ex.getConstraintViolations().forEach(v ->
      details.put(v.getPropertyPath().toString(), v.getMessage())
    );
    String localizedMessage = localizedMessage("error.constraint_violation");

    log.warn(
      "[{}] Violation de contrainte a {}: {}",
      correlationId,
      request.getRequestURI(),
      details
    );

    return ResponseEntity.badRequest().body(
      ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .code(ErrorCode.INVALID_INPUT.name())
        .message(localizedMessage)
        .path(request.getRequestURI())
        .correlationId(correlationId)
        .details(details)
        .build()
    );
  }

  // Masque les details techniques des violations d'integrite en base.
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(
    DataIntegrityViolationException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.error(
      "[{}] Violation d'intégrité des données: {}",
      correlationId,
      ex.getMessage()
    );
    String localizedMessage = localizedMessage("error.data_integrity");

    return ResponseEntity.status(HttpStatus.CONFLICT).body(
      ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.CONFLICT.value())
        .error(HttpStatus.CONFLICT.getReasonPhrase())
        .code("DATA_INTEGRITY_VIOLATION")
        .message(localizedMessage)
        .path(request.getRequestURI())
        .correlationId(correlationId)
        .build()
    );
  }

  // Signale les acces refuses par les regles de securite.
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
    AccessDeniedException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.warn(
      "[{}] Accès refusé a {}: {}",
      correlationId,
      request.getRequestURI(),
      ex.getMessage()
    );

    String localizedMessage = localizedMessage(
      ErrorCode.PERMISSION_DENIED.getMessageKey()
    );

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
      ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.FORBIDDEN.value())
        .error(HttpStatus.FORBIDDEN.getReasonPhrase())
        .code(ErrorCode.PERMISSION_DENIED.name())
        .message(localizedMessage)
        .path(request.getRequestURI())
        .correlationId(correlationId)
        .build()
    );
  }

  // Signale les valeurs d'entree invalides sans exposer d'exception technique.
  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentTypeMismatchException.class,
  })
  public ResponseEntity<ErrorResponse> handleInvalidInput(
    Exception ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.warn(
      "[{}] Entrée invalide a {}: {}",
      correlationId,
      request.getRequestURI(),
      ex.getMessage()
    );

    String localizedMessage = localizedMessage(
      ErrorCode.INVALID_INPUT.getMessageKey()
    );

    return ResponseEntity.badRequest().body(
      ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .code(ErrorCode.INVALID_INPUT.name())
        .message(localizedMessage)
        .path(request.getRequestURI())
        .correlationId(correlationId)
        .build()
    );
  }

  // Masque les exceptions non prevues derriere une erreur technique generique.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(
    Exception ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.error(
      "[{}] Exception inattendue a {}: ",
      correlationId,
      request.getRequestURI(),
      ex
    );

    String localizedMessage = localizedMessage(
      ErrorCode.INTERNAL_SERVER_ERROR.getMessageKey()
    );

    return ResponseEntity.internalServerError().body(
      ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
        .code(ErrorCode.INTERNAL_SERVER_ERROR.name())
        .message(localizedMessage)
        .path(request.getRequestURI())
        .correlationId(correlationId)
        .build()
    );
  }

  // Resout un message localise a partir de sa cle de traduction.
  private String localizedMessage(String messageKey) {
    return localizedMessage(messageKey, messageKey);
  }

  // Resout le message localise associe au code d'erreur.

  private String localizedMessage(String messageKey, String fallback) {
    return messageSource.getMessage(
      messageKey,
      null,
      fallback,
      LocaleContextHolder.getLocale()
    );
  }

  // Identifie une cle i18n afin de ne jamais l'exposer telle quelle a l'interface.
  private boolean isMessageKey(String value) {
    return value != null && value.matches("[a-z0-9_.-]+");
  }

  // Recupere l'identifiant de correlation du MDC, ou en genere un court a defaut.
  private String getCorrelationId() {
    String id = MDC.get("correlationId");
    return id == null || id.isEmpty()
      ? "req-" + UUID.randomUUID().toString().substring(0, 8)
      : id;
  }
}
