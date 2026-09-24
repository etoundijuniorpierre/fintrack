// Gestion d'erreur : definit le comportement d'erreur lie a entity not found.

package com.fintrack.audit.exception;

// Exception levee lorsqu'une entite demandee est introuvable.
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
