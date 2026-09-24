// Entite metier : represente les donnees persistees liees a user.

package com.fintrack.user.model.entity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Entite metier representant utilisateur.

@Getter
@Setter
@ToString(
  exclude = {
    "roles",
    "permissions",
    "revokedPermissions",
    "agency",
    "service",
    "managedServices",
    "managedAgency",
  }
)
@EqualsAndHashCode(
  callSuper = true,
  exclude = {
    "roles",
    "permissions",
    "revokedPermissions",
    "agency",
    "service",
    "managedServices",
    "managedAgency",
  }
)
@Entity
@Table(name = "users")
@Access(AccessType.FIELD)
public class User extends BaseEntity {

  @Column(nullable = false, unique = true, length = 100)
  private String username;

  @Column(unique = true, length = 100)
  private String email;

  @Column(unique = true, length = 20)
  private Long phoneNumber;

  @Column(name = "password", length = 255)
  private String password;

  @Column(name = "first_name", length = 100)
  private String firstName;

  @Column(name = "last_name", length = 100)
  private String lastName;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = false;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts = 0;

  @Column(name = "last_login")
  private LocalDateTime lastLogin;

  @Column(name = "is_first_login", nullable = false)
  private boolean isFirstLogin = true;

  @Column(name = "avatar_document_id")
  private UUID avatarDocumentId;

  @Column(name = "temp_password")
  private String tempPassword;

  @Column(name = "temp_password_created_at")
  private LocalDateTime tempPasswordCreatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agency_id")
  private Agency agency;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id")
  private ServiceEntity service;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "user_managed_services",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "service_id")
  )
  private Set<ServiceEntity> managedServices;

  @OneToOne(mappedBy = "headOfAgency", fetch = FetchType.LAZY)
  private Agency managedAgency;

  @NotEmpty
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private Set<Role> roles;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "user_permissions",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "permission_id")
  )
  private Set<Permission> permissions;

  // Permissions explicitement retirees a cet utilisateur : soustraites de l'union
  // directes ∪ role lors du calcul des permissions effectives (permet de retirer
  // une permission heritee d'un role pour un utilisateur precis).
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "user_revoked_permissions",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "permission_id")
  )
  private Set<Permission> revokedPermissions;

  // Verifie que les regles metier autorisent l operation sur service interne.

  public boolean hasRole(String roleName) {
    return (
      roles != null &&
      roles.stream().anyMatch(role -> role.getName().equals(roleName))
    );
  }
}
