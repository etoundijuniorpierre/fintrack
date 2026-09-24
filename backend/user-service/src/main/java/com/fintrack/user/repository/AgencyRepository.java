// Acces aux donnees : expose les requetes persistantes liees a agency.

package com.fintrack.user.repository;

import com.fintrack.user.model.entity.Agency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// Repertoire Spring Data JPA pour l'acces aux donnees des agences.

@Repository
public interface AgencyRepository extends JpaRepository<Agency, UUID> {
  // Liste les agences selon le perimetre demande.
  @Override
  @EntityGraph(attributePaths = { "headOfAgency" })
  Page<Agency> findAll(Pageable pageable);

  // Liste les agences selon le perimetre demande.

  @Override
  @EntityGraph(attributePaths = { "headOfAgency" })
  List<Agency> findAll();

  // Recherche les agences par identifiant.

  @Override
  @EntityGraph(attributePaths = { "headOfAgency", "users" })
  Optional<Agency> findById(UUID id);

  // Recherche les agences par nom.

  Optional<Agency> findByName(String name);
  // Verifie l'existence de name.
  boolean existsByName(String name);

  // Recherche les agences pour codes by prefix.

  @Query("SELECT a.code FROM Agency a WHERE a.code LIKE CONCAT(:prefix, '%')")
  List<String> findCodesByPrefix(@Param("prefix") String prefix);

  // Recherche les agences par head of agence identifiant.

  List<Agency> findByHeadOfAgencyId(UUID headOfAgencyId);
}
