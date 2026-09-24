// Gestion d'erreur : definit le comportement d'erreur lie a invalid status transition.

package com.fintrack.user.exception;

// Exception levee lorsqu une transition de statut est interdite.

public class InvalidStatusTransitionException extends FinTrackException {

  // Cree l'exception metier avec le code d'erreur associe.
  public InvalidStatusTransitionException(String message) {
    super(ErrorCode.INVALID_STATUS_TRANSITION, message);
  }
}
