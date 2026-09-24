// DTO : transporte les donnees d'erreur entre les couches.

package com.fintrack.document.model.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse standardise renvoye au client en cas d'erreur.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

  private LocalDateTime timestamp;
  private int status;
  private String error;
  private String code;
  private String message;
  private String path;
  private String correlationId;
  private Map<String, Object> details;
}
