// Contrat metier : expose les operations du domaine department.

package com.fintrack.user.service;

import com.fintrack.user.model.entity.ServiceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Interface du service de gestion des departements internes.

public interface DepartmentService {
  // Liste les departements selon le perimetre demande.
  Page<ServiceEntity> findAll(Pageable pageable);
  // Liste les departements selon le perimetre demande.
  List<ServiceEntity> findAll();
  // Recherche un ensemble de departements pour les appels internes groupés.
  List<ServiceEntity> findAllByIds(java.util.Set<UUID> ids);
  // Recherche les departements par identifiant.
  ServiceEntity findById(UUID id);
  // Recherche les departements par identifiant with head.
  ServiceEntity findByIdWithHead(UUID id);
  // Prepare l'enregistrement de la ressource selon les regles metier.
  ServiceEntity create(ServiceEntity service);
  // Applique le changement demande apres validation metier.
  ServiceEntity update(UUID id, ServiceEntity service);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  void delete(UUID id);
  // Applique le changement demande apres validation metier.
  ServiceEntity assignHead(UUID serviceId, UUID userId);
}
