// Entite metier : represente les donnees persistees liees a role.

package com.fintrack.user.model.entity;

import jakarta.persistence.*;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Entite JPA representant un profil utilisateur dote de permissions.

@Getter
@Setter
@ToString(exclude = { "permissions" })
@EqualsAndHashCode(callSuper = true, exclude = { "permissions" })
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  // Libelle lisible affiche dans l'UI (ex. Chef d'agence ). Optionnel :
  // si absent, le frontend retombe sur un mapping i18n puis sur le nom technique.
  @Column(name = "display_name", length = 100)
  private String displayName;

  @Column(length = 1000)
  private String description;

  @Column(name = "is_system", nullable = false)
  private boolean isSystem = false;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "role_permissions",
    joinColumns = @JoinColumn(name = "role_id"),
    inverseJoinColumns = @JoinColumn(name = "permission_id")
  )
  private Set<Permission> permissions;
}
