// Service metier : coordonne les operations du domaine role.

package com.fintrack.user.service.impl;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.client.audit.constant.AuditAction;
import com.fintrack.user.client.audit.constant.AuditStatus;
import com.fintrack.user.exception.DuplicateResourceException;
import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.model.constant.PermissionMatrix;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.repository.PermissionRepository;
import com.fintrack.user.repository.RoleRepository;
import com.fintrack.user.security.CurrentActorProvider;
import com.fintrack.user.service.RoleService;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementation du service de gestion des roles systeme.

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final AuditServiceClientService auditServiceClientService;
  private final CurrentActorProvider currentActorProvider;
  private final MessageSource messageSource;

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  // Plancher universel resolu en entites Permission gerees : aucun role ne doit
  // descendre sous ce socle (profil, notifications, dashboard), ni a la creation
  // ni a la mise a jour.
  private Set<Permission> resolveBaselinePermissions() {
    Set<Permission> baseline = new HashSet<>();
    for (String name : PermissionMatrix.getBaselinePermissions()) {
      permissionRepository.findByName(name).ifPresent(baseline::add);
    }
    return baseline;
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les roles selon le perimetre demande.
  public Page<Role> findAll(Pageable pageable) {
    return roleRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les roles selon le perimetre demande.
  public List<Role> findAll() {
    return roleRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les roles par identifiant.
  public Role findById(UUID id) {
    return roleRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          t("user.error.role_not_found_id", id)
        )
      );
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les roles par nom.
  public Role findByName(String name) {
    return roleRepository
      .findByName(name)
      .orElseThrow(() ->
        new EntityNotFoundException(t("user.error.role_not_found_name", name))
      );
  }

  @Override
  @Transactional
  // Cree un element du domaine role apres validation metier.
  public Role create(Role role) {
    if (roleRepository.existsByName(role.getName())) {
      throw new DuplicateResourceException(
        t("user.error.role_already_exists", role.getName())
      );
    }
    Role agentRole = roleRepository
      .findByName(RoleConstants.AGENT.getName())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Role systeme AGENT introuvable; creation de role impossible"
        )
      );
    Set<Permission> permissions =
      agentRole.getPermissions() == null
        ? new HashSet<>()
        : new HashSet<>(agentRole.getPermissions());
    if (role.getPermissions() != null) {
      permissions.addAll(role.getPermissions());
    }
    permissions.addAll(resolveBaselinePermissions());
    role.setPermissions(permissions);
    role.setId(null);
    Role saved = roleRepository.save(role);
    auditServiceClientService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.ROLE_CREATE.getName(),
      "ROLE",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      roleSnapshot(saved)
    );
    return saved;
  }

  @Override
  @Transactional
  // Met a jour un element du domaine role avec les donnees validees.
  public Role update(UUID id, Role roleDetails) {
    Role role = findById(id);
    role.setName(roleDetails.getName());
    role.setDisplayName(roleDetails.getDisplayName());
    role.setDescription(roleDetails.getDescription());
    Set<Permission> permissions =
      roleDetails.getPermissions() == null
        ? new HashSet<>()
        : new HashSet<>(roleDetails.getPermissions());
    permissions.addAll(resolveBaselinePermissions());
    role.setPermissions(permissions);
    Role saved = roleRepository.saveAndFlush(role);
    auditServiceClientService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.ROLE_UPDATE.getName(),
      "ROLE",
      id.toString(),
      AuditStatus.SUCCESS.getName(),
      roleSnapshot(saved)
    );
    return saved;
  }

  @Override
  @Transactional
  // Supprime le role et conserve un instantane complet dans l'audit.
  public void delete(UUID id) {
    // On charge le role avant suppression pour figer son instantane dans l'audit.
    Role role = findById(id);
    Map<String, Object> snapshot = roleSnapshot(role);

    roleRepository.deleteById(id);
    auditServiceClientService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.ROLE_DELETE.getName(),
      "ROLE",
      id.toString(),
      AuditStatus.SUCCESS.getName(),
      snapshot
    );
  }

  /** Instantané des champs d'un rôle, conservé dans l'audit. */
  // Realise l'intention metier role instantane.
  private Map<String, Object> roleSnapshot(Role role) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("name", role.getName());
    snapshot.put("displayName", role.getDisplayName());
    snapshot.put("description", role.getDescription());
    if (role.getPermissions() != null) {
      snapshot.put(
        "permissions",
        role.getPermissions().stream().map(Permission::getName).toList()
      );
    }
    return snapshot;
  }
}

