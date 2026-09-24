// Controleur REST : expose les operations HTTP liees a cache management.

package com.fintrack.incident.controller;

import com.fintrack.incident.service.CacheManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Controleur REST d'administration pour invalider et inspecter les caches applicatifs
@RestController
@RequestMapping("/api/v1/admin/cache")
@Tag(
  name = "Cache Management",
  description = "Endpoints d'administration pour la gestion du cache"
)
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class CacheManagementController {

  private final CacheManagementService cacheManagementService;

  // Supprime ou invalide les donnees ciblees apres controle metier.

  @PostMapping("/users/{userId}/evict")
  @Operation(summary = "Invalide le cache d'un utilisateur spécifique")
  public ResponseEntity<Void> evictUserCache(@PathVariable UUID userId) {
    cacheManagementService.evictUserCache(userId);
    return ResponseEntity.ok().build();
  }

  // Supprime ou invalide les donnees ciblees apres controle metier.

  @PostMapping("/agencies/{agencyId}/evict")
  @Operation(summary = "Invalide le cache d'une agence spécifique")
  public ResponseEntity<Void> evictAgencyCache(@PathVariable UUID agencyId) {
    cacheManagementService.evictAgencyCache(agencyId);
    return ResponseEntity.ok().build();
  }

  // Supprime ou invalide les donnees ciblees apres controle metier.

  @PostMapping("/services/{serviceId}/evict")
  @Operation(summary = "Invalide le cache d'un service spécifique")
  public ResponseEntity<Void> evictServiceCache(@PathVariable UUID serviceId) {
    cacheManagementService.evictServiceCache(serviceId);
    return ResponseEntity.ok().build();
  }

  // Supprime ou invalide les donnees ciblees apres controle metier.

  @PostMapping("/evict-all")
  @Operation(summary = "Invalide tous les caches")
  public ResponseEntity<Void> evictAllCaches() {
    cacheManagementService.evictAllCaches();
    return ResponseEntity.ok().build();
  }

  // Fournit cache statistiques a la couche appelante.

  @GetMapping("/stats")
  @Operation(summary = "Récupère les statistiques du cache")
  public ResponseEntity<String> getCacheStats() {
    return ResponseEntity.ok(cacheManagementService.getCacheStats());
  }

  @GetMapping("/check/{cacheName}/{key}")
  @Operation(summary = "Vérifie si une entrée existe dans le cache")
  // Verifie si cached.
  public ResponseEntity<Boolean> isCached(
    @PathVariable String cacheName,
    @PathVariable String key
  ) {
    boolean isCached = cacheManagementService.isCached(cacheName, key);
    return ResponseEntity.ok(isCached);
  }
}
