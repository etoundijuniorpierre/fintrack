// Contrat metier : expose les operations du domaine agence.

package com.fintrack.user.service;

import com.fintrack.user.model.entity.Agency;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Interface du service de gestion operationnelle des agences.

public interface AgencyService {
  // Liste les agences selon le perimetre demande.
  Page<Agency> findAll(Pageable pageable);
  // Liste les agences selon le perimetre demande.
  List<Agency> findAll();
  // Recherche un ensemble d'agences pour les appels internes groupés.
  List<Agency> findAllByIds(java.util.Set<UUID> ids);
  // Recherche les agences par identifiant.
  Agency findById(UUID id);
  // Prepare l'enregistrement de la ressource selon les regles metier.
  Agency create(Agency agency);
  // Applique le changement demande apres validation metier.
  Agency update(UUID id, Agency agency);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  void delete(UUID id);
  // Applique le changement demande apres validation metier.
  Agency assignHead(UUID agencyId, UUID userId);
}

