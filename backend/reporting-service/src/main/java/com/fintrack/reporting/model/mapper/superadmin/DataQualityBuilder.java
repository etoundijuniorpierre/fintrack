// Mapper : convertit les donnees liees a data quality builder entre modeles.

package com.fintrack.reporting.model.mapper.superadmin;

import com.fintrack.reporting.model.constant.IncidentStatusSets;
import static com.fintrack.reporting.model.mapper.superadmin.SuperAdminMaps.addIssue;

import com.fintrack.reporting.client.incident.dto.IncidentClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentTypeConfigClientResponse;
import com.fintrack.reporting.client.superadmin.dto.AgencyClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.ServiceClientResponse;
import com.fintrack.reporting.client.superadmin.dto.SuperAdminUserClientResponse;
import com.fintrack.reporting.model.dto.response.superadmin.DataQualityIssueResponse;
import com.fintrack.reporting.model.dto.response.superadmin.DataQualityOverviewResponse;
import com.fintrack.reporting.model.dto.response.superadmin.SimpleEntityResponse;
import com.fintrack.reporting.model.dto.response.superadmin.UnusedIncidentTypeResponse;
import com.fintrack.reporting.model.entity.GeneratedReport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

// Assure les conversions du domaine data quality builder.

@Component
public class DataQualityBuilder {

  // Construit la representation de rapport attendue par le cas d'usage.
  public DataQualityOverviewResponse build(
    List<IncidentClientResponse> incidents,
    List<SuperAdminUserClientResponse> users,
    List<AgencyClientResponse> agencies,
    List<ServiceClientResponse> services,
    NotificationStatsClientResponse notificationStats,
    List<GeneratedReport> reports,
    List<IncidentTypeConfigClientResponse> incidentTypeConfigs
  ) {
    Set<String> inactiveUserIds = users
      .stream()
      .filter(user -> !user.isActive())
      .map(user -> user.getId() != null ? user.getId().toString() : "")
      .filter(val -> !val.isBlank())
      .collect(Collectors.toSet());

    List<SimpleEntityResponse> agenciesWithoutHead = agencies
      .stream()
      .filter(
        agency ->
          agency.getHeadOfAgency() == null ||
          agency.getHeadOfAgency().getId() == null
      )
      .map(agency ->
        new SimpleEntityResponse(
          agency.getId() != null ? agency.getId().toString() : "",
          agency.getName() != null ? agency.getName() : ""
        )
      )
      .toList();

    List<SimpleEntityResponse> servicesWithoutHead = services
      .stream()
      .filter(
        service ->
          service.getHeadOfService() == null ||
          service.getHeadOfService().getId() == null
      )
      .map(service ->
        new SimpleEntityResponse(
          service.getId() != null ? service.getId().toString() : "",
          service.getName() != null ? service.getName() : ""
        )
      )
      .toList();

    List<SimpleEntityResponse> reportsWithoutFile = reports
      .stream()
      .filter(report -> "AVAILABLE".equalsIgnoreCase(report.getStatus()))
      .filter(
        report ->
          report.getFilePath() == null ||
          report.getFilePath().isBlank() ||
          report.getFileSize() == null ||
          report.getFileSize() <= 0
      )
      .map(report ->
        new SimpleEntityResponse(
          String.valueOf(report.getId()),
          String.valueOf(report.getName())
        )
      )
      .toList();

    List<SimpleEntityResponse> incidentsWithoutAgency = incidents
      .stream()
      .filter(inc -> inc.getAgency() == null)
      .map(inc ->
        new SimpleEntityResponse(
          inc.getId() != null ? inc.getId().toString() : "",
          inc.getTitle() != null
            ? inc.getTitle()
            : inc.getId() != null
              ? inc.getId().toString()
              : ""
        )
      )
      .toList();

    List<SimpleEntityResponse> incidentsWithoutService = incidents
      .stream()
      .filter(
        inc ->
          inc.getCreatorServiceId() == null &&
          inc.getTransferredToService() == null
      )
      .map(inc ->
        new SimpleEntityResponse(
          inc.getId() != null ? inc.getId().toString() : "",
          inc.getTitle() != null
            ? inc.getTitle()
            : inc.getId() != null
              ? inc.getId().toString()
              : ""
        )
      )
      .toList();

    List<SimpleEntityResponse> incidentsAssignedInactiveUser = incidents
      .stream()
      .filter(
        inc ->
          inc.getAssignedTo() != null &&
          requiresTreatment(inc) &&
          inactiveUserIds.contains(inc.getAssignedTo().getId().toString())
      )
      .map(inc ->
        new SimpleEntityResponse(
          inc.getId() != null ? inc.getId().toString() : "",
          inc.getTitle() != null
            ? inc.getTitle()
            : inc.getId() != null
              ? inc.getId().toString()
              : ""
        )
      )
      .toList();

    List<SimpleEntityResponse> usersWithoutRole = users
      .stream()
      .filter(user -> user.getRoles() == null || user.getRoles().isEmpty())
      .map(user ->
        new SimpleEntityResponse(
          user.getId() != null ? user.getId().toString() : "",
          user.getUsername() != null
            ? user.getUsername()
            : user.getEmail() != null
              ? user.getEmail()
              : ""
        )
      )
      .toList();

    List<SimpleEntityResponse> usersWithoutScope = users
      .stream()
      .filter(
        user ->
          !userHasRole(user, "SUPER_ADMIN") &&
          !userHasRole(user, "ADMIN") &&
          user.getAgency() == null &&
          user.getService() == null
      )
      .map(user ->
        new SimpleEntityResponse(
          user.getId() != null ? user.getId().toString() : "",
          user.getUsername() != null
            ? user.getUsername()
            : user.getEmail() != null
              ? user.getEmail()
              : ""
        )
      )
      .toList();

    List<UnusedIncidentTypeResponse> unusedTypes = computeUnusedIncidentTypes(
      incidentTypeConfigs,
      incidents
    );

    List<DataQualityIssueResponse> issues = new ArrayList<>();
    addIssue(
      issues,
      "incidents-without-agency",
      incidentsWithoutAgency.size(),
      "high"
    );
    addIssue(
      issues,
      "incidents-without-service",
      incidentsWithoutService.size(),
      "high"
    );
    addIssue(issues, "users-without-role", usersWithoutRole.size(), "medium");
    addIssue(issues, "users-without-scope", usersWithoutScope.size(), "medium");
    addIssue(
      issues,
      "incidents-assigned-inactive-user",
      incidentsAssignedInactiveUser.size(),
      "high"
    );
    addIssue(
      issues,
      "reports-without-file",
      reportsWithoutFile.size(),
      "medium"
    );
    addIssue(
      issues,
      "sent-notifications-without-trace",
      notificationStats != null
        ? notificationStats.getSentWithoutTraceCount()
        : 0L,
      "medium"
    );
    addIssue(
      issues,
      "invalid-recipient-notifications",
      notificationStats != null
        ? notificationStats.getInvalidRecipientCount()
        : 0L,
      "medium"
    );
    addIssue(
      issues,
      "agencies-without-head",
      agenciesWithoutHead.size(),
      "low"
    );
    addIssue(
      issues,
      "services-without-head",
      servicesWithoutHead.size(),
      "low"
    );
    addIssue(issues, "unused-incident-types", unusedTypes.size(), "low");

    long totalIssues = issues
      .stream()
      .mapToLong(DataQualityIssueResponse::getCount)
      .sum();

    return new DataQualityOverviewResponse(
      issues,
      totalIssues,
      unusedTypes,
      agenciesWithoutHead,
      servicesWithoutHead,
      reportsWithoutFile,
      notificationStats != null &&
        notificationStats.getSentWithoutTraceSample() != null
        ? notificationStats.getSentWithoutTraceSample()
        : List.of(),
      notificationStats != null &&
        notificationStats.getInvalidRecipientsSample() != null
        ? notificationStats.getInvalidRecipientsSample()
        : List.of(),
      incidentsWithoutAgency,
      incidentsWithoutService,
      incidentsAssignedInactiveUser,
      usersWithoutRole,
      usersWithoutScope,
      Instant.now().toString()
    );
  }

  // Realise l'intention metier user has role.

  private static boolean userHasRole(
    SuperAdminUserClientResponse user,
    String roleName
  ) {
    if (user.getRoles() == null) return false;
    return user
      .getRoles()
      .stream()
      .anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
  }

  // Un incident attend un traitement tant qu'il est dans le circuit, hors solution deja
  // livree (RESOLU) et hors attente de la Direction (DRAFT). Les statuts terminaux
  // viennent de la reference : ANNULE manquait a la liste ecrite ici, et un incident
  // annule etait donc compte comme attendant un traitement.
  private static boolean requiresTreatment(IncidentClientResponse incident) {
    if (incident.getStatus() == null) {
      return false;
    }
    String status = incident.getStatus().toUpperCase(java.util.Locale.ROOT);
    return (
      !IncidentStatusSets.TERMINAL.contains(status) &&
      !"RESOLVED".equals(status) &&
      !"DRAFT".equals(status)
    );
  }

  // Calcule unused incident types.
  private List<UnusedIncidentTypeResponse> computeUnusedIncidentTypes(
    List<IncidentTypeConfigClientResponse> incidentTypeConfigs,
    List<IncidentClientResponse> incidents
  ) {
    if (incidentTypeConfigs == null || incidentTypeConfigs.isEmpty()) {
      return List.of();
    }
    Set<String> usedTypeIds = incidents
      .stream()
      .map(incident -> {
        if (incident.getType() != null && incident.getType().getId() != null) {
          return incident.getType().getId().toString();
        }
        return null;
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    return incidentTypeConfigs
      .stream()
      .filter(config -> {
        String id = config.getId() != null ? config.getId().toString() : "";
        return !id.isBlank() && !usedTypeIds.contains(id);
      })
      .map(config ->
        new UnusedIncidentTypeResponse(
          config.getId() != null ? config.getId().toString() : "",
          config.getName() != null ? config.getName() : "",
          Boolean.TRUE.equals(config.getActive())
        )
      )
      .toList();
  }
}
