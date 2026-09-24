// DTO d'integration : transporte la liste des sessions utilisateur actives.

package com.fintrack.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Represente l'etat de presence retourne par notification-service.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PresenceResponse {

  @Builder.Default
  private Set<String> online = Set.of();
}
