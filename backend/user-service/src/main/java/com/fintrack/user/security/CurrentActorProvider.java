// Securite : applique l'authentification et les autorisations liees a current actor provider.

package com.fintrack.user.security;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Fournit l'identité de l'utilisateur authentifié à l'origine de l'action courante.
 * Centralise la lecture du {@code SecurityContext} pour que tous les services
 * estampillent leurs journaux d'audit avec le même acteur.
 */
@Component
// Modelise la responsabilite applicative liee a utilisateur.
public class CurrentActorProvider {

  /** Utilisateur authentifié courant, ou {@code null} hors contexte de requête. */
  // Realise l'intention metier current.
  public UserDetailsImpl current() {
    Authentication authentication =
      SecurityContextHolder.getContext().getAuthentication();
    if (
      authentication != null &&
      authentication.getPrincipal() instanceof UserDetailsImpl actor
    ) {
      return actor;
    }
    return null;
  }

  /** Identifiant de l'acteur courant, ou {@code null}. */
  // Realise l'intention metier current id.
  public UUID currentId() {
    UserDetailsImpl actor = current();
    return actor != null ? actor.getId() : null;
  }

  /** Nom d'utilisateur de l'acteur courant, ou {@code null}. */
  // Realise l'intention metier current username.
  public String currentUsername() {
    UserDetailsImpl actor = current();
    return actor != null ? actor.getUsername() : null;
  }

  /** Rôles (ROLE_*) de l'acteur courant, ou {@code null} hors contexte de requête. */
  // Realise l'intention metier current roles.
  public List<String> currentRoles() {
    UserDetailsImpl actor = current();
    if (actor == null) {
      return null;
    }
    return actor
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .filter(authority -> authority.startsWith("ROLE_"))
      .toList();
  }
}
