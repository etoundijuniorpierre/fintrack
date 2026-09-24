// Securite : applique l'authentification et les autorisations liees a user access guard.

package com.fintrack.user.security;

import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Garde de securite pour valider l'acces aux donnees utilisateur.

@Component("userAccess")
@RequiredArgsConstructor
public class UserAccessGuard {

  private static final String USER_VIEW_ALL = "USER_VIEW_ALL";
  private static final String USER_VIEW_AGENCY = "USER_VIEW_AGENCY";
  private static final String USER_VIEW_SERVICE = "USER_VIEW_SERVICE";
  private static final String SETTINGS_SYSTEM = "SETTINGS_SYSTEM";

  private final UserRepository userRepository;

  // Verifie la presence d'une autorite dans le contexte courant.
  public boolean hasAuthority(String authority) {
    return currentAuthentication()
      .map(authentication ->
        authentication
          .getAuthorities()
          .stream()
          .anyMatch(grantedAuthority ->
            authority.equals(grantedAuthority.getAuthority())
          )
      )
      .orElse(false);
  }

  // Verifie si self.

  public boolean isSelf(UUID userId) {
    return currentUserId().map(userId::equals).orElse(false);
  }

  /**
   * Indique si l'utilisateur courant possède le rôle SUPER_ADMIN. Seul un
   * Super Admin peut voir les comptes Super Admin dans les listes : un Admin,
   * bien qu'ayant USER_VIEW_ALL, voit tous les autres utilisateurs mais pas
   * les Super Admin.
   */
  @Transactional(readOnly = true)
  // Verifie si super admin.
  public boolean isSuperAdmin() {
    if (hasAuthority("ROLE_SUPER_ADMIN") || hasAuthority("SUPER_ADMIN")) {
      return true;
    }
    return currentUser()
      .map(user -> user.hasRole(RoleConstants.SUPER_ADMIN.getName()))
      .orElse(false);
  }

  @Transactional(readOnly = true)
  // Verifie si l'utilisateur courant peut consulter le profil demande.
  public boolean canViewUser(UUID targetUserId) {
    if (hasAuthority(USER_VIEW_ALL) || isSelf(targetUserId)) {
      return true;
    }
    Optional<User> targetUser = userRepository.findById(targetUserId);
    if (targetUser.isEmpty()) {
      return false;
    }
    if (hasAuthority(USER_VIEW_AGENCY)) {
      Optional<UUID> currentAgencyId = currentAgencyId();
      Optional<UUID> targetAgencyId = targetUser
        .map(User::getAgency)
        .map(agency -> agency.getId());
      return (
        currentAgencyId.isPresent() &&
        targetAgencyId.isPresent() &&
        currentAgencyId.get().equals(targetAgencyId.get())
      );
    }
    if (hasAuthority(USER_VIEW_SERVICE)) {
      Optional<UUID> currentServiceId = currentServiceId();
      Optional<UUID> targetServiceId = targetUser
        .map(User::getService)
        .map(service -> service.getId());
      return (
        currentServiceId.isPresent() &&
        targetServiceId.isPresent() &&
        currentServiceId.get().equals(targetServiceId.get())
      );
    }
    return false;
  }

  @Transactional(readOnly = true)
  // Verifie si l'utilisateur courant peut consulter l'agence demandee.
  public boolean canViewAgency(UUID agencyId) {
    if (hasAuthority(USER_VIEW_ALL) || hasAuthority(SETTINGS_SYSTEM)) {
      return true;
    }
    return (
      hasAuthority(USER_VIEW_AGENCY) &&
      currentAgencyId().map(agencyId::equals).orElse(false)
    );
  }

  @Transactional(readOnly = true)
  // Extrait l'agence portee par l'authentification courante.
  public Optional<UUID> currentAgencyId() {
    return currentUser()
      .map(User::getAgency)
      .map(agency -> agency.getId());
  }

  @Transactional(readOnly = true)
  // Extrait le service porte par l'authentification courante.
  public Optional<UUID> currentServiceId() {
    return currentUser()
      .map(User::getService)
      .map(service -> service.getId());
  }

  // Realise l'intention metier current user id.

  private Optional<UUID> currentUserId() {
    return currentAuthentication().flatMap(authentication -> {
      Object principal = authentication.getPrincipal();
      if (principal instanceof UserDetailsImpl userDetails) {
        return Optional.of(userDetails.getId());
      }
      try {
        return Optional.of(UUID.fromString(authentication.getName()));
      } catch (IllegalArgumentException ignored) {
        return Optional.empty();
      }
    });
  }

  // Realise l'intention metier current user.

  private Optional<User> currentUser() {
    Optional<UUID> id = currentUserId();
    if (id.isPresent()) {
      return userRepository.findById(id.get());
    }
    return currentAuthentication()
      .map(Authentication::getName)
      .flatMap(userRepository::findByUsername);
  }

  // Realise l'intention metier current authentication.

  private Optional<Authentication> currentAuthentication() {
    Authentication authentication =
      SecurityContextHolder.getContext().getAuthentication();
    if (
      authentication == null ||
      !authentication.isAuthenticated() ||
      "anonymousUser".equals(authentication.getPrincipal())
    ) {
      return Optional.empty();
    }
    return Optional.of(authentication);
  }
}
