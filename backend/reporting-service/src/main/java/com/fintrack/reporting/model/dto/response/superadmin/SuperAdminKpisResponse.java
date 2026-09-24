// DTO : transporte les donnees liees a super admin kpis entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de super-administration kpis.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminKpisResponse {

  private long criticalAnomalies;
  private long failedEmails;
  private long lockedAccounts;
  private long unassignedIncidents;
}
