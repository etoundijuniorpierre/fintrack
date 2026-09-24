// Constantes metier : centralise les valeurs stables liees a api constants.

package com.fintrack.user.constant;

// Constantes des chemins d'API exposes par le user-service (prefixe de base et endpoints).
public class ApiConstants {

  public static final String API_BASE_PATH = "/api/v1/userService";

  // Chemins complets de chaque groupe d'endpoints.
  public static final class Endpoints {

    public static final String AUTH = API_BASE_PATH + "/auth";
    public static final String USERS = API_BASE_PATH + "/users";
    public static final String AGENCIES = API_BASE_PATH + "/agencies";
    public static final String DEPARTMENTS = API_BASE_PATH + "/departments";
    public static final String ROLES = API_BASE_PATH + "/roles";
    public static final String PERMISSIONS = API_BASE_PATH + "/permissions";
  }
}
