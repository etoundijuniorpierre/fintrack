// Gestion d'erreur : definit le comportement d'erreur lie a fin track.

package com.fintrack.document.exception;

import java.util.Map;
import lombok.Getter;

// Exception de base de l'application portant un code d'erreur et des details optionnels.
@Getter
public abstract class FinTrackException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  // Cree une exception a partir du code d'erreur localisable.
  protected FinTrackException(ErrorCode errorCode) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
    this.details = null;
  }

  // Cree une exception avec un message de secours.
  protected FinTrackException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
    this.details = null;
  }

  // Cree une exception avec des details metier exposes par la reponse.
  protected FinTrackException(
    ErrorCode errorCode,
    String message,
    Map<String, Object> details
  ) {
    super(message);
    this.errorCode = errorCode;
    this.details = details;
  }

  // Cree une exception avec une cause.
  protected FinTrackException(
    ErrorCode errorCode,
    String message,
    Throwable cause
  ) {
    super(message, cause);
    this.errorCode = errorCode;
    this.details = null;
  }
}
