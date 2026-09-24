// DTO : transporte les donnees liees a service entre les couches.

package com.fintrack.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintrack.user.model.dto.BaseDto;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

// DTO de reponse decrivant un service interne.

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceResponse extends BaseDto {

  private String name;
  private String description;

  @JsonProperty("isActive")
  private boolean isActive;

  private UserSummaryResponse headOfService;
  private List<UserSummaryResponse> members;
}
