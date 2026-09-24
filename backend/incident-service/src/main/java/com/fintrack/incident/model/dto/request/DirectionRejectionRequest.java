package com.fintrack.incident.model.dto.request;

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
public class DirectionRejectionRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 20000, message = "{validation.size.max}")
  private String rejectionReason;
}
