package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolutionProposalRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 5000, message = "{validation.size.max}")
  private String proposedSolution;

  // Temps de résolution estimé (en heures) ; s'il est renseigné il pilote le SLA.
  @Min(value = 1, message = "{validation.incident.estimated_hours_positive}")
  private Integer estimatedResolutionHours;
}
