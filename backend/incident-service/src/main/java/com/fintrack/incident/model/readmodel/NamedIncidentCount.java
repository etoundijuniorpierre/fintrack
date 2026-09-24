// Composant backend : porte la logique liee a named incident count.

package com.fintrack.incident.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte la responsabilite applicative liee a named inincident count.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedIncidentCount {

  private String name;
  private long count;
}
