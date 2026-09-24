// DTO : transporte les donnees liees a type distribution item entre les couches.

package com.fintrack.incident.model.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO representant la distribution des incidents par type.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeDistributionItem {

  private UUID typeId;
  private String typeName;
  private long count;
}
