package com.fintrack.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.repository.PermissionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

  @Mock
  private PermissionRepository permissionRepository;

  @InjectMocks
  private PermissionServiceImpl permissionService;

  private Permission permission;
  private UUID permissionId;

  @BeforeEach
  void setUp() {
    permissionId = UUID.randomUUID();
    permission = new Permission();
    permission.setId(permissionId);
    permission.setName("USER_CREATE");
  }

  @Test
  @DisplayName("findById - Should return permission when it exists")
  void findById_PermissionExists_ReturnsPermission() {
    when(permissionRepository.findById(permissionId)).thenReturn(
      Optional.of(permission)
    );

    Permission result = permissionService.findById(permissionId);

    assertThat(result).isEqualTo(permission);
    verify(permissionRepository).findById(permissionId);
  }

  @Test
  @DisplayName(
    "findById - Should throw EntityNotFoundException when permission does not exist"
  )
  void findById_PermissionNotFound_ThrowsException() {
    when(permissionRepository.findById(permissionId)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      permissionService.findById(permissionId)
    ).isInstanceOf(EntityNotFoundException.class);
  }
}
