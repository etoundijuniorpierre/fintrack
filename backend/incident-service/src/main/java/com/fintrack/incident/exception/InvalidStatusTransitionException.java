// Gestion d'erreur : definit le comportement d'erreur lie a invalid status transition.

package com.fintrack.incident.exception;

// Exception levee lors d'une transition de statut d'incident interdite par le workflow
public class InvalidStatusTransitionException extends FinTrackException {

  // Cree l'exception metier avec le code d'erreur associe.

  public InvalidStatusTransitionException(String message) {
    super(ErrorCode.INVALID_STATUS_TRANSITION, message);
  }
}
