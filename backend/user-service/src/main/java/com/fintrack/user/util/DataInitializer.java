// Utilitaire : regroupe les helpers techniques lies a data initializer.

package com.fintrack.user.util;

import com.fintrack.user.model.constant.PermissionMatrix;
import com.fintrack.user.model.constant.audit.AuditPermission;
import com.fintrack.user.model.constant.incident.IncidentPermission;
import com.fintrack.user.model.constant.notification.NotificationPermission;
import com.fintrack.user.model.constant.reporting.ReportingPermission;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.constant.role.RolePermission;
import com.fintrack.user.model.constant.settings.SettingsPermission;
import com.fintrack.user.model.constant.user.UserPermission;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.repository.PermissionRepository;
import com.fintrack.user.repository.RoleRepository;
import com.fintrack.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Composant d'initialisation inserant les donnees requises au demarrage.

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordGenerator passwordGenerator;
  private final JdbcTemplate jdbcTemplate;

  // Bootstrap du super-admin systeme (pilote par configuration, voir application.properties).
  @Value("${fintrack.bootstrap.super-admin.enabled:true}")
  private boolean superAdminBootstrapEnabled;

  @Value("${fintrack.bootstrap.super-admin.username:}")
  private String superAdminUsername;

  @Value("${fintrack.bootstrap.super-admin.email:}")
  private String superAdminEmail;

  @Value("${fintrack.bootstrap.super-admin.password:}")
  private String superAdminPassword;

  @Override
  @Transactional
  // Initialise les referentiels systeme requis au demarrage.
  public void run(String... args) {
    log.info("Démarrage de synchronisation des permissions et roles...");
    syncPermissions();
    migrateLegacyAgentCreationPermission();
    syncRoles();
    ensureUserManageProfilePermission();
    bootstrapSuperAdmin();
    enforceSuperAdminDatabaseConstraint();
    log.info("Synchronisation terminee.");
  }

  // Synchronise permissions.

  private void syncPermissions() {
    List<IncidentPermission> incidentConstants = List.of(
      IncidentPermission.values()
    );
    List<UserPermission> userConstants = List.of(UserPermission.values());
    List<RolePermission> roleConstants = List.of(RolePermission.values());
    List<ReportingPermission> reportingConstants = List.of(
      ReportingPermission.values()
    );
    List<NotificationPermission> notificationConstants = List.of(
      NotificationPermission.values()
    );
    List<SettingsPermission> settingsConstants = List.of(
      SettingsPermission.values()
    );
    List<AuditPermission> auditConstants = List.of(AuditPermission.values());

    syncPermissionConstants(incidentConstants, "incident");
    syncPermissionConstants(userConstants, "user");
    syncPermissionConstants(roleConstants, "role");
    syncPermissionConstants(reportingConstants, "reporting");
    syncPermissionConstants(notificationConstants, "notification");
    syncPermissionConstants(settingsConstants, "settings");
    syncPermissionConstants(auditConstants, "audit");
  }

  // Synchronise permission constants.

  private <T extends Enum<T>> void syncPermissionConstants(
    List<T> constants,
    String category
  ) {
    for (T constant : constants) {
      if (constant instanceof UserPermission userPermission && userPermission.isLegacy()) {
        continue;
      }
      String permissionName = ((Enum<?>) constant).name();
      if (!permissionRepository.existsByName(permissionName)) {
        Permission permission = new Permission();
        permission.setName(permissionName);
        permission.setDescription("permission." + permissionName);
        permissionRepository.save(permission);
        log.info("Permission {} synchronisée : {}", category, permissionName);
      }
    }
  }

  // Migre l'ancien droit de creation d'agent sans intervention manuelle sur la base.
  private void migrateLegacyAgentCreationPermission() {
    var legacy = permissionRepository.findByName("USER_CREATE_AGENT");
    var replacement = permissionRepository.findByName("USER_CREATE_ALL_AGENT");
    if (legacy.isEmpty() || replacement.isEmpty()) return;

    jdbcTemplate.update(
      "INSERT INTO role_permissions (role_id, permission_id) " +
        "SELECT role_id, ? FROM role_permissions WHERE permission_id = ? " +
        "ON CONFLICT (role_id, permission_id) DO NOTHING",
      replacement.get().getId(),
      legacy.get().getId()
    );
    jdbcTemplate.update(
      "INSERT INTO user_permissions (user_id, permission_id) " +
        "SELECT user_id, ? FROM user_permissions WHERE permission_id = ? " +
        "ON CONFLICT (user_id, permission_id) DO NOTHING",
      replacement.get().getId(),
      legacy.get().getId()
    );
    jdbcTemplate.update(
      "DELETE FROM role_permissions WHERE permission_id = ?",
      legacy.get().getId()
    );
    jdbcTemplate.update(
      "DELETE FROM user_permissions WHERE permission_id = ?",
      legacy.get().getId()
    );
    permissionRepository.delete(legacy.get());
    permissionRepository.flush();
    log.info("Permission historique USER_CREATE_AGENT migree vers USER_CREATE_ALL_AGENT.");
  }

  // Synchronise roles.

  private void syncRoles() {
    Map<String, List<String>> matrix = PermissionMatrix.getRoleMatrix();

    for (Map.Entry<String, List<String>> entry : matrix.entrySet()) {
      String roleName = entry.getKey();
      List<String> rolePermissionNames = entry.getValue();

      Role role = roleRepository.findByName(roleName).orElseGet(() -> {
        Role newRole = new Role();
        newRole.setName(roleName);
        newRole.setSystem(true);
        newRole.setDescription("role." + roleName + ".description");
        log.info("Création de role systeme: {}", roleName);
        return newRole;
      });

      Set<Permission> permissions = rolePermissionNames
        .stream()
        .map(permName ->
          permissionRepository
            .findByName(permName)
            .orElseThrow(() ->
              new RuntimeException("Permission introuvable : " + permName)
            )
        )
        .collect(Collectors.toSet());

      role.setPermissions(permissions);
      roleRepository.save(role);
    }
  }

  // Garantit user manage profile permission.

  private void ensureUserManageProfilePermission() {
    permissionRepository
      .findByName(UserPermission.USER_MANAGE_PROFILE.getName())
      .ifPresent(profilePermission -> {
        List<User> allUsers = userRepository.findAll();
        int count = 0;
        for (User user : allUsers) {
          boolean hasIt =
            user.getPermissions() != null &&
            user
              .getPermissions()
              .stream()
              .anyMatch(p ->
                UserPermission.USER_MANAGE_PROFILE.getName().equals(p.getName())
              );
          if (!hasIt) {
            if (user.getPermissions() == null) user.setPermissions(
              new HashSet<>()
            );
            user.getPermissions().add(profilePermission);
            count++;
          }
        }
        if (count > 0) log.info(
          "Permission USER_MANAGE_PROFILE ajoutée rétroactivement pour {} utilisateur(s) existant(s)",
          count
        );
      });
  }

  // Initialise le super-admin systeme au premier demarrage. Idempotent : ne fait rien
  // si un SUPER_ADMIN existe deja. Identite (username/email) et mot de passe pilotes par
  // configuration. Sans mot de passe fourni, un mot de passe temporaire est genere et
  // impose au premier login (changement obligatoire).
  private void bootstrapSuperAdmin() {
    if (!superAdminBootstrapEnabled) {
      log.info("Initialisation super-admin désactivée ; traitement ignoré.");
      return;
    }

    Role superAdminRole = roleRepository
      .findByName(RoleConstants.SUPER_ADMIN.getName())
      .orElseThrow(() ->
        new IllegalStateException(
          "Role SUPER_ADMIN introuvable ; initialisation super-admin impossible"
        )
      );

    // Idempotence : un seul super-admin systeme suffit.
    if (
      !userRepository
        .findByRolesIn(List.of(RoleConstants.SUPER_ADMIN.getName()))
        .isEmpty()
    ) {
      log.info("A SUPER_ADMIN existe déjà; initialisation ignoree.");
      return;
    }

    if (userRepository.existsByUsername(superAdminUsername)) {
      log.warn(
        "Identifiant d'initialisation '{}' déjà utilisé ; initialisation super-admin ignorée.",
        superAdminUsername
      );
      return;
    }

    User superAdmin = new User();
    superAdmin.setUsername(superAdminUsername);
    superAdmin.setEmail(superAdminEmail);
    superAdmin.setFirstName("System");
    superAdmin.setLastName("Administrator");
    superAdmin.setCreatedAt(LocalDateTime.now());
    superAdmin.setFailedLoginAttempts(0);
    superAdmin.setRoles(Set.of(superAdminRole));

    if (superAdminPassword != null && !superAdminPassword.isBlank()) {
      // Mot de passe fourni par configuration : compte actif immediatement.
      superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
      superAdmin.setTempPassword(null);
      superAdmin.setTempPasswordCreatedAt(null);
      superAdmin.setActive(true);
      superAdmin.setFirstLogin(false);
      userRepository.save(superAdmin);
      log.info(
        "Super-admin système '{}' créé avec le mot de passe configuré (actif).",
        superAdminUsername
      );
    } else {
      // Repli securise : mot de passe temporaire genere, changement force au premier login.
      String tempPassword = passwordGenerator.generateTemporaryPassword();
      superAdmin.setPassword(null);
      superAdmin.setTempPassword(passwordEncoder.encode(tempPassword));
      superAdmin.setTempPasswordCreatedAt(java.time.LocalDateTime.now());
      superAdmin.setActive(false);
      superAdmin.setFirstLogin(true);
      userRepository.save(superAdmin);
      log.warn(
        "==================== INITIALISATION SUPER-ADMIN ===================="
      );
      log.warn(
        "SUPER_ADMIN_PASSWORD non defini. Mot de passe temporaire a usage unique genere."
      );
      log.warn(
        "mot de passe pour '{}' (définir SUPER_ADMIN_PASSWORD pour éviter cela) :",
        superAdminUsername
      );
      log.warn("    {}", tempPassword);
      log.warn(
        "Connectez-vous une fois avec ce mot de passe, puis définissez un mot de passe permanent (obligatoire)."
      );
      log.warn(
        "==============================================================="
      );
    }
  }

  // Ajoute un trigger au niveau de la base de données pour garantir qu'un seul SUPER_ADMIN
  // puisse être assigné dans la table de jointure user_roles.
  private void enforceSuperAdminDatabaseConstraint() {
    try {
      log.info(
        "Installation ou vérification du trigger de contrainte d'unicité SUPER_ADMIN..."
      );
      String functionSql = """
            CREATE OR REPLACE FUNCTION check_single_super_admin()
            RETURNS TRIGGER AS $$
            DECLARE
                role_name VARCHAR;
                super_admin_count INT;
            BEGIN
                SELECT name INTO role_name FROM roles WHERE id = NEW.role_id;

                IF role_name = 'SUPER_ADMIN' THEN
                    SELECT COUNT(*) INTO super_admin_count
                    FROM user_roles ur
                    JOIN roles r ON ur.role_id = r.id
                    WHERE r.name = 'SUPER_ADMIN';

                    IF super_admin_count > 0 THEN
                        RAISE EXCEPTION 'Violation de règle métier DB : Il ne peut y avoir qu''un seul SUPER_ADMIN dans le système.';
                    END IF;
                END IF;

                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql;
        """;

      String dropTriggerSql =
        "DROP TRIGGER IF EXISTS trg_enforce_single_super_admin ON user_roles;";
      String createTriggerSql = """
            CREATE TRIGGER trg_enforce_single_super_admin
            BEFORE INSERT ON user_roles
            FOR EACH ROW
            EXECUTE FUNCTION check_single_super_admin();
        """;

      jdbcTemplate.execute(functionSql);
      jdbcTemplate.execute(dropTriggerSql);
      jdbcTemplate.execute(createTriggerSql);
      log.info(
        "Trigger de contrainte d'unicité SUPER_ADMIN installé avec succès."
      );
    } catch (Exception e) {
      log.error(
        "Erreur lors de l'installation du trigger de contrainte SUPER_ADMIN. " +
          "Ce log peut être ignoré si la base n'est pas PostgreSQL ou s'il manque des droits DDL.",
        e
      );
    }
  }
}
