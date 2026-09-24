// Acces aux donnees : expose les requetes persistantes liees a system setting.

package com.fintrack.reporting.repository;

import com.fintrack.reporting.model.entity.SystemSetting;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Definit le contrat system setting attendu par les autres couches.

@Repository
public interface SystemSettingRepository
  extends JpaRepository<SystemSetting, UUID>
{
  // Recherche les rapports par setting key.

  Optional<SystemSetting> findBySettingKey(String settingKey);
  // Recherche les rapports par category.

  List<SystemSetting> findByCategory(String category);
}
