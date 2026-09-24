// Gestion d'erreur : definit le comportement d'erreur lie a entity not found.

package com.fintrack.user.exception;

// Exception personnalisee levee lorsqu'une entite est introuvable.

public class EntityNotFoundException extends FinTrackException {

  // Cree l'exception metier avec le code d'erreur associe.
  public EntityNotFoundException(String message) {
    super(ErrorCode.ENTITY_NOT_FOUND, message);
  }

  // Cree l'exception metier avec le code d'erreur associe.

  public EntityNotFoundException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
