// Utilitaire : regroupe les helpers techniques lies a password generator.

package com.fintrack.user.util;

import java.security.SecureRandom;
import java.util.Random;
import org.springframework.stereotype.Component;

// Utilitaire de generation de mots de passe temporaires securises.

@Component
public class PasswordGenerator {

  private static final String CHARACTERS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
  private static final int PASSWORD_LENGTH = 12;
  private final Random random = new SecureRandom();

  // Genere un mot de passe temporaire conforme aux regles de securite.
  public String generateTemporaryPassword() {
    StringBuilder password = new StringBuilder(PASSWORD_LENGTH);

    password.append(getRandomChar("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    password.append(getRandomChar("abcdefghijklmnopqrstuvwxyz"));
    password.append(getRandomChar("0123456789"));
    password.append(getRandomChar("!@#$%^&*"));

    for (int i = 4; i < PASSWORD_LENGTH; i++) {
      password.append(getRandomChar(CHARACTERS));
    }

    return shuffleString(password.toString());
  }

  // Fournit random char a la couche appelante.

  private char getRandomChar(String characters) {
    return characters.charAt(random.nextInt(characters.length()));
  }

  // Melange les caracteres d'une chaine generee.

  private String shuffleString(String input) {
    char[] characters = input.toCharArray();
    for (int i = 0; i < characters.length; i++) {
      int randomIndex = random.nextInt(characters.length);
      char temp = characters[i];
      characters[i] = characters[randomIndex];
      characters[randomIndex] = temp;
    }
    return new String(characters);
  }
}
