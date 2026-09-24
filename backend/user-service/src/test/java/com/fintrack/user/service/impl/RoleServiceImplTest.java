package com.fintrack.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.exception.DuplicateResourceException;
import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.model.constant.PermissionMatrix;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.repository.PermissionRepository;
import com.fintrack.user.repository.RoleRepository;
import com.fintrack.user.security.CurrentActorProvider;
import java.util.HashSet;
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
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private PermissionRepository permissionRepository;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @Mock
  private CurrentActorProvider currentActorProvider;

  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private RoleServiceImpl roleService;

  private Role role;
  private UUID roleId;

  @BeforeEach
  void setUp() {
    roleId = UUID.randomUUID();
    role = new Role();
    role.setId(roleId);
    role.setName("ROLE_USER");
  }

  @Test
  @DisplayName("findById - Should return role when it exists")
  void findById_RoleExists_ReturnsRole() {
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

    Role result = roleService.findById(roleId);

    assertThat(result).isEqualTo(role);
    verify(roleRepository).findById(roleId);
  }

  @Test
  @DisplayName("findByName - Should return role when it exists")
  void findByName_RoleExists_ReturnsRole() {
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));

    Role result = roleService.findByName("ROLE_USER");

    assertThat(result).isEqualTo(role);
    verify(roleRepository).findByName("ROLE_USER");
  }

  @Test
  @DisplayName("create - Should save and return role")
  void create_ValidRole_ReturnsCreatedRole() {
    Permission baselinePermission = new Permission();
    baselinePermission.setName("INCIDENT_CREATE");
    Role agentRole = new Role();
    agentRole.setName(RoleConstants.AGENT.getName());
    agentRole.setPermissions(Set.of(baselinePermission));
    when(roleRepository.existsByName("ROLE_USER")).thenReturn(false);
    when(roleRepository.findByName(RoleConstants.AGENT.getName())).thenReturn(
      Optional.of(agentRole)
    );
    // create() remet l'id à null avant save : le repository simule la génération d'id.
    when(roleRepository.save(role)).thenAnswer(invocation -> {
      Role saved = invocation.getArgument(0);
      saved.setId(UUID.randomUUID());
      return saved;
    });

    Role created = roleService.create(role);

    assertThat(created).isEqualTo(role);
    assertThat(created.getPermissions()).containsExactly(baselinePermission);
    verify(roleRepository).save(role);
  }

  @Test
  @DisplayName(
    "create - Should throw DuplicateResourceException when name exists"
  )
  void create_NameExists_ThrowsException() {
    when(roleRepository.existsByName("ROLE_USER")).thenReturn(true);

    assertThatThrownBy(() -> roleService.create(role)).isInstanceOf(
      DuplicateResourceException.class
    );
  }

  @Test
  @DisplayName(
    "findById - Should throw EntityNotFoundException when role does not exist"
  )
  void findById_RoleNotFound_ThrowsException() {
    when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleService.findById(roleId)).isInstanceOf(
      EntityNotFoundException.class
    );
  }

  @Test
  @DisplayName("create - Should always include the baseline floor permissions")
  void create_AlwaysIncludesBaselineFloor() {
    Role agentRole = new Role();
    agentRole.setName(RoleConstants.AGENT.getName());
    agentRole.setPermissions(Set.of());
    List<Permission> baseline = stubBaselinePermissions();
    when(roleRepository.existsByName("ROLE_USER")).thenReturn(false);
    when(roleRepository.findByName(RoleConstants.AGENT.getName())).thenReturn(
      Optional.of(agentRole)
    );
    when(roleRepository.save(role)).thenAnswer(invocation -> {
      Role saved = invocation.getArgument(0);
      saved.setId(UUID.randomUUID());
      return saved;
    });

    Role created = roleService.create(role);

    assertThat(created.getPermissions()).containsAll(baseline);
  }

  @Test
  @DisplayName(
    "update - Should re-impose the baseline floor even when omitted from the request"
  )
  void update_ReimposesBaselineFloor() {
    Permission custom = perm("INCIDENT_VIEW_OWN");
    List<Permission> baseline = stubBaselinePermissions();
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
    when(roleRepository.saveAndFlush(role)).thenAnswer(invocation ->
      invocation.getArgument(0)
    );

    Role details = new Role();
    details.setName("ROLE_X");
    details.setPermissions(new HashSet<>(Set.of(custom)));

    Role updated = roleService.update(roleId, details);

    assertThat(updated.getPermissions()).containsAll(baseline).contains(custom);
  }

  // Le plancher universel evolue : on stubbe exactement ce que la matrice declare.
  private List<Permission> stubBaselinePermissions() {
    return PermissionMatrix.getBaselinePermissions()
      .stream()
      .map(name -> {
        Permission permission = perm(name);
        when(permissionRepository.findByName(name)).thenReturn(
          Optional.of(permission)
        );
        return permission;
      })
      .toList();
  }

  private Permission perm(String name) {
    Permission permission = new Permission();
    permission.setName(name);
    return permission;
  }
}
