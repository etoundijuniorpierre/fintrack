// Constantes metier : centralise les valeurs stables liees a api constants.

package com.fintrack.incident.constant;

// Constantes centralisant les chemins de base et les URL des endpoints REST du service
public class ApiConstants {

  public static final String API_BASE_PATH = "/api/v1/incidentService";

  // Regroupe les endpoints REST exposes par le service incident.
  public static final class Endpoints {

    public static final String INCIDENTS = API_BASE_PATH + "/incidents";
    public static final String INCIDENT_TYPE_CONFIGS =
      API_BASE_PATH + "/incident-type-configs";
    public static final String INCIDENT_HISTORY = API_BASE_PATH + "/incidents";
    public static final String INCIDENT_COMMENTS = API_BASE_PATH + "/incidents";
    public static final String DASHBOARD = API_BASE_PATH + "/dashboard";
  }
}
