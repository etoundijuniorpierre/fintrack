// Gestion d'erreur : definit le comportement associe aux codes d'erreur.

package com.fintrack.reporting.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// Enumeration des codes d'erreur avec leur statut HTTP et leur cle de message i18n
@Getter
public enum ErrorCode {
  // Erreurs generales
  INTERNAL_SERVER_ERROR(
    HttpStatus.INTERNAL_SERVER_ERROR,
    "error.internal_server_error"
  ),
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "error.invalid_input"),
  PERMISSION_DENIED(HttpStatus.FORBIDDEN, "error.permission_denied"),

  REPORT_SCHEDULE_NOT_FOUND(
    HttpStatus.NOT_FOUND,
    "error.report_schedule.not_found"
  ),
  ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "error.entity.not_found"),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "error.duplicate_resource"),
  BUSINESS_RULE_VIOLATION(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.business_rule_violation"
  ),

  EXPORT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.export");

  private final HttpStatus httpStatus;
  private final String messageKey;

  ErrorCode(HttpStatus httpStatus, String messageKey) {
    this.httpStatus = httpStatus;
    this.messageKey = messageKey;
  }
}
