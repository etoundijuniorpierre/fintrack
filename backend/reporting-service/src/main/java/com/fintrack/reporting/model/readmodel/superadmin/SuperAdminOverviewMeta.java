// Composant backend : porte la logique liee a super admin overview meta.

package com.fintrack.reporting.model.readmodel.superadmin;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte la responsabilite applicative liee a super-administration vue d ensemble meta.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminOverviewMeta {

  private String generatedAt;
  private Long durationMs;
  private List<String> degraded;
}
