// Gestion d'erreur : definit le comportement associe aux codes d'erreur.

package com.fintrack.document.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// Enumeration des codes d'erreur du service, associant statut HTTP et cle de message i18n.
@Getter
public enum ErrorCode {
  // Erreurs generales
  INTERNAL_SERVER_ERROR(
    HttpStatus.INTERNAL_SERVER_ERROR,
    "error.internal_server_error"
  ),
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "error.invalid_input"),
  PERMISSION_DENIED(HttpStatus.FORBIDDEN, "error.permission_denied"),

  ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "error.attachment.not_found"),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "error.duplicate_resource"),
  ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "error.entity.not_found"),
  BUSINESS_RULE_VIOLATION(
    HttpStatus.UNPROCESSABLE_CONTENT,
    "error.business_rule_violation"
  ),

  FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.file.storage"),
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.file.not_found"),
  FILE_SIZE_EXCEEDED(
    HttpStatus.CONTENT_TOO_LARGE,
    "error.file.size_exceeded"
  ),
  UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "error.file.unsupported_type");

  private final HttpStatus httpStatus;
  private final String messageKey;

  ErrorCode(HttpStatus httpStatus, String messageKey) {
    this.httpStatus = httpStatus;
    this.messageKey = messageKey;
  }
}
