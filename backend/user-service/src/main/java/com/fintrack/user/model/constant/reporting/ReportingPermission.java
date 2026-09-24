// Constantes metier : centralise les valeurs stables liees a reporting permission.

package com.fintrack.user.model.constant.reporting;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Definition des permissions liees a l'acces aux rapports.

@Getter
@RequiredArgsConstructor
public enum ReportingPermission {
  REPORT_VIEW_OWN("REPORT_VIEW_OWN"),
  REPORT_VIEW_AGENCY("REPORT_VIEW_AGENCY"),
  REPORT_VIEW_SERVICE("REPORT_VIEW_SERVICE"),
  REPORT_VIEW_ALL("REPORT_VIEW_ALL"),
  REPORT_EXPORT("REPORT_EXPORT"),
  REPORT_DELETE("REPORT_DELETE"),
  REPORT_GENERATE("REPORT_GENERATE"),
  REPORT_GENERATE_ALL_SCOPES("REPORT_GENERATE_ALL_SCOPES"),
  REPORT_SEND_EMAIL("REPORT_SEND_EMAIL"),
  DASHBOARD_CONFIGURE("DASHBOARD_CONFIGURE");

  private final String name;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(ReportingPermission::getName)
      .toArray(String[]::new);
  }
}
