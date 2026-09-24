// Contrat metier : expose les operations du domaine permission.

package com.fintrack.user.service;

import com.fintrack.user.model.entity.Permission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Interface du service d'administration des permissions.

public interface PermissionService {
  // Liste les permissions selon le perimetre demande.
  Page<Permission> findAll(Pageable pageable);
  // Liste les permissions selon le perimetre demande.
  List<Permission> findAll();
  // Recherche les permissions par identifiant.
  Permission findById(UUID id);
}
