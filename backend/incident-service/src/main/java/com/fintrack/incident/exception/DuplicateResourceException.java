// Gestion d'erreur : definit le comportement d'erreur lie a duplicate resource.

package com.fintrack.incident.exception;

// Exception levee lors d'une tentative de creation d'une ressource deja existante
public class DuplicateResourceException extends FinTrackException {

  // Cree l'exception metier avec le code d'erreur associe.

  public DuplicateResourceException(String message) {
    super(ErrorCode.DUPLICATE_RESOURCE, message);
  }

  // Cree l'exception metier avec le code d'erreur associe.

  public DuplicateResourceException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
