// Modele de lecture : rassemble un instantane coherent des presences utilisateur.

package com.fintrack.notification.model.readmodel;

import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Expose la liste en ligne et le dernier historique connu par utilisateur.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceSnapshot {

  @Builder.Default
  private Set<String> online = Set.of();

  @Builder.Default
  private Map<String, PresenceState> states = Map.of();
}
