// Gestion d'erreur : definit le comportement associe aux codes d'erreur.

package com.fintrack.incident.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// Enumeration des codes d'erreur, associant chaque cas a un statut HTTP et une cle de message i18n
@Getter
public enum ErrorCode {
  INTERNAL_SERVER_ERROR(
    HttpStatus.INTERNAL_SERVER_ERROR,
    "error.internal_server_error"
  ),
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "error.invalid_input"),
  PERMISSION_DENIED(HttpStatus.FORBIDDEN, "error.permission_denied"),

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

  INCIDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "error.incident.not_found"),
  INCIDENT_AGENCY_REQUIRED(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.incident.agency_required"
  ),
  INCIDENT_TYPE_NOT_FOUND(
    HttpStatus.NOT_FOUND,
    "error.incident.type_not_found"
  ),
  INCIDENT_TYPE_INACTIVE(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.incident.type_inactive"
  ),
  INCIDENT_TRANSFER_REASON_REQUIRED(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.incident.transfer_reason_required"
  ),
  INCIDENT_TRANSFER_SAME_SERVICE(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.incident.transfer_same_service"
  ),
  INCIDENT_REOPEN_REASON_REQUIRED(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.incident.reopen_reason_required"
  ),
  INCIDENT_START_REQUIRES_ASSIGNEE(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.incident.start_requires_assignee"
  ),
  INCIDENT_COMMENT_LOCKED(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.incident.comment_locked"
  ),
  INCIDENT_COMMENT_NOT_FOUND(
    HttpStatus.NOT_FOUND,
    "error.incident.comment_not_found"
  ),
  INCIDENT_COMMENT_WRONG_INCIDENT(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.incident.comment_wrong_incident"
  ),
  INCIDENT_COMMENT_NOT_AUTHOR(
    HttpStatus.FORBIDDEN,
    "error.incident.comment_not_author"
  ),
  INCIDENT_COMMENT_HAS_REPLY(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.incident.comment_has_reply"
  ),
  INCIDENT_COMMENT_SUPERSEDED(
    HttpStatus.UNPROCESSABLE_ENTITY,
    "error.incident.comment_superseded"
  ),
  INCIDENT_TYPE_CONFIG_NOT_FOUND(
    HttpStatus.NOT_FOUND,
    "error.incident_type_config.not_found"
  ),
  INCIDENT_TYPE_CONFIG_DUPLICATE_NAME(
    HttpStatus.CONFLICT,
    "error.incident_type_config.duplicate_name"
  ),

  STATS_CUSTOM_PERIOD_DATES_REQUIRED(
    HttpStatus.BAD_REQUEST,
    "error.stats.custom_period_dates_required"
  ),
  STATS_CUSTOM_PERIOD_DATE_ORDER(
    HttpStatus.BAD_REQUEST,
    "error.stats.custom_period_date_order"
  ),
  STATS_INVALID_COMPARISON_ENTITY(
    HttpStatus.BAD_REQUEST,
    "error.stats.invalid_comparison_entity"
  ),
  STATS_PERMISSION_DENIED(
    HttpStatus.FORBIDDEN,
    "error.stats.permission_denied"
  );

  private final HttpStatus httpStatus;
  private final String messageKey;

  ErrorCode(HttpStatus httpStatus, String messageKey) {
    this.httpStatus = httpStatus;
    this.messageKey = messageKey;
  }
}
