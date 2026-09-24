// DTO : transporte les donnees liees a incident summary entre les couches.

package com.fintrack.incident.model.dto.response;

import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentCause;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.dto.BaseDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

// DTO de reponse contenant le resume des informations d'un incident.

@Data
@EqualsAndHashCode(callSuper = true)
public class IncidentSummaryResponse extends BaseDto {

  private String reference;
  private String title;
  private IncidentTypeConfigResponse type;
  private Criticality criticality;
  private IncidentStatus status;
  private LocalDateTime dueDate;
  private UserSummaryResponse createdBy;
  private UserSummaryResponse assignedTo;
  private AgencySummaryResponse agency;
  private UUID creatorServiceId;
  private ServiceSummaryResponse transferredToService;
  private LocalDateTime resolvedAt;
  private LocalDateTime closedAt;
  private LocalDate incidentDate;
  private String description;
  private IncidentCause cause;
  private String causeDetail;
  private Set<UUID> participantUserIds;
}
