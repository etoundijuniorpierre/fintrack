// Configuration Spring : declare les regles techniques liees a pagination.

package com.fintrack.document.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
// Serialisation DIRECT des Page : conserve la forme JSON historique (content +
// totalElements/totalPages/number/size/first/last a plat) et supprime l'avertissement
// "Serializing PageImpl as-is is not supported". NE PAS passer en VIA_DTO sans
// adapter le frontend (il lit ces champs a plat).
@EnableSpringDataWebSupport(
  pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.DIRECT
)
public class PaginationConfig {}
