// Contrat metier : expose les operations du domaine role.

package com.fintrack.user.service;

import com.fintrack.user.model.entity.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Interface du service d'administration des roles applicatifs.

public interface RoleService {
  // Liste les roles selon le perimetre demande.
  Page<Role> findAll(Pageable pageable);
  // Liste les roles selon le perimetre demande.
  List<Role> findAll();
  // Recherche les roles par identifiant.
  Role findById(UUID id);
  // Recherche les roles par nom.
  Role findByName(String name);
  // Prepare l'enregistrement de la ressource selon les regles metier.
  Role create(Role role);
  // Applique le changement demande apres validation metier.
  Role update(UUID id, Role role);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  void delete(UUID id);
}

