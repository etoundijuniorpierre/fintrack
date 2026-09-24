// DTO : transporte les donnees liees a incidents page client entre les couches.

package com.fintrack.reporting.client.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a inincidents page client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncidentsPageClientResponse {

  private List<IncidentClientResponse> content;
  private long totalElements;
  private int totalPages;
  private int size;
  private int number;
}
