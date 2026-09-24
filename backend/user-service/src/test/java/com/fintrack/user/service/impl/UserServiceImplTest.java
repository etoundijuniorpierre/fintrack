package com.fintrack.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.client.notification.NotificationClientService;
import com.fintrack.user.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.user.exception.BusinessRuleViolationException;
import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.exception.ErrorCode;
import com.fintrack.user.model.constant.user.UserPermission;
import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.ServiceEntity;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.entity.UserAuthInfo;
import com.fintrack.user.repository.AgencyRepository;
import com.fintrack.user.repository.PermissionRepository;
import com.fintrack.user.repository.ServiceRepository;
import com.fintrack.user.repository.UserRepository;
import com.fintrack.user.security.CurrentActorProvider;
import com.fintrack.user.service.EmailService;
import com.fintrack.user.service.RoleService;
import com.fintrack.user.service.UserService;
import com.fintrack.user.util.PasswordGenerator;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private AgencyRepository agencyRepository;

  @Mock
  private ServiceRepository serviceRepository;

  @Mock
  private PermissionRepository permissionRepository;

  @Mock
  private RoleService roleService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private EmailService emailService;

  @Mock
  private PasswordGenerator passwordGenerator;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @Mock
  private CurrentActorProvider currentActorProvider;

  @Mock
  private MessageSource messageSource;

  @Mock
  private UserService self;

  @Mock
  private ReportingSystemConfigClientService reportingSystemConfigClientService;

  @Mock
  private NotificationClientService notificationClientService;

  @InjectMocks
  private UserServiceImpl userService;

  private User user;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = new User();
    user.setId(userId);
    user.setUsername("testuser");
    user.setFirstName("First");
    user.setLastName("Last");
    user.setEmail("test@fintrack.com");
    user.setPassword("encodedPassword");
    user.setRoles(new HashSet<>());
    ReflectionTestUtils.setField(userService, "self", self);
    ReflectionTestUtils.setField(userService, "messageSource", messageSource);
    // Seuils distants : on retourne le repli (constante locale) pour préserver le comportement des tests.
    lenient()
      .when(
        reportingSystemConfigClientService.getThresholdInt(
          anyString(),
          anyInt()
        )
      )
      .thenAnswer(inv -> inv.getArgument(1));
    lenient()
      .when(
        reportingSystemConfigClientService.getThresholdLong(
          anyString(),
          anyLong()
        )
      )
      .thenAnswer(inv -> inv.getArgument(1));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("findById - Should return user when user exists")
  void findById_UserExists_ReturnsUser() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    assertThat(userService.findById(userId)).isEqualTo(user);
  }

  @Test
  @DisplayName(
    "findById - Should throw EntityNotFoundException when user does not exist"
  )
  void findById_UserDoesNotExist_ThrowsEntityNotFoundException() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () ->
      userService.findById(userId)
    );
  }

  @Test
  @DisplayName("create - Should save user with encrypted temporary password")
  void create_ValidData_SavesAndReturnsUser() {
    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(roleService.findByName(anyString())).thenReturn(new Role());
    when(passwordGenerator.generateTemporaryPassword()).thenReturn("temp123");
    when(passwordEncoder.encode("temp123")).thenReturn("enc-temp123");
    when(userRepository.save(any(User.class))).thenAnswer(i ->
      i.getArgument(0)
    );

    User result = userService.create(user);

    assertThat(result.getPassword()).isNull();
    assertThat(result.getTempPassword()).isEqualTo("enc-temp123");
    assertThat(result.getTempPasswordCreatedAt()).isNotNull();
    verify(emailService).sendTemporaryPassword(
      eq(user.getEmail()),
      eq(user.getUsername()),
      eq(user.getLastName()),
      eq(user.getFirstName()),
      eq("temp123"),
      anyLong()
    );
    verify(passwordEncoder).encode("temp123");
  }

  @Test
  @DisplayName("create - Should auto-generate unique username")
  void create_GeneratesUniqueUsername() {
    when(userRepository.existsByUsername("first.last")).thenReturn(true);
    when(userRepository.existsByUsername("first.last1")).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(roleService.findByName(anyString())).thenReturn(new Role());
    when(passwordGenerator.generateTemporaryPassword()).thenReturn("temp123");
    when(userRepository.save(any(User.class))).thenAnswer(i ->
      i.getArgument(0)
    );

    User result = userService.create(user);

    assertThat(result.getUsername()).isEqualTo("first.last1");
  }

  @Test
  @DisplayName(
    "create - Should throw exception when operational role and no agency"
  )
  void create_OperationalRoleWithoutAgency_ThrowsException() {
    Role opRole = new Role();
    opRole.setName("AGENT");
    user.setRoles(Set.of(opRole));
    user.setAgency(null);

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.create(user)
    );
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MISSING_AGENCY);
  }

  @Test
  @DisplayName(
    "create - Should require agency for a custom (non-system) operational role"
  )
  void create_CustomRoleWithoutAgency_ThrowsException() {
    Role customRole = new Role();
    customRole.setName("CAISSIER");
    user.setRoles(Set.of(customRole));
    user.setAgency(null);

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.create(user)
    );
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MISSING_AGENCY);
  }

  @Test
  @DisplayName(
    "create - Agency manager scope forces agency and clears service"
  )
  void create_AgencyManagerScope_ForcesAgencyAndClearsService() {
    UUID creatorId = UUID.randomUUID();
    Agency creatorAgency = new Agency();
    creatorAgency.setId(UUID.randomUUID());
    ServiceEntity selectedService = new ServiceEntity();
    selectedService.setId(UUID.randomUUID());
    User creator = new User();
    creator.setId(creatorId);
    creator.setAgency(creatorAgency);

    Role agentRole = new Role();
    agentRole.setName("AGENT");
    agentRole.setPermissions(Set.of());
    user.setRoles(Set.of(agentRole));
    user.setAgency(null);
    user.setService(selectedService);

    authenticate(
      creatorId,
      "USER_CREATE_AGENT_AGENCY",
      "USER_VIEW_AGENCY"
    );
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    stubSuccessfulCreate();

    User result = userService.create(user);

    assertThat(result.getAgency()).isSameAs(creatorAgency);
    assertThat(result.getService()).isNull();
  }

  @Test
  @DisplayName("create - Service manager scope forces agency and service")
  void create_ServiceManagerScope_ForcesAgencyAndService() {
    UUID creatorId = UUID.randomUUID();
    Agency creatorAgency = new Agency();
    creatorAgency.setId(UUID.randomUUID());
    ServiceEntity creatorService = new ServiceEntity();
    creatorService.setId(UUID.randomUUID());
    User creator = new User();
    creator.setId(creatorId);
    creator.setAgency(creatorAgency);
    creator.setService(creatorService);

    Role agentRole = new Role();
    agentRole.setName("AGENT");
    agentRole.setPermissions(Set.of());
    user.setRoles(Set.of(agentRole));
    user.setAgency(new Agency());
    user.setService(new ServiceEntity());

    authenticate(
      creatorId,
      "USER_CREATE_AGENT_SERVICE",
      "USER_VIEW_SERVICE"
    );
    when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
    stubSuccessfulCreate();

    User result = userService.create(user);

    assertThat(result.getAgency()).isSameAs(creatorAgency);
    assertThat(result.getService()).isSameAs(creatorService);
  }

  @Test
  @DisplayName(
    "create - Persists direct permissions without copying inherited permissions"
  )
  void create_WithRoleAssign_PersistsOnlyDirectPermissions() {
    Role agentRole = new Role();
    agentRole.setName("AGENT");
    Permission inheritedPermission = new Permission();
    inheritedPermission.setId(UUID.randomUUID());
    inheritedPermission.setName("INCIDENT_CREATE");
    agentRole.setPermissions(Set.of(inheritedPermission));
    Permission explicitPermission = new Permission();
    explicitPermission.setId(UUID.randomUUID());
    explicitPermission.setName("REPORT_EXPORT");
    Agency agency = new Agency();
    agency.setId(UUID.randomUUID());
    user.setRoles(Set.of(agentRole));
    user.setPermissions(Set.of(explicitPermission, inheritedPermission));
    user.setAgency(agency);

    authenticate(
      UUID.randomUUID(),
      "USER_CREATE_ALL_AGENT",
      "USER_VIEW_ALL",
      "ROLE_ASSIGN"
    );
    stubSuccessfulCreate();

    User result = userService.create(user);

    assertThat(result.getPermissions())
      .containsExactly(explicitPermission)
      .doesNotContain(inheritedPermission);
  }

  // Installe une authentification minimale pour verifier les restrictions de perimetre.
  private void authenticate(UUID userId, String... authorities) {
    var grantedAuthorities = java.util.Arrays.stream(authorities)
      .map(SimpleGrantedAuthority::new)
      .toList();
    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(
        userId.toString(),
        "n/a",
        grantedAuthorities
      )
    );
  }

  // Configure les collaborateurs communs a une creation utilisateur reussie.
  private void stubSuccessfulCreate() {
    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordGenerator.generateTemporaryPassword()).thenReturn("temp123");
    when(passwordEncoder.encode("temp123")).thenReturn("enc-temp123");
    when(userRepository.save(any(User.class))).thenAnswer(invocation ->
      invocation.getArgument(0)
    );
  }

  @Test
  @DisplayName("update - Should update user fields without changing password")
  void update_ValidData_LeavesPasswordUnchanged() {
    User details = new User();
    details.setUsername("updateduser");
    details.setFirstName("Updated");
    details.setLastName("Name");
    details.setEmail("updated@test.com");
    details.setPhoneNumber(691000000L);
    details.setRoles(new HashSet<>());

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i ->
      i.getArgument(0)
    );

    userService.update(userId, details);

    // Verifie que le nom d'utilisateur n'est pas modifie.
    assertThat(user.getUsername()).isEqualTo("testuser");
    assertThat(user.getFirstName()).isEqualTo("Updated");
    assertThat(user.getPhoneNumber()).isEqualTo(691000000L);
    assertThat(user.getPassword()).isEqualTo("encodedPassword");
    assertThat(user.isFirstLogin()).isTrue();
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  @DisplayName("findFiltered - Should apply WebSocket presence in one repository query")
  void findFiltered_connectedFilter_usesOnlineUsernames() {
    var pageable = PageRequest.of(0, 10);
    Set<String> onlineUsernames = Set.of("alice", "bob");
    when(notificationClientService.getOnlineUsernames()).thenReturn(
      onlineUsernames
    );
    when(
      userRepository.findFiltered(
        null,
        null,
        null,
        null,
        null,
        true,
        onlineUsernames,
        pageable
      )
    ).thenReturn(Page.empty(pageable));

    userService.findFiltered(
      null,
      null,
      null,
      null,
      null,
      true,
      pageable
    );

    verify(notificationClientService, times(1)).getOnlineUsernames();
    verify(userRepository, times(1)).findFiltered(
      null,
      null,
      null,
      null,
      null,
      true,
      onlineUsernames,
      pageable
    );
  }

  @Test
  @DisplayName(
    "update - Persists direct permissions without copying inherited permissions"
  )
  void update_WithPermissions_PersistsOnlyDirectPermissions() {
    Permission inheritedPermission = new Permission();
    inheritedPermission.setId(UUID.randomUUID());
    inheritedPermission.setName("INCIDENT_CREATE");
    Permission directPermission = new Permission();
    directPermission.setId(UUID.randomUUID());
    directPermission.setName("REPORT_EXPORT");
    Role agentRole = new Role();
    agentRole.setName("AGENT");
    agentRole.setPermissions(Set.of(inheritedPermission));
    user.setRoles(Set.of(agentRole));
    Agency agency = new Agency();
    agency.setId(UUID.randomUUID());
    user.setAgency(agency);
    Permission manageProfilePermission = new Permission();
    manageProfilePermission.setId(UUID.randomUUID());
    manageProfilePermission.setName(
      UserPermission.USER_MANAGE_PROFILE.getName()
    );

    User details = new User();
    details.setFirstName(user.getFirstName());
    details.setLastName(user.getLastName());
    details.setEmail(user.getEmail());
    details.setPhoneNumber(user.getPhoneNumber());
    details.setAgency(agency);
    details.setRoles(Set.of(agentRole));
    details.setPermissions(Set.of(inheritedPermission, directPermission));

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(
      permissionRepository.findByName(
        UserPermission.USER_MANAGE_PROFILE.getName()
      )
    ).thenReturn(Optional.of(manageProfilePermission));
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation ->
      invocation.getArgument(0)
    );

    User result = userService.update(userId, details);

    assertThat(result.getPermissions())
      .containsExactlyInAnyOrder(directPermission, manageProfilePermission)
      .doesNotContain(inheritedPermission);
  }

  @Test
  @DisplayName("delete - Should delete user when user exists")
  void delete_UserExists_DeletesUser() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    userService.delete(userId);
    verify(userRepository).deleteById(userId);
  }

  @Test
  @DisplayName(
    "delete - Should keep a snapshot of the deleted user in the audit log"
  )
  void delete_UserExists_AuditKeepsSnapshot() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    userService.delete(userId);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(
      Map.class
    );
    verify(auditServiceClientService).audit(
      any(),
      any(),
      any(),
      eq("USER_DELETE"),
      eq("USER"),
      eq(userId.toString()),
      eq("SUCCESS"),
      detailsCaptor.capture()
    );
    Map<String, Object> snapshot = detailsCaptor.getValue();
    assertThat(snapshot).isNotNull();
    assertThat(snapshot).containsEntry("username", "testuser");
    assertThat(snapshot).containsEntry("email", "test@fintrack.com");
  }

  @Test
  @DisplayName("authenticate - Should return user on valid normal login")
  void authenticate_ValidNormalLogin_ReturnsUser() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getId()).thenReturn(userId);
    when(auth.getPassword()).thenReturn("encoded");
    when(auth.getActive()).thenReturn(true);
    when(auth.getFirstLogin()).thenReturn(false);

    when(userRepository.findAuthInfoByUsername("user")).thenReturn(
      Optional.of(auth)
    );
    when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    User result = userService.authenticate("user", "pass");

    assertThat(result).isEqualTo(user);
  }

  @Test
  @DisplayName("authenticate - Trims username boundaries and ignores case")
  void authenticate_NormalizesUsernameWithoutRemovingInternalSpaces() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getId()).thenReturn(userId);
    when(auth.getPassword()).thenReturn("encoded");
    when(auth.getActive()).thenReturn(true);
    when(auth.getFirstLogin()).thenReturn(false);
    when(userRepository.findAuthInfoByUsername("Jean Pierre")).thenReturn(
      Optional.of(auth)
    );
    when(passwordEncoder.matches("pass", "encoded")).thenReturn(true);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    userService.authenticate("  Jean Pierre  ", "pass");

    verify(userRepository).findAuthInfoByUsername("Jean Pierre");
  }

  @Test
  @DisplayName("authenticate - Unknown username uses generic invalid credentials")
  void authenticate_UnknownUsername_DoesNotExposeAccountExistence() {
    when(userRepository.findAuthInfoByUsername("unknown")).thenReturn(
      Optional.empty()
    );

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.authenticate("unknown", "pass")
    );

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  @DisplayName("incrementFailedAttempts - Uses an atomic repository update")
  void incrementFailedAttempts_UsesAtomicRepositoryUpdate() {
    user.setFailedLoginAttempts(1);
    when(userRepository.incrementFailedLoginAttempts(userId, 5)).thenReturn(1);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    int attempts = userService.incrementFailedAttempts(userId);

    assertThat(attempts).isEqualTo(1);
    verify(userRepository).incrementFailedLoginAttempts(userId, 5);
    verify(userRepository, never()).saveAndFlush(any(User.class));
  }

  @Test
  @DisplayName("updateProfile - Normalizes and changes username")
  void updateProfile_UsernameChanged_NormalizesAndSaves() {
    User request = new User();
    request.setUsername("  Jean Pierre  ");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(
      userRepository.existsByUsernameIgnoreCase("Jean Pierre")
    ).thenReturn(false);
    when(userRepository.findByRolesIn(any())).thenReturn(List.of());
    when(userRepository.save(user)).thenReturn(user);

    User result = userService.updateProfile(userId, request);

    assertThat(result.getUsername()).isEqualTo("Jean Pierre");
    verify(userRepository).existsByUsernameIgnoreCase("Jean Pierre");
  }

  @Test
  @DisplayName("updateProfile - Rejects a case-insensitive duplicate username")
  void updateProfile_DuplicateUsername_ThrowsDuplicateUsername() {
    User request = new User();
    request.setUsername("Other User");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(
      userRepository.existsByUsernameIgnoreCase("Other User")
    ).thenReturn(true);

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.updateProfile(userId, request)
    );

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USERNAME);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName(
    "authenticate - Should NOT activate user on first login — deferred to changePassword"
  )
  void authenticate_ValidFirstLoginTemp_SetsLastLoginButDoesNotActivate() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getId()).thenReturn(userId);
    when(auth.getTempPassword()).thenReturn("enc-temp123");
    when(auth.getFirstLogin()).thenReturn(true);
    when(auth.getUpdatedAt()).thenReturn(LocalDateTime.now());

    when(userRepository.findAuthInfoByUsername("user")).thenReturn(
      Optional.of(auth)
    );
    when(passwordEncoder.matches("temp123", "enc-temp123")).thenReturn(true);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    userService.authenticate("user", "temp123");

    assertThat(user.isActive()).isFalse();
    verify(passwordEncoder).matches("temp123", "enc-temp123");
  }

  @Test
  @DisplayName(
    "authenticate - Should reject an expired temporary password at login"
  )
  void authenticate_ExpiredTempPassword_ThrowsException() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getTempPassword()).thenReturn("enc-temp123");
    when(auth.getFirstLogin()).thenReturn(true);
    when(auth.getUpdatedAt()).thenReturn(LocalDateTime.now().minusMinutes(31));

    when(userRepository.findAuthInfoByUsername("user")).thenReturn(
      Optional.of(auth)
    );

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.authenticate("user", "temp123")
    );
    assertThat(ex.getErrorCode()).isEqualTo(
      ErrorCode.TEMPORARY_PASSWORD_EXPIRED
    );
  }

  @Test
  @DisplayName("authenticate - Locked first-login account remains locked")
  void authenticate_FirstLoginAtFailedAttemptThreshold_ThrowsAccountLocked() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getFirstLogin()).thenReturn(true);
    when(auth.getFailedLoginAttempts()).thenReturn(5);
    when(userRepository.findAuthInfoByUsername("user")).thenReturn(
      Optional.of(auth)
    );

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.authenticate("user", "correct-temp-password")
    );

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  @DisplayName(
    "authenticate - Uses configured validity and the dedicated temporary password timestamp"
  )
  void authenticate_ConfiguredValidity_UsesTempPasswordCreatedAt() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getTempPassword()).thenReturn("enc-temp123");
    when(auth.getFirstLogin()).thenReturn(true);
    when(auth.getTempPasswordCreatedAt()).thenReturn(
      LocalDateTime.now().minusMinutes(121)
    );
    when(
      reportingSystemConfigClientService.getThresholdLong(
        "tempPasswordValidityMinutes",
        30
      )
    ).thenReturn(120L);
    when(userRepository.findAuthInfoByUsername("user")).thenReturn(
      Optional.of(auth)
    );

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.authenticate("user", "temp123")
    );

    assertThat(ex.getErrorCode()).isEqualTo(
      ErrorCode.TEMPORARY_PASSWORD_EXPIRED
    );
  }

  @Test
  @DisplayName(
    "authenticate - Should throw BusinessRuleViolationException on invalid credentials"
  )
  void authenticate_InvalidCredentials_ThrowsException() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getId()).thenReturn(userId);
    when(auth.getFirstLogin()).thenReturn(false);
    when(auth.getActive()).thenReturn(true);
    when(auth.getPassword()).thenReturn("encoded");
    when(userRepository.findAuthInfoByUsername("user")).thenReturn(
      Optional.of(auth)
    );
    when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
    when(self.incrementFailedAttempts(any())).thenReturn(1);

    assertThrows(BusinessRuleViolationException.class, () ->
      userService.authenticate("user", "wrong")
    );
  }

  @Test
  @DisplayName("authenticate - Should lock account after 5 failed attempts")
  void authenticate_FiveFailedAttempts_LocksAccount() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getId()).thenReturn(userId);
    when(auth.getFirstLogin()).thenReturn(false);
    when(auth.getActive()).thenReturn(true);
    when(auth.getPassword()).thenReturn("encoded");
    user.setFailedLoginAttempts(4);
    when(userRepository.findAuthInfoByUsername("user")).thenReturn(
      Optional.of(auth)
    );
    when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
    when(self.incrementFailedAttempts(userId)).thenReturn(5);

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.authenticate("user", "wrong")
    );

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
    assertThat(user.isActive()).isFalse();
  }

  @Test
  @DisplayName(
    "authenticate - Should throw ACCOUNT_LOCKED when account is inactive due to failed attempts"
  )
  void authenticate_AccountLockedByFailedAttempts_ThrowsAccountLocked() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getFirstLogin()).thenReturn(false);
    when(auth.getActive()).thenReturn(false);
    when(auth.getFailedLoginAttempts()).thenReturn(5);
    when(userRepository.findAuthInfoByUsername("user")).thenReturn(
      Optional.of(auth)
    );

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.authenticate("user", "pass")
    );

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
  }

  @Test
  @DisplayName(
    "authenticate - Should throw ACCOUNT_INACTIVE when account is disabled by admin (0 failed attempts)"
  )
  void authenticate_AccountDisabledByAdmin_ThrowsAccountInactive() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getFirstLogin()).thenReturn(false);
    when(auth.getActive()).thenReturn(false);
    when(auth.getFailedLoginAttempts()).thenReturn(0);
    when(userRepository.findAuthInfoByUsername("user")).thenReturn(
      Optional.of(auth)
    );

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.authenticate("user", "pass")
    );

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
  }

  @Test
  @DisplayName(
    "authenticate - Should handle null failedLoginAttempts from projection without error"
  )
  void authenticate_NullFailedLoginAttempts_DoesNotThrowNPE() {
    UserAuthInfo auth = mock(UserAuthInfo.class);
    when(auth.getId()).thenReturn(userId);
    when(auth.getFirstLogin()).thenReturn(false);
    when(auth.getActive()).thenReturn(true);
    when(auth.getPassword()).thenReturn("encoded");
    when(userRepository.findAuthInfoByUsername("user")).thenReturn(
      Optional.of(auth)
    );
    when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
    when(self.incrementFailedAttempts(any())).thenReturn(1);

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> userService.authenticate("user", "wrong")
    );

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  @DisplayName("toggleStatus - Should reset failed attempts when reactivating")
  void toggleStatus_Reactivate_ResetsFailedAttempts() {
    user.setActive(false);
    user.setFailedLoginAttempts(5);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i ->
      i.getArgument(0)
    );

    User result = userService.toggleStatus(userId);

    assertThat(result.isActive()).isTrue();
    assertThat(result.getFailedLoginAttempts()).isEqualTo(0);
  }

  @Test
  @DisplayName(
    "changePassword - Should update password when current password matches"
  )
  void changePassword_ValidCurrent_UpdatesPassword() {
    user.setPassword("encodedOld");
    user.setFirstLogin(false);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("old", "encodedOld")).thenReturn(true);
    when(passwordEncoder.encode("new")).thenReturn("encodedNew");
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i ->
      i.getArgument(0)
    );

    userService.changePassword(userId, "old", "new");

    assertThat(user.getPassword()).isEqualTo("encodedNew");
    assertThat(user.getTempPassword()).isNull();
  }

  @Test
  @DisplayName(
    "changePassword - First login: skips current password check, activates user"
  )
  void changePassword_FirstLogin_ActivatesUserWithoutCurrentPasswordCheck() {
    user.setTempPassword("temp123");
    user.setFirstLogin(true);
    user.setActive(false);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("NewPass123!")).thenReturn("encodedNew");
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i ->
      i.getArgument(0)
    );

    userService.changePassword(userId, "", "NewPass123!");

    assertThat(user.getPassword()).isEqualTo("encodedNew");
    assertThat(user.getTempPassword()).isNull();
    assertThat(user.isFirstLogin()).isFalse();
    assertThat(user.isActive()).isTrue();
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  @DisplayName(
    "changePassword - Legacy active first login also skips current password"
  )
  void changePassword_LegacyActiveFirstLogin_ActivatesWithDedicatedToken() {
    user.setTempPassword(null);
    user.setPassword("encodedOld");
    user.setFirstLogin(true);
    user.setActive(true);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("NewPass123!")).thenReturn("encodedNew");
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i ->
      i.getArgument(0)
    );

    userService.changePassword(userId, null, "NewPass123!");

    assertThat(user.getPassword()).isEqualTo("encodedNew");
    assertThat(user.isFirstLogin()).isFalse();
    assertThat(user.isActive()).isTrue();
    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  @DisplayName("toggleStatus - Should flip isActive from true to false")
  void toggleStatus_ActiveUser_Deactivates() {
    user.setActive(true);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i ->
      i.getArgument(0)
    );

    User result = userService.toggleStatus(userId);

    assertThat(result.isActive()).isFalse();
    verify(userRepository).saveAndFlush(user);
  }

  @Test
  @DisplayName("toggleStatus - Should flip isActive from false to true")
  void toggleStatus_InactiveUser_Activates() {
    user.setActive(false);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i ->
      i.getArgument(0)
    );

    User result = userService.toggleStatus(userId);

    assertThat(result.isActive()).isTrue();
    verify(userRepository).saveAndFlush(user);
  }

  @Test
  @DisplayName(
    "toggleStatus - Should include the related user and agency in the notification"
  )
  void toggleStatus_NotifiesWithUserAndAgencyContext() {
    Agency agency = new Agency();
    agency.setId(UUID.randomUUID());
    agency.setName("Agency Alpha");
    user.setAgency(agency);

    User admin = new User();
    admin.setId(UUID.randomUUID());
    admin.setUsername("fintrack");
    admin.setFirstName("System");
    admin.setLastName("Administrator");

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.findByRolesIn(any())).thenReturn(List.of(admin));
    when(userRepository.saveAndFlush(user)).thenReturn(user);

    userService.toggleStatus(userId);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(
      Map.class
    );
    verify(notificationClientService).sendInternal(
      eq("fintrack"),
      any(),
      any(),
      paramsCaptor.capture(),
      anyString()
    );
    assertThat(paramsCaptor.getValue())
      .containsEntry("agency_id", agency.getId().toString())
      .containsEntry("agency_name", "Agency Alpha")
      .containsEntry("recipient_full_name", "System Administrator");
    assertThat(paramsCaptor.getValue().get("concerned_users"))
      .asList()
      .hasSize(2);
  }

  @Test
  @DisplayName(
    "cleanupExpiredTemporaryPasswords - Should call repository with cutoff time"
  )
  void cleanupExpiredTemporaryPasswords_Always_CallsRepository() {
    userService.cleanupExpiredTemporaryPasswords();
    verify(userRepository).resetExpiredTemporaryPasswords(
      any(LocalDateTime.class)
    );
  }

  private static class JqwikTestContext {

    final UserRepository userRepo = mock(UserRepository.class);
    final PermissionRepository permRepo = mock(PermissionRepository.class);
    final RoleService roleService = mock(RoleService.class);
    final PasswordGenerator pwdGen = mock(PasswordGenerator.class);
    final ReportingSystemConfigClientService reportingConfig = mock(
      ReportingSystemConfigClientService.class
    );

    {
      when(reportingConfig.getThresholdInt(anyString(), anyInt())).thenAnswer(
        inv -> inv.getArgument(1)
      );
      when(reportingConfig.getThresholdLong(anyString(), anyLong())).thenAnswer(
        inv -> inv.getArgument(1)
      );
    }

    final UserServiceImpl userService = new UserServiceImpl(
      userRepo,
      mock(AgencyRepository.class),
      mock(ServiceRepository.class),
      permRepo,
      mock(PasswordEncoder.class),
      roleService,
      mock(EmailService.class),
      pwdGen,
      mock(AuditServiceClientService.class),
      mock(CurrentActorProvider.class),
      mock(MessageSource.class),
      reportingConfig,
      mock(NotificationClientService.class)
    );
  }

  @Property(tries = 100)
  @Label("Property 1 - USER_MANAGE_PROFILE always present after create()")
  void property1_userManageProfileAlwaysPresentAfterCreate(
    @ForAll("validUsernames") String username,
    @ForAll("optionalEmails") String email
  ) {
    JqwikTestContext ctx = new JqwikTestContext();

    Permission ump = new Permission();
    ump.setName(UserPermission.USER_MANAGE_PROFILE.getName());

    when(ctx.userRepo.existsByUsername(anyString())).thenReturn(false);
    when(ctx.userRepo.existsByEmail(anyString())).thenReturn(false);
    when(ctx.roleService.findByName(anyString())).thenReturn(new Role());
    when(ctx.pwdGen.generateTemporaryPassword()).thenReturn("Temp1234!");
    when(ctx.userRepo.save(any(User.class))).thenAnswer(i -> {
      User u = i.getArgument(0);
      if (u.getId() == null) u.setId(UUID.randomUUID());
      return u;
    });
    when(
      ctx.permRepo.findByName(UserPermission.USER_MANAGE_PROFILE.getName())
    ).thenReturn(Optional.of(ump));

    User testUser = new User();
    testUser.setUsername(username);
    testUser.setEmail(email.isEmpty() ? null : email);
    testUser.setRoles(new HashSet<>());

    User created = ctx.userService.create(testUser);

    assertThat(created.getPermissions())
      .as(
        "USER_MANAGE_PROFILE must be present after create() for username=%s",
        username
      )
      .extracting(Permission::getName)
      .contains(UserPermission.USER_MANAGE_PROFILE.getName());
  }

  @Property(tries = 100)
  @Label("Property 2 - USER_MANAGE_PROFILE survives any role assignment")
  void property2_userManageProfileSurvivesAnyRoleAssignment(
    @ForAll("arbitraryRoleSets") Set<Role> roles
  ) {
    JqwikTestContext ctx = new JqwikTestContext();

    Permission ump = new Permission();
    ump.setName(UserPermission.USER_MANAGE_PROFILE.getName());

    when(ctx.userRepo.existsByUsername(anyString())).thenReturn(false);
    when(ctx.userRepo.existsByEmail(anyString())).thenReturn(false);
    when(ctx.roleService.findByName(anyString())).thenReturn(new Role());
    when(ctx.pwdGen.generateTemporaryPassword()).thenReturn("Temp1234!");
    when(ctx.userRepo.save(any(User.class))).thenAnswer(i -> {
      User u = i.getArgument(0);
      if (u.getId() == null) u.setId(UUID.randomUUID());
      return u;
    });
    when(
      ctx.permRepo.findByName(UserPermission.USER_MANAGE_PROFILE.getName())
    ).thenReturn(Optional.of(ump));

    User testUser = new User();
    testUser.setUsername("testuser");
    testUser.setRoles(roles);
    // Agence requise pour tout role operationnel : orthogonal a la propriete testee ici.
    testUser.setAgency(new Agency());

    User created = ctx.userService.create(testUser);

    assertThat(created.getPermissions())
      .as("USER_MANAGE_PROFILE must be present regardless of assigned roles")
      .extracting(Permission::getName)
      .contains(UserPermission.USER_MANAGE_PROFILE.getName());
  }

  @Provide
  Arbitrary<Set<Role>> arbitraryRoleSets() {
    Arbitrary<Permission> nonUmpPermission = Arbitraries.strings()
      .withCharRange('A', 'Z')
      .ofMinLength(3)
      .ofMaxLength(20)
      .filter(
        name -> !name.equals(UserPermission.USER_MANAGE_PROFILE.getName())
      )
      .map(name -> {
        Permission p = new Permission();
        p.setName(name);
        return p;
      });

    Arbitrary<Set<Permission>> permissionSets = nonUmpPermission
      .set()
      .ofMaxSize(5);

    Arbitrary<Role> role = permissionSets.map(perms -> {
      Role r = new Role();
      r.setName("ROLE_" + perms.size());
      r.setPermissions(perms);
      return r;
    });

    return role.set().ofMaxSize(5);
  }

  @Provide
  Arbitrary<String> validUsernames() {
    return Arbitraries.strings()
      .withCharRange('a', 'z')
      .ofMinLength(3)
      .ofMaxLength(20);
  }

  @Provide
  Arbitrary<String> optionalEmails() {
    return Arbitraries.oneOf(
      Arbitraries.just(""),
      Arbitraries.strings()
        .withCharRange('a', 'z')
        .ofMinLength(3)
        .ofMaxLength(10)
        .map(s -> s + "@example.com")
    );
  }

  @Property(tries = 100)
  @Label(
    "Property 12 - updateProfile ignores protected fields (email, roles, permissions, active)"
  )
  void property12_updateProfileIgnoresNonProfileFields(
    @ForAll("arbitraryProfileRequests") User request
  ) {
    JqwikTestContext ctx = new JqwikTestContext();
    UUID testUserId = UUID.randomUUID();

    Role role = new Role();
    role.setName("ROLE_AGENT");

    Permission perm = new Permission();
    perm.setName("USER_MANAGE_PROFILE");

    Set<Role> originalRoles = new HashSet<>(Set.of(role));
    Set<Permission> originalPermissions = new HashSet<>(Set.of(perm));

    User existingUser = new User();
    existingUser.setId(testUserId);
    existingUser.setUsername("originaluser");
    existingUser.setFirstName("First");
    existingUser.setLastName("Last");
    existingUser.setEmail("original@example.com");
    existingUser.setActive(true);
    existingUser.setRoles(originalRoles);
    existingUser.setPermissions(originalPermissions);

    when(ctx.userRepo.findById(testUserId)).thenReturn(
      Optional.of(existingUser)
    );
    when(ctx.userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

    ctx.userService.updateProfile(testUserId, request);

    assertThat(existingUser.getRoles())
      .as("updateProfile must not modify roles")
      .isEqualTo(originalRoles);
    assertThat(existingUser.getPermissions())
      .as("updateProfile must not modify permissions")
      .isEqualTo(originalPermissions);
    assertThat(existingUser.isActive())
      .as("updateProfile must not modify isActive")
      .isTrue();
    assertThat(existingUser.getEmail())
      .as("updateProfile must not modify email")
      .isEqualTo("original@example.com");
  }

  @Provide
  Arbitrary<User> arbitraryProfileRequests() {
    Arbitrary<String> usernames = Arbitraries.strings()
      .withCharRange('a', 'z')
      .ofMinLength(3)
      .ofMaxLength(20);

    Arbitrary<String> optionalEmail = Arbitraries.oneOf(
      Arbitraries.just((String) null),
      Arbitraries.strings()
        .withCharRange('a', 'z')
        .ofMinLength(3)
        .ofMaxLength(10)
        .map(s -> s + "@example.com")
    );

    Arbitrary<Long> optionalPhone = Arbitraries.oneOf(
      Arbitraries.just((Long) null),
      Arbitraries.longs().between(600000000L, 699999999L)
    );

    return Combinators.combine(usernames, optionalEmail, optionalPhone).as(
      (username, email, phone) -> {
        User profile = new User();
        profile.setUsername(username);
        profile.setEmail(email);
        profile.setPhoneNumber(phone);
        return profile;
      }
    );
  }
}
