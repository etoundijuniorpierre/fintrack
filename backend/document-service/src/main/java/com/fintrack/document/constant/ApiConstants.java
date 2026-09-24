// Constantes metier : centralise les valeurs stables liees a api constants.

package com.fintrack.document.constant;

// Constantes definissant les chemins de base et les endpoints REST du service document.
public class ApiConstants {

  public static final String API_BASE_PATH = "/api/v1/documentService";

  // Regroupe les endpoints REST exposes par le service document.
  public static final class Endpoints {

    public static final String ATTACHMENTS = API_BASE_PATH + "/attachments";
    public static final String FILES = API_BASE_PATH + "/files";
  }
}
