// Gestion d'erreur : definit le comportement d'erreur lie a fin track.

package com.fintrack.user.exception;

import java.util.Map;
import lombok.Getter;

// Exception de base pour toutes les anomalies specifiques a FinTrack.

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
}
