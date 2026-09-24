// DTO : transporte les donnees liees a incident client entre les couches.

package com.fintrack.reporting.client.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fintrack.reporting.client.superadmin.dto.AgencyClientResponse;
import com.fintrack.reporting.client.superadmin.dto.ServiceClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserSummaryClientResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a inincident client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncidentClientResponse {

  private String id;
  private String title;
  private String description;
  private IncidentTypeConfigClientResponse type;
  private String criticality;
  private String status;
  private UserSummaryClientResponse createdBy;
  private UserSummaryClientResponse assignedTo;
  private ServiceClientResponse transferredToService;
  private Integer reopenCount;
  private Integer transferCount;
  private AgencyClientResponse agency;
  private Object createdAt;
  private Object resolvedAt;
  private Object closedAt;
  private String creatorServiceId;
  private List<IncidentHistoryClientResponse> history;
  private Set<UUID> participantUserIds;
  // Champs ajoutes pour la generation de rapports : Feign les deserialise
  // depuis IncidentSummaryResponse renvoyee par incident-service.
  private String reference;
  private Object dueDate;
  private Object incidentDate;
  private String cause;
  private String causeDetail;
}
