// Entite metier : porte le compteur annuel des references d'incident.

package com.fintrack.incident.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une ligne par annee : la numerotation des incidents repart a 1 chaque 1er janvier.
 * L'annee sert de cle primaire, ce qui rend l'allocation atomique via un upsert.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incident_reference_sequences")
public class IncidentReferenceSequence {

  @Id
  @Column(name = "reference_year", nullable = false)
  private int referenceYear;

  @Column(name = "last_number", nullable = false)
  private long lastNumber;
}
