// Gestion d'erreur : definit le comportement associe aux codes d'erreur.

package com.fintrack.notification.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// Enumeration regroupant les codes d'erreurs standardises de l'application.

@Getter
public enum ErrorCode {
  // General
  INTERNAL_SERVER_ERROR(
    HttpStatus.INTERNAL_SERVER_ERROR,
    "error.internal_server_error"
  ),
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "error.invalid_input"),
  PERMISSION_DENIED(HttpStatus.FORBIDDEN, "error.permission_denied"),

  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "error.notification.not_found"),
  ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "error.entity.not_found"),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "error.duplicate_resource"),
  BUSINESS_RULE_VIOLATION(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.business_rule_violation"
  ),
  INVALID_STATUS_TRANSITION(
    HttpStatus.BAD_REQUEST,
    "error.invalid_status_transition"
  );

  private final HttpStatus httpStatus;
  private final String messageKey;

  ErrorCode(HttpStatus httpStatus, String messageKey) {
    this.httpStatus = httpStatus;
    this.messageKey = messageKey;
  }
}
