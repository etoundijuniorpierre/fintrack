// Service metier : coordonne les operations du domaine agence.

package com.fintrack.user.service.impl;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.client.audit.constant.AuditAction;
import com.fintrack.user.client.audit.constant.AuditStatus;
import com.fintrack.user.exception.BusinessRuleViolationException;
import com.fintrack.user.exception.DuplicateResourceException;
import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.exception.ErrorCode;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.repository.AgencyRepository;
import com.fintrack.user.repository.UserRepository;
import com.fintrack.user.security.CurrentActorProvider;
import com.fintrack.user.service.AgencyService;
import com.fintrack.user.util.AgencyCodeGenerator;
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

// Implementation du service metier de gestion des agences.

@Service
@RequiredArgsConstructor
public class AgencyServiceImpl implements AgencyService {

  private final AgencyRepository agencyRepository;
  private final UserRepository userRepository;
  private final AgencyCodeGenerator agencyCodeGenerator;
  private final AuditServiceClientService auditService;
  private final CurrentActorProvider currentActorProvider;
  private final MessageSource messageSource;

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les agences selon le perimetre demande.
  public Page<Agency> findAll(Pageable pageable) {
    return agencyRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les agences selon le perimetre demande.
  public List<Agency> findAll() {
    return agencyRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  // Charge les agences demandees en une seule requete pour les clients internes.
  public List<Agency> findAllByIds(java.util.Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return agencyRepository.findAllById(ids);
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les agences par identifiant.
  public Agency findById(UUID id) {
    return agencyRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Agence introuvable avec l'identifiant : " + id
        )
      );
  }

  @Override
  @Transactional
  // Cree un element du domaine agence apres validation metier.
  public Agency create(Agency agency) {
    if (agencyRepository.existsByName(agency.getName())) {
      throw new DuplicateResourceException(
        t("user.error.agency_already_exists", agency.getName())
      );
    }
    agency.setId(null);
    agency.setCode(
      agencyCodeGenerator.generate(agency.getCity(), agency.getName())
    );
    validateHeadRole(agency.getHeadOfAgency());
    Agency saved = agencyRepository.save(agency);
    auditService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.AGENCY_CREATE.getName(),
      "AGENCY",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName()
    );
    return saved;
  }

  @Override
  @Transactional
  // Met a jour un element du domaine agence avec les donnees validees.
  public Agency update(UUID id, Agency agencyDetails) {
    Agency agency = findById(id);
    agency.setName(agencyDetails.getName());
    agency.setCity(agencyDetails.getCity());
    agency.setAddress(agencyDetails.getAddress());
    agency.setActive(agencyDetails.isActive());
    validateHeadRole(agencyDetails.getHeadOfAgency());
    agency.setHeadOfAgency(agencyDetails.getHeadOfAgency());
    Agency saved = agencyRepository.saveAndFlush(agency);
    auditService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.AGENCY_UPDATE.getName(),
      "AGENCY",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName()
    );
    return saved;
  }

  @Override
  @Transactional
  // Supprime un element du domaine agence apres controle metier.
  public void delete(UUID id) {
    agencyRepository.deleteById(id);
    auditService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.AGENCY_DELETE.getName(),
      "AGENCY",
      id.toString(),
      AuditStatus.SUCCESS.getName()
    );
  }

  @Override
  @Transactional
  // Assigne le responsable metier d'une agence.
  public Agency assignHead(UUID agencyId, UUID userId) {
    Agency agency = findById(agencyId);
    User user = userRepository
      .findById(userId)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Utilisateur introuvable avec l'identifiant : " + userId
        )
      );
    validateHeadRole(user);
    if (
      user.getAgency() == null || !user.getAgency().getId().equals(agencyId)
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.BUSINESS_RULE_VIOLATION,
        t("user.error.agency_head_wrong_agency")
      );
    }
    agency.setHeadOfAgency(user);
    auditService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.AGENCY_ASSIGN_HEAD.getName(),
      "AGENCY",
      agencyId.toString(),
      AuditStatus.SUCCESS.getName(),
      Map.of("headOfAgency", userId)
    );
    return agency;
  }

  // Verifie que les regles metier autorisent l operation sur agence.

  private void validateHeadRole(User user) {
    if (user != null && !user.hasRole(RoleConstants.CHEF_AGENCE.getName())) {
      throw new BusinessRuleViolationException(
        ErrorCode.BUSINESS_RULE_VIOLATION,
        t("user.error.agency_head_missing_role", RoleConstants.CHEF_AGENCE.getName())
      );
    }
  }
}

