// Read-model : reglage e-mail d'un evenement (actif + utilisateurs exclus).
// Non persiste tel quel : reconstitue depuis les SystemSetting.

package com.fintrack.reporting.model.readmodel.superadmin;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Reglage d'envoi e-mail pour un evenement donne.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationEventSetting {

  /** Nom de l'evenement (EmailNotificationEvent). */
  private String event;
  /** Famille fonctionnelle (LIFECYCLE / SCHEDULED / DIRECTION). */
  private String category;
  /** Envoi e-mail autorise pour cet evenement. */
  private boolean enabled;
  /** Vrai si l'evenement accepte une liste d'exclusion. */
  private boolean supportsExclusion;
  /** Utilisateurs a exclure de la reception (meme admins). */
  private List<UUID> excludedUserIds;
}
