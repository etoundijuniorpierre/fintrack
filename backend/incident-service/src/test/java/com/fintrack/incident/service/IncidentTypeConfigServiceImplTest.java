package com.fintrack.incident.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.exception.DuplicateResourceException;
import com.fintrack.incident.exception.EntityNotFoundException;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentActorRole;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.readmodel.ExternalService;
import com.fintrack.incident.model.readmodel.ExternalUser;
import com.fintrack.incident.repository.IncidentTypeConfigRepository;
import com.fintrack.incident.service.impl.IncidentTypeConfigServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class IncidentTypeConfigServiceImplTest {

  @Mock
  private IncidentTypeConfigRepository incidentTypeConfigRepository;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @Mock
  private UserClientService userClientService;

  @InjectMocks
  private IncidentTypeConfigServiceImpl service;

  private IncidentTypeConfig config;
  private UUID configId;
  private final UUID actorId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    configId = UUID.randomUUID();
    config = new IncidentTypeConfig();
    config.setId(configId);
    config.setName("informatique");
    config.setDisplayName("Informatique");
    config.setDescription("Incidents informatiques");
    config.setActive(true);
    config.setSlaHours(24);
  }

  // ── findAll (paginated) ───────────────────────────────────────────────────

  @Test
  @DisplayName("findAll(pageable) - Returns page of configs")
  void findAll_pageable_returnsPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<IncidentTypeConfig> page = new PageImpl<>(
      List.of(config),
      pageable,
      1
    );
    when(incidentTypeConfigRepository.findAll(pageable)).thenReturn(page);

    Page<IncidentTypeConfig> result = service.findAll(pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("informatique");
    verify(incidentTypeConfigRepository).findAll(pageable);
  }

  // ── findAll (list) ────────────────────────────────────────────────────────

  @Test
  @DisplayName("findAll() - Returns all configs as list")
  void findAll_list_returnsAllConfigs() {
    when(incidentTypeConfigRepository.findAll()).thenReturn(List.of(config));

    List<IncidentTypeConfig> result = service.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("informatique");
    verify(incidentTypeConfigRepository).findAll();
  }

  // ── findById ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("findById - Returns config when found")
  void findById_exists_returnsConfig() {
    when(incidentTypeConfigRepository.findById(configId)).thenReturn(
      Optional.of(config)
    );

    IncidentTypeConfig result = service.findById(configId);

    assertThat(result.getId()).isEqualTo(configId);
    assertThat(result.getName()).isEqualTo("informatique");
  }

  @Test
  @DisplayName("findById - Throws EntityNotFoundException when not found")
  void findById_notFound_throwsEntityNotFoundException() {
    UUID unknownId = UUID.randomUUID();
    when(incidentTypeConfigRepository.findById(unknownId)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() -> service.findById(unknownId)).isInstanceOf(
      EntityNotFoundException.class
    );
  }

  // ── findByName ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("findByName - Returns config when found")
  void findByName_exists_returnsConfig() {
    when(incidentTypeConfigRepository.findByName("informatique")).thenReturn(
      Optional.of(config)
    );

    IncidentTypeConfig result = service.findByName("informatique");

    assertThat(result.getName()).isEqualTo("informatique");
  }

  @Test
  @DisplayName("findByName - Throws EntityNotFoundException when not found")
  void findByName_notFound_throwsEntityNotFoundException() {
    when(incidentTypeConfigRepository.findByName("unknown")).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() -> service.findByName("unknown")).isInstanceOf(
      EntityNotFoundException.class
    );
  }

  // ── create ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("create - Saves and returns new config")
  void create_validConfig_savesAndReturns() {
    IncidentTypeConfig newConfig = new IncidentTypeConfig();
    newConfig.setName("reseau");
    newConfig.setDisplayName("Réseau");

    when(incidentTypeConfigRepository.existsByName("reseau")).thenReturn(false);
    when(incidentTypeConfigRepository.save(any())).thenAnswer(inv -> {
      IncidentTypeConfig saved = inv.getArgument(0);
      saved.setId(UUID.randomUUID());
      return saved;
    });

    IncidentTypeConfig result = service.create(newConfig, actorId);

    assertThat(result.getName()).isEqualTo("reseau");
    assertThat(result.getId()).isNotNull();
    verify(auditServiceClientService).audit(
      any(),
      any(),
      any(),
      any(),
      eq("INCIDENT_TYPE_CONFIG"),
      any(),
      any(),
      any()
    );
  }

  @Test
  @DisplayName(
    "create - Throws DuplicateResourceException when name already exists"
  )
  void create_duplicateName_throwsDuplicateResourceException() {
    IncidentTypeConfig duplicate = new IncidentTypeConfig();
    duplicate.setName("informatique");
    duplicate.setDisplayName("Duplicate");

    when(incidentTypeConfigRepository.existsByName("informatique")).thenReturn(
      true
    );

    assertThatThrownBy(() -> service.create(duplicate, actorId)).isInstanceOf(
      DuplicateResourceException.class
    );

    verify(incidentTypeConfigRepository, never()).save(any());
  }

  @Test
  @DisplayName(
    "create - Accepts config with both defaultTargetServiceId and defaultTargetUserId"
  )
  void create_withBothTargets_succeeds() {
    IncidentTypeConfig valid = new IncidentTypeConfig();
    valid.setName("type-both");
    valid.setDisplayName("Type Both");
    valid.setDefaultTargetServiceId(UUID.randomUUID());
    valid.setDefaultTargetUserId(UUID.randomUUID());

    when(incidentTypeConfigRepository.existsByName("type-both")).thenReturn(
      false
    );

    ExternalService serviceRes = new ExternalService();
    serviceRes.setId(valid.getDefaultTargetServiceId());
    serviceRes.setActive(true);
    when(
      userClientService.getService(valid.getDefaultTargetServiceId())
    ).thenReturn(serviceRes);

    ExternalUser userRes = new ExternalUser();
    userRes.setId(valid.getDefaultTargetUserId());
    userRes.setActive(true);
    userRes.setPermissions(Set.of("INCIDENT_TREAT"));
    userRes.setServiceId(valid.getDefaultTargetServiceId());
    when(userClientService.getUser(valid.getDefaultTargetUserId())).thenReturn(
      userRes
    );

    when(incidentTypeConfigRepository.save(any())).thenAnswer(inv -> {
      IncidentTypeConfig saved = inv.getArgument(0);
      saved.setId(UUID.randomUUID());
      return saved;
    });

    IncidentTypeConfig result = service.create(valid, actorId);

    assertThat(result.getDefaultTargetServiceId()).isNotNull();
    assertThat(result.getDefaultTargetUserId()).isNotNull();
  }

  // ── update ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("update - Updates and returns modified config")
  void update_validData_updatesAndReturns() {
    UUID validatorId = UUID.randomUUID();
    ExternalUser mockValidator = new ExternalUser();
    mockValidator.setId(validatorId);
    mockValidator.setActive(true);
    mockValidator.setPermissions(Set.of("VALIDATION_DIRECTION"));
    when(userClientService.getUser(validatorId)).thenReturn(mockValidator);

    IncidentTypeConfig updates = new IncidentTypeConfig();
    updates.setName("informatique");
    updates.setDisplayName("Informatique Updated");
    updates.setDescription("Updated description");
    updates.setActive(false);
    updates.setSlaHours(48);
    updates.setDefaultCriticality(Criticality.HIGH);
    updates.setRequiresDirectionValidation(true);
    updates.setDirectionValidatorIds(Set.of(validatorId));

    when(incidentTypeConfigRepository.findById(configId)).thenReturn(
      Optional.of(config)
    );
    when(incidentTypeConfigRepository.save(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );

    IncidentTypeConfig result = service.update(configId, updates, actorId);

    assertThat(result.getDisplayName()).isEqualTo("Informatique Updated");
    assertThat(result.getSlaHours()).isEqualTo(48);
    assertThat(result.getDefaultCriticality()).isEqualTo(Criticality.HIGH);
    assertThat(result.isActive()).isFalse();
    assertThat(result.isRequiresDirectionValidation()).isTrue();
    assertThat(result.getDirectionValidatorIds()).containsExactly(validatorId);
    verify(auditServiceClientService).audit(
      any(),
      any(),
      any(),
      any(),
      eq("INCIDENT_TYPE_CONFIG"),
      any(),
      any(),
      any()
    );
  }

  @Test
  @DisplayName(
    "update - Updates defaultTargetServiceId and defaultTargetUserId"
  )
  void update_withBothTargets_updatesFields() {
    UUID targetServiceId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    IncidentTypeConfig updates = new IncidentTypeConfig();
    updates.setName("informatique");
    updates.setDisplayName("Informatique");
    updates.setDefaultTargetServiceId(targetServiceId);
    updates.setDefaultTargetUserId(targetUserId);

    when(incidentTypeConfigRepository.findById(configId)).thenReturn(
      Optional.of(config)
    );

    ExternalService serviceRes = new ExternalService();
    serviceRes.setId(targetServiceId);
    serviceRes.setActive(true);
    when(userClientService.getService(targetServiceId)).thenReturn(serviceRes);

    ExternalUser userRes = new ExternalUser();
    userRes.setId(targetUserId);
    userRes.setActive(true);
    userRes.setPermissions(Set.of("INCIDENT_TREAT"));
    userRes.setServiceId(targetServiceId);
    when(userClientService.getUser(targetUserId)).thenReturn(userRes);

    when(incidentTypeConfigRepository.save(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );

    IncidentTypeConfig result = service.update(configId, updates, actorId);

    assertThat(result.getDefaultTargetServiceId()).isEqualTo(targetServiceId);
    assertThat(result.getDefaultTargetUserId()).isEqualTo(targetUserId);
  }

  @Test
  @DisplayName(
    "update - Throws DuplicateResourceException when new name already taken"
  )
  void update_newNameAlreadyTaken_throwsDuplicateResourceException() {
    IncidentTypeConfig updates = new IncidentTypeConfig();
    updates.setName("reseau");
    updates.setDisplayName("Réseau");

    when(incidentTypeConfigRepository.findById(configId)).thenReturn(
      Optional.of(config)
    );
    when(incidentTypeConfigRepository.existsByName("reseau")).thenReturn(true);

    assertThatThrownBy(() ->
      service.update(configId, updates, actorId)
    ).isInstanceOf(DuplicateResourceException.class);

    verify(incidentTypeConfigRepository, never()).save(any());
  }

  @Test
  @DisplayName("update - Allows keeping the same name")
  void update_sameName_doesNotCheckDuplicate() {
    IncidentTypeConfig updates = new IncidentTypeConfig();
    updates.setName("informatique");
    updates.setDisplayName("Updated Display");

    when(incidentTypeConfigRepository.findById(configId)).thenReturn(
      Optional.of(config)
    );
    when(incidentTypeConfigRepository.save(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );

    IncidentTypeConfig result = service.update(configId, updates, actorId);

    assertThat(result.getDisplayName()).isEqualTo("Updated Display");
    verify(incidentTypeConfigRepository, never()).existsByName(any());
  }

  @Test
  @DisplayName("update - Throws EntityNotFoundException when config not found")
  void update_notFound_throwsEntityNotFoundException() {
    UUID unknownId = UUID.randomUUID();
    when(incidentTypeConfigRepository.findById(unknownId)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      service.update(unknownId, config, actorId)
    ).isInstanceOf(EntityNotFoundException.class);
  }

  // ── roles multi-choix (mapping des listes + defauts) ──────────────────────

  @Test
  @DisplayName("create - Applique les roles par defaut quand aucun n'est configure")
  void create_noRoles_appliesStepDefaults() {
    IncidentTypeConfig newConfig = new IncidentTypeConfig();
    newConfig.setName("sans-roles");
    newConfig.setDisplayName("Sans roles");

    when(incidentTypeConfigRepository.existsByName("sans-roles")).thenReturn(
      false
    );
    when(incidentTypeConfigRepository.save(any())).thenAnswer(inv -> {
      IncidentTypeConfig saved = inv.getArgument(0);
      saved.setId(UUID.randomUUID());
      return saved;
    });

    IncidentTypeConfig result = service.create(newConfig, actorId);

    assertThat(result.getTreaterRoles())
      .containsExactly(IncidentActorRole.ASSIGNEE);
    assertThat(result.getResolverRoles())
      .containsExactly(IncidentActorRole.SOURCE_AGENCY_MANAGER);
    assertThat(result.getCloserRoles())
      .containsExactly(IncidentActorRole.ASSIGNEE);
    assertThat(result.getReopenerRoles())
      .containsExactly(IncidentActorRole.SOURCE_AGENCY_MANAGER);
  }

  @Test
  @DisplayName("create - Conserve les roles multi-choix configures")
  void create_withRoles_preservesConfiguredRoles() {
    IncidentTypeConfig newConfig = new IncidentTypeConfig();
    newConfig.setName("multi");
    newConfig.setDisplayName("Multi");
    newConfig.setCloserRoles(
      Set.of(IncidentActorRole.CREATOR, IncidentActorRole.ASSIGNEE)
    );
    newConfig.setReopenerRoles(Set.of(IncidentActorRole.CHEF_SERVICE));

    when(incidentTypeConfigRepository.existsByName("multi")).thenReturn(false);
    when(incidentTypeConfigRepository.save(any())).thenAnswer(inv -> {
      IncidentTypeConfig saved = inv.getArgument(0);
      saved.setId(UUID.randomUUID());
      return saved;
    });

    IncidentTypeConfig result = service.create(newConfig, actorId);

    assertThat(result.getCloserRoles())
      .containsExactlyInAnyOrder(
        IncidentActorRole.CREATOR,
        IncidentActorRole.ASSIGNEE
      );
    assertThat(result.getReopenerRoles())
      .containsExactly(IncidentActorRole.CHEF_SERVICE);
  }

  @Test
  @DisplayName("update - Remplace franchement les listes de roles")
  void update_withRoles_replacesRoles() {
    config.setCloserRoles(Set.of(IncidentActorRole.ASSIGNEE));

    IncidentTypeConfig updates = new IncidentTypeConfig();
    updates.setName("informatique");
    updates.setDisplayName("Informatique");
    updates.setCloserRoles(
      Set.of(IncidentActorRole.CREATOR, IncidentActorRole.CHEF_SERVICE)
    );
    updates.setReopenerRoles(Set.of(IncidentActorRole.SOURCE_AGENCY_MANAGER));

    when(incidentTypeConfigRepository.findById(configId)).thenReturn(
      Optional.of(config)
    );
    when(incidentTypeConfigRepository.save(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );

    IncidentTypeConfig result = service.update(configId, updates, actorId);

    assertThat(result.getCloserRoles())
      .containsExactlyInAnyOrder(
        IncidentActorRole.CREATOR,
        IncidentActorRole.CHEF_SERVICE
      );
    assertThat(result.getReopenerRoles())
      .containsExactly(IncidentActorRole.SOURCE_AGENCY_MANAGER);
  }

  @Test
  @DisplayName("update - Restaure les defauts d'etape quand la liste est videe")
  void update_emptyRoles_fallsBackToDefaults() {
    config.setTreaterRoles(Set.of(IncidentActorRole.CHEF_SERVICE));

    IncidentTypeConfig updates = new IncidentTypeConfig();
    updates.setName("informatique");
    updates.setDisplayName("Informatique");
    updates.setTreaterRoles(Set.of());

    when(incidentTypeConfigRepository.findById(configId)).thenReturn(
      Optional.of(config)
    );
    when(incidentTypeConfigRepository.save(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );

    IncidentTypeConfig result = service.update(configId, updates, actorId);

    assertThat(result.getTreaterRoles())
      .containsExactly(IncidentActorRole.ASSIGNEE);
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("delete - Deletes config when found")
  void delete_exists_deletesConfig() {
    when(incidentTypeConfigRepository.findById(configId)).thenReturn(
      Optional.of(config)
    );

    service.delete(configId, actorId);

    verify(incidentTypeConfigRepository).deleteById(configId);
    verify(auditServiceClientService).audit(
      any(),
      any(),
      any(),
      any(),
      eq("INCIDENT_TYPE_CONFIG"),
      any(),
      any(),
      any()
    );
  }

  @Test
  @DisplayName("delete - Throws EntityNotFoundException when not found")
  void delete_notFound_throwsEntityNotFoundException() {
    UUID unknownId = UUID.randomUUID();
    when(incidentTypeConfigRepository.findById(unknownId)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() -> service.delete(unknownId, actorId)).isInstanceOf(
      EntityNotFoundException.class
    );

    verify(incidentTypeConfigRepository, never()).deleteById(any());
  }
}
