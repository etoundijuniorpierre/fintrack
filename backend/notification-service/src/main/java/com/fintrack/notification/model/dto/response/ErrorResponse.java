// DTO : transporte les donnees d'erreur entre les couches.

package com.fintrack.notification.model.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse structurant les informations d'erreur renvoyees par l'API.

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
