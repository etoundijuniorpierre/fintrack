// Service metier : coordonne les operations du domaine utilisateur.

package com.fintrack.user.service.impl;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.client.audit.constant.AuditAction;
import com.fintrack.user.client.audit.constant.AuditStatus;
import com.fintrack.user.client.notification.BilingualText;
import com.fintrack.user.client.notification.NotificationClientService;
import com.fintrack.user.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.user.exception.*;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.constant.role.RolePolicy;
import com.fintrack.user.model.constant.user.UserPermission;
import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.entity.BaseEntity;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.entity.UserAuthInfo;
import com.fintrack.user.repository.AgencyRepository;
import com.fintrack.user.repository.PermissionRepository;
import com.fintrack.user.repository.ServiceRepository;
import com.fintrack.user.repository.UserRepository;
import com.fintrack.user.security.CurrentActorProvider;
import com.fintrack.user.security.UserDetailsImpl;
import com.fintrack.user.service.EmailService;
import com.fintrack.user.service.RoleService;
import com.fintrack.user.service.UserService;
import com.fintrack.user.util.PasswordGenerator;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Implementation du service d'administration et de gestion des utilisateurs.

@Slf4j
@Service
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final AgencyRepository agencyRepository;
  private final ServiceRepository serviceRepository;
  private final PermissionRepository permissionRepository;
  private final PasswordEncoder passwordEncoder;
  private final RoleService roleService;
  private final EmailService emailService;
  private final PasswordGenerator passwordGenerator;
  private final AuditServiceClientService auditServiceClientService;
  private final CurrentActorProvider currentActorProvider;
  private final MessageSource messageSource;
  private final ReportingSystemConfigClientService reportingSystemConfigClientService;
  private final NotificationClientService notificationClientService;

  private static final String NO_ONLINE_USER_SENTINEL =
    "__fintrack_no_online_user__";

  @Lazy
  @Autowired
  private UserService self;

  // Initialise le composant avec ses dependances obligatoires.
  public UserServiceImpl(
    UserRepository userRepository,
    AgencyRepository agencyRepository,
    ServiceRepository serviceRepository,
    PermissionRepository permissionRepository,
    PasswordEncoder passwordEncoder,
    RoleService roleService,
    EmailService emailService,
    PasswordGenerator passwordGenerator,
    AuditServiceClientService auditServiceClientService,
    CurrentActorProvider currentActorProvider,
    MessageSource messageSource,
    ReportingSystemConfigClientService reportingSystemConfigClientService,
    NotificationClientService notificationClientService
  ) {
    this.userRepository = userRepository;
    this.agencyRepository = agencyRepository;
    this.serviceRepository = serviceRepository;
    this.permissionRepository = permissionRepository;
    this.passwordEncoder = passwordEncoder;
    this.roleService = roleService;
    this.emailService = emailService;
    this.passwordGenerator = passwordGenerator;
    this.auditServiceClientService = auditServiceClientService;
    this.currentActorProvider = currentActorProvider;
    this.messageSource = messageSource;
    this.reportingSystemConfigClientService =
      reportingSystemConfigClientService;
    this.notificationClientService = notificationClientService;
  }

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, org.springframework.context.i18n.LocaleContextHolder.getLocale());
  }

  // Seuils cables sur Super Admin Parametres systeme (repli sur les constantes locales si indisponible).
  private int maxFailedAttempts() {
    return reportingSystemConfigClientService.getThresholdInt(
      "loginMaxFailedAttempts",
      MAX_FAILED_ATTEMPTS
    );
  }

  // Recupere la duree de validite des mots de passe temporaires.

  private long tempPasswordValidityMinutes() {
    return reportingSystemConfigClientService.getThresholdLong(
      "tempPasswordValidityMinutes",
      TEMP_PASSWORD_VALIDITY_MINUTES
    );
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les utilisateurs selon le perimetre demande.
  public Page<User> findAll(String keyword, Pageable pageable) {
    return userRepository.findAll(keyword, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les utilisateurs selon les criteres de table transmis par l'interface.
  public Page<User> findFiltered(
    String keyword,
    String roleName,
    UUID agencyId,
    UUID serviceId,
    Boolean active,
    Boolean connected,
    Pageable pageable
  ) {
    Set<String> onlineUsernames = resolveOnlineUsernames(connected);
    return hydratePageAssociations(
      userRepository.findFiltered(
        keyword,
        roleName,
        agencyId,
        serviceId,
        active,
        connected,
        onlineUsernames,
        pageable
      )
    );
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les comptes verrouilles : tentatives echouees >= seuil systeme courant.
  public Page<User> findLocked(Pageable pageable) {
    return hydratePageAssociations(
      userRepository.findByFailedLoginAttemptsGreaterThanEqual(
        maxFailedAttempts(),
        pageable
      )
    );
  }

  @Override
  @Transactional(readOnly = true)
  // Qualite : utilisateurs sans aucun role.
  public Page<User> findWithoutRole(Pageable pageable) {
    return hydratePageAssociations(userRepository.findByRolesIsEmpty(pageable));
  }

  @Override
  @Transactional(readOnly = true)
  // Qualite : utilisateurs sans perimetre (hors profils globaux).
  public Page<User> findWithoutScope(Pageable pageable) {
    return hydratePageAssociations(userRepository.findWithoutScope(pageable));
  }

  @Override
  @Transactional(readOnly = true)
  // IDs des utilisateurs inactifs (appel interne incident-service).
  public List<UUID> findInactiveIds() {
    return userRepository.findInactiveIds();
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findAll() {
    log.debug("Récupération de tous les utilisateurs");
    return userRepository.findAllWithRolesAndPermissions();
  }

  @Override
  @Transactional(readOnly = true)
  // Charge les utilisateurs actifs avec leurs rattachements pour les rapports groupes.
  public List<User> findActiveReportSubjects() {
    return userRepository.findActiveReportSubjects();
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findAllByIds(Set<UUID> ids) {
    log.debug("Récupération groupée de {} utilisateurs", ids.size());
    if (ids.isEmpty()) return List.of();
    return userRepository.findAllById(ids);
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les utilisateurs selon le perimetre demande.
  public Page<User> findAllExcludingRole(
    String roleName,
    String keyword,
    Pageable pageable
  ) {
    return userRepository.findAllExcludingRole(roleName, keyword, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les utilisateurs filtres en excluant les comptes reserves.
  public Page<User> findFilteredExcludingRole(
    String excludedRoleName,
    String keyword,
    String roleName,
    UUID agencyId,
    UUID serviceId,
    Boolean active,
    Boolean connected,
    Pageable pageable
  ) {
    Set<String> onlineUsernames = resolveOnlineUsernames(connected);
    return hydratePageAssociations(
      userRepository.findFilteredExcludingRole(
        excludedRoleName,
        keyword,
        roleName,
        agencyId,
        serviceId,
        active,
        connected,
        onlineUsernames,
        pageable
      )
    );
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les utilisateurs selon le perimetre demande.
  public List<User> findAllExcludingRole(String roleName) {
    return userRepository.findAllExcludingRole(roleName);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findAssignableUsers(UUID serviceId) {
    return userRepository.findAssignableUsers(serviceId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findDirectionValidators() {
    return userRepository.findDirectionValidators();
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les utilisateurs par agence identifiant avec pagination.
  public Page<User> findByAgencyId(
    UUID agencyId,
    String keyword,
    Pageable pageable
  ) {
    return userRepository.findByAgencyId(agencyId, keyword, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les utilisateurs filtres limites au perimetre agence courant.
  public Page<User> findFilteredByAgencyId(
    UUID scopeAgencyId,
    String keyword,
    String roleName,
    UUID agencyId,
    UUID serviceId,
    Boolean active,
    Boolean connected,
    Pageable pageable
  ) {
    Set<String> onlineUsernames = resolveOnlineUsernames(connected);
    return hydratePageAssociations(
      userRepository.findFilteredByAgencyId(
        scopeAgencyId,
        keyword,
        roleName,
        agencyId,
        serviceId,
        active,
        connected,
        onlineUsernames,
        pageable
      )
    );
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les utilisateurs par agence identifiant.
  public List<User> findByAgencyId(UUID agencyId) {
    return userRepository.findByAgencyId(agencyId);
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les utilisateurs par service identifiant avec pagination.
  public Page<User> findByServiceId(
    UUID serviceId,
    String keyword,
    Pageable pageable
  ) {
    return userRepository.findByServiceId(serviceId, keyword, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les utilisateurs filtres limites au perimetre service courant.
  public Page<User> findFilteredByServiceId(
    UUID scopeServiceId,
    String keyword,
    String roleName,
    UUID agencyId,
    UUID serviceId,
    Boolean active,
    Boolean connected,
    Pageable pageable
  ) {
    Set<String> onlineUsernames = resolveOnlineUsernames(connected);
    return hydratePageAssociations(
      userRepository.findFilteredByServiceId(
        scopeServiceId,
        keyword,
        roleName,
        agencyId,
        serviceId,
        active,
        connected,
        onlineUsernames,
        pageable
      )
    );
  }

  // Complete les associations de la page en une requete groupee, sans fetch-join sur la requete paginee.
  private Page<User> hydratePageAssociations(Page<User> page) {
    List<UUID> ids = page.getContent().stream().map(User::getId).toList();
    if (!ids.isEmpty()) {
      userRepository.hydratePageAssociations(ids);
    }
    return page;
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les utilisateurs par service identifiant.
  public List<User> findByServiceId(UUID serviceId) {
    return userRepository.findByServiceId(serviceId);
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les utilisateurs pour administrateurs.
  public List<User> findAdmins() {
    return userRepository.findByRolesIn(
      List.of(
        RoleConstants.ADMIN.getName(),
        RoleConstants.SUPER_ADMIN.getName()
      )
    );
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les utilisateurs par identifiant.
  public User findById(UUID id) {
    return userRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Utilisateur introuvable avec l'identifiant : " + id
        )
      );
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les utilisateurs par nom utilisateur.
  public User findByUsername(String username) {
    String normalizedUsername = username == null ? "" : username.trim();
    return userRepository
      .findByUsernameIgnoreCase(normalizedUsername)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.USER_NOT_FOUND,
          "User not found with username: " + normalizedUsername
        )
      );
  }

  @Override
  @Transactional
  // Cree un element du domaine utilisateur apres validation metier.
  public User create(User user) {
    String generatedUsername = generateUniqueUsername(
      user.getFirstName(),
      user.getLastName()
    );
    user.setUsername(generatedUsername);

    if (
      user.getEmail() != null &&
      !user.getEmail().trim().isEmpty() &&
      userRepository.existsByEmail(user.getEmail())
    ) {
      throw new DuplicateResourceException(
        ErrorCode.DUPLICATE_EMAIL,
        t("user.error.email_already_registered", user.getEmail())
      );
    }

    if (user.getRoles() == null || user.getRoles().isEmpty()) {
      user.setRoles(
        Set.of(roleService.findByName(RoleConstants.AGENT.getName()))
      );
    }

    user.setPermissions(
      directPermissionsOnly(user.getPermissions(), user.getRoles())
    );
    validateCreatePermissions(user);
    ensureManageProfilePermission(user);
    enforceAgencyRestrictionOnCreate(user);
    validateAgencyForOperationalRoles(user.getRoles(), user.getAgency());
    user.setActive(false);
    user.setFirstLogin(true);
    user.setPassword(null);
    Set<UUID> managedServiceIds = serviceIds(user.getManagedServices());
    UUID managedAgencyId =
      user.getManagedAgency() != null ? user.getManagedAgency().getId() : null;
    User saved = setupTemporaryPassword(user);
    boolean needsUpdate = false;
    if (managedServiceIds != null) {
      assignManagedServices(saved, managedServiceIds);
      needsUpdate = true;
    }
    if (managedAgencyId != null) {
      requireAuthority(
        "ROLE_ASSIGN",
        "Only ROLE_ASSIGN can assign a managed agency"
      );
      assignManagedAgency(saved, managedAgencyId);
      needsUpdate = true;
    }
    if (needsUpdate) {
      saved = userRepository.saveAndFlush(saved);
    }
    auditServiceClientService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.USER_CREATE.getName(),
      "USER",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName()
    );
    notifyAdmins(
      "notification.user.created.subject",
      "notification.user.created.content",
      new Object[] {
        saved.getUsername(),
        currentActorProvider.currentUsername(),
      },
      saved
    );
    return saved;
  }

  // Genere la sortie attendue pour le domaine utilisateur.

  private String generateUniqueUsername(String firstName, String lastName) {
    String baseUsername = generateBaseUsername(firstName, lastName);
    String username = baseUsername;
    int suffix = 1;
    while (userRepository.existsByUsername(username)) {
      username = baseUsername + suffix;
      suffix++;
    }
    return username;
  }

  // Genere la sortie attendue pour le domaine utilisateur.

  private String generateBaseUsername(String firstName, String lastName) {
    if (firstName == null) firstName = "";
    if (lastName == null) lastName = "";

    String firstWordFirst = firstName.trim().split("\\s+")[0];
    String firstWordLast = lastName.trim().split("\\s+")[0];

    String cleanFirst = stripAccentsAndSpecialChars(
      firstWordFirst.toLowerCase()
    );
    String cleanLast = stripAccentsAndSpecialChars(
      firstWordLast.toLowerCase()
    );

    if (cleanFirst.isEmpty() && cleanLast.isEmpty()) {
      return "user";
    } else if (cleanFirst.isEmpty()) {
      return cleanLast;
    } else if (cleanLast.isEmpty()) {
      return cleanFirst;
    } else {
      return cleanFirst + "." + cleanLast;
    }
  }

  // Normalise une chaine pour la rendre exploitable en identifiant.

  private String stripAccentsAndSpecialChars(String input) {
    if (input == null) return null;
    String normalized = Normalizer.normalize(input, Form.NFD);
    return normalized
      .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
      .replaceAll("[^a-z0-9]", "");
  }

  // Verifie que les regles metier autorisent l operation sur service interne.

  private void validateAgencyForOperationalRoles(
    Set<Role> roles,
    Agency agency
  ) {
    if (roles == null) return;

    // Operationnel = tout role non-global : robuste aux roles custom (Caissier...)
    // qui doivent eux aussi etre rattaches a une agence.
    Set<String> roleNames = roles
      .stream()
      .map(Role::getName)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    if (RolePolicy.requiresAgency(roleNames) && agency == null) {
      String errorMessage = t("user.error.missing_agency", String.join(", ", roleNames));
      throw new BusinessRuleViolationException(
        ErrorCode.MISSING_AGENCY,
        errorMessage
      );
    }
  }

  // Applique le changement demande apres validation metier.

  private User setupTemporaryPassword(User user) {
    String tempPassword = passwordGenerator.generateTemporaryPassword();
    user.setPassword(null);
    // Hache de facon irreversible (Lot 6)
    user.setTempPassword(passwordEncoder.encode(tempPassword));
    user.setTempPasswordCreatedAt(LocalDateTime.now());
    user.setActive(false);
    user.setFirstLogin(true);
    user.setFailedLoginAttempts(0);

    User savedUser = userRepository.save(user);

    if (
      savedUser.getEmail() != null && !savedUser.getEmail().trim().isEmpty()
    ) {
      emailService.sendTemporaryPassword(
        savedUser.getEmail(),
        savedUser.getUsername(),
        savedUser.getLastName(),
        savedUser.getFirstName(),
        tempPassword,
        tempPasswordValidityMinutes()
      );
    }

    return savedUser;
  }

  /**
   * protège le compte SUPER_ADMIN : seul le Super Admin lui-même
   * peut agir sur son propre compte. Les autres utilisateurs (y compris les
   * ADMIN) peuvent consulter un Super Admin mais ni le modifier ni le supprimer.
   */
  // Protege super admin.
  private void guardSuperAdmin(User user) {
    if (!user.hasRole(RoleConstants.SUPER_ADMIN.getName())) {
      return;
    }
    UUID currentId = currentUserId();
    if (currentId != null && currentId.equals(user.getId())) {
      return;
    }
    throw new PermissionDeniedException(
      "Le compte SUPER_ADMIN ne peut être modifié, désactivé ou supprimé que par lui-même"
    );
  }

  // Realise l'intention metier current user id.

  private UUID currentUserId() {
    Authentication authentication =
      SecurityContextHolder.getContext().getAuthentication();
    if (
      authentication == null ||
      !authentication.isAuthenticated() ||
      "anonymousUser".equals(authentication.getPrincipal())
    ) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserDetailsImpl userDetails) {
      return userDetails.getId();
    }
    try {
      return UUID.fromString(authentication.getName());
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  @Override
  @Transactional
  // Met a jour un element du domaine utilisateur avec les donnees validees.
  public User update(UUID id, User userDetails) {
    User user = findById(id);
    guardSuperAdmin(user);
    boolean agencyRestricted = enforceAgencyRestrictionOnUpdate(user);
    Map<String, Object> diff = new LinkedHashMap<>();

    // Username n'est plus modifiable, on ne le traite plus dans l'update.
    // On conserve la logique pour les autres champs.
    if (!Objects.equals(user.getFirstName(), userDetails.getFirstName())) {
      diff.put(
        "firstName",
        safeMap(user.getFirstName(), userDetails.getFirstName())
      );
      user.setFirstName(userDetails.getFirstName());
    }
    if (!Objects.equals(user.getLastName(), userDetails.getLastName())) {
      diff.put(
        "lastName",
        safeMap(user.getLastName(), userDetails.getLastName())
      );
      user.setLastName(userDetails.getLastName());
    }
    if (!Objects.equals(user.getEmail(), userDetails.getEmail())) {
      diff.put("email", safeMap(user.getEmail(), userDetails.getEmail()));
      user.setEmail(userDetails.getEmail());
    }
    if (!Objects.equals(user.getPhoneNumber(), userDetails.getPhoneNumber())) {
      diff.put(
        "phoneNumber",
        safeMap(user.getPhoneNumber(), userDetails.getPhoneNumber())
      );
      user.setPhoneNumber(userDetails.getPhoneNumber());
    }

    // un chef d'agence ne peut pas deplacer un agent vers une autre agence.
    if (!agencyRestricted) {
      UUID oldAgencyId =
        user.getAgency() != null ? user.getAgency().getId() : null;
      UUID newAgencyId =
        userDetails.getAgency() != null
          ? userDetails.getAgency().getId()
          : null;
      if (!Objects.equals(oldAgencyId, newAgencyId)) {
        diff.put(
          "agency",
          safeMap(
            oldAgencyId != null ? oldAgencyId.toString() : null,
            newAgencyId != null ? newAgencyId.toString() : null
          )
        );
      }
      user.setAgency(userDetails.getAgency());

      UUID oldServiceId =
        user.getService() != null ? user.getService().getId() : null;
      UUID newServiceId =
        userDetails.getService() != null
          ? userDetails.getService().getId()
          : null;
      if (!Objects.equals(oldServiceId, newServiceId)) {
        diff.put(
          "service",
          safeMap(
            oldServiceId != null ? oldServiceId.toString() : null,
            newServiceId != null ? newServiceId.toString() : null
          )
        );
      }
      user.setService(userDetails.getService());
    }

    if (userDetails.getRoles() != null) {
      requireAuthority("ROLE_ASSIGN", "Only ROLE_ASSIGN can update user roles");
      // Le role SUPER_ADMIN reste reserve a l'initialisation systeme :
      // on ne peut ni le retirer (un user SUPER_ADMIN est deja protege
      // par guardSuperAdmin) ni l'attribuer a un autre compte.
      boolean targetWantsSuperAdmin = userDetails
        .getRoles()
        .stream()
        .map(Role::getName)
        .anyMatch(RoleConstants.SUPER_ADMIN.getName()::equals);
      if (
        targetWantsSuperAdmin &&
        !user.hasRole(RoleConstants.SUPER_ADMIN.getName())
      ) {
        throw new PermissionDeniedException(
          "Le rôle SUPER_ADMIN ne peut pas être attribué via l'API : il est unique et initialisé par le système"
        );
      }

      Set<String> oldRoles =
        user.getRoles() != null
          ? user
              .getRoles()
              .stream()
              .map(Role::getName)
              .collect(Collectors.toSet())
          : Set.of();
      Set<String> newRoles = userDetails
        .getRoles()
        .stream()
        .map(Role::getName)
        .collect(Collectors.toSet());
      if (!oldRoles.equals(newRoles)) {
        diff.put(
          "roles",
          Map.of(
            "from",
            String.join(", ", oldRoles),
            "to",
            String.join(", ", newRoles)
          )
        );
        notifyAdmins(
          "notification.user.role_changed.subject",
          "notification.user.role_changed.content",
          new Object[] {
            user.getUsername(),
            currentActorProvider.currentUsername(),
          },
          user
        );
      }
      user.setRoles(userDetails.getRoles());
    }

    if (userDetails.getPermissions() != null) {
      requireAuthority(
        "ROLE_ASSIGN",
        "Only ROLE_ASSIGN can update user permissions"
      );
      Set<String> oldPermissions = permissionNames(user.getPermissions());
      user.setPermissions(
        directPermissionsOnly(userDetails.getPermissions(), user.getRoles())
      );
      ensureManageProfilePermission(user);
      Set<String> newPermissions = permissionNames(user.getPermissions());
      if (!oldPermissions.equals(newPermissions)) {
        diff.put(
          "permissions",
          Map.of(
            "from",
            String.join(", ", oldPermissions),
            "to",
            String.join(", ", newPermissions)
          )
        );
        notifyAdmins(
          "notification.user.permission_changed.subject",
          "notification.user.permission_changed.content",
          new Object[] {
            user.getUsername(),
            currentActorProvider.currentUsername(),
          },
          user
        );
      }
    }

    // Permissions revoquees : soustraites de l'union directes ∪ role (permet de
    // retirer a un utilisateur une permission heritee d'un role). Le socle applicatif
    // (baseline) ne peut jamais etre revoque.
    if (userDetails.getRevokedPermissions() != null) {
      requireAuthority(
        "ROLE_ASSIGN",
        "Only ROLE_ASSIGN can update user permissions"
      );
      // Tout type de permission peut etre revoque (aucune restriction de categorie).
      Set<Permission> revoked = new HashSet<>(
        userDetails.getRevokedPermissions()
      );

      Set<String> oldRevoked = user.getRevokedPermissions() == null
        ? Set.of()
        : user
          .getRevokedPermissions()
          .stream()
          .map(Permission::getName)
          .collect(Collectors.toSet());
      Set<String> newRevoked = revoked
        .stream()
        .map(Permission::getName)
        .collect(Collectors.toSet());
      user.setRevokedPermissions(revoked);

      // Notification aux admins + trace d'audit (diff) uniquement en cas de changement.
      if (!oldRevoked.equals(newRevoked)) {
        diff.put(
          "revokedPermissions",
          Map.of(
            "from",
            String.join(", ", oldRevoked),
            "to",
            String.join(", ", newRevoked)
          )
        );
        notifyAdmins(
          "notification.user.permission_changed.subject",
          "notification.user.permission_changed.content",
          new Object[] {
            user.getUsername(),
            currentActorProvider.currentUsername(),
          },
          user
        );
      }
    }

    if (userDetails.getManagedServices() != null) {
      requireAuthority(
        "ROLE_ASSIGN",
        "Only ROLE_ASSIGN can update managed services"
      );
      assignManagedServices(user, serviceIds(userDetails.getManagedServices()));
    } else if (!user.hasRole(RoleConstants.CHEF_SERVICE.getName())) {
      assignManagedServices(user, Set.of());
    }

    if (userDetails.getManagedAgency() != null) {
      requireAuthority(
        "ROLE_ASSIGN",
        "Only ROLE_ASSIGN can update managed agency"
      );
      assignManagedAgency(user, userDetails.getManagedAgency().getId());
    } else if (!user.hasRole(RoleConstants.CHEF_AGENCE.getName())) {
      assignManagedAgency(user, null);
    }

    validateAgencyForOperationalRoles(user.getRoles(), user.getAgency());

    User saved = userRepository.saveAndFlush(user);

    if (!diff.isEmpty()) {
      auditServiceClientService.audit(
        currentActorProvider.currentId(),
        currentActorProvider.currentUsername(),
        currentActorProvider.currentRoles(),
        AuditAction.USER_UPDATE.getName(),
        "USER",
        id.toString(),
        AuditStatus.SUCCESS.getName(),
        diff
      );
    }
    return saved;
  }

  @Override
  @Transactional
  // Met a jour un element du domaine utilisateur avec les donnees validees.
  public User updateProfile(UUID id, User profile) {
    User user = findById(id);

    if (profile.getUsername() != null) {
      String normalizedUsername = profile.getUsername().trim();
      if (
        !normalizedUsername.equalsIgnoreCase(user.getUsername()) &&
        userRepository.existsByUsernameIgnoreCase(normalizedUsername)
      ) {
        throw new BusinessRuleViolationException(
          ErrorCode.DUPLICATE_USERNAME,
          t("user.error.username_already_taken")
        );
      }
      user.setUsername(normalizedUsername);
    }

    if (profile.getPhoneNumber() != null) {
      user.setPhoneNumber(profile.getPhoneNumber());
    }
    if (profile.getAvatarDocumentId() != null) {
      user.setAvatarDocumentId(profile.getAvatarDocumentId());
    }
    notifyAdmins(
      "notification.user.profile_updated.subject",
      "notification.user.profile_updated.content",
      new Object[] { user.getUsername() },
      user
    );
    return userRepository.save(user);
  }

  @Override
  @Transactional
  // Supprime un element du domaine utilisateur apres controle metier.
  public void delete(UUID id) {
    User user = findById(id);
    guardSuperAdmin(user);
    // L'utilisateur supprime disparait de la base : on fige un instantane
    // complet dans l'audit pour qu'il reste consultable apres coup.
    Map<String, Object> snapshot = userSnapshot(user);

    // Remove from head of agency
    List<Agency> managedAgencies = agencyRepository.findByHeadOfAgencyId(id);
    for (Agency agency : managedAgencies) {
      agency.setHeadOfAgency(null);
      agencyRepository.save(agency);
    }

    // Remove from head of service
    var managedServices = serviceRepository.findByHeadOfServiceId(id);
    for (var service : managedServices) {
      service.setHeadOfService(null);
      serviceRepository.save(service);
    }

    userRepository.deleteById(id);

    auditServiceClientService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.USER_DELETE.getName(),
      "USER",
      id.toString(),
      AuditStatus.SUCCESS.getName(),
      snapshot
    );
    notifyAdmins(
      "notification.user.deleted.subject",
      "notification.user.deleted.content",
      new Object[] {
        user.getUsername(),
        currentActorProvider.currentUsername(),
      },
      user
    );
  }

  /** Instantané des champs métier d'un utilisateur, conservé dans l'audit après suppression. */
  // Realise l'intention metier user instantane.
  private Map<String, Object> userSnapshot(User user) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("username", user.getUsername());
    snapshot.put("firstName", user.getFirstName());
    snapshot.put("lastName", user.getLastName());
    snapshot.put("email", user.getEmail());
    snapshot.put("phoneNumber", user.getPhoneNumber());
    snapshot.put("active", user.isActive());
    if (user.getAgency() != null) {
      snapshot.put("agency", user.getAgency().getName());
    }
    if (user.getService() != null) {
      snapshot.put("service", user.getService().getName());
    }
    if (user.getRoles() != null) {
      snapshot.put(
        "roles",
        user.getRoles().stream().map(Role::getName).toList()
      );
    }
    return snapshot;
  }

  private static final int MAX_FAILED_ATTEMPTS = 5;

  /** Durée de validité d'un mot de passe temporaire, en minutes. */
  private static final long TEMP_PASSWORD_VALIDITY_MINUTES = 30;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  // Incremente le compteur d'echecs de connexion d'un utilisateur.
  public int incrementFailedAttempts(UUID userId) {
    int maxAttempts = maxFailedAttempts();
    int updated = userRepository.incrementFailedLoginAttempts(
      userId,
      maxAttempts
    );
    if (updated == 0) {
      throw new EntityNotFoundException(
        ErrorCode.USER_NOT_FOUND,
        "User not found with id: " + userId
      );
    }
    User user = findById(userId);
    int attempts = user.getFailedLoginAttempts();

    if (attempts == maxAttempts) {
      notifyAdmins(
        "notification.user.account_locked.subject",
        "notification.user.account_locked.content",
        new Object[] { user.getUsername(), attempts },
        user
      );
    }
    return attempts;
  }

  @Override
  @Transactional
  // Reinitialise le compteur d'echecs apres authentification reussie.
  public void resetFailedAttempts(UUID userId) {
    userRepository.resetFailedLoginAttempts(userId);
  }

  @Override
  @Transactional
  // Authentifie l'utilisateur et applique les regles de verrouillage.
  public User authenticate(String username, String password) {
    String normalizedUsername = username == null ? "" : username.trim();
    UserAuthInfo authInfo = userRepository
      .findAuthInfoByUsername(normalizedUsername)
      .orElseThrow(() ->
        new BusinessRuleViolationException(
          ErrorCode.INVALID_CREDENTIALS,
          t("user.error.invalid_credentials")
        )
      );

    LocalDateTime now = LocalDateTime.now();
    if (Boolean.TRUE.equals(authInfo.getFirstLogin())) {
      handleFirstLoginWithProjection(authInfo, password, now);
    } else {
      handleNormalLoginWithProjection(authInfo, password, now);
    }

    return findById(authInfo.getId());
  }

  // Traite ce cas applicatif et renvoie la reponse adaptee.

  private void handleFirstLoginWithProjection(
    UserAuthInfo authInfo,
    String password,
    LocalDateTime now
  ) {
    int failedAttempts =
      authInfo.getFailedLoginAttempts() == null
        ? 0
        : authInfo.getFailedLoginAttempts();
    if (failedAttempts >= maxFailedAttempts()) {
      throw new BusinessRuleViolationException(
        ErrorCode.ACCOUNT_LOCKED,
        t("user.error.account_locked_attempts"),
        Map.of(
          "failed_login_attempts",
          failedAttempts,
          "max_failed_attempts",
          maxFailedAttempts()
        )
      );
    }

    if (authInfo.getTempPassword() != null) {
      handleFirstLoginWithTempPasswordProjection(authInfo, password, now);
      return;
    }
    if (authInfo.getPassword() != null) {
      handleFirstLoginWhenActiveAndFlaggedWithProjection(
        authInfo,
        password,
        now
      );
      return;
    }
    throw new BusinessRuleViolationException(
      ErrorCode.TEMPORARY_PASSWORD_EXPIRED,
      t("user.error.temp_password_expired")
    );
  }

  // Traite ce cas applicatif et renvoie la reponse adaptee.

  private void handleFirstLoginWhenActiveAndFlaggedWithProjection(
    UserAuthInfo authInfo,
    String password,
    LocalDateTime now
  ) {
    if (!Boolean.TRUE.equals(authInfo.getActive())) {
      throw new BusinessRuleViolationException(
        ErrorCode.ACCOUNT_INACTIVE,
        t("user.error.account_inactive")
      );
    }
    if (!passwordEncoder.matches(password, authInfo.getPassword())) {
      int attempts = self.incrementFailedAttempts(authInfo.getId());
      if (attempts >= maxFailedAttempts()) {
        throw new BusinessRuleViolationException(
          ErrorCode.ACCOUNT_LOCKED,
          t("user.error.account_locked_attempts"),
          Map.of(
            "failed_login_attempts",
            attempts,
            "max_failed_attempts",
            maxFailedAttempts()
          )
        );
      }
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_CREDENTIALS,
        t("user.error.invalid_credentials"),
        Map.of(
          "failed_login_attempts",
          attempts,
          "max_failed_attempts",
          maxFailedAttempts()
        )
      );
    }

    resetFailedAttempts(authInfo.getId());
    User user = findById(authInfo.getId());
    user.setLastLogin(now);
  }

  // Traite ce cas applicatif et renvoie la reponse adaptee.

  private void handleFirstLoginWithTempPasswordProjection(
    UserAuthInfo authInfo,
    String password,
    LocalDateTime now
  ) {
    if (authInfo.getTempPassword() == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_CREDENTIALS,
        t("user.error.invalid_first_login_state")
      );
    }

    // Expiration verifiee a la connexion (et plus seulement par le job de
    // nettoyage horaire) : un mot de passe temporaire trop ancien est refuse
    // immediatement, garantissant reellement la fenetre de validite.
    LocalDateTime tempPasswordCreatedAt =
      authInfo.getTempPasswordCreatedAt() != null
        ? authInfo.getTempPasswordCreatedAt()
        : authInfo.getUpdatedAt();
    if (
      tempPasswordCreatedAt == null ||
      tempPasswordCreatedAt.isBefore(
        now.minusMinutes(tempPasswordValidityMinutes())
      )
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.TEMPORARY_PASSWORD_EXPIRED,
        t("user.error.temp_password_expired")
      );
    }

    if (!passwordEncoder.matches(password, authInfo.getTempPassword())) {
      int attempts = self.incrementFailedAttempts(authInfo.getId());
      if (attempts >= maxFailedAttempts()) {
        throw new BusinessRuleViolationException(
          ErrorCode.ACCOUNT_LOCKED,
          t("user.error.account_locked_attempts"),
          Map.of(
            "failed_login_attempts",
            attempts,
            "max_failed_attempts",
            maxFailedAttempts()
          )
        );
      }
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_CREDENTIALS,
        t("user.error.invalid_temp_password"),
        Map.of(
          "failed_login_attempts",
          attempts,
          "max_failed_attempts",
          maxFailedAttempts()
        )
      );
    }

    resetFailedAttempts(authInfo.getId());
    User user = findById(authInfo.getId());
    user.setLastLogin(now);
  }

  // Traite ce cas applicatif et renvoie la reponse adaptee.

  private void handleNormalLoginWithProjection(
    UserAuthInfo authInfo,
    String password,
    LocalDateTime now
  ) {
    if (!Boolean.TRUE.equals(authInfo.getActive())) {
      int failedAttempts =
        authInfo.getFailedLoginAttempts() == null
          ? 0
          : authInfo.getFailedLoginAttempts();
      if (failedAttempts >= maxFailedAttempts()) {
        throw new BusinessRuleViolationException(
          ErrorCode.ACCOUNT_LOCKED,
          t("user.error.account_locked_attempts"),
          Map.of(
            "failed_login_attempts",
            failedAttempts,
            "max_failed_attempts",
            maxFailedAttempts()
          )
        );
      }
      throw new BusinessRuleViolationException(
        ErrorCode.ACCOUNT_INACTIVE,
        t("user.error.account_inactive")
      );
    }

    if (
      authInfo.getPassword() == null ||
      !passwordEncoder.matches(password, authInfo.getPassword())
    ) {
      int attempts = self.incrementFailedAttempts(authInfo.getId());
      if (attempts >= maxFailedAttempts()) {
        throw new BusinessRuleViolationException(
          ErrorCode.ACCOUNT_LOCKED,
          t("user.error.account_locked_attempts"),
          Map.of(
            "failed_login_attempts",
            attempts,
            "max_failed_attempts",
            maxFailedAttempts()
          )
        );
      }
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_CREDENTIALS,
        t("user.error.invalid_credentials"),
        Map.of(
          "failed_login_attempts",
          attempts,
          "max_failed_attempts",
          maxFailedAttempts()
        )
      );
    }

    resetFailedAttempts(authInfo.getId());
    User user = findById(authInfo.getId());
    user.setLastLogin(now);
  }

  // Isole les permissions personnelles afin de ne pas les confondre avec celles des roles.
  private Set<Permission> directPermissionsOnly(
    Set<Permission> requestedPermissions,
    Set<Role> roles
  ) {
    if (requestedPermissions == null || requestedPermissions.isEmpty()) {
      return new HashSet<>();
    }
    Set<UUID> inheritedPermissionIds = roles == null
      ? Set.of()
      : roles
          .stream()
          .filter(role -> role.getPermissions() != null)
          .flatMap(role -> role.getPermissions().stream())
          .map(Permission::getId)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    return requestedPermissions
      .stream()
      .filter(permission ->
        permission.getId() == null ||
        !inheritedPermissionIds.contains(permission.getId())
      )
      .collect(Collectors.toCollection(HashSet::new));
  }

  // Fournit une liste non vide compatible avec les clauses JPQL IN/NOT IN.
  private Set<String> resolveOnlineUsernames(Boolean connected) {
    if (connected == null) return Set.of(NO_ONLINE_USER_SENTINEL);
    Set<String> onlineUsernames = notificationClientService.getOnlineUsernames();
    return onlineUsernames.isEmpty()
      ? Set.of(NO_ONLINE_USER_SENTINEL)
      : onlineUsernames;
  }

  // Garantit a chaque compte le droit minimal de gerer son propre profil.
  private void ensureManageProfilePermission(User user) {
    Set<Permission> directPermissions = user.getPermissions() == null
      ? new HashSet<>()
      : new HashSet<>(user.getPermissions());
    permissionRepository
      .findByName(UserPermission.USER_MANAGE_PROFILE.getName())
      .ifPresent(directPermissions::add);
    user.setPermissions(directPermissions);
  }

  // Verifie que les regles metier autorisent l operation sur service interne.

  private void validateCreatePermissions(User user) {
    Set<String> requestedRoles = user
      .getRoles()
      .stream()
      .map(Role::getName)
      .collect(Collectors.toSet());

    // Garde-fou universel : le role SUPER_ADMIN n'est jamais assignable
    // via l'API publique, quel que soit le profil appelant. Il est
    // initialise une seule fois par DataInitializer au demarrage et reste
    // unique pendant tout le cycle de vie de la base.
    if (requestedRoles.contains(RoleConstants.SUPER_ADMIN.getName())) {
      throw new PermissionDeniedException(
        "Le rôle SUPER_ADMIN ne peut pas être attribué via l'API : il est unique et initialisé par le système"
      );
    }

    Set<String> authorities = currentAuthorities();
    if (authorities.isEmpty()) {
      return;
    }

    if (user.getPermissions() != null && !user.getPermissions().isEmpty()) {
      requireAuthority(
        "ROLE_ASSIGN",
        "Only ROLE_ASSIGN can assign direct permissions during user creation"
      );
    }

    if (authorities.contains(UserPermission.USER_CREATE_ADMIN.getName())) {
      return;
    }

    boolean canCreateAllAgents = authorities.contains(
      UserPermission.USER_CREATE_ALL_AGENT.getName()
    );
    boolean canCreateAgencyAgents = authorities.contains(
      UserPermission.USER_CREATE_AGENT_AGENCY.getName()
    );
    boolean canCreateServiceAgents = authorities.contains(
      UserPermission.USER_CREATE_AGENT_SERVICE.getName()
    );
    boolean canCreateAgencyManager = authorities.contains(
      UserPermission.USER_CREATE_CHEF_AGENCE.getName()
    );
    boolean canCreateServiceManager = authorities.contains(
      UserPermission.USER_CREATE_CHEF_SERVICE.getName()
    );

    for (Role role : user.getRoles()) {
      String roleName = role.getName();
      boolean allowed;
      if (RoleConstants.CHEF_AGENCE.getName().equals(roleName)) {
        allowed = canCreateAgencyManager;
      } else if (RoleConstants.CHEF_SERVICE.getName().equals(roleName)) {
        allowed = canCreateServiceManager;
      } else if (!RolePolicy.isManagement(roleName)) {
        // Un role subordonne est cree selon le perimetre de creation accorde au createur.
        Set<String> roleAuthorities =
          role.getPermissions() == null
            ? Set.of()
            : role
                .getPermissions()
                .stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
        allowed =
          (canCreateAllAgents || canCreateAgencyAgents || canCreateServiceAgents) &&
          !RolePolicy.grantsElevatedAuthority(roleAuthorities);
      } else {
        // ADMIN : exige USER_CREATE_ADMIN, deja court-circuite en amont.
        allowed = false;
      }
      if (!allowed) {
        throw new PermissionDeniedException(
          t("user.error.insufficient_permission_create_role")
        );
      }
    }
  }

  // Exige authority.

  private void requireAuthority(String authority, String message) {
    Set<String> authorities = currentAuthorities();
    if (!authorities.isEmpty() && !authorities.contains(authority)) {
      throw new PermissionDeniedException(message);
    }
  }

  // Realise l'intention metier current authorities.

  private Set<String> currentAuthorities() {
    Authentication authentication =
      SecurityContextHolder.getContext().getAuthentication();
    if (
      authentication == null ||
      !authentication.isAuthenticated() ||
      "anonymousUser".equals(authentication.getPrincipal())
    ) {
      return Set.of();
    }
    return authentication
      .getAuthorities()
      .stream()
      .map(grantedAuthority -> grantedAuthority.getAuthority())
      .collect(Collectors.toSet());
  }

  // Realise l'intention metier current user entity.

  private User currentUserEntity() {
    UUID id = currentUserId();
    return id != null ? userRepository.findById(id).orElse(null) : null;
  }

  // Force l'agence du créateur et, pour un chef de service, son service.
  private void enforceAgencyRestrictionOnCreate(User newUser) {
    Set<String> authorities = currentAuthorities();
    if (
      authorities.isEmpty() ||
      authorities.contains(UserPermission.USER_VIEW_ALL.getName())
    ) {
      return;
    }
    User creator = currentUserEntity();
    if (creator == null || creator.getAgency() == null) {
      throw new PermissionDeniedException(
        "Votre compte n'est rattaché à aucune agence; création d'utilisateur impossible"
      );
    }
    newUser.setAgency(creator.getAgency());
    if (
      authorities.contains(
        UserPermission.USER_CREATE_AGENT_SERVICE.getName()
      )
    ) {
      if (creator.getService() == null) {
        throw new PermissionDeniedException(
          "Votre compte n'est rattaché à aucun service; création d'utilisateur impossible"
        );
      }
      newUser.setService(creator.getService());
    } else if (
      authorities.contains(UserPermission.USER_CREATE_AGENT_AGENCY.getName())
    ) {
      newUser.setService(null);
    }
  }

  // Controle agency restriction on update.

  private boolean enforceAgencyRestrictionOnUpdate(User target) {
    Set<String> authorities = currentAuthorities();
    if (
      authorities.isEmpty() ||
      authorities.contains(UserPermission.USER_UPDATE.getName())
    ) {
      return false;
    }
    User updater = currentUserEntity();
    if (updater == null) {
      throw new PermissionDeniedException(
        t("user.error.account_not_found_modify")
      );
    }
    boolean sameScope;
    if (authorities.contains(UserPermission.USER_VIEW_SERVICE.getName())) {
      // Un service n'etant pas rattache a une agence, on exige aussi
      // l'egalite d'agence pour empecher tout debordement inter-agences.
      sameScope =
        updater.getService() != null &&
        target.getService() != null &&
        updater.getService().getId().equals(target.getService().getId()) &&
        updater.getAgency() != null &&
        target.getAgency() != null &&
        updater.getAgency().getId().equals(target.getAgency().getId());
    } else {
      sameScope =
        updater.getAgency() != null &&
        target.getAgency() != null &&
        updater.getAgency().getId().equals(target.getAgency().getId());
    }
    if (!sameScope) {
      throw new PermissionDeniedException(
        "Vous ne pouvez modifier que les utilisateurs de votre propre périmètre"
      );
    }
    // Un chef ne gere que des subordonnes (aucun role d'encadrement),
    // pas seulement le role litteral AGENT : couvre les roles custom.
    Set<String> targetRoles =
      target.getRoles() == null
        ? Set.of()
        : target
            .getRoles()
            .stream()
            .map(Role::getName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (!RolePolicy.isSubordinate(targetRoles)) {
      throw new PermissionDeniedException(
        "Vous ne pouvez modifier que des utilisateurs subordonnés (hors encadrement)"
      );
    }
    return true;
  }

  @Override
  @Transactional
  // Rattache un utilisateur a l'agence metier demandee.
  public User assignToAgency(UUID userId, UUID agencyId) {
    User user = findById(userId);
    guardSuperAdmin(user);
    String previousAgencyId = Objects.toString(
      user.getAgency() != null ? user.getAgency().getId() : null,
      ""
    );
    user.setAgency(
      agencyRepository
        .findById(agencyId)
        .orElseThrow(() ->
          new EntityNotFoundException(
            t("user.error.not_found", agencyId)
          )
        )
    );
    auditUserAdminUpdate(
      userId,
      Map.of(
        "operation",
        "assign_agency",
        "from",
        previousAgencyId,
        "to",
        agencyId.toString()
      )
    );
    return user;
  }

  @Override
  @Transactional
  // Remplace les permissions directes d'un utilisateur.
  public User assignPermissions(UUID userId, List<UUID> permissionIds) {
    User user = findById(userId);
    guardSuperAdmin(user);
    Set<UUID> previousPermissionIds = permissionIds(user.getPermissions());
    user.setPermissions(
      new HashSet<>(permissionRepository.findAllById(permissionIds))
    );
    auditUserAdminUpdate(
      userId,
      Map.of(
        "operation",
        "assign_permissions",
        "from",
        previousPermissionIds,
        "to",
        permissionIds(user.getPermissions())
      )
    );
    return user;
  }

  // Applique le changement demande apres validation metier.

  private void assignManagedServices(User user, Set<UUID> serviceIds) {
    var currentManaged = user.getManagedServices();
    if (currentManaged == null) {
      currentManaged = new HashSet<>();
    }

    if (serviceIds == null || serviceIds.isEmpty()) {
      // Remove user from all previously managed services
      for (var s : currentManaged) {
        if (user.equals(s.getHeadOfService())) {
          s.setHeadOfService(null);
          serviceRepository.save(s);
        }
      }
      user.setManagedServices(new HashSet<>());
      return;
    }

    if (!user.hasRole(RoleConstants.CHEF_SERVICE.getName())) {
      throw new BusinessRuleViolationException(
        ErrorCode.BUSINESS_RULE_VIOLATION,
        t("user.error.service_head_must_be_chef_service")
      );
    }

    var services = serviceRepository.findAllById(serviceIds);
    if (services.size() != serviceIds.size()) {
      throw new EntityNotFoundException(
        t("user.error.one_or_more_services_not_found")
      );
    }

    var newManaged = new HashSet<>(services);

    // Retire l'utilisateur des services qu'il ne gere plus.
    for (var s : currentManaged) {
      if (!newManaged.contains(s)) {
        if (user.equals(s.getHeadOfService())) {
          s.setHeadOfService(null);
          serviceRepository.save(s);
        }
      }
    }

    // Add user as head to new managed services
    for (var s : newManaged) {
      User oldHead = s.getHeadOfService();
      if (oldHead != null && !oldHead.equals(user)) {
        if (oldHead.getManagedServices() != null) {
          oldHead.getManagedServices().remove(s);
        }
        userRepository.save(oldHead);
      }
      s.setHeadOfService(user);
      serviceRepository.save(s);
    }

    user.setManagedServices(newManaged);
  }

  // Applique le changement demande apres validation metier.

  private void assignManagedAgency(User user, UUID agencyId) {
    Agency currentAgency = user.getManagedAgency();

    if (agencyId == null) {
      // Remove user as head of the current agency
      if (
        currentAgency != null && user.equals(currentAgency.getHeadOfAgency())
      ) {
        currentAgency.setHeadOfAgency(null);
        agencyRepository.saveAndFlush(currentAgency);
      }
      user.setManagedAgency(null);
      return;
    }

    // Un chef d'agence doit porter le role CHEF_AGENCE (coherent avec les chefs de service).
    if (!user.hasRole(RoleConstants.CHEF_AGENCE.getName())) {
      throw new BusinessRuleViolationException(
        ErrorCode.BUSINESS_RULE_VIOLATION,
        t("user.error.agency_head_must_be_chef_agence")
      );
    }

    // Ignore l'ajout lorsque le lien proprietaire pointe deja vers cet utilisateur.
    if (
      currentAgency != null &&
      currentAgency.getId().equals(agencyId) &&
      user.equals(currentAgency.getHeadOfAgency())
    ) {
      return;
    }

    // Remove from current agency if any
    if (currentAgency != null && user.equals(currentAgency.getHeadOfAgency())) {
      currentAgency.setHeadOfAgency(null);
      agencyRepository.saveAndFlush(currentAgency);
    }

    Agency newAgency = agencyRepository
      .findById(agencyId)
      .orElseThrow(() -> new EntityNotFoundException("Agence introuvable"));

    // If the new agency already has a head, that head loses the agency
    User oldHead = newAgency.getHeadOfAgency();
    if (oldHead != null && !oldHead.equals(user)) {
      // Cote inverse uniquement : la FK head_user_id est reecrite via newAgency ci-dessous.
      oldHead.setManagedAgency(null);
    }

    newAgency.setHeadOfAgency(user);
    agencyRepository.save(newAgency);
    user.setManagedAgency(newAgency);
  }

  // Realise l'intention metier service ids.

  private Set<UUID> serviceIds(Set<? extends BaseEntity> services) {
    if (services == null) {
      return null;
    }
    return services
      .stream()
      .filter(Objects::nonNull)
      .map(BaseEntity::getId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  @Override
  @Transactional
  // Cree un element du domaine utilisateur apres validation metier.
  public User addPermission(UUID userId, UUID permissionId) {
    User user = findById(userId);
    guardSuperAdmin(user);
    if (user.getPermissions() == null) user.setPermissions(new HashSet<>());
    user
      .getPermissions()
      .add(
        permissionRepository
          .findById(permissionId)
          .orElseThrow(() ->
            new EntityNotFoundException(
              "Permission introuvable avec l'identifiant : " + permissionId
            )
          )
      );
    auditUserAdminUpdate(
      userId,
      Map.of("operation", "add_permission", "permissionId", permissionId)
    );
    return user;
  }

  @Override
  @Transactional
  // Supprime un element du domaine utilisateur apres controle metier.
  public User removePermission(UUID userId, UUID permissionId) {
    User user = findById(userId);
    guardSuperAdmin(user);
    if (user.getPermissions() != null) {
      user.getPermissions().removeIf(p -> p.getId().equals(permissionId));
    }
    auditUserAdminUpdate(
      userId,
      Map.of("operation", "remove_permission", "permissionId", permissionId)
    );
    return user;
  }

  @Override
  @Transactional
  // Bascule l'activation d'un compte utilisateur.
  public User toggleStatus(UUID userId) {
    User user = findById(userId);
    guardSuperAdmin(user);
    boolean previousStatus = user.isActive();
    user.setActive(!user.isActive());
    if (user.isActive()) {
      user.setFailedLoginAttempts(0);
    }
    notifyAdmins(
      "notification.user.status_changed.subject",
      "notification.user.status_changed.content",
      locale ->
        new Object[] {
          user.getUsername(),
          statusLabel(user.isActive(), locale),
          currentActorProvider.currentUsername(),
        },
      user
    );
    User saved = userRepository.saveAndFlush(user);
    auditUserAdminUpdate(
      userId,
      Map.of(
        "operation",
        "toggle_status",
        "from",
        previousStatus,
        "to",
        saved.isActive()
      )
    );
    return saved;
  }

  private Set<UUID> permissionIds(Set<Permission> permissions) {
    if (permissions == null) {
      return Set.of();
    }
    return permissions
      .stream()
      .filter(Objects::nonNull)
      .map(Permission::getId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  private Set<String> permissionNames(Set<Permission> permissions) {
    if (permissions == null) {
      return Set.of();
    }
    return permissions
      .stream()
      .filter(Objects::nonNull)
      .map(Permission::getName)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  private void auditUserAdminUpdate(
    UUID targetUserId,
    Map<String, Object> details
  ) {
    auditServiceClientService.audit(
      currentActorProvider.currentId(),
      currentActorProvider.currentUsername(),
      currentActorProvider.currentRoles(),
      AuditAction.USER_UPDATE.getName(),
      "USER",
      targetUserId.toString(),
      AuditStatus.SUCCESS.getName(),
      details
    );
  }

  @Override
  @Transactional
  // Excute le traitement pour regeneratePassword.
  public User regeneratePassword(UUID userId) {
    User user = findById(userId);
    guardSuperAdmin(user);
    User saved = setupTemporaryPassword(user);
    notifyAdmins(
      "notification.user.password_reset.subject",
      "notification.user.password_reset.content",
      new Object[] {
        user.getUsername(),
        currentActorProvider.currentUsername(),
      },
      user
    );

    auditServiceClientService.audit(
      userId,
      user.getUsername(),
      null,
      AuditAction.USER_PASSWORD_RESET.getName(),
      "user",
      userId.toString(),
      AuditStatus.SUCCESS.getName(),
      java.util.Map.of("resetBy", currentActorProvider.currentUsername())
    );

    return saved;
  }

  @Override
  @Transactional
  // Change le mot de passe d'un utilisateur apres validation.
  public User changePassword(
    UUID userId,
    String currentPassword,
    String newPassword
  ) {
    User user = findById(userId);

    if (user.isFirstLogin()) {
      user.setPassword(passwordEncoder.encode(newPassword));
      user.setTempPassword(null);
      user.setTempPasswordCreatedAt(null);
      user.setFirstLogin(false);
      user.setActive(true);
      User saved = userRepository.saveAndFlush(user);

      auditServiceClientService.audit(
        userId,
        user.getUsername(),
        null,
        AuditAction.USER_PASSWORD_CHANGE.getName(),
        "user",
        userId.toString(),
        AuditStatus.SUCCESS.getName(),
        null
      );

      return saved;
    }

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_CREDENTIALS,
        t("user.error.current_password_incorrect")
      );
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    user.setTempPassword(null);
    user.setTempPasswordCreatedAt(null);

    if (user.isFirstLogin()) {
      user.setFirstLogin(false);
      user.setActive(true);
    }

    User saved = userRepository.saveAndFlush(user);

    auditServiceClientService.audit(
      userId,
      user.getUsername(),
      null,
      AuditAction.USER_PASSWORD_CHANGE.getName(),
      "user",
      userId.toString(),
      AuditStatus.SUCCESS.getName(),
      null
    );

    return saved;
  }

  // Toutes les 15 min : la validite etant deja imposee a la connexion, ce job
  // ne fait que purger les mots de passe temporaires expires non consommes.
  @Scheduled(fixedRate = 15 * 60 * 1000)
  @Transactional
  public void scheduledTemporaryPasswordCleanup() {
    cleanupExpiredTemporaryPasswords();
  }

  @Override
  @Transactional
  // Reinitialise les mdp temporaires deja expires ; renvoie le nombre purge.
  public int cleanupExpiredTemporaryPasswords() {
    LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(
      tempPasswordValidityMinutes()
    );
    return userRepository.resetExpiredTemporaryPasswords(cutoffTime);
  }

  // Realise l'intention metier contact admin.
  @Override
  public void contactAdmin(String username, String subject, String message) {
    Optional<User> requesterOpt = userRepository.findByUsername(username);

    if (requesterOpt.isEmpty()) {
      log.warn(
        "Demande de contact administrateur pour username inconnu : {}",
        username
      );
      return;
    }

    User requester = requesterOpt.get();
    String email = requester.getEmail();
    String senderName =
      requester.getFirstName() + " " + requester.getLastName();
    String senderUsername = requester.getUsername();
    boolean requesterIsAdmin = requester
      .getRoles()
      .stream()
      .anyMatch(
        r ->
          RoleConstants.ADMIN.getName().equals(r.getName()) ||
          RoleConstants.SUPER_ADMIN.getName().equals(r.getName())
      );

    List<User> targetAdmins = requesterIsAdmin
      ? userRepository.findByRolesIn(
          List.of(RoleConstants.SUPER_ADMIN.getName())
        )
      : findAdmins();

    List<User> filteredAdmins = targetAdmins
      .stream()
      .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
      .filter(u -> !u.getEmail().equalsIgnoreCase(email))
      .toList();

    if (!filteredAdmins.isEmpty()) {
      emailService.contactAdmin(
        email,
        senderName,
        senderUsername,
        subject,
        message,
        filteredAdmins
      );
    }
  }

  // Diffuse l'information du domaine utilisateur aux destinataires concernes.

  private void notifyAdmins(
    String subjectKey,
    String contentKey,
    Object[] args,
    User relatedUser
  ) {
    notifyAdmins(subjectKey, contentKey, locale -> args, relatedUser);
  }

  // Libelle du statut d'un compte dans la langue demandee.
  private String statusLabel(boolean active, Locale locale) {
    String key = active
      ? "notification.user.status.active"
      : "notification.user.status.inactive";
    return messageSource.getMessage(key, null, key, locale);
  }

  // Diffuse aux admins avec des arguments propres a chaque langue.
  private void notifyAdmins(
    String subjectKey,
    String contentKey,
    Function<Locale, Object[]> argsFor,
    User relatedUser
  ) {
    BilingualText baseSubject = new BilingualText(
      messageSource.getMessage(subjectKey, null, subjectKey, Locale.FRENCH),
      messageSource.getMessage(subjectKey, null, subjectKey, Locale.ENGLISH)
    );
    String relatedUserName = userDisplayName(relatedUser);
    String agencyNameFr = userAgencyName(relatedUser, Locale.FRENCH);
    String agencyNameEn = userAgencyName(relatedUser, Locale.ENGLISH);
    BilingualText subject = new BilingualText(
      messageSource.getMessage(
        "notification.user.subject.context",
        new Object[] { baseSubject.fr(), relatedUserName, agencyNameFr },
        "notification.user.subject.context",
        Locale.FRENCH
      ),
      messageSource.getMessage(
        "notification.user.subject.context",
        new Object[] { baseSubject.en(), relatedUserName, agencyNameEn },
        "notification.user.subject.context",
        Locale.ENGLISH
      )
    );
    BilingualText message = new BilingualText(
      messageSource.getMessage(
        contentKey,
        argsFor.apply(Locale.FRENCH),
        contentKey,
        Locale.FRENCH
      ),
      messageSource.getMessage(
        contentKey,
        argsFor.apply(Locale.ENGLISH),
        contentKey,
        Locale.ENGLISH
      )
    );
    List<User> admins = findAdmins()
      .stream()
      .filter(admin -> admin.getUsername() != null)
      .toList();
    if (!admins.isEmpty()) {
      String eventPayload =
        subjectKey +
        "|" +
        message.fr() +
        "|" +
        admins
          .stream()
          .map(User::getUsername)
          .sorted()
          .collect(Collectors.joining(","));
      String eventKey =
        "user-service:" +
        subjectKey +
        ":" +
        UUID.nameUUIDFromBytes(eventPayload.getBytes(StandardCharsets.UTF_8));
      for (User admin : admins) {
        Map<String, Object> params = userNotificationParams(
          admin,
          relatedUser,
          agencyNameFr,
          agencyNameEn
        );
        params.put("message_body", message.fr());
        try {
          notificationClientService.sendInternal(
            admin.getUsername(),
            subject,
            message,
            params,
            eventKey + ":INTERNAL:" + admin.getUsername()
          );
        } catch (RuntimeException ex) {
          log.error(
            "Notification administrateur non envoyee a {} : {}",
            admin.getUsername(),
            ex.getMessage()
          );
        }
      }
    }
  }

  // Construit le contexte lisible d'une notification administrative.
  private Map<String, Object> userNotificationParams(
    User recipient,
    User relatedUser,
    String agencyNameFr,
    String agencyNameEn
  ) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put(
      "agency_id",
      relatedUser != null &&
      relatedUser.getAgency() != null &&
      relatedUser.getAgency().getId() != null
        ? relatedUser.getAgency().getId().toString()
        : ""
    );
    params.put("agency_name", agencyNameFr);
    params.put("agency_name_en", agencyNameEn);
    params.put("recipient_full_name", userDisplayName(recipient));

    Map<String, Object> recipientInfo = concernedUser("recipient", recipient);
    Map<String, Object> relatedUserInfo = concernedUser("user", relatedUser);
    params.put(
      "concerned_users",
      relatedUser != null &&
      relatedUser.getId() != null &&
      relatedUser.getId().equals(recipient.getId())
        ? List.of(recipientInfo)
        : List.of(recipientInfo, relatedUserInfo)
    );
    return params;
  }

  // Decrit une personne concernee sans exposer ses identifiants de connexion.
  private Map<String, Object> concernedUser(String role, User user) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("role", role);
    item.put("id", user != null && user.getId() != null ? user.getId().toString() : "");
    item.put("fullName", userDisplayName(user));
    return item;
  }

  // Produit le nom complet d'un utilisateur avec un repli lisible.
  private String userDisplayName(User user) {
    if (user == null) {
      return messageSource.getMessage(
        "notification.user.unknown",
        null,
        "Utilisateur non renseigné",
        Locale.FRENCH
      );
    }
    String firstName = user.getFirstName() != null
      ? user.getFirstName().trim()
      : "";
    String lastName = user.getLastName() != null
      ? user.getLastName().trim()
      : "";
    String fullName = (firstName + " " + lastName).trim();
    return fullName.isBlank() ? user.getUsername() : fullName;
  }

  // Retourne l'agence rattachee au compte ou le siege pour un compte central.
  private String userAgencyName(User user, Locale locale) {
    if (
      user != null &&
      user.getAgency() != null &&
      user.getAgency().getName() != null &&
      !user.getAgency().getName().isBlank()
    ) {
      return user.getAgency().getName();
    }
    return messageSource.getMessage(
      "notification.user.agency.head_office",
      null,
      "Direction générale",
      locale
    );
  }

  // Fournit une lecture tolerante pour les donnees du domaine utilisateur.

  private Map<String, Object> safeMap(Object from, Object to) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("from", from);
    map.put("to", to);
    return map;
  }
}
