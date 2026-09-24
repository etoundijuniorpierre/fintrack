// Mapper : convertit les donnees liees a super admin maps entre modeles.

package com.fintrack.reporting.model.mapper.superadmin;

import com.fintrack.reporting.model.dto.response.superadmin.DataQualityIssueResponse;
import java.util.List;

// Assure les conversions du domaine super admin maps.

public final class SuperAdminMaps {

  // Initialise le mapper avec ses dependances de construction.

  private SuperAdminMaps() {}

  // Realise l'intention metier chaine.

  public static String string(Object value, String fallback) {
    return value == null ? fallback : String.valueOf(value);
  }

  // Verifie que les regles metier autorisent l operation sur rapport.

  public static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  // Realise l'intention metier rate.

  public static double rate(long count, long total) {
    return total <= 0 ? 0.0 : Math.round((count * 1000.0) / total) / 10.0;
  }

  // Cree un element du domaine super-administration maps apres validation metier.

  public static void addIssue(
    List<DataQualityIssueResponse> issues,
    String key,
    long count,
    String severity
  ) {
    if (count > 0) {
      issues.add(new DataQualityIssueResponse(key, key, count, severity));
    }
  }
}
