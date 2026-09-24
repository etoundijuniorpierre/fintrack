// Modele de lecture : decrit les derniers changements de presence d'un utilisateur.

package com.fintrack.notification.model.readmodel;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte l'etat courant et les horodatages reels issus des sessions WebSocket.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceState {

  private boolean online;
  private LocalDateTime connectedAt;
  private LocalDateTime disconnectedAt;
}
