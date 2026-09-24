// Acces aux donnees : expose les requetes persistantes liees a role.

package com.fintrack.user.repository;

import com.fintrack.user.model.entity.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repertoire Spring Data JPA pour les profils et roles.

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
  // Recherche les roles par nom.
  Optional<Role> findByName(String name);
  // Verifie l'existence de name.
  boolean existsByName(String name);
}
