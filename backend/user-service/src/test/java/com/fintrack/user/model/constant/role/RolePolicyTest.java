// Tests : verifie la classification structurelle des roles (RolePolicy).

package com.fintrack.user.model.constant.role;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RolePolicyTest {

  @Test
  @DisplayName("isGlobal - only ADMIN and SUPER_ADMIN are global")
  void isGlobal_recognisesGlobalRoles() {
    assertThat(RolePolicy.isGlobal("ADMIN")).isTrue();
    assertThat(RolePolicy.isGlobal("SUPER_ADMIN")).isTrue();
    assertThat(RolePolicy.isGlobal("AGENT")).isFalse();
    assertThat(RolePolicy.isGlobal("CHEF_AGENCE")).isFalse();
    assertThat(RolePolicy.isGlobal("CAISSIER")).isFalse();
  }

  @Test
  @DisplayName(
    "requiresAgency - any non-global role (incl. custom) requires an agency"
  )
  void requiresAgency_trueForOperationalRoles() {
    assertThat(RolePolicy.requiresAgency(Set.of("AGENT"))).isTrue();
    assertThat(RolePolicy.requiresAgency(Set.of("CHEF_SERVICE"))).isTrue();
    // Role custom cree a l'execution : doit lui aussi exiger une agence.
    assertThat(RolePolicy.requiresAgency(Set.of("CAISSIER"))).isTrue();
    assertThat(
      RolePolicy.requiresAgency(Set.of("COMPTABLE", "ADMIN"))
    ).isTrue();
  }

  @Test
  @DisplayName("requiresAgency - false when only global roles or empty/null")
  void requiresAgency_falseForGlobalOnly() {
    assertThat(RolePolicy.requiresAgency(Set.of("ADMIN"))).isFalse();
    assertThat(
      RolePolicy.requiresAgency(Set.of("ADMIN", "SUPER_ADMIN"))
    ).isFalse();
    assertThat(RolePolicy.requiresAgency(Set.of())).isFalse();
    assertThat(RolePolicy.requiresAgency(null)).isFalse();
  }

  @Test
  @DisplayName(
    "isManagement - management roles are the system chiefs and admins"
  )
  void isManagement_recognisesManagementRoles() {
    assertThat(RolePolicy.isManagement("ADMIN")).isTrue();
    assertThat(RolePolicy.isManagement("SUPER_ADMIN")).isTrue();
    assertThat(RolePolicy.isManagement("CHEF_AGENCE")).isTrue();
    assertThat(RolePolicy.isManagement("CHEF_SERVICE")).isTrue();
    assertThat(RolePolicy.isManagement("AGENT")).isFalse();
    assertThat(RolePolicy.isManagement("CAISSIER")).isFalse();
  }

  @Test
  @DisplayName("isSubordinate - true only when no role is a management role")
  void isSubordinate_trueForNonManagementRoles() {
    assertThat(RolePolicy.isSubordinate(Set.of("AGENT"))).isTrue();
    assertThat(RolePolicy.isSubordinate(Set.of("CAISSIER"))).isTrue();
    assertThat(RolePolicy.isSubordinate(List.of("AGENT", "CAISSIER"))).isTrue();

    assertThat(RolePolicy.isSubordinate(Set.of("CHEF_AGENCE"))).isFalse();
    assertThat(RolePolicy.isSubordinate(Set.of("AGENT", "ADMIN"))).isFalse();
    assertThat(RolePolicy.isSubordinate(Set.of())).isFalse();
    assertThat(RolePolicy.isSubordinate(null)).isFalse();
  }

  @Test
  @DisplayName(
    "grantsElevatedAuthority - detects management authorities on a role"
  )
  void grantsElevatedAuthority_detectsEscalation() {
    assertThat(
      RolePolicy.grantsElevatedAuthority(Set.of("USER_CREATE_ADMIN"))
    ).isTrue();
    assertThat(
      RolePolicy.grantsElevatedAuthority(Set.of("USER_VIEW_ALL"))
    ).isTrue();
    assertThat(
      RolePolicy.grantsElevatedAuthority(Set.of("ROLE_ASSIGN"))
    ).isTrue();

    assertThat(
      RolePolicy.grantsElevatedAuthority(
        Set.of("INCIDENT_CREATE", "USER_MANAGE_PROFILE")
      )
    ).isFalse();
    assertThat(RolePolicy.grantsElevatedAuthority(Set.of())).isFalse();
    assertThat(RolePolicy.grantsElevatedAuthority(null)).isFalse();
  }
}
