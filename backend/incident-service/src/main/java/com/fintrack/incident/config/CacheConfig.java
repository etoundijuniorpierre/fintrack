// Configuration Spring : declare les regles techniques liees a cache.

package com.fintrack.incident.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du cache Caffeine pour les données distantes (utilisateurs,
 * agences, services, configs de type).
 */
@Configuration
@EnableCaching
// Regroupe la configuration Spring necessaire au module.
public class CacheConfig {

  // Declare le gestionnaire de cache et la liste des caches nommes utilises par l'application
  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(caffeineCacheBuilder());
    cacheManager.setCacheNames(
      List.of(
        "users",
        "userDetails",
        "agencies",
        "agencyDetails",
        "services",
        "serviceDetails",
        "serviceHeads",
        "agencyHeads",
        "admins",
        "incidentTypeConfigs",
        "dashboardMetrics",
        "dashboardComparisons"
      )
    );
    return cacheManager;
  }

  // Parametres communs des caches : taille, expiration et statistiques
  private Caffeine<Object, Object> caffeineCacheBuilder() {
    return Caffeine.newBuilder()
      .initialCapacity(100)
      .maximumSize(500)
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .expireAfterAccess(5, TimeUnit.MINUTES)
      .recordStats()
      .removalListener((key, value, cause) ->
        LoggerFactory.getLogger(CacheConfig.class).debug(
          "Cache entry removed: key={}, cause={}",
          key,
          cause
        )
      );
  }
}
