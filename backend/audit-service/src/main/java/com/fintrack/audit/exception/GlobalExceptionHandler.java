// Gestion d'erreur : definit le comportement d'erreur lie a global exception handler.

package com.fintrack.audit.exception;

import com.fintrack.audit.model.dto.response.ErrorResponse;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Gestionnaire global d'exceptions : transforme les erreurs serveur en reponses HTTP localisees.
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  // Traite ce cas applicatif et renvoie la reponse adaptee.

  @ExceptionHandler(FinTrackException.class)
  public ResponseEntity<ErrorResponse> handleFinTrackException(
    FinTrackException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    ErrorCode errorCode = ex.getErrorCode();
    String localizedMessage = messageSource.getMessage(
      errorCode.getMessageKey(),
      null,
      ex.getMessage(),
      LocaleContextHolder.getLocale()
    );

    log.error(
      "[{}] Exception métier: {} - {}",
      correlationId,
      errorCode.name(),
      localizedMessage
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

  // Traite ce cas applicatif et renvoie la reponse adaptee.

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

  // Traite ce cas applicatif et renvoie la reponse adaptee.

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
    ConstraintViolationException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    Map<String, Object> details = new HashMap<>();
    ex.getConstraintViolations().forEach(v ->
      details.put(v.getPropertyPath().toString(), v.getMessage())
    );
    String localizedMessage = localizedMessage("error.constraint_violation");

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

  // Traite ce cas applicatif et renvoie la reponse adaptee.

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
      .status(ErrorCode.PERMISSION_DENIED.getHttpStatus().value())
      .error(ErrorCode.PERMISSION_DENIED.getHttpStatus().getReasonPhrase())
      .code(ErrorCode.PERMISSION_DENIED.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();

    return new ResponseEntity<>(
      response,
      ErrorCode.PERMISSION_DENIED.getHttpStatus()
    );
  }

  // Traite ce cas applicatif et renvoie la reponse adaptee.

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
    HttpRequestMethodNotSupportedException ex,
    HttpServletRequest request
  ) {
    // Signale les methodes HTTP non supportees avec un statut 405 coherent.
    String correlationId = getCorrelationId();
    log.warn(
      "[{}] Méthode non supportée a {}: {}",
      correlationId,
      request.getRequestURI(),
      ex.getMessage()
    );
    String localizedMessage = localizedMessage("error.method_not_allowed");

    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.METHOD_NOT_ALLOWED.value())
      .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
      .code("METHOD_NOT_ALLOWED")
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();

    return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
  }

  // Normalise les corps JSON absents ou illisibles.
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(
    HttpMessageNotReadableException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.warn(
      "[{}] Corps de requête mal formé a {}: {}",
      correlationId,
      request.getRequestURI(),
      ex.getMessage()
    );
    return buildErrorResponse(
      HttpStatus.BAD_REQUEST,
      "MALFORMED_REQUEST",
      localizedMessage("error.malformed_request"),
      request,
      correlationId
    );
  }

  // Signale les parametres obligatoires absents.
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParam(
    MissingServletRequestParameterException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.warn(
      "[{}] Paramètre obligatoire manquant '{}' a {}",
      correlationId,
      ex.getParameterName(),
      request.getRequestURI()
    );
    return buildErrorResponse(
      HttpStatus.BAD_REQUEST,
      "MISSING_PARAMETER",
      localizedMessage("error.missing_parameter"),
      request,
      correlationId
    );
  }

  // Signale les parametres dont le type ou la valeur est invalide.
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
    MethodArgumentTypeMismatchException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.warn(
      "[{}] Type incompatible pour '{}' à {} : {}",
      correlationId,
      ex.getName(),
      request.getRequestURI(),
      ex.getMessage()
    );
    return buildErrorResponse(
      HttpStatus.BAD_REQUEST,
      "INVALID_PARAMETER",
      localizedMessage("error.invalid_parameter"),
      request,
      correlationId
    );
  }

  // Masque les details techniques des violations d'integrite de donnees.
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(
    DataIntegrityViolationException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.warn(
      "[{}] Violation d'intégrité des données a {}: {}",
      correlationId,
      request.getRequestURI(),
      ex.getMostSpecificCause().getMessage()
    );
    return buildErrorResponse(
      HttpStatus.CONFLICT,
      "DATA_INTEGRITY_VIOLATION",
      localizedMessage("error.data_integrity"),
      request,
      correlationId
    );
  }

  // Construit la representation de journal d'audit attendue par le cas d'usage.
  private ResponseEntity<ErrorResponse> buildErrorResponse(
    HttpStatus status,
    String code,
    String message,
    HttpServletRequest request,
    String correlationId
  ) {
    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(status.value())
      .error(status.getReasonPhrase())
      .code(code)
      .message(message)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();
    return new ResponseEntity<>(response, status);
  }

  // Traite ce cas applicatif et renvoie la reponse adaptee.

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
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
      .code(ErrorCode.INTERNAL_SERVER_ERROR.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();

    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // Resout le message localise associe au code d'erreur.

  private String localizedMessage(String messageKey) {
    return messageSource.getMessage(
      messageKey,
      null,
      messageKey,
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
