// Contrat metier : expose les operations du domaine cache management.

package com.fintrack.incident.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

// Interface du service de gestion et de vidage du cache applicatif.

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheManagementService {

  private final CacheManager cacheManager;

  @Caching(
    evict = {
      @CacheEvict(value = "users", key = "#userId"),
      @CacheEvict(value = "userDetails", key = "#userId"),
    }
  )
  // Invalide les informations utilisateur mises en cache.
  public void evictUserCache(UUID userId) {
    log.debug("Memoire locale invalidee pour l'utilisateur : {}", userId);
  }

  @Caching(
    evict = {
      @CacheEvict(value = "agencies", key = "#agencyId"),
      @CacheEvict(value = "agencyDetails", key = "#agencyId"),
    }
  )
  // Invalide les informations agence mises en cache.
  public void evictAgencyCache(UUID agencyId) {
    log.debug("Memoire locale invalidee pour l'agence : {}", agencyId);
  }

  @Caching(
    evict = {
      @CacheEvict(value = "services", key = "#serviceId"),
      @CacheEvict(value = "serviceDetails", key = "#serviceId"),
    }
  )
  // Invalide les informations service mises en cache.
  public void evictServiceCache(UUID serviceId) {
    log.debug("Memoire locale invalidee pour le service : {}", serviceId);
  }

  // Vide tous les caches locaux lies aux referentiels externes.
  public void evictAllCaches() {
    cacheManager.getCacheNames().forEach(cacheName -> {
      var cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
        log.info("Memoire locale videe : {}", cacheName);
      }
    });
  }

  // Fournit cache statistiques a la couche appelante.

  public String getCacheStats() {
    StringBuilder stats = new StringBuilder("Cache Statistics:\n");

    cacheManager.getCacheNames().forEach(cacheName -> {
      stats.append(String.format("  %s: ", cacheName));
      stats.append("(statistiques disponibles via /actuator/metrics/cache.)\n");
    });

    return stats.toString();
  }

  // Verifie si cached.

  public boolean isCached(String cacheName, Object key) {
    var cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      return cache.get(key) != null;
    }
    return false;
  }
}
