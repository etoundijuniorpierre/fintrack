// DTO : transporte les donnees liees a service incident count entre les couches.

package com.fintrack.incident.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Fragment wire du tableau de bord : compte par service (mappe depuis le read-model).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceIncidentCountResponse {

  private String serviceName;
  private long count;
}
