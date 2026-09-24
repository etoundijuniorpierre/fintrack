// Acces aux donnees : expose les requetes persistantes liees a permission.

package com.fintrack.user.repository;

import com.fintrack.user.model.entity.Permission;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repertoire Spring Data JPA pour la gestion en base des permissions.

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
  // Recherche les permissions par nom.
  Optional<Permission> findByName(String name);
  // Verifie l'existence de name.
  boolean existsByName(String name);
}
