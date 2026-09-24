// Gestion d'erreur : definit le comportement associe aux codes d'erreur.

package com.fintrack.user.exception;

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

  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.user.not_found"),
  DUPLICATE_USERNAME(HttpStatus.CONFLICT, "error.user.duplicate_username"),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "error.user.duplicate_email"),
  INVALID_CREDENTIALS(
    HttpStatus.UNAUTHORIZED,
    "error.user.invalid_credentials"
  ),
  TEMPORARY_PASSWORD_EXPIRED(
    HttpStatus.UNAUTHORIZED,
    "error.user.temporary_password_expired"
  ),
  ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "error.user.account_inactive"),
  ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "error.user.account_locked"),

  ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "error.entity.not_found"),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "error.duplicate_resource"),

  BUSINESS_RULE_VIOLATION(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.business_rule_violation"
  ),

  INVALID_STATUS_TRANSITION(
    HttpStatus.BAD_REQUEST,
    "error.invalid_status_transition"
  ),

  STATS_CUSTOM_PERIOD_DATES_REQUIRED(
    HttpStatus.BAD_REQUEST,
    "error.stats.custom_period_dates_required"
  ),
  STATS_CUSTOM_PERIOD_DATE_ORDER(
    HttpStatus.BAD_REQUEST,
    "error.stats.custom_period_date_order"
  ),
  MISSING_AGENCY(HttpStatus.UNPROCESSABLE_ENTITY, "error.user.missing_agency");

  private final HttpStatus httpStatus;
  private final String messageKey;

  ErrorCode(HttpStatus httpStatus, String messageKey) {
    this.httpStatus = httpStatus;
    this.messageKey = messageKey;
  }
}
