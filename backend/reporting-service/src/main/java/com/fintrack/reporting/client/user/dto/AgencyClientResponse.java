// DTO : transporte une agence legere pour les regroupements de rapports.
package com.fintrack.reporting.client.user.dto;

import java.util.UUID;
import lombok.Data;

// Represente une agence active exposee par le contrat interne utilisateur.
@Data
public class AgencyClientResponse {

  private UUID id;
  private String name;
  private String code;
  private boolean isActive;
}
