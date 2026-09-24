// DTO : transporte les donnees liees a service incident count client entre les couches.

package com.fintrack.reporting.client.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a service inincident count client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceIncidentCountClientResponse {

  private String serviceName;
  private long count;
}
