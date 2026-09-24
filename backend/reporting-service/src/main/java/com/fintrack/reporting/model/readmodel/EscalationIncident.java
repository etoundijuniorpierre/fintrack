// Composant backend : porte la logique liee a escalation incident.

package com.fintrack.reporting.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Vue interne minimale d'un incident pour les regles d'escalade.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationIncident {

  // Sans eux, l'alerte annonce « 3 incidents » sans pouvoir les nommer.
  private String reference;
  private String title;
  private String criticality;
  private String status;
  private int transferCount;
  private String createdAt;
}
