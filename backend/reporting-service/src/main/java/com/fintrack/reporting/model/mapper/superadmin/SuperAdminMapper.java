// Mapper : convertit les donnees liees a super admin entre modeles.

package com.fintrack.reporting.model.mapper.superadmin;

import com.fintrack.reporting.client.incident.dto.IncidentClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentTypeConfigClientResponse;
import com.fintrack.reporting.client.superadmin.dto.AgencyClientResponse;
import com.fintrack.reporting.client.superadmin.dto.ServiceClientResponse;
import com.fintrack.reporting.client.superadmin.dto.SuperAdminUserClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserPermissionClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserRoleClientResponse;
import com.fintrack.reporting.model.dto.response.superadmin.*;
import com.fintrack.reporting.model.readmodel.superadmin.SuperAdminOverviewMeta;
import com.fintrack.reporting.model.readmodel.superadmin.SuperAdminSectionMetadata;
import com.fintrack.reporting.model.readmodel.superadmin.SuperAdminSnapshot;
import com.fintrack.reporting.model.readmodel.superadmin.SystemThresholds;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Mapper central des snapshots Super Admin vers DTO de reponse.
@Component
@RequiredArgsConstructor
public class SuperAdminMapper {

  private final DataQualityBuilder dataQualityBuilder;
  private final SystemHealthBuilder systemHealthBuilder;
  private final NotificationsBuilder notificationsBuilder;

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public GovernanceSectionResponse toGovernanceResponse(
    SuperAdminSnapshot snapshot
  ) {
    DataQualityOverviewResponse dataQuality = dataQuality(snapshot);
    GovernanceSectionResponse governance = GovernanceBuilder.build(
      snapshot.getIncidentMetrics(),
      snapshot.getUserStats(),
      incidents(snapshot),
      users(snapshot),
      agencies(snapshot),
      services(snapshot),
      snapshot.getNotificationStats(),
      dataQuality
    );
    governance.setMetadata(toMetadataResponse(snapshot.getMetadata()));
    return governance;
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public SystemHealthSectionResponse toSystemHealthResponse(
    SuperAdminSnapshot snapshot
  ) {
    return new SystemHealthSectionResponse(
      systemHealthBuilder.build(snapshot),
      toMetadataResponse(snapshot.getMetadata())
    );
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public List<HealthRowResponse> toSystemHealthRows(
    SuperAdminSnapshot snapshot
  ) {
    return systemHealthBuilder.build(snapshot);
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public ControlsQualitySectionResponse toControlsQualityResponse(
    SuperAdminSnapshot snapshot
  ) {
    return new ControlsQualitySectionResponse(
      PermissionsBuilder.build(
        roles(snapshot),
        permissions(snapshot),
        snapshot.getAuditStats()
      ),
      dataQuality(snapshot),
      toMetadataResponse(snapshot.getMetadata())
    );
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public OperationsSectionResponse toOperationsResponse(
    SuperAdminSnapshot snapshot
  ) {
    return new OperationsSectionResponse(
      notificationsBuilder.build(snapshot.getNotificationStats()),
      toMetadataResponse(snapshot.getMetadata())
    );
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public AuditSectionResponse toAuditOverviewResponse(
    SuperAdminSnapshot snapshot
  ) {
    AuditSectionResponse audit = AuditBuilder.build(snapshot.getAuditStats());
    audit.setMetadata(toMetadataResponse(snapshot.getMetadata()));
    return audit;
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public SystemConfigSectionResponse toSystemConfigResponse(
    SuperAdminSnapshot snapshot
  ) {
    return new SystemConfigSectionResponse(
      toThresholdsResponse(snapshot.getThresholds()),
      toMetadataResponse(snapshot.getMetadata())
    );
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public ReportingSectionResponse toReportingOverviewResponse(
    SuperAdminSnapshot snapshot
  ) {
    ReportingSectionResponse reporting = ReportingBuilder.build(
      snapshot.getReports()
    );
    reporting.setMetadata(toMetadataResponse(snapshot.getMetadata()));
    return reporting;
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public SuperAdminOverviewResponse toOverviewResponse(
    SuperAdminSnapshot snapshot
  ) {
    DataQualityOverviewResponse dataQuality = dataQuality(snapshot);
    return new SuperAdminOverviewResponse(
      GovernanceBuilder.build(
        snapshot.getIncidentMetrics(),
        snapshot.getUserStats(),
        incidents(snapshot),
        users(snapshot),
        agencies(snapshot),
        services(snapshot),
        snapshot.getNotificationStats(),
        dataQuality
      ),
      systemHealthBuilder.build(snapshot),
      AuditBuilder.build(snapshot.getAuditStats()),
      PermissionsBuilder.build(
        roles(snapshot),
        permissions(snapshot),
        snapshot.getAuditStats()
      ),
      toThresholdsResponse(snapshot.getThresholds()),
      dataQuality,
      ReportingBuilder.build(snapshot.getReports()),
      notificationsBuilder.build(snapshot.getNotificationStats()),
      toOverviewMetaResponse(snapshot.getOverviewMeta())
    );
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public ThresholdsUpdateResponse toThresholdsUpdateResponse(
    SuperAdminSnapshot snapshot
  ) {
    return new ThresholdsUpdateResponse(
      toThresholdsResponse(snapshot.getAppliedThresholds())
    );
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public SystemThresholds toThresholdsReadModel(
    SystemThresholdsResponse response
  ) {
    if (response == null) {
      return null;
    }
    return new SystemThresholds(
      response.getDefaultSlaHours(),
      response.getCriticalIncidentHours(),
      response.getSlaReminderIntervalHours(),
      response.getMaxTransfersBeforeAlert(),
      response.getNotificationMaxRetryCount(),
      response.getLoginMaxFailedAttempts(),
      response.getTempPasswordValidityMinutes(),
      response.getEscalationScanIntervalMinutes(),
      response.getMaxReopenCount(),
      response.getReopenTimeLimitHours(),
      response.getAutoBlockOverdueWorkingDays(),
      response.getBlockedReminderIntervalDays(),
      response.getCriticalReminderIntervalHours(),
      response.getProlongedWaitDays(),
      response.getValidationDelayHours(),
      response.getValidationReminderEnabled(),
      response.getServiceManagerSelfValidationEnabled(),
      response.getPendingActionReminderIntervalHours(),
      response.getPendingActionInternalEnabled(),
      response.getReportRetentionDays(),
      response.getBackupScheduleEnabled(),
      response.getBackupScheduleHour()
    );
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  public SystemThresholdsResponse toThresholdsResponse(
    SystemThresholds thresholds
  ) {
    if (thresholds == null) {
      return null;
    }
    return new SystemThresholdsResponse(
      thresholds.getDefaultSlaHours(),
      thresholds.getCriticalIncidentHours(),
      thresholds.getSlaReminderIntervalHours(),
      thresholds.getMaxTransfersBeforeAlert(),
      thresholds.getNotificationMaxRetryCount(),
      thresholds.getLoginMaxFailedAttempts(),
      thresholds.getTempPasswordValidityMinutes(),
      thresholds.getEscalationScanIntervalMinutes(),
      thresholds.getMaxReopenCount(),
      thresholds.getReopenTimeLimitHours(),
      thresholds.getAutoBlockOverdueWorkingDays(),
      thresholds.getBlockedReminderIntervalDays(),
      thresholds.getCriticalReminderIntervalHours(),
      thresholds.getProlongedWaitDays(),
      thresholds.getValidationDelayHours(),
      thresholds.getValidationReminderEnabled(),
      thresholds.getServiceManagerSelfValidationEnabled(),
      thresholds.getPendingActionReminderIntervalHours(),
      thresholds.getPendingActionInternalEnabled(),
      thresholds.getReportRetentionDays(),
      thresholds.getBackupScheduleEnabled(),
      thresholds.getBackupScheduleHour()
    );
  }

  // Realise l'intention metier data quality.

  private DataQualityOverviewResponse dataQuality(SuperAdminSnapshot snapshot) {
    return dataQualityBuilder.build(
      incidents(snapshot),
      users(snapshot),
      agencies(snapshot),
      services(snapshot),
      snapshot.getNotificationStats(),
      snapshot.getReports(),
      incidentTypeConfigs(snapshot)
    );
  }

  // Realise l'intention metier incidents.

  @SuppressWarnings("unchecked")
  private List<IncidentClientResponse> incidents(SuperAdminSnapshot snapshot) {
    return (List<IncidentClientResponse>) snapshot.getIncidents();
  }

  // Realise l'intention metier incident type configs.

  @SuppressWarnings("unchecked")
  private List<IncidentTypeConfigClientResponse> incidentTypeConfigs(
    SuperAdminSnapshot snapshot
  ) {
    return (List<IncidentTypeConfigClientResponse>) snapshot.getIncidentTypeConfigs();
  }

  // Realise l'intention metier users.

  @SuppressWarnings("unchecked")
  private List<SuperAdminUserClientResponse> users(
    SuperAdminSnapshot snapshot
  ) {
    return (List<SuperAdminUserClientResponse>) snapshot.getUsers();
  }

  // Realise l'intention metier roles.

  @SuppressWarnings("unchecked")
  private List<UserRoleClientResponse> roles(SuperAdminSnapshot snapshot) {
    return (List<UserRoleClientResponse>) snapshot.getRoles();
  }

  // Realise l'intention metier agencies.

  @SuppressWarnings("unchecked")
  private List<AgencyClientResponse> agencies(SuperAdminSnapshot snapshot) {
    return (List<AgencyClientResponse>) snapshot.getAgencies();
  }

  // Realise l'intention metier services.

  @SuppressWarnings("unchecked")
  private List<ServiceClientResponse> services(SuperAdminSnapshot snapshot) {
    return (List<ServiceClientResponse>) snapshot.getServices();
  }

  // Realise l'intention metier permissions.

  @SuppressWarnings("unchecked")
  private List<UserPermissionClientResponse> permissions(
    SuperAdminSnapshot snapshot
  ) {
    return (List<UserPermissionClientResponse>) snapshot.getPermissions();
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  private SectionMetadataResponse toMetadataResponse(
    SuperAdminSectionMetadata metadata
  ) {
    if (metadata == null) {
      return null;
    }
    return new SectionMetadataResponse(
      metadata.getSection(),
      metadata.getGeneratedAt(),
      metadata.getDurationMs(),
      metadata.getDegraded()
    );
  }

  // Convertit les donnees du domaine super-administration entre les modeles utilises.

  private SuperAdminOverviewMetaResponse toOverviewMetaResponse(
    SuperAdminOverviewMeta meta
  ) {
    if (meta == null) {
      return null;
    }
    return new SuperAdminOverviewMetaResponse(
      meta.getGeneratedAt(),
      meta.getDurationMs(),
      meta.getDegraded()
    );
  }
}
