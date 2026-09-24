// Read-model : incidents adossant les compteurs de flux d'une periode
// (traites / resolus / clotures), pour rendre chaque compteur auditable.

package com.fintrack.incident.model.readmodel;

import com.fintrack.incident.model.entity.Incident;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte les trois populations de flux d'une periode, dans la meme portee que les compteurs.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodActivityIncidents {

  private List<Incident> treated;
  private List<Incident> resolved;
  private List<Incident> closed;
}
