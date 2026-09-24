// Gestion d'erreur : definit le comportement d'erreur lie a permission denied.

package com.fintrack.user.exception;

// Exception levee lorsqu'une action depasse les droits de l'utilisateur.

public class PermissionDeniedException extends FinTrackException {

  // Cree l'exception metier avec le code d'erreur associe.
  public PermissionDeniedException(String message) {
    super(ErrorCode.PERMISSION_DENIED, message);
  }
}
