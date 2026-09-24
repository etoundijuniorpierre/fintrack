// Service metier : coordonne les operations du domaine department.

package com.fintrack.user.service.impl;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.client.audit.constant.AuditAction;
import com.fintrack.user.client.audit.constant.AuditStatus;
import com.fintrack.user.exception.BusinessRuleViolationException;
import com.fintrack.user.exception.DuplicateResourceException;
import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.exception.ErrorCode;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.entity.ServiceEntity;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.repository.ServiceRepository;
import com.fintrack.user.repository.UserRepository;
import com.fintrack.user.security.CurrentActorProvider;
import com.fintrack.user.service.DepartmentService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implemente les regles metier du domaine department.

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

  private final ServiceRepository serviceRepository;
  private final UserRepository userRepository;
  private final AuditServiceClientService auditService;
  private final CurrentActorProvider currentActorProvider;
  private final MessageSource messageSource;

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  // Liste les departements selon le perimetre demande.

  @Transactional(readOnly = true)
  public Page<ServiceEntity> findAll(Pageable pageable) {
    return serviceRepository.findAll(pageable);
  }

  // Liste les departements selon le perimetre demande.

  @Transactional(readOnly = true)
  public List<ServiceEntity> findAll() {
    return serviceRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  // Charge les departements demandes en une seule requete pour les clients internes.
  public List<ServiceEntity> findAllByIds(java.util.Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return serviceRepository.findAllById(ids);
  }

  // Recherche les departements par identifiant.

  @Transactional(readOnly = true)
  public ServiceEntity findById(UUID id) {
    return serviceRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Service introuvable avec l'identifiant : " + id
        )
      );
  }

  // Recherche les departements par identifiant with head.

  @Transactional(readOnly = true)
  public ServiceEntity findByIdWithHead(UUID id) {
    return serviceRepository
      .findByIdWithHead(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Service introuvable avec l'identifiant : " + id
        )
      );
  }

  // Prepare l'enregistrement de la ressource selon les regles metier.

  @Transactional
  public ServiceEntity create(ServiceEntity service) {
    if (serviceRepository.existsByName(service.getName())) {
      throw new DuplicateResourceException(
        t("user.error.service_already_exists", service.getName())
      );
    }
    service.setId(null);
    validateHeadRole(service.getHeadOfService());
    ServiceEntity saved = serviceRepository.save(service);
    if (saved.getHeadOfService() != null) {
      User head = saved.getHeadOfService();
      if (head.getManagedServices() == null) {
        head.setManagedServices(new HashSet<>());
      }
      head.getManagedServices().add(saved);
      userRepository.save(head);
    }
    auditService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.DEPARTMENT_CREATE.getName(),
      "DEPARTMENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName()
    );
    return saved;
  }

  // Applique le changement demande apres validation metier.

  @Transactional
  public ServiceEntity update(UUID id, ServiceEntity serviceDetails) {
    ServiceEntity service = findById(id);
    service.setName(serviceDetails.getName());
    service.setDescription(serviceDetails.getDescription());
    service.setActive(serviceDetails.isActive());
    validateHeadRole(serviceDetails.getHeadOfService());

    User previousHead = service.getHeadOfService();
    User newHead = serviceDetails.getHeadOfService();

    if (
      previousHead != null &&
      (newHead == null || !previousHead.getId().equals(newHead.getId()))
    ) {
      if (previousHead.getManagedServices() != null) {
        previousHead.getManagedServices().remove(service);
        userRepository.save(previousHead);
      }
    }

    service.setHeadOfService(newHead);

    if (
      newHead != null &&
      (previousHead == null || !newHead.getId().equals(previousHead.getId()))
    ) {
      if (newHead.getManagedServices() == null) {
        newHead.setManagedServices(new HashSet<>());
      }
      newHead.getManagedServices().add(service);
      userRepository.save(newHead);
    }

    ServiceEntity saved = serviceRepository.saveAndFlush(service);
    auditService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.DEPARTMENT_UPDATE.getName(),
      "DEPARTMENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName()
    );
    return saved;
  }

  // Supprime ou invalide les donnees ciblees apres controle metier.

  @Transactional
  public void delete(UUID id) {
    serviceRepository.deleteById(id);
    auditService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.DEPARTMENT_DELETE.getName(),
      "DEPARTMENT",
      id.toString(),
      AuditStatus.SUCCESS.getName()
    );
  }

  // Applique le changement demande apres validation metier.

  @Transactional
  public ServiceEntity assignHead(UUID serviceId, UUID userId) {
    ServiceEntity service = findById(serviceId);
    User user = userRepository
      .findById(userId)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Utilisateur introuvable avec l'identifiant : " + userId
        )
      );
    validateHeadRole(user);
    User previousHead = service.getHeadOfService();
    if (
      previousHead != null &&
      previousHead.getManagedServices() != null &&
      !previousHead.getId().equals(user.getId())
    ) {
      previousHead.getManagedServices().remove(service);
      userRepository.save(previousHead);
    }
    service.setHeadOfService(user);
    if (user.getManagedServices() == null) {
      user.setManagedServices(new HashSet<>());
    }
    user.getManagedServices().add(service);
    ServiceEntity saved = serviceRepository.save(service);
    auditService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.DEPARTMENT_ASSIGN_HEAD.getName(),
      "DEPARTMENT",
      serviceId.toString(),
      AuditStatus.SUCCESS.getName(),
      Map.of("headOfService", userId)
    );
    return saved;
  }

  // Verifie que les regles metier autorisent l operation sur departement.

  private void validateHeadRole(User user) {
    if (user != null && !user.hasRole(RoleConstants.CHEF_SERVICE.getName())) {
      throw new BusinessRuleViolationException(
        ErrorCode.BUSINESS_RULE_VIOLATION,
        t("user.error.service_head_missing_role", RoleConstants.CHEF_SERVICE.getName())
      );
    }
  }
}
