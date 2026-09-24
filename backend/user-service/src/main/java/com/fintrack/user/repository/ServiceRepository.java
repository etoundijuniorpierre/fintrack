// Acces aux donnees : expose les requetes persistantes liees a service.

package com.fintrack.user.repository;

import com.fintrack.user.model.entity.ServiceEntity;
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

// Repertoire Spring Data JPA pour les departements internes.

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID> {
  // Liste les services internes selon le perimetre demande.
  @Override
  @EntityGraph(attributePaths = { "headOfService" })
  Page<ServiceEntity> findAll(Pageable pageable);

  // Liste les services internes selon le perimetre demande.

  @Override
  @EntityGraph(attributePaths = { "headOfService" })
  List<ServiceEntity> findAll();

  // Recherche les services internes par identifiant.

  @Override
  @EntityGraph(attributePaths = { "headOfService", "users" })
  Optional<ServiceEntity> findById(UUID id);

  // Recherche les services internes par nom.

  Optional<ServiceEntity> findByName(String name);
  // Verifie l'existence de name.
  boolean existsByName(String name);

  @EntityGraph(
    attributePaths = {
      "headOfService",
      "headOfService.roles",
      "headOfService.roles.permissions",
      "headOfService.permissions",
      "headOfService.agency",
      "headOfService.service",
    }
  )
  // Recherche les services internes par identifiant with head.
  @Query("SELECT s FROM Service s WHERE s.id = :id")
  Optional<ServiceEntity> findByIdWithHead(@Param("id") UUID id);

  /**
   * Liste tous les services dont l'utilisateur donné est le chef.
   * Le modèle autorise déjà ce cas (Service.headOfService = ManyToOne) ;
   * cette requête le rend exploitable côté API (badge multi-services dans
   * les détails utilisateur, vérification de permissions étendues).
   */
  // Recherche les services internes par head of service identifiant.
  List<ServiceEntity> findByHeadOfServiceId(UUID headOfServiceId);
}
