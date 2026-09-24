// Securite : applique l'authentification et les autorisations liees a incident access guard.

package com.fintrack.incident.security;

import com.fintrack.incident.model.entity.Incident;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Contrôle de portée par incident, partagé par tous les contrôleurs
 * (incidents, commentaires, historique) afin que la vérification d'accès
 * à un incident précis soit appliquée de façon cohérente.
 */
@Component
// Modelise la responsabilite applicative liee a incident.
public class IncidentAccessGuard {

  // Verifie que les regles metier autorisent l operation sur incident.

  public boolean hasAuthority(UserDetailsImpl user, String authority) {
    return user
      .getAuthorities()
      .stream()
      .anyMatch(a -> authority.equalsIgnoreCase(a.getAuthority()));
  }

  /**
   * Vérifie que l'utilisateur a le droit de consulter cet incident précis :
   * vue globale, créateur, assigné, ou périmètre agence/service correspondant.
   */
  // Verifie que les regles metier autorisent l'operation sur incident.
  public void assertCanView(Incident incident, UserDetailsImpl user) {
    if (hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      return;
    }
    UUID uid = user.getId();
    if (
      uid != null &&
      (uid.equals(incident.getCreatedBy()) ||
        uid.equals(incident.getAssignedTo()))
    ) {
      return;
    }
    if (
      hasAuthority(user, "INCIDENT_VIEW_AGENCY") &&
      user.getAgencyId() != null &&
      user.getAgencyId().equals(incident.getAgencyId())
    ) {
      return;
    }
    if (
      hasAuthority(user, "INCIDENT_VIEW_SERVICE") &&
      user.getServiceId() != null &&
      (user.getServiceId().equals(incident.getCreatorServiceId()) ||
        user.getServiceId().equals(incident.getTransferredToService()))
    ) {
      return;
    }
    throw new AccessDeniedException(
      "Vous n'avez pas la permission d'acceder a cet incident"
    );
  }
}
