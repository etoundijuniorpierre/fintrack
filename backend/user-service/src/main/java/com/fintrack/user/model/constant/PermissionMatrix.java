// Constantes metier : centralise les valeurs stables liees a permission matrix.

package com.fintrack.user.model.constant;

import com.fintrack.user.model.constant.audit.AuditPermission;
import com.fintrack.user.model.constant.incident.IncidentPermission;
import com.fintrack.user.model.constant.notification.NotificationPermission;
import com.fintrack.user.model.constant.reporting.ReportingPermission;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.constant.role.RolePermission;
import com.fintrack.user.model.constant.settings.SettingsPermission;
import com.fintrack.user.model.constant.user.UserPermission;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Matrice de definition des correspondances entre roles et permissions.

public class PermissionMatrix {

  // Fournit role matrice a la couche appelante.

  public static Map<String, List<String>> getRoleMatrix() {
    return Map.of(
      RoleConstants.SUPER_ADMIN.getName(),
      getAllPermissions()
        .stream()
        .filter(p -> !IncidentPermission.VALIDATION_DIRECTION.getName().equals(p))
        .collect(Collectors.toList()),

      RoleConstants.ADMIN.getName(),
      getAllPermissions()
        .stream()
        .filter(
          p ->
            !List.of(
              UserPermission.USER_CREATE_ADMIN.getName(),
              UserPermission.USER_CREATE_AGENT_AGENCY.getName(),
              UserPermission.USER_CREATE_AGENT_SERVICE.getName(),
              ReportingPermission.REPORT_VIEW_AGENCY.getName(),
              ReportingPermission.REPORT_VIEW_SERVICE.getName(),
              ReportingPermission.REPORT_VIEW_ALL.getName(),
              IncidentPermission.VALIDATION_DIRECTION.getName()
            ).contains(p)
        )
        .collect(Collectors.toList()),

      RoleConstants.CHEF_SERVICE.getName(),
      List.of(
        IncidentPermission.INCIDENT_CREATE.getName(),
        IncidentPermission.INCIDENT_UPDATE.getName(),
        IncidentPermission.INCIDENT_REOPEN.getName(),
        IncidentPermission.INCIDENT_VIEW_OWN.getName(),
        IncidentPermission.INCIDENT_VIEW_SERVICE.getName(),
        IncidentPermission.INCIDENT_VALIDATE.getName(),
        IncidentPermission.INCIDENT_REJECT.getName(),
        IncidentPermission.INCIDENT_TRANSFER.getName(),
        // Sans elle, valider un incident destine a son propre service le laissait
        // au statut Valide, sans routage ni assignation (cf. IncidentServiceImpl.validate).
        IncidentPermission.INCIDENT_AUTO_TRANSFER.getName(),
        IncidentPermission.INCIDENT_TRANSFER_WITH_REASON.getName(),
        IncidentPermission.INCIDENT_ASSIGN.getName(),
        IncidentPermission.INCIDENT_TREAT.getName(),
        IncidentPermission.INCIDENT_RESOLVE.getName(),
        IncidentPermission.INCIDENT_CLOSE.getName(),
        IncidentPermission.INCIDENT_CANCEL.getName(),
        UserPermission.USER_VIEW_SERVICE.getName(),
        UserPermission.USER_MANAGE_PROFILE.getName(),
        ReportingPermission.REPORT_VIEW_OWN.getName(),
        ReportingPermission.REPORT_VIEW_SERVICE.getName(),
        ReportingPermission.REPORT_GENERATE.getName(),
        ReportingPermission.REPORT_EXPORT.getName(),
        ReportingPermission.REPORT_DELETE.getName(),
        ReportingPermission.REPORT_SEND_EMAIL.getName(),
        ReportingPermission.DASHBOARD_CONFIGURE.getName(),
        NotificationPermission.NOTIFICATION_VIEW_OWN.getName(),
        NotificationPermission.NOTIFICATION_MANAGE.getName()
      ),

      RoleConstants.CHEF_AGENCE.getName(),
      List.of(
        IncidentPermission.INCIDENT_CREATE.getName(),
        IncidentPermission.INCIDENT_UPDATE.getName(),
        IncidentPermission.INCIDENT_REOPEN.getName(),
        IncidentPermission.INCIDENT_VIEW_OWN.getName(),
        IncidentPermission.INCIDENT_VIEW_AGENCY.getName(),
        IncidentPermission.INCIDENT_VALIDATE.getName(),
        IncidentPermission.INCIDENT_TRANSFER.getName(),
        IncidentPermission.INCIDENT_AUTO_TRANSFER.getName(),
        IncidentPermission.INCIDENT_REJECT.getName(),
        IncidentPermission.INCIDENT_ASSIGN.getName(),
        IncidentPermission.INCIDENT_TREAT.getName(),
        IncidentPermission.INCIDENT_RESOLVE.getName(),
        IncidentPermission.INCIDENT_CLOSE.getName(),
        IncidentPermission.INCIDENT_CANCEL.getName(),
        UserPermission.USER_VIEW_AGENCY.getName(),
        UserPermission.USER_MANAGE_PROFILE.getName(),
        ReportingPermission.REPORT_VIEW_OWN.getName(),
        ReportingPermission.REPORT_VIEW_AGENCY.getName(),
        ReportingPermission.REPORT_GENERATE.getName(),
        ReportingPermission.REPORT_EXPORT.getName(),
        ReportingPermission.REPORT_DELETE.getName(),
        ReportingPermission.REPORT_SEND_EMAIL.getName(),
        ReportingPermission.DASHBOARD_CONFIGURE.getName(),
        NotificationPermission.NOTIFICATION_VIEW_OWN.getName(),
        NotificationPermission.NOTIFICATION_MANAGE.getName()
      ),

      RoleConstants.AGENT.getName(),
      List.of(
        IncidentPermission.INCIDENT_CREATE.getName(),
        IncidentPermission.INCIDENT_UPDATE.getName(),
        IncidentPermission.INCIDENT_VIEW_OWN.getName(),
        IncidentPermission.INCIDENT_TREAT.getName(),
        IncidentPermission.INCIDENT_RESOLVE.getName(),
        IncidentPermission.INCIDENT_REOPEN.getName(),
        IncidentPermission.INCIDENT_CLOSE.getName(),
        IncidentPermission.INCIDENT_CANCEL.getName(),
        UserPermission.USER_MANAGE_PROFILE.getName(),
        ReportingPermission.REPORT_VIEW_OWN.getName(),
        ReportingPermission.REPORT_GENERATE.getName(),
        ReportingPermission.REPORT_EXPORT.getName(),
        ReportingPermission.REPORT_DELETE.getName(),
        ReportingPermission.REPORT_SEND_EMAIL.getName(),
        ReportingPermission.DASHBOARD_CONFIGURE.getName(),
        NotificationPermission.NOTIFICATION_VIEW_OWN.getName(),
        NotificationPermission.NOTIFICATION_MANAGE.getName()
      )
    );
  }

  // Plancher universel : permissions qu'aucun role ne doit perdre (coquille
  // applicative minimale). USER_MANAGE_PROFILE (gestion de son profil),
  // NOTIFICATION_VIEW_OWN (consulter ET supprimer ses notifications) et
  // DASHBOARD_CONFIGURE (page d'accueil). Impose en creation ET mise a jour.
  public static List<String> getBaselinePermissions() {
    return List.of(
      UserPermission.USER_MANAGE_PROFILE.getName(),
      NotificationPermission.NOTIFICATION_VIEW_OWN.getName(),
      NotificationPermission.NOTIFICATION_MANAGE.getName(),
      ReportingPermission.DASHBOARD_CONFIGURE.getName()
    );
  }

  // Fournit all permissions a la couche appelante.

  private static List<String> getAllPermissions() {
    return Stream.of(
      Arrays.stream(IncidentPermission.values()).map(
        IncidentPermission::getName
      ),
      Arrays.stream(UserPermission.values())
        .filter(permission -> !permission.isLegacy())
        .map(UserPermission::getName),
      Arrays.stream(RolePermission.values()).map(RolePermission::getName),
      Arrays.stream(ReportingPermission.values()).map(
        ReportingPermission::getName
      ),
      Arrays.stream(NotificationPermission.values()).map(
        NotificationPermission::getName
      ),
      Arrays.stream(SettingsPermission.values()).map(
        SettingsPermission::getName
      ),
      Arrays.stream(AuditPermission.values()).map(AuditPermission::getName)
    )
      .flatMap(stream -> stream)
      .collect(Collectors.toList());
  }
}
