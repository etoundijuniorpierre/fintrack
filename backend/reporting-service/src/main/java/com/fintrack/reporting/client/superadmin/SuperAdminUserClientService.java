// Client inter-services : communique avec les services externes lies a super admin user client.

package com.fintrack.reporting.client.superadmin;

import com.fintrack.reporting.client.superadmin.dto.SuperAdminUserClientResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Adapter de lecture des utilisateurs pour les vues Super Admin.
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminUserClientService {

  private final SuperAdminUserClient userClient;

  // Resout les usernames des detenteurs des roles (destinataires de notifs internes).
  public List<String> resolveUsernamesByRoles(
    String primaryRole,
    String secondaryRole
  ) {
    try {
      return userClient
        .getUsers()
        .stream()
        .filter(
          user ->
            hasRole(user, primaryRole) ||
            (secondaryRole != null &&
              !secondaryRole.equalsIgnoreCase(primaryRole) &&
              hasRole(user, secondaryRole))
        )
        .map(SuperAdminUserClientResponse::getUsername)
        .filter(username -> username != null && !username.isBlank())
        .distinct()
        .toList();
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de resolve les usernames pour roles {}/{}: {}",
        primaryRole,
        secondaryRole,
        ex.toString()
      );
      return List.of();
    }
  }

  // Verifie que les regles metier autorisent l operation sur rapport.

  private boolean hasRole(SuperAdminUserClientResponse user, String role) {
    if (user.getRoles() == null) {
      return false;
    }
    return user
      .getRoles()
      .stream()
      .anyMatch(item -> role.equalsIgnoreCase(item.getName()));
  }
}
