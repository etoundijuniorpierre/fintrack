// Composant backend : porte la logique liee a super admin section metadata.

package com.fintrack.reporting.model.readmodel.superadmin;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Metadata interne d'une section Super Admin.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminSectionMetadata {

  private String section;
  private String generatedAt;
  private long durationMs;
  private List<String> degraded;
}
