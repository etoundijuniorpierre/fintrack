// DTO : transporte les donnees liees a incident search entre les couches.

package com.fintrack.incident.model.dto.request;

import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de requete contenant les filtres de recherche d'incidents.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentSearchRequest {

  private String view; // own, assigned, agency, service, all
  private Set<IncidentStatus> statuses;
  private Set<UUID> typeIds;
  private Set<Criticality> criticalities;
  private UUID assignedTo;
  private Boolean unassignedOnly;
  private UUID createdBy;
  private UUID subjectUserId;
  private UUID agencyId;
  private UUID serviceId;
  private Set<UUID> subjectUserIds;
  private Set<UUID> agencyIds;
  private Set<UUID> serviceIds;
  private LocalDate startDate;
  private LocalDate endDate;
  private String keyword;
  private Boolean missingAgency;
  private Boolean missingService;
  private Boolean assignedToInactive;
}
