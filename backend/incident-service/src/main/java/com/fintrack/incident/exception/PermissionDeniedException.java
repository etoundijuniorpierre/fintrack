// Gestion d'erreur : definit le comportement d'erreur lie a permission denied.

package com.fintrack.incident.exception;

// Exception levee lorsqu'un utilisateur ne dispose pas de la permission requise
public class PermissionDeniedException extends FinTrackException {

  // Cree l'exception metier avec le code d'erreur associe.

  public PermissionDeniedException(String message) {
    super(ErrorCode.PERMISSION_DENIED, message);
  }
}
