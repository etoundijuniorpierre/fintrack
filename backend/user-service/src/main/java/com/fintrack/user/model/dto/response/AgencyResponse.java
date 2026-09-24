// DTO : transporte les donnees liees a agency entre les couches.

package com.fintrack.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintrack.user.model.dto.BaseDto;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

// DTO de reponse standard d'une agence.

@Data
@EqualsAndHashCode(callSuper = true)
public class AgencyResponse extends BaseDto {

  private String name;
  private String code;
  private String city;
  private String address;

  @JsonProperty("isActive")
  private boolean isActive;

  private UserSummaryResponse headOfAgency;
  private List<UserSummaryResponse> members;
}
