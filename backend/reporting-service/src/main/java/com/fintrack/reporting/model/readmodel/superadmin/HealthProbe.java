// Composant backend : porte la logique liee a health probe.

package com.fintrack.reporting.model.readmodel.superadmin;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte la responsabilite applicative liee a sante probe.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProbe {

  private String key;
  private String baseUrl;
  private String lastCheckedAt;
  private String status;
  private Long responseTimeMs;
  private String endpoint;
  private String error;
  private Map<String, Object> components;
}
