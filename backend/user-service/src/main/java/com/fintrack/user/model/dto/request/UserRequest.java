// DTO : transporte les donnees liees a user entre les couches.

package com.fintrack.user.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

// DTO de requete pour creer ou modifier un compte utilisateur.
@Data
public class UserRequest {

  @Size(min = 3, max = 100)
  private String username;

  @NotBlank(message = "{user.email.required}")
  @Email(message = "{user.email.invalid}")
  private String email;

  @Min(value = 100000000L, message = "{user.phone.invalid}")
  @Max(value = 999999999999L, message = "{user.phone.invalid}")
  private Long phoneNumber;

  private String firstName;
  private String lastName;

  @AssertTrue(message = "{user.name.required}")
  public boolean isNameProvided() {
    return (
      (firstName != null && !firstName.trim().isEmpty()) ||
      (lastName != null && !lastName.trim().isEmpty())
    );
  }

  // Aligne la cle JSON sur "isActive" (celle du frontend et des reponses) :
  // sans cela Jackson lierait "active".
  @JsonProperty("isActive")
  private boolean isActive;

  private UUID agencyId;
  private UUID serviceId;
  private UUID avatarDocumentId;
  private Set<UUID> managedServiceIds;
  private UUID managedAgencyId;
  private Set<UUID> roleIds;
  private Set<UUID> permissionIds;
  private Set<UUID> revokedPermissionIds;
}
