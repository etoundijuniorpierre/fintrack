// DTO : transporte les donnees liees a repeated action entre les couches.

package com.fintrack.audit.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de repeated action.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepeatedAction {

  private String key;
  private String username;
  private String action;
  private long count;
}
