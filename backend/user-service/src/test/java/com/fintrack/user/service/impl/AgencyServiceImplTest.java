package com.fintrack.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.exception.BusinessRuleViolationException;
import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.repository.AgencyRepository;
import com.fintrack.user.repository.UserRepository;
import com.fintrack.user.security.CurrentActorProvider;
import com.fintrack.user.util.AgencyCodeGenerator;
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
class AgencyServiceImplTest {

  @Mock
  private AgencyRepository agencyRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AgencyCodeGenerator agencyCodeGenerator;

  @Mock
  private AuditServiceClientService auditService;

  @Mock
  private CurrentActorProvider currentActorProvider;

  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private AgencyServiceImpl agencyService;

  private Agency agency;
  private UUID agencyId;

  @BeforeEach
  void setUp() {
    agencyId = UUID.randomUUID();
    agency = new Agency();
    agency.setId(agencyId);
    agency.setName("Central Agency");
    agency.setCode("AG-001");
  }

  @Test
  @DisplayName("findById - Should return agency when it exists")
  void findById_AgencyExists_ReturnsAgency() {
    when(agencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));

    Agency result = agencyService.findById(agencyId);

    assertThat(result).isEqualTo(agency);
    verify(agencyRepository).findById(agencyId);
  }

  @Test
  @DisplayName(
    "findById - Should throw EntityNotFoundException when agency does not exist"
  )
  void findById_AgencyNotFound_ThrowsException() {
    when(agencyRepository.findById(agencyId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> agencyService.findById(agencyId)).isInstanceOf(
      EntityNotFoundException.class
    );
  }

  @Test
  @DisplayName("findAllByIds - Should return matching agencies")
  void findAllByIds_ValidIds_ReturnsAgencies() {
    Set<UUID> ids = Set.of(agencyId);
    when(agencyRepository.findAllById(ids)).thenReturn(List.of(agency));

    List<Agency> result = agencyService.findAllByIds(ids);

    assertThat(result).containsExactly(agency);
    verify(agencyRepository).findAllById(ids);
  }

  @Test
  @DisplayName("create - Should save and return agency")
  void create_ValidAgency_ReturnsCreatedAgency() {
    when(agencyRepository.save(agency)).thenAnswer(invocation -> {
      Agency saved = invocation.getArgument(0);
      saved.setId(agencyId);
      return saved;
    });

    Agency created = agencyService.create(agency);

    assertThat(created).isEqualTo(agency);
    verify(agencyRepository).save(agency);
  }

  @Test
  @DisplayName(
    "assignHead - Should set head of agency when user has CHEF_AGENCE role"
  )
  void assignHead_ValidUserWithRole_SetsHead() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setAgency(agency);
    Role chefRole = new Role();
    chefRole.setName(RoleConstants.CHEF_AGENCE.getName());
    user.setRoles(Collections.singleton(chefRole));
    user.setAgency(agency);

    when(agencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    Agency result = agencyService.assignHead(agencyId, userId);

    assertThat(result.getHeadOfAgency()).isEqualTo(user);
  }

  @Test
  @DisplayName(
    "assignHead - Should throw BusinessRuleViolationException when user lacks CHEF_AGENCE role"
  )
  void assignHead_UserWithoutRole_ThrowsException() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setRoles(Collections.emptySet());

    when(agencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() ->
      agencyService.assignHead(agencyId, userId)
    ).isInstanceOf(BusinessRuleViolationException.class);
  }
}
