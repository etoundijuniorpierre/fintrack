// DTO : transporte les donnees liees a incident client entre les couches.

package com.fintrack.document.client.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

// DTO de reception des donnees d'un incident renvoyees par incident-service.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncidentClientResponse {

  private UUID id;
  private String title;
  private String status;
  private UUID createdById;
  private UUID assignedToId;

  // Applique le changement demande apres validation metier.

  @JsonProperty("createdBy")
  @SuppressWarnings("unchecked")
  public void setCreatedByFromJson(Object createdBy) {
    if (createdBy == null) {
      this.createdById = null;
      return;
    }
    if (createdBy instanceof Map<?, ?> map) {
      Object id = ((Map<String, Object>) map).get("id");
      if (id != null) {
        this.createdById = UUID.fromString(id.toString());
      }
    }
  }

  // Extrait l'identifiant de l'assigne courant depuis l'objet imbrique.
  @JsonProperty("assignedTo")
  @SuppressWarnings("unchecked")
  public void setAssignedToFromJson(Object assignedTo) {
    if (assignedTo == null) {
      this.assignedToId = null;
      return;
    }
    if (assignedTo instanceof Map<?, ?> map) {
      Object id = ((Map<String, Object>) map).get("id");
      if (id != null) {
        this.assignedToId = UUID.fromString(id.toString());
      }
    }
  }
}
