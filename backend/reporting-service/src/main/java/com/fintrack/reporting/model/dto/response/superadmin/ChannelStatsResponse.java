// DTO : transporte les donnees liees a channel stats entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de canal statistiques.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelStatsResponse {

  private long total;
  private long sent;
  private long failed;
  private long pending;
  private Map<String, Long> byStatus;
}
