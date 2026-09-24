// Gestion d'erreur : definit le comportement d'erreur lie a global exception handler.

package com.fintrack.user.exception;

import com.fintrack.user.model.dto.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
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

// Gestionnaire global d'exceptions : transforme les erreurs utilisateur en reponses HTTP localisees.

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

    String localizedMessage = localizedMessage(
      errorCode.getMessageKey(),
      ex.getMessage()
    );

    log.error(
      "[{}] Exception métier: {} - {} | precisions: {}",
      correlationId,
      errorCode.name(),
      localizedMessage,
      ex.getDetails()
    );

    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(errorCode.getHttpStatus().value())
      .error(errorCode.getHttpStatus().getReasonPhrase())
      .code(errorCode.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .details(ex.getDetails())
      .build();

    return new ResponseEntity<>(response, errorCode.getHttpStatus());
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

    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(ErrorCode.INVALID_INPUT.getHttpStatus().value())
      .error(ErrorCode.INVALID_INPUT.getHttpStatus().getReasonPhrase())
      .code(ErrorCode.INVALID_INPUT.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .details(details)
      .build();

    return new ResponseEntity<>(
      response,
      ErrorCode.INVALID_INPUT.getHttpStatus()
    );
  }

  // Retourne les violations de contraintes de parametres avec le detail par propriete.
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
    ConstraintViolationException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    Map<String, Object> details = new HashMap<>();

    ex.getConstraintViolations().forEach(violation ->
      details.put(
        violation.getPropertyPath().toString(),
        violation.getMessage()
      )
    );

    String localizedMessage = localizedMessage("error.constraint_violation");

    log.warn(
      "[{}] Violation de contrainte a {}: {}",
      correlationId,
      request.getRequestURI(),
      details
    );

    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(ErrorCode.INVALID_INPUT.getHttpStatus().value())
      .error(ErrorCode.INVALID_INPUT.getHttpStatus().getReasonPhrase())
      .code(ErrorCode.INVALID_INPUT.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .details(details)
      .build();

    return new ResponseEntity<>(
      response,
      ErrorCode.INVALID_INPUT.getHttpStatus()
    );
  }

  // Masque les details techniques des violations d'integrite de donnees.
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
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

    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.CONFLICT.value())
      .error(HttpStatus.CONFLICT.getReasonPhrase())
      .code(ErrorCode.DUPLICATE_RESOURCE.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();

    return new ResponseEntity<>(response, HttpStatus.CONFLICT);
  }

  // Signale les acces refuses par les regles de securite.
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
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

    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.FORBIDDEN.value())
      .error(HttpStatus.FORBIDDEN.getReasonPhrase())
      .code(ErrorCode.PERMISSION_DENIED.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();

    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
  }

  // Transforme les entites absentes JPA en reponse fonctionnelle 404.
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePersistenceEntityNotFoundException(
    EntityNotFoundException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.warn(
      "[{}] Entité persistante introuvable a {}: {}",
      correlationId,
      request.getRequestURI(),
      ex.getMessage()
    );
    String localizedMessage = localizedMessage(
      ErrorCode.ENTITY_NOT_FOUND.getMessageKey()
    );

    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.NOT_FOUND.value())
      .error(HttpStatus.NOT_FOUND.getReasonPhrase())
      .code(ErrorCode.ENTITY_NOT_FOUND.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();

    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  // Masque les erreurs de chargement relationnel derriere une erreur technique generique.
  @ExceptionHandler(LazyInitializationException.class)
  public ResponseEntity<ErrorResponse> handleLazyInitializationException(
    LazyInitializationException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.error(
      "[{}] Exception de chargement paresseux a {}: {}",
      correlationId,
      request.getRequestURI(),
      ex.getMessage()
    );
    String localizedMessage = localizedMessage(
      ErrorCode.INTERNAL_SERVER_ERROR.getMessageKey()
    );

    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
      .code(ErrorCode.INTERNAL_SERVER_ERROR.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();

    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // Masque les exceptions non prevues derriere une erreur technique generique.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(
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

    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value())
      .error(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().getReasonPhrase())
      .code(ErrorCode.INTERNAL_SERVER_ERROR.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();

    return new ResponseEntity<>(
      response,
      ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()
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

  // Recupere l'identifiant de correlation courant.

  private String getCorrelationId() {
    String correlationId = MDC.get("correlationId");
    return correlationId == null || correlationId.isEmpty()
      ? "req-" + UUID.randomUUID().toString().substring(0, 8)
      : correlationId;
  }
}
