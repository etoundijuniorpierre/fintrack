// Tests unitaires : verifient les permissions attribuees aux roles systeme.

package com.fintrack.user.model.constant;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.user.model.constant.incident.IncidentPermission;
import com.fintrack.user.model.constant.reporting.ReportingPermission;
import com.fintrack.user.model.constant.role.RoleConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

// Protege la separation entre consultation et generation des rapports.
class PermissionMatrixTest {

  // Verifie que l'administrateur ne consulte que ses propres rapports.
  @Test
  void adminShouldNotViewReportsGeneratedByOtherUsers() {
    List<String> permissions = PermissionMatrix
      .getRoleMatrix()
      .get(RoleConstants.ADMIN.getName());

    assertThat(permissions)
      .contains(ReportingPermission.REPORT_VIEW_OWN.getName())
      .doesNotContain(
        ReportingPermission.REPORT_VIEW_AGENCY.getName(),
        ReportingPermission.REPORT_VIEW_SERVICE.getName(),
        ReportingPermission.REPORT_VIEW_ALL.getName()
      );
  }

  // Verifie que l'administrateur conserve tous les perimetres de generation.
  @Test
  void adminShouldGenerateReportsWithoutScopeRestriction() {
    List<String> permissions = PermissionMatrix
      .getRoleMatrix()
      .get(RoleConstants.ADMIN.getName());

    assertThat(permissions).contains(
      ReportingPermission.REPORT_GENERATE.getName(),
      ReportingPermission.REPORT_GENERATE_ALL_SCOPES.getName()
    );
  }

  // Verifie que le chef de service peut router automatiquement ce qu'il valide.
  @Test
  void serviceHeadShouldAutoRouteWhatTheyValidate() {
    List<String> permissions = PermissionMatrix
      .getRoleMatrix()
      .get(RoleConstants.CHEF_SERVICE.getName());

    // Valider sans INCIDENT_AUTO_TRANSFER laisse l'incident au statut Valide,
    // sans routage ni assignation : le chef valide dans le vide.
    assertThat(permissions).contains(
      IncidentPermission.INCIDENT_VALIDATE.getName(),
      IncidentPermission.INCIDENT_AUTO_TRANSFER.getName()
    );
  }

  // Verifie que le Super Admin conserve la consultation globale.
  @Test
  void superAdminShouldKeepGlobalReportVisibility() {
    List<String> permissions = PermissionMatrix
      .getRoleMatrix()
      .get(RoleConstants.SUPER_ADMIN.getName());

    assertThat(permissions).contains(
      ReportingPermission.REPORT_VIEW_ALL.getName(),
      ReportingPermission.REPORT_GENERATE_ALL_SCOPES.getName()
    );
  }
}
