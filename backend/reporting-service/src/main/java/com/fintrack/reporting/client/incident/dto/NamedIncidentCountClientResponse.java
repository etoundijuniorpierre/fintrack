// DTO : transporte les donnees liees a named incident count client entre les couches.

package com.fintrack.reporting.client.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a named inincident count client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedIncidentCountClientResponse {

  private String name;
  private long count;
}
