// Mapper : convertit les donnees liees a notification entre modeles.

package com.fintrack.notification.model.mapper;

import com.fintrack.notification.model.dto.request.NotificationRequest;
import com.fintrack.notification.model.dto.response.BulkPurgeResponse;
import com.fintrack.notification.model.dto.response.IncidentSummaryResponse;
import com.fintrack.notification.model.dto.response.NotificationResponse;
import com.fintrack.notification.model.dto.response.NotificationSample;
import com.fintrack.notification.model.dto.response.NotificationStatsResponse;
import com.fintrack.notification.model.entity.Notification;
import com.fintrack.notification.model.readmodel.NotificationPurgeResult;
import com.fintrack.notification.model.readmodel.NotificationStats;
import java.util.List;
import java.util.Map;
import org.mapstruct.*;

// Mapper MapStruct pour convertir les notifications entre entite et DTO.

@Mapper(
  componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class NotificationMapper {

  // Precise l'intention metier associee a notification.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "sentAt", ignore = true)
  @Mapping(target = "retryAt", ignore = true)
  @Mapping(target = "nextRetry", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "retryCount", ignore = true)
  public abstract Notification toDocument(NotificationRequest request);

  // Convertit les donnees du domaine notification entre les modeles utilises.

  @Mapping(target = "incidentId", ignore = true)
  public abstract NotificationResponse toResponse(Notification document);

  // Convertit les donnees du domaine notification entre les modeles utilises.

  public List<NotificationResponse> toResponses(
    List<Notification> notifications
  ) {
    return notifications.stream().map(this::toResponse).toList();
  }

  // Precise l'intention metier associee a notification.

  @Mapping(
    target = "type",
    expression = "java(document.getType() != null ? document.getType().name() : null)"
  )
  @Mapping(
    target = "status",
    expression = "java(document.getStatus() != null ? document.getStatus().name() : null)"
  )
  @Mapping(
    target = "createdAt",
    expression = "java(document.getCreatedAt() != null ? document.getCreatedAt().toString() : null)"
  )
  @Mapping(
    target = "sentAt",
    expression = "java(document.getSentAt() != null ? document.getSentAt().toString() : null)"
  )
  @Mapping(
    target = "retryAt",
    expression = "java(document.getRetryAt() != null ? document.getRetryAt().toString() : null)"
  )
  @Mapping(
    target = "nextRetry",
    expression = "java(document.getNextRetry() != null ? document.getNextRetry().toString() : null)"
  )
  @Mapping(
    target = "incidentId",
    expression = "java(document.getIncidentId() != null ? document.getIncidentId().toString() : null)"
  )
  public abstract NotificationSample toSample(Notification document);

  // Convertit les donnees du domaine notification entre les modeles utilises.

  public NotificationStatsResponse toStatsResponse(NotificationStats stats) {
    if (stats == null) {
      return null;
    }
    return NotificationStatsResponse.builder()
      .total(stats.getTotal())
      .sent(stats.getSent())
      .failed(stats.getFailed())
      .pending(stats.getPending())
      .byStatus(stats.getByStatus())
      .byType(stats.getByType())
      .byTypeAndStatus(stats.getByTypeAndStatus())
      .invalidRecipientCount(stats.getInvalidRecipientCount())
      .sentWithoutTraceCount(stats.getSentWithoutTraceCount())
      .failedSample(
        stats.getFailedSample().stream().map(this::toSample).toList()
      )
      .invalidRecipientsSample(
        stats.getInvalidRecipientsSample().stream().map(this::toSample).toList()
      )
      .sentWithoutTraceSample(
        stats.getSentWithoutTraceSample().stream().map(this::toSample).toList()
      )
      .computedAt(stats.getComputedAt())
      .build();
  }

  // Convertit les donnees du domaine notification entre les modeles utilises.

  public BulkPurgeResponse toBulkPurgeResponse(NotificationPurgeResult result) {
    if (result == null) {
      return null;
    }
    return BulkPurgeResponse.builder()
      .scope(result.getScope())
      .deletedCount(result.getDeletedCount())
      .preservedCount(result.getPreservedCount())
      .dryRun(result.isDryRun())
      .executedAt(result.getExecutedAt())
      .build();
  }

  // Convertit les donnees du domaine notification entre les modeles utilises.

  @AfterMapping
  protected void mapIncidentSummary(
    Notification document,
    @MappingTarget NotificationResponse response
  ) {
    if (document.getIncidentId() != null) {
      IncidentSummaryResponse summary = new IncidentSummaryResponse();
      summary.setId(document.getIncidentId());
      Map<String, Object> params = document.getTemplateParams();
      if (params != null) {
        if (params.containsKey("incident_title")) {
          summary.setTitle(String.valueOf(params.get("incident_title")));
        }
        if (params.containsKey("incident_status_code")) {
          summary.setStatus(String.valueOf(params.get("incident_status_code")));
        } else if (params.containsKey("incident_status")) {
          summary.setStatus(String.valueOf(params.get("incident_status")));
        }
        if (params.containsKey("incident_reference")) {
          summary.setReference(
            String.valueOf(params.get("incident_reference"))
          );
        }
      }
      response.setIncidentId(summary);
    }
  }

  // Le contenu Base64 d'une piece jointe (rapports e-mailes) est stocke dans
  @AfterMapping
  protected void stripHeavyTemplateParams(
    @MappingTarget NotificationResponse response
  ) {
    Map<String, Object> params = response.getTemplateParams();
    if (params != null) {
      params.remove("attachment_content");
    }
  }
}
