// Gestion d'erreur : definit le comportement d'erreur lie a business rule violation.

package com.fintrack.document.exception;

// Exception levee lorsqu'une regle metier est violee.
public class BusinessRuleViolationException extends FinTrackException {

  // Cree l'exception metier avec le code d'erreur associe.
  public BusinessRuleViolationException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
