// Contrat metier : expose les operations du domaine utilisateur.

package com.fintrack.user.service;

import com.fintrack.user.model.entity.User;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Interface du service de gestion des comptes et profils utilisateurs.

public interface UserService {
  // Liste les utilisateurs selon le perimetre demande.
  Page<User> findAll(String keyword, Pageable pageable);
  // Liste les utilisateurs avec filtres serveur de table.
  Page<User> findFiltered(
    String keyword,
    String roleName,
    UUID agencyId,
    UUID serviceId,
    Boolean active,
    Boolean connected,
    Pageable pageable
  );
  // Liste les comptes verrouilles (tentatives de connexion echouees >= seuil systeme).
  Page<User> findLocked(Pageable pageable);
  // Qualite : utilisateurs sans aucun role.
  Page<User> findWithoutRole(Pageable pageable);
  // Qualite : utilisateurs sans perimetre (ni agence ni service), hors profils globaux.
  Page<User> findWithoutScope(Pageable pageable);
  // IDs des utilisateurs inactifs (appel interne incident-service).
  List<UUID> findInactiveIds();
  // Liste les utilisateurs selon le perimetre demande.
  // Liste les utilisateurs selon le perimetre demande.
  List<User> findAll();

  List<User> findActiveReportSubjects();
  // Recherche de multiples utilisateurs par leurs identifiants.
  List<User> findAllByIds(Set<UUID> ids);
  // Liste les utilisateurs selon le perimetre demande.
  Page<User> findAllExcludingRole(
    String roleName,
    String keyword,
    Pageable pageable
  );
  // Liste les utilisateurs filtres en excluant le role protege donne.
  Page<User> findFilteredExcludingRole(
    String excludedRoleName,
    String keyword,
    String roleName,
    UUID agencyId,
    UUID serviceId,
    Boolean active,
    Boolean connected,
    Pageable pageable
  );
  List<User> findAllExcludingRole(String roleName);
  List<User> findAssignableUsers(UUID serviceId);
  List<User> findDirectionValidators();
  // Recherche les utilisateurs par agence identifiant avec pagination.
  Page<User> findByAgencyId(UUID agencyId, String keyword, Pageable pageable);
  // Recherche les utilisateurs filtres dans une agence.
  Page<User> findFilteredByAgencyId(
    UUID scopeAgencyId,
    String keyword,
    String roleName,
    UUID agencyId,
    UUID serviceId,
    Boolean active,
    Boolean connected,
    Pageable pageable
  );
  // Recherche les utilisateurs par agence identifiant.
  List<User> findByAgencyId(UUID agencyId);
  // Recherche les utilisateurs par service identifiant avec pagination.
  Page<User> findByServiceId(UUID serviceId, String keyword, Pageable pageable);
  // Recherche les utilisateurs filtres dans un service.
  Page<User> findFilteredByServiceId(
    UUID scopeServiceId,
    String keyword,
    String roleName,
    UUID agencyId,
    UUID serviceId,
    Boolean active,
    Boolean connected,
    Pageable pageable
  );
  // Recherche les utilisateurs par service identifiant.
  List<User> findByServiceId(UUID serviceId);
  // Recherche les utilisateurs pour administrateurs.
  List<User> findAdmins();
  // Recherche les utilisateurs par identifiant.
  User findById(UUID id);
  // Recherche les utilisateurs par nom utilisateur.
  User findByUsername(String username);
  // Prepare l'enregistrement de la ressource selon les regles metier.
  User create(User user);
  // Applique le changement demande apres validation metier.
  User update(UUID id, User user);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  void delete(UUID id);
  // Valide l'identite de l'utilisateur avant ouverture de session.
  User authenticate(String username, String password);
  // Applique le changement demande apres validation metier.
  User assignToAgency(UUID userId, UUID agencyId);
  // Applique le changement demande apres validation metier.
  User assignPermissions(UUID userId, List<UUID> permissionIds);
  // Cree un element du domaine utilisateur apres validation metier.
  User addPermission(UUID userId, UUID permissionId);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  User removePermission(UUID userId, UUID permissionId);
  // Applique le changement demande apres validation metier.
  User toggleStatus(UUID userId);
  // Applique le changement demande apres validation metier.
  User changePassword(UUID userId, String currentPassword, String newPassword);
  // Applique le changement demande apres validation metier.
  User updateProfile(UUID id, User profile);
  // Incremente les tentatives de connexion echouees.
  int incrementFailedAttempts(UUID userId);
  // Reinitialise les tentatives de connexion echouees.
  void resetFailedAttempts(UUID userId);
  // Realise l'intention metier regenerate password.

  User regeneratePassword(UUID userId);
  // Realise l'intention metier contact admin.
  void contactAdmin(String username, String subject, String message);
  // Retire les utilisateurs cibles apres contrele metier.
  int cleanupExpiredTemporaryPasswords();
}

