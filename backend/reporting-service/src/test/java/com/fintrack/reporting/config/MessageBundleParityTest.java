// Tests : verrouille la parite des bundles i18n. Le bundle sans suffixe est le repli
// pour toute locale autre que fr et en ; une clef ajoutee aux seules traductions y
// laisse un trou qui ne se voit qu'en production, sur une locale inattendue.

package com.fintrack.reporting.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MessageBundleParityTest {

  private static final Path I18N = Paths.get("src/main/resources/i18n");

  @ParameterizedTest
  @ValueSource(strings = { "messages", "notifications" })
  @DisplayName("i18n - Every locale of a bundle declares the same keys")
  void bundle_DeclaresTheSameKeysInEveryLocale(String bundle)
    throws IOException {
    Path base = I18N.resolve(bundle + ".properties");
    if (!Files.exists(base)) {
      return;
    }
    Set<String> reference = keysOf(base);
    assertThat(reference).isNotEmpty();

    for (String locale : List.of("fr", "en")) {
      Path translation = I18N.resolve(bundle + "_" + locale + ".properties");
      assertThat(translation).exists();
      assertThat(keysOf(translation))
        .describedAs(
          "%s_%s.properties doit declarer exactement les clefs de %s.properties",
          bundle,
          locale,
          bundle
        )
        .containsExactlyInAnyOrderElementsOf(reference);
    }
  }

  // Clefs declarees par un fichier .properties, commentaires et lignes vides exclus.
  private Set<String> keysOf(Path file) throws IOException {
    try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
      return lines
        .map(String::trim)
        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
        .filter(line -> line.contains("="))
        .map(line -> line.substring(0, line.indexOf('=')).trim())
        .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }
  }
}
