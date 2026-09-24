// Gestion d'erreur : definit le comportement d'erreur lie a global exception handler.

package com.fintrack.notification.exception;

import com.fintrack.notification.constant.ApiConstants;
import com.fintrack.notification.model.dto.response.ErrorResponse;
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
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Gestionnaire global d'exceptions : transforme les erreurs notification en reponses HTTP localisees.

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

    ErrorResponse response = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(ErrorCode.INVALID_INPUT.getHttpStatus().value())
      .error(ErrorCode.INVALID_INPUT.getHttpStatus().getReasonPhrase())
      .code(ErrorCode.INVALID_INPUT.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();

    return new ResponseEntity<>(
      response,
      ErrorCode.INVALID_INPUT.getHttpStatus()
    );
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

  // Signale les methodes HTTP non supportees avec un statut 405 coherent.
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
    HttpRequestMethodNotSupportedException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    log.warn(
      "[{}] Méthode non supportée a {}: {}",
      correlationId,
      request.getRequestURI(),
      ex.getMessage()
    );
    return buildErrorResponse(
      HttpStatus.METHOD_NOT_ALLOWED,
      "METHOD_NOT_ALLOWED",
      localizedMessage("error.method_not_allowed"),
      request,
      correlationId
    );
  }

  // Ignore les reponses SockJS deja fermees et normalise les autres echecs de serialisation.
  @ExceptionHandler(HttpMessageNotWritableException.class)
  public ResponseEntity<?> handleMessageNotWritable(
    HttpMessageNotWritableException ex,
    HttpServletRequest request
  ) {
    String correlationId = getCorrelationId();
    if (isSockJsTransportRequest(request)) {
      log.debug(
        "[{}] Réponse de transport SockJS déjà fermée a {}: {}",
        correlationId,
        request.getRequestURI(),
        ex.getMessage()
      );
      return ResponseEntity.noContent().build();
    }

    log.error(
      "[{}] Échec de sérialisation de la réponse a {}: {}",
      correlationId,
      request.getRequestURI(),
      ex.getMessage(),
      ex
    );
    return buildErrorResponse(
      HttpStatus.INTERNAL_SERVER_ERROR,
      ErrorCode.INTERNAL_SERVER_ERROR.name(),
      localizedMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessageKey()),
      request,
      correlationId
    );
  }

  // Construit la representation de notification attendue par le cas d'usage.
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

  // Verifie si la requete cible un transport SockJS.

  private boolean isSockJsTransportRequest(HttpServletRequest request) {
    return request
      .getRequestURI()
      .startsWith(ApiConstants.API_BASE_PATH + "/ws/");
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
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
      .code(ErrorCode.INTERNAL_SERVER_ERROR.name())
      .message(localizedMessage)
      .path(request.getRequestURI())
      .correlationId(correlationId)
      .build();

    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
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
