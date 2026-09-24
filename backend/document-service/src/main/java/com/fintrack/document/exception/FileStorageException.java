// Gestion d'erreur : definit le comportement d'erreur lie a file storage.

package com.fintrack.document.exception;

// Exception levee lors d'une erreur de stockage ou de recuperation de fichier.
public class FileStorageException extends FinTrackException {

  // Cree l'exception metier avec le code d'erreur associe.
  public FileStorageException(String message) {
    super(ErrorCode.FILE_STORAGE_ERROR, message);
  }

  // Cree l'exception metier avec le code d'erreur associe.

  public FileStorageException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  // Cree l'exception avec cause.
  public FileStorageException(String message, Throwable cause) {
    super(ErrorCode.FILE_STORAGE_ERROR, message, cause);
  }

  // Cree l'exception avec cause et code specifique.
  public FileStorageException(
    ErrorCode errorCode,
    String message,
    Throwable cause
  ) {
    super(errorCode, message, cause);
  }
}
