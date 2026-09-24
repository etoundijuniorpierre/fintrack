// DTO : transporte les donnees liees a internal job entre les couches.

package com.fintrack.user.model.dto.response.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de interne job.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalJobResponse {

  private String name;
  private String description;
  private String schedule;
  private String lastRunAt;
  private String nextRunAt;
  private Long failureCount;
  private Boolean triggerable;
}
