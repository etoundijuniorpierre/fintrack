// DTO : transporte les donnees d'erreur entre les couches.

package com.fintrack.audit.model.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO decrivant une reponse d'erreur standardisee renvoyee au client.
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
