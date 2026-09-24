// Service metier : coordonne les operations du domaine permission.

package com.fintrack.user.service.impl;

import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.repository.PermissionRepository;
import com.fintrack.user.service.PermissionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementation du service de consultation des permissions (lecture seule).

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

  private final PermissionRepository permissionRepository;

  @Override
  @Transactional(readOnly = true)
  // Liste les permissions selon le perimetre demande.
  public Page<Permission> findAll(Pageable pageable) {
    return permissionRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les permissions selon le perimetre demande.
  public List<Permission> findAll() {
    return permissionRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les permissions par identifiant.
  public Permission findById(UUID id) {
    return permissionRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Permission introuvable avec l'identifiant : " + id
        )
      );
  }
}
