// Acces aux donnees : expose les requetes persistantes liees a user.

package com.fintrack.user.repository;

import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.entity.UserAuthInfo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
// Repertoire Spring Data JPA pour la gestion des comptes utilisateurs.

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// Definit le contrat user attendu par les autres couches.

@Repository
public interface UserRepository
  extends
    JpaRepository<User, UUID>,
    JpaSpecificationExecutor<User>,
    UserStatsAggregationRepository
{
  @EntityGraph(attributePaths = { "agency", "service", "managedAgency" })
  @Query(
    "SELECT u FROM User u WHERE " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))"
  )
  // Liste les utilisateurs selon le perimetre demande.
  Page<User> findAll(@Param("keyword") String keyword, Pageable pageable);

  @EntityGraph(
    attributePaths = {
      "agency",
      "service",
      "service.headOfService",
      "managedAgency",
    }
  )
  @Query(
    value = "SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
      "(:roleName IS NULL OR r.name = :roleName) AND " +
      "(:agencyId IS NULL OR u.agency.id = :agencyId) AND " +
      "(:serviceId IS NULL OR u.service.id = :serviceId) AND " +
      "(:active IS NULL OR u.isActive = :active) AND " +
      "(:connected IS NULL OR (:connected = true AND u.username IN :onlineUsernames) OR " +
      "(:connected = false AND u.username NOT IN :onlineUsernames))",
    countQuery = "SELECT COUNT(DISTINCT u) FROM User u LEFT JOIN u.roles r WHERE " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
      "(:roleName IS NULL OR r.name = :roleName) AND " +
      "(:agencyId IS NULL OR u.agency.id = :agencyId) AND " +
      "(:serviceId IS NULL OR u.service.id = :serviceId) AND " +
      "(:active IS NULL OR u.isActive = :active) AND " +
      "(:connected IS NULL OR (:connected = true AND u.username IN :onlineUsernames) OR " +
      "(:connected = false AND u.username NOT IN :onlineUsernames))"
  )
  // Liste les utilisateurs avec les filtres serveur de la table d'administration.
  Page<User> findFiltered(
    @Param("keyword") String keyword,
    @Param("roleName") String roleName,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("active") Boolean active,
    @Param("connected") Boolean connected,
    @Param("onlineUsernames") Set<String> onlineUsernames,
    Pageable pageable
  );

  // Comptes verrouilles : tentatives de connexion echouees >= au seuil systeme.
  Page<User> findByFailedLoginAttemptsGreaterThanEqual(
    int threshold,
    Pageable pageable
  );

  // Qualite des donnees : utilisateurs sans aucun role (lien Super Admin).
  Page<User> findByRolesIsEmpty(Pageable pageable);

  // Qualite des donnees : utilisateurs sans perimetre (ni agence ni service),
  // hors profils globaux (SUPER_ADMIN / ADMIN) qui n'en ont pas besoin.
  @Query(
    "SELECT u FROM User u WHERE u.agency IS NULL AND u.service IS NULL " +
      "AND NOT EXISTS (SELECT r FROM u.roles r WHERE r.name IN ('SUPER_ADMIN', 'ADMIN'))"
  )
  Page<User> findWithoutScope(Pageable pageable);

  // IDs des utilisateurs inactifs (consomme par incident-service).
  @Query("SELECT u.id FROM User u WHERE u.isActive = false")
  List<UUID> findInactiveIds();

  // Utilisateurs assignables pour le traitement/resolution d'incidents.
  @EntityGraph(attributePaths = { "service" })
  @Query(
    "SELECT DISTINCT u FROM User u " +
      "LEFT JOIN u.roles r " +
      "LEFT JOIN r.permissions rp " +
      "LEFT JOIN u.permissions up " +
      "LEFT JOIN u.managedServices ms " +
      "WHERE u.isActive = true AND " +
      "(rp.name IN ('INCIDENT_TREAT', 'INCIDENT_RESOLVE') OR up.name IN ('INCIDENT_TREAT', 'INCIDENT_RESOLVE')) AND " +
      "(:serviceId IS NULL OR u.service.id = :serviceId OR ms.id = :serviceId)"
  )
  List<User> findAssignableUsers(
    @Param("serviceId") UUID serviceId
  );

  // Utilisateurs possedant la permission VALIDATION_DIRECTION (valideurs direction).
  @EntityGraph(attributePaths = { "roles", "permissions", "agency", "service" })
  @Query(
    "SELECT DISTINCT u FROM User u " +
      "LEFT JOIN u.roles r " +
      "LEFT JOIN r.permissions rp " +
      "LEFT JOIN u.permissions up " +
      "WHERE u.isActive = true AND " +
      "(rp.name = 'VALIDATION_DIRECTION' OR up.name = 'VALIDATION_DIRECTION')"
  )
  List<User> findDirectionValidators();

  @Override
  // service.headOfService est charge ici pour permettre a l'ecran Mon Profil
  // de signaler qu'un utilisateur est le chef de son propre service
  @EntityGraph(
    attributePaths = {
      "roles",
      "roles.permissions",
      "permissions",
      "agency",
      "service",
      "service.headOfService",
      "managedAgency",
    }
  )
  Optional<User> findById(UUID id);

  // Recherche les utilisateurs par nom utilisateur.

  Optional<User> findByUsername(String username);

  Optional<User> findByUsernameIgnoreCase(String username);
  // Recherche les utilisateurs par e-mail.
  Optional<User> findByEmail(String email);
  // Verifie l'existence de username.
  boolean existsByUsername(String username);
  boolean existsByUsernameIgnoreCase(String username);
  // Verifie l'existence de email.
  boolean existsByEmail(String email);

  // Liste les utilisateurs selon le perimetre demande.

  @EntityGraph(
    attributePaths = {
      "roles",
      "roles.permissions",
      "permissions",
      "agency",
      "service",
      "managedAgency",
    }
  )
  @Query("SELECT u FROM User u")
  List<User> findAllWithRolesAndPermissions();

  @EntityGraph(attributePaths = { "agency", "service" })
  @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.username")
  List<User> findActiveReportSubjects();

  @Query(
    "SELECT DISTINCT u FROM User u " +
      "LEFT JOIN FETCH u.roles r " +
      "LEFT JOIN FETCH r.permissions " +
      "LEFT JOIN FETCH u.permissions " +
      "LEFT JOIN FETCH u.managedServices " +
      "WHERE u.id IN :ids"
  )
  // Hydrate en une requete les collections necessaires au mapping des utilisateurs pagines.
  List<User> hydratePageAssociations(@Param("ids") List<UUID> ids);

  @EntityGraph(attributePaths = { "agency", "service", "managedAgency" })
  @Query(
    "SELECT u FROM User u WHERE u.id NOT IN " +
      "(SELECT u2.id FROM User u2 JOIN u2.roles r WHERE r.name = :roleName) AND " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))"
  )
  // Liste les utilisateurs selon le perimetre demande.
  Page<User> findAllExcludingRole(
    @Param("roleName") String roleName,
    @Param("keyword") String keyword,
    Pageable pageable
  );

  @EntityGraph(
    attributePaths = {
      "agency",
      "service",
      "service.headOfService",
      "managedAgency",
    }
  )
  @Query(
    value = "SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE u.id NOT IN " +
      "(SELECT u2.id FROM User u2 JOIN u2.roles er WHERE er.name = :excludedRoleName) AND " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
      "(:roleName IS NULL OR r.name = :roleName) AND " +
      "(:agencyId IS NULL OR u.agency.id = :agencyId) AND " +
      "(:serviceId IS NULL OR u.service.id = :serviceId) AND " +
      "(:active IS NULL OR u.isActive = :active) AND " +
      "(:connected IS NULL OR (:connected = true AND u.username IN :onlineUsernames) OR " +
      "(:connected = false AND u.username NOT IN :onlineUsernames))",
    countQuery = "SELECT COUNT(DISTINCT u) FROM User u LEFT JOIN u.roles r WHERE u.id NOT IN " +
      "(SELECT u2.id FROM User u2 JOIN u2.roles er WHERE er.name = :excludedRoleName) AND " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
      "(:roleName IS NULL OR r.name = :roleName) AND " +
      "(:agencyId IS NULL OR u.agency.id = :agencyId) AND " +
      "(:serviceId IS NULL OR u.service.id = :serviceId) AND " +
      "(:active IS NULL OR u.isActive = :active) AND " +
      "(:connected IS NULL OR (:connected = true AND u.username IN :onlineUsernames) OR " +
      "(:connected = false AND u.username NOT IN :onlineUsernames))"
  )
  // Liste les utilisateurs filtres en excluant les comptes proteges du role donne.
  Page<User> findFilteredExcludingRole(
    @Param("excludedRoleName") String excludedRoleName,
    @Param("keyword") String keyword,
    @Param("roleName") String roleName,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("active") Boolean active,
    @Param("connected") Boolean connected,
    @Param("onlineUsernames") Set<String> onlineUsernames,
    Pageable pageable
  );

  @EntityGraph(
    attributePaths = {
      "roles",
      "roles.permissions",
      "permissions",
      "agency",
      "service",
      "managedAgency",
    }
  )
  @Query(
    "SELECT u FROM User u WHERE u.id NOT IN " +
      "(SELECT u2.id FROM User u2 JOIN u2.roles r WHERE r.name = :roleName)"
  )
  // Liste les utilisateurs selon le perimetre demande.
  List<User> findAllExcludingRole(@Param("roleName") String roleName);

  // Recherche les utilisateurs par rôles.
  @EntityGraph(
    attributePaths = { "roles", "agency", "service", "managedAgency" }
  )
  @Query(
    "SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN (:roles)"
  )
  List<User> findByRolesIn(@Param("roles") List<String> roles);

  @EntityGraph(attributePaths = { "agency", "service", "managedAgency" })
  @Query(
    "SELECT u FROM User u WHERE u.agency.id = :agencyId AND " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))"
  )
  // Recherche les utilisateurs par identifiant d'agence avec pagination.
  Page<User> findByAgencyId(
    @Param("agencyId") UUID agencyId,
    @Param("keyword") String keyword,
    Pageable pageable
  );

  @EntityGraph(
    attributePaths = {
      "agency",
      "service",
      "service.headOfService",
      "managedAgency",
    }
  )
  @Query(
    value = "SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE u.agency.id = :scopeAgencyId AND " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
      "(:roleName IS NULL OR r.name = :roleName) AND " +
      "(:agencyId IS NULL OR u.agency.id = :agencyId) AND " +
      "(:serviceId IS NULL OR u.service.id = :serviceId) AND " +
      "(:active IS NULL OR u.isActive = :active) AND " +
      "(:connected IS NULL OR (:connected = true AND u.username IN :onlineUsernames) OR " +
      "(:connected = false AND u.username NOT IN :onlineUsernames))",
    countQuery = "SELECT COUNT(DISTINCT u) FROM User u LEFT JOIN u.roles r WHERE u.agency.id = :scopeAgencyId AND " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
      "(:roleName IS NULL OR r.name = :roleName) AND " +
      "(:agencyId IS NULL OR u.agency.id = :agencyId) AND " +
      "(:serviceId IS NULL OR u.service.id = :serviceId) AND " +
      "(:active IS NULL OR u.isActive = :active) AND " +
      "(:connected IS NULL OR (:connected = true AND u.username IN :onlineUsernames) OR " +
      "(:connected = false AND u.username NOT IN :onlineUsernames))"
  )
  // Liste les utilisateurs filtres dans le perimetre agence courant.
  Page<User> findFilteredByAgencyId(
    @Param("scopeAgencyId") UUID scopeAgencyId,
    @Param("keyword") String keyword,
    @Param("roleName") String roleName,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("active") Boolean active,
    @Param("connected") Boolean connected,
    @Param("onlineUsernames") Set<String> onlineUsernames,
    Pageable pageable
  );

  // Recherche les utilisateurs par identifiant d'agence.

  @EntityGraph(
    attributePaths = {
      "roles",
      "roles.permissions",
      "permissions",
      "agency",
      "service",
      "managedAgency",
    }
  )
  List<User> findByAgencyId(UUID agencyId);

  @EntityGraph(attributePaths = { "agency", "service", "managedAgency" })
  @Query(
    "SELECT u FROM User u WHERE u.service.id = :serviceId AND " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))"
  )
  // Recherche les utilisateurs par identifiant de service avec pagination.
  Page<User> findByServiceId(
    @Param("serviceId") UUID serviceId,
    @Param("keyword") String keyword,
    Pageable pageable
  );

  @EntityGraph(
    attributePaths = {
      "agency",
      "service",
      "service.headOfService",
      "managedAgency",
    }
  )
  @Query(
    value = "SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE u.service.id = :scopeServiceId AND " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
      "(:roleName IS NULL OR r.name = :roleName) AND " +
      "(:agencyId IS NULL OR u.agency.id = :agencyId) AND " +
      "(:serviceId IS NULL OR u.service.id = :serviceId) AND " +
      "(:active IS NULL OR u.isActive = :active) AND " +
      "(:connected IS NULL OR (:connected = true AND u.username IN :onlineUsernames) OR " +
      "(:connected = false AND u.username NOT IN :onlineUsernames))",
    countQuery = "SELECT COUNT(DISTINCT u) FROM User u LEFT JOIN u.roles r WHERE u.service.id = :scopeServiceId AND " +
      "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
      "(:roleName IS NULL OR r.name = :roleName) AND " +
      "(:agencyId IS NULL OR u.agency.id = :agencyId) AND " +
      "(:serviceId IS NULL OR u.service.id = :serviceId) AND " +
      "(:active IS NULL OR u.isActive = :active) AND " +
      "(:connected IS NULL OR (:connected = true AND u.username IN :onlineUsernames) OR " +
      "(:connected = false AND u.username NOT IN :onlineUsernames))"
  )
  // Liste les utilisateurs filtres dans le perimetre service courant.
  Page<User> findFilteredByServiceId(
    @Param("scopeServiceId") UUID scopeServiceId,
    @Param("keyword") String keyword,
    @Param("roleName") String roleName,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("active") Boolean active,
    @Param("connected") Boolean connected,
    @Param("onlineUsernames") Set<String> onlineUsernames,
    Pageable pageable
  );

  // Recherche les utilisateurs par identifiant de service.

  @EntityGraph(
    attributePaths = {
      "roles",
      "roles.permissions",
      "permissions",
      "agency",
      "service",
      "managedAgency",
    }
  )
  List<User> findByServiceId(UUID serviceId);

  @Query(
    "SELECT u.id AS id, u.username AS username, u.password AS password, " +
      "u.tempPassword AS tempPassword, u.isActive AS active, u.isFirstLogin AS firstLogin, " +
      "u.failedLoginAttempts AS failedLoginAttempts, " +
      "u.tempPasswordCreatedAt AS tempPasswordCreatedAt, u.updatedAt AS updatedAt " +
      "FROM User u WHERE LOWER(u.username) = LOWER(:username)"
  )
  // Recherche les informations d'authentification par nom d'utilisateur.
  Optional<UserAuthInfo> findAuthInfoByUsername(
    @Param("username") String username
  );

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    value = "UPDATE users SET failed_login_attempts = failed_login_attempts + 1, " +
      "is_active = CASE WHEN failed_login_attempts + 1 >= :maxAttempts THEN false ELSE is_active END " +
      "WHERE id = :userId",
    nativeQuery = true
  )
  int incrementFailedLoginAttempts(
    @Param("userId") UUID userId,
    @Param("maxAttempts") int maxAttempts
  );

  @Modifying
  @Query(
    "UPDATE User u SET u.tempPassword = NULL, u.tempPasswordCreatedAt = NULL, " +
      "u.isActive = false, u.isFirstLogin = true " +
      "WHERE u.tempPassword IS NOT NULL AND u.isActive = false AND u.isFirstLogin = true " +
      "AND COALESCE(u.tempPasswordCreatedAt, u.updatedAt) < :cutoffTime"
  )
  // Realise l'intention metier reset expired temporary passwords.
  int resetExpiredTemporaryPasswords(LocalDateTime cutoffTime);

  // Realise l'intention metier param.

  @Modifying
  @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.id = :userId")
  void resetFailedLoginAttempts(@Param("userId") UUID userId);

  // Compte les elements du domaine utilisateur selon is active.

  long countByIsActive(boolean isActive);
  // Compte les elements du domaine utilisateur selon last login after.

  long countByLastLoginAfter(LocalDateTime threshold);
  // Compte les elements du domaine utilisateur selon last login is null.

  long countByLastLoginIsNull();
  // Compte les utilisateurs selon un nombre minimal de tentatives de connexion echouees.

  long countByFailedLoginAttemptsGreaterThanEqual(int threshold);
  // Compte les elements du domaine utilisateur selon is first login.

  long countByIsFirstLogin(boolean isFirstLogin);
  // Compte les utilisateurs crees dans une periode donnee.

  long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
  // Compte les utilisateurs crees apres une date donnee.

  long countByCreatedAtAfter(LocalDateTime from);
}
