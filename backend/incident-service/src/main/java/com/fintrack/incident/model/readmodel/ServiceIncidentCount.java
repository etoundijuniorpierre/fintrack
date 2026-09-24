// Composant backend : porte la logique liee a service incident count.

package com.fintrack.incident.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte la responsabilite applicative liee a service inincident count.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceIncidentCount {

  private String serviceName;
  private long count;
}
