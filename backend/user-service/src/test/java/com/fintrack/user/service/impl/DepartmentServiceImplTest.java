package com.fintrack.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.exception.BusinessRuleViolationException;
import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.ServiceEntity;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.repository.ServiceRepository;
import com.fintrack.user.repository.UserRepository;
import com.fintrack.user.security.CurrentActorProvider;
import java.util.Collections;
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
class DepartmentServiceImplTest {

  @Mock
  private ServiceRepository serviceRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AuditServiceClientService auditService;

  @Mock
  private CurrentActorProvider currentActorProvider;

  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private DepartmentServiceImpl departmentService;

  private ServiceEntity service;
  private UUID serviceId;

  @BeforeEach
  void setUp() {
    serviceId = UUID.randomUUID();
    service = new ServiceEntity();
    service.setId(serviceId);
    service.setName("IT Department");
  }

  @Test
  @DisplayName("findById - Should return service when it exists")
  void findById_ServiceExists_ReturnsService() {
    when(serviceRepository.findById(serviceId)).thenReturn(
      Optional.of(service)
    );

    ServiceEntity result = departmentService.findById(serviceId);

    assertThat(result).isEqualTo(service);
    verify(serviceRepository).findById(serviceId);
  }

  @Test
  @DisplayName(
    "findById - Should throw EntityNotFoundException when service does not exist"
  )
  void findById_ServiceNotFound_ThrowsException() {
    when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      departmentService.findById(serviceId)
    ).isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("findAllByIds - Should return matching services")
  void findAllByIds_ValidIds_ReturnsServices() {
    Set<UUID> ids = Set.of(serviceId);
    when(serviceRepository.findAllById(ids)).thenReturn(List.of(service));

    List<ServiceEntity> result = departmentService.findAllByIds(ids);

    assertThat(result).containsExactly(service);
    verify(serviceRepository).findAllById(ids);
  }

  @Test
  @DisplayName("create - Should save and return service")
  void create_ValidService_ReturnsCreatedService() {
    when(serviceRepository.save(service)).thenAnswer(invocation -> {
      ServiceEntity saved = invocation.getArgument(0);
      saved.setId(serviceId);
      return saved;
    });

    ServiceEntity created = departmentService.create(service);

    assertThat(created).isEqualTo(service);
    verify(serviceRepository).save(service);
  }

  @Test
  @DisplayName(
    "assignHead - Should set head of service when user has CHEF_SERVICE role"
  )
  void assignHead_ValidUserWithRole_SetsHead() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setService(service);
    Role chefRole = new Role();
    chefRole.setName(RoleConstants.CHEF_SERVICE.getName());
    user.setRoles(Collections.singleton(chefRole));
    user.setService(service);

    when(serviceRepository.findById(serviceId)).thenReturn(
      Optional.of(service)
    );
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(serviceRepository.save(service)).thenReturn(service);

    ServiceEntity result = departmentService.assignHead(serviceId, userId);

    assertThat(result.getHeadOfService()).isEqualTo(user);
  }

  @Test
  @DisplayName(
    "assignHead - Should throw BusinessRuleViolationException when user lacks CHEF_SERVICE role"
  )
  void assignHead_UserWithoutRole_ThrowsException() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setRoles(Collections.emptySet());

    when(serviceRepository.findById(serviceId)).thenReturn(
      Optional.of(service)
    );
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() ->
      departmentService.assignHead(serviceId, userId)
    ).isInstanceOf(BusinessRuleViolationException.class);
  }
}
