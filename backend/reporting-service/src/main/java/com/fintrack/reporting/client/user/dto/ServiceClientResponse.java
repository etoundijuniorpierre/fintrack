// DTO : transporte un service leger pour les regroupements de rapports.
package com.fintrack.reporting.client.user.dto;

import java.util.UUID;
import lombok.Data;

// Represente un service actif expose par le contrat interne utilisateur.
@Data
public class ServiceClientResponse {

  private UUID id;
  private String name;
  private boolean isActive;
}
