// Configuration Spring : declare les regles techniques liees a cache.

package com.fintrack.reporting.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Caches Super Admin avec TTL ajuste par section.
// TTL court (10s) pour la sante des services (donnee critique, change vite).
// TTL long (5min) pour la configuration systeme (donnee stable, change rarement).
@Configuration
@EnableCaching
public class CacheConfig {

  // Taille max par cache Caffeine (reglage infra, surcharge par environnement).
  // Les TTL restent volontairement differencies par section (sante 10s, config 5min...).
  @Value("${fintrack.cache.maximum-size:256}")
  private long cacheMaximumSize;

  public static final String SUPER_ADMIN_OVERVIEW_CACHE = "superAdminOverview";
  public static final String SUPER_ADMIN_GOVERNANCE_CACHE =
    "superAdminGovernance";
  public static final String SUPER_ADMIN_HEALTH_CACHE = "superAdminHealth";
  public static final String SUPER_ADMIN_CONTROLS_CACHE = "superAdminControls";
  public static final String SUPER_ADMIN_OPERATIONS_CACHE =
    "superAdminOperations";
  public static final String SUPER_ADMIN_AUDIT_CACHE = "superAdminAudit";
  public static final String SUPER_ADMIN_CONFIG_CACHE = "superAdminConfig";
  public static final String SUPER_ADMIN_REPORTING_CACHE =
    "superAdminReporting";
  public static final String SYSTEM_CONFIG_CACHE = "systemConfig";

  public static final String[] SUPER_ADMIN_TAB_CACHES = {
    SUPER_ADMIN_OVERVIEW_CACHE,
    SUPER_ADMIN_GOVERNANCE_CACHE,
    SUPER_ADMIN_HEALTH_CACHE,
    SUPER_ADMIN_CONTROLS_CACHE,
    SUPER_ADMIN_OPERATIONS_CACHE,
    SUPER_ADMIN_AUDIT_CACHE,
    SUPER_ADMIN_CONFIG_CACHE,
    SUPER_ADMIN_REPORTING_CACHE,
  };

  // Configure les caches applicatifs du service.

  @Bean
  public CacheManager cacheManager() {
    SimpleCacheManager manager = new SimpleCacheManager();
    manager.setCaches(
      List.of(
        caffeine(SUPER_ADMIN_OVERVIEW_CACHE, Duration.ofSeconds(30)),
        caffeine(SUPER_ADMIN_GOVERNANCE_CACHE, Duration.ofSeconds(30)),
        caffeine(SUPER_ADMIN_HEALTH_CACHE, Duration.ofSeconds(10)),
        caffeine(SUPER_ADMIN_CONTROLS_CACHE, Duration.ofSeconds(60)),
        caffeine(SUPER_ADMIN_OPERATIONS_CACHE, Duration.ofSeconds(30)),
        caffeine(SUPER_ADMIN_AUDIT_CACHE, Duration.ofSeconds(30)),
        caffeine(SUPER_ADMIN_CONFIG_CACHE, Duration.ofMinutes(5)),
        caffeine(SUPER_ADMIN_REPORTING_CACHE, Duration.ofSeconds(60)),
        caffeine(SYSTEM_CONFIG_CACHE, Duration.ofMinutes(5)),
        caffeine("users", Duration.ofMinutes(15))
      )
    );
    return manager;
  }

  // Definit la politique d'expiration des caches locaux.

  private CaffeineCache caffeine(String name, Duration ttl) {
    return new CaffeineCache(
      name,
      Caffeine.newBuilder()
        .expireAfterWrite(ttl)
        .maximumSize(cacheMaximumSize)
        .recordStats()
        .build()
    );
  }
}
