// Repository : acces aux donnees du compteur annuel de references.

package com.fintrack.incident.repository;

import com.fintrack.incident.model.entity.IncidentReferenceSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentReferenceSequenceRepository
  extends JpaRepository<IncidentReferenceSequence, Integer> {
  /**
   * Alloue le numero suivant de l'annee en une seule instruction atomique : deux
   * creations simultanees ne peuvent pas obtenir le meme numero, sans verrou explicite.
   */
  @Query(
    value = "INSERT INTO incident_reference_sequences (reference_year, last_number) " +
      "VALUES (:year, 1) " +
      "ON CONFLICT (reference_year) DO UPDATE " +
      "SET last_number = incident_reference_sequences.last_number + 1 " +
      "RETURNING last_number",
    nativeQuery = true
  )
  Long allocateNext(@Param("year") int year);
}
