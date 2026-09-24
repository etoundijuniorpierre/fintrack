// Politique de classification des roles : raisonne par propriete structurelle
// plutot que par comparaison du nom litteral "AGENT", afin de couvrir aussi
// les roles crees a l'execution (ex. Caissier, Comptable).

package com.fintrack.user.model.constant.role;

import com.fintrack.user.model.constant.user.UserPermission;
import java.util.Collection;
import java.util.Set;

// Classe les roles en globaux (sans perimetre) vs operationnels, et en encadrement
// vs subordonne. Tout role non-global est operationnel (agence requise) ; tout role
// non-encadrement est un subordonne gerable par un chef.
public final class RolePolicy {

  private RolePolicy() {}

  // Seuls roles legitimement rattaches a aucune agence.
  private static final Set<String> GLOBAL_ROLES = Set.of(
    RoleConstants.ADMIN.getName(),
    RoleConstants.SUPER_ADMIN.getName()
  );

  // Un createur restreint (chef) ne peut ni les creer ni gerer leurs porteurs.
  private static final Set<String> MANAGEMENT_ROLES = Set.of(
    RoleConstants.ADMIN.getName(),
    RoleConstants.SUPER_ADMIN.getName(),
    RoleConstants.CHEF_AGENCE.getName(),
    RoleConstants.CHEF_SERVICE.getName()
  );

  // Authorities qui elevent un role au rang d'encadrement quel que soit son nom :
  // Garde anti-escalade quand un createur restreint attribue un role eleve.
  // un role custom.
  private static final Set<String> ELEVATED_AUTHORITIES = Set.of(
    UserPermission.USER_CREATE_ALL_AGENT.getName(),
    UserPermission.USER_CREATE_AGENT_AGENCY.getName(),
    UserPermission.USER_CREATE_AGENT_SERVICE.getName(),
    UserPermission.USER_CREATE_CHEF_AGENCE.getName(),
    UserPermission.USER_CREATE_CHEF_SERVICE.getName(),
    UserPermission.USER_CREATE_ADMIN.getName(),
    UserPermission.USER_UPDATE.getName(),
    UserPermission.USER_DELETE.getName(),
    UserPermission.USER_VIEW_ALL.getName(),
    RolePermission.ROLE_CREATE.getName(),
    RolePermission.ROLE_UPDATE.getName(),
    RolePermission.ROLE_DELETE.getName(),
    RolePermission.ROLE_ASSIGN.getName()
  );

  // Vrai si le role est global (aucun rattachement agence requis).
  public static boolean isGlobal(String roleName) {
    return GLOBAL_ROLES.contains(roleName);
  }

  // Vrai si au moins un role impose un rattachement a une agence.
  public static boolean requiresAgency(Collection<String> roleNames) {
    return (
      roleNames != null &&
      roleNames.stream().anyMatch(name -> name != null && !isGlobal(name))
    );
  }

  // Vrai si le role releve de l'encadrement.
  public static boolean isManagement(String roleName) {
    return MANAGEMENT_ROLES.contains(roleName);
  }

  // Vrai si tous les roles fournis sont des subordonnes (aucun role d'encadrement).
  public static boolean isSubordinate(Collection<String> roleNames) {
    return (
      roleNames != null &&
      !roleNames.isEmpty() &&
      roleNames.stream().noneMatch(RolePolicy::isManagement)
    );
  }

  // Vrai si l'ensemble d'authorities confere un pouvoir d'encadrement/eleve.
  public static boolean grantsElevatedAuthority(
    Collection<String> authorities
  ) {
    return (
      authorities != null &&
      authorities.stream().anyMatch(ELEVATED_AUTHORITIES::contains)
    );
  }
}
