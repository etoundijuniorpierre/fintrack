// Gestion d'erreur : definit le comportement d'erreur lie a business rule violation.

package com.fintrack.user.exception;

import java.util.Map;

// Exception personnalisee levee en cas de violation d'une regle metier.

public class BusinessRuleViolationException extends FinTrackException {

  // Cree l'exception metier avec le code d'erreur associe.
  public BusinessRuleViolationException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  // Cree l'exception metier avec le code d'erreur associe.

  public BusinessRuleViolationException(
    ErrorCode errorCode,
    String message,
    Map<String, Object> details
  ) {
    super(errorCode, message, details);
  }
}
