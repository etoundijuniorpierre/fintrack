// Configuration Spring : declare les regles techniques liees a pagination.

package com.fintrack.incident.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SortHandlerMethodArgumentResolverCustomizer;

@Configuration
// Serialisation DIRECT des Page : conserve la forme JSON historique (champs a plat)
// + supprime l'avertissement PageImpl. NE PAS passer en VIA_DTO sans adapter le frontend.
@EnableSpringDataWebSupport(
  pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.DIRECT
)
// Configuration du tri par defaut des listes paginees.
public class PaginationConfig {

  /**
   * Tri appliqué aux listes paginées quand la requête ne fournit pas son propre
   * paramètre {@code sort} : du plus récent au plus ancien.
   */
  @Bean
  // Definit le tri par defaut des requetes paginees.
  public SortHandlerMethodArgumentResolverCustomizer defaultSortCustomizer() {
    return resolver ->
      resolver.setFallbackSort(Sort.by(Sort.Direction.DESC, "createdAt"));
  }
}
