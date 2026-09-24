// DTO : transporte les donnees liees a report filters entre les couches.

package com.fintrack.reporting.model.dto.request;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de requete transportant les parametres de rapport filters.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilters {

  private String view;
  private List<String> incidentTypes;
  private List<String> criticalities;
  private List<String> statuses;
  private List<String> services;
  private List<String> assignedUsers;
  private UUID subjectUserId;
  private UUID agencyId;
  private UUID serviceId;
  private List<UUID> subjectUserIds;
  private List<UUID> agencyIds;
  private List<UUID> serviceIds;
}
