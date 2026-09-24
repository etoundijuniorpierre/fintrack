// Utilitaire : regroupe les helpers techniques lies a agency code generator.

package com.fintrack.user.util;

import com.fintrack.user.repository.AgencyRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Génère le code d'agence immuable au format {VILLE}-{SEQ}-{NOM}
 * (e.g. YDE-05-ESSOS). The sequence is incremented per city.
 */

// Generateur utilitaire de codes uniques pour les agences.

@Component
@RequiredArgsConstructor
public class AgencyCodeGenerator {

  private static final Map<String, String> CITY_ABBREVIATIONS = Map.ofEntries(
    Map.entry("YAOUNDE", "YDE"),
    Map.entry("DOUALA", "DLA"),
    Map.entry("BAFOUSSAM", "BAF"),
    Map.entry("BAMENDA", "BAM"),
    Map.entry("GAROUA", "GAR"),
    Map.entry("MAROUA", "MAR"),
    Map.entry("BERTOUA", "BER"),
    Map.entry("NGAOUNDERE", "NGD"),
    Map.entry("EBOLOWA", "EBO"),
    Map.entry("BUEA", "BUE"),
    Map.entry("KRIBI", "KRI"),
    Map.entry("LIMBE", "LMB"),
    Map.entry("EDEA", "EDE"),
    Map.entry("KUMBA", "KUM"),
    Map.entry("DSCHANG", "DSC")
  );

  private final AgencyRepository agencyRepository;

  // Genere la sortie attendue pour le domaine agence code generator.
  public String generate(String city, String agencyName) {
    String cityCode = cityCode(city);
    String namePart = normalize(agencyName);
    int sequence = nextSequence(cityCode);
    return String.format("%s-%02d-%s", cityCode, sequence, namePart);
  }

  // Construit le prefixe de code agence depuis la ville.

  private String cityCode(String city) {
    String normalized = normalize(city);
    String abbreviation = CITY_ABBREVIATIONS.get(normalized);
    if (abbreviation != null) {
      return abbreviation;
    }
    return normalized.length() <= 3 ? normalized : normalized.substring(0, 3);
  }

  // Calcule la prochaine sequence disponible pour un code agence.

  private int nextSequence(String cityCode) {
    List<String> existingCodes = agencyRepository.findCodesByPrefix(
      cityCode + "-"
    );
    Pattern pattern = Pattern.compile(
      "^" + Pattern.quote(cityCode) + "-(\\d+)-"
    );
    int max = 0;
    for (String code : existingCodes) {
      Matcher matcher = pattern.matcher(code);
      if (matcher.find()) {
        max = Math.max(max, Integer.parseInt(matcher.group(1)));
      }
    }
    return max + 1;
  }

  // Normalise les valeurs du domaine agence code generator avant traitement.

  private String normalize(String value) {
    String stripped = Normalizer.normalize(
      value,
      Normalizer.Form.NFD
    ).replaceAll("\\p{M}", "");
    return stripped.toUpperCase().replaceAll("[^A-Z0-9]", "");
  }
}
