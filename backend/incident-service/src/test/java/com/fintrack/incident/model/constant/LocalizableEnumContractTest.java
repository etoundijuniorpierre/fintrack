// Tests unitaires : garde-fou de contrat entre les enums metier et les bundles i18n.

package com.fintrack.incident.model.constant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Chaque valeur d'un LocalizableEnum annonce deux clefs i18n. Rien ne garantissait
 * qu'elles existent : une valeur ajoutee sans ses libelles s'affiche sous forme de
 * clef brute (« enum.action_type.CRITICALITY_CHANGE.name ») cote interface, sans
 * qu'aucun test ne bronche. Ce contrat ferme ce trou pour tous les bundles.
 */
class LocalizableEnumContractTest {

  // Toutes les enumerations exposees a l'interface via LocalizableEnum.
  private static final List<Class<? extends LocalizableEnum>> ENUMS = List.of(
    ActionType.class,
    Criticality.class,
    IncidentActorRole.class,
    IncidentCause.class,
    IncidentScope.class,
    IncidentStatus.class,
    IncidentValidatorRole.class,
    IncidentValidatorScope.class,
    PeriodType.class
  );

  @ParameterizedTest(name = "{0}")
  @ValueSource(
    strings = {
      "i18n/messages.properties",
      "i18n/messages_fr.properties",
      "i18n/messages_en.properties",
    }
  )
  @DisplayName(
    "Every localizable enum value has its label and description in each bundle"
  )
  void everyEnumValueIsTranslated(String bundle) throws Exception {
    Properties messages = load(bundle);

    List<String> missing = new ArrayList<>();
    for (Class<? extends LocalizableEnum> type : ENUMS) {
      for (LocalizableEnum value : type.getEnumConstants()) {
        Stream.of(value.getNameKey(), value.getDescriptionKey())
          .filter(key -> !messages.containsKey(key))
          .forEach(key ->
            missing.add(type.getSimpleName() + "." + value.getName() + " -> " + key)
          );
      }
    }

    assertThat(missing)
      .as("clefs i18n absentes de %s", bundle)
      .isEmpty();
  }

  private Properties load(String bundle) throws Exception {
    Properties properties = new Properties();
    try (
      InputStream stream = getClass()
        .getClassLoader()
        .getResourceAsStream(bundle)
    ) {
      assertThat(stream).as("bundle %s introuvable", bundle).isNotNull();
      properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
    return properties;
  }
}
