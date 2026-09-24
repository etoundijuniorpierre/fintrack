// Constantes metier : centralise les valeurs stables liees a api constants.

package com.fintrack.reporting.constant;

// Constantes des chemins d'API exposes par le reporting-service
public class ApiConstants {

  public static final String API_BASE_PATH = "/api/v1/reportingService";

  // Chemins complets des differents groupes d'endpoints
  public static final class Endpoints {

    public static final String REPORT_SCHEDULES =
      API_BASE_PATH + "/report-schedules";
    public static final String SUPER_ADMIN = API_BASE_PATH + "/super-admin";
  }
}
