// Constantes metier : centralise les valeurs stables liees a api constants.

package com.fintrack.notification.constant;

// Constantes centralisant les chemins de base et les endpoints de l'API
public class ApiConstants {

  public static final String API_BASE_PATH = "/api/v1/notificationService";

  // Regroupe les endpoints REST exposes par le service notification.
  public static final class Endpoints {

    public static final String NOTIFICATIONS = API_BASE_PATH + "/notifications";
  }
}
