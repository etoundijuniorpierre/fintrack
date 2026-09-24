// Composant backend : porte la logique liee a external user.

package com.fintrack.incident.model.readmodel;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modele interne d'un utilisateur resolu depuis user-service.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalUser {

  private UUID id;
  private String username;
  private String firstName;
  private String lastName;
  private String email;
  private boolean active;
  private Set<String> roles;
  private Set<String> permissions;
  private UUID agencyId;
  private UUID serviceId;
  private Set<UUID> managedServiceIds;
}
