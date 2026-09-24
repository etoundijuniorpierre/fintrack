// Constantes metier : centralise les valeurs stables liees a api constants.

package com.fintrack.audit.constant;

// Constantes des chemins d'URL exposes par le service d'audit.
public class ApiConstants {

  public static final String API_BASE_PATH = "/api/v1/auditService";

  // Regroupe les chemins des differents endpoints.
  public static final class Endpoints {

    public static final String AUDIT_LOGS = API_BASE_PATH + "/audit-logs";
  }
}
