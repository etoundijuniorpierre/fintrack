// Securite : applique l'authentification et les autorisations liees a temp password cipher.

package com.fintrack.user.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Choix volontaire d'un chiffrement réversible plutôt que d'un hash : un
 * administrateur doit pouvoir reconsulter le mot de passe temporaire pendant sa
 * durée de validité afin de le transmettre oralement (cas d'un utilisateur sans
 * adresse e-mail).
 */
@Component
// Modelise la responsabilite applicative liee a utilisateur.
public class TempPasswordCipher {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;

  private final SecretKey key;
  private final SecureRandom secureRandom = new SecureRandom();

  // Initialise le composant avec ses dependances obligatoires.
  public TempPasswordCipher(
    @Value(
      "${fintrack.security.temp-password-secret:${fintrack.jwt.secret}}"
    ) String secret
  ) {
    try {
      byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(
        secret.getBytes(StandardCharsets.UTF_8)
      );
      this.key = new SecretKeySpec(keyBytes, "AES");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(
        "L'algorithme SHA-256 est indisponible",
        e
      );
    }
  }

  /** Chiffre un mot de passe en clair ; renvoie null si l'entrée est null. */
  // Chiffre encrypt.
  public String encrypt(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    try {
      byte[] iv = new byte[IV_LENGTH];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(
        Cipher.ENCRYPT_MODE,
        key,
        new GCMParameterSpec(TAG_LENGTH_BITS, iv)
      );
      byte[] ciphertext = cipher.doFinal(
        plaintext.getBytes(StandardCharsets.UTF_8)
      );

      byte[] payload = ByteBuffer.allocate(iv.length + ciphertext.length)
        .put(iv)
        .put(ciphertext)
        .array();
      return Base64.getEncoder().encodeToString(payload);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(
        "Le chiffrement du mot de passe temporaire a echoue",
        e
      );
    }
  }

  /**
   * Déchiffre une valeur produite par. Renvoie null si
   * l'entrée est null ou n'est pas un cryptogramme valide (ex. ancienne
   * donnée en clair, secret modifié) — l'appelant ne doit alors pas l'exposer.
   */
  // Dechiffre decrypt.
  public String decrypt(String ciphertext) {
    if (ciphertext == null) {
      return null;
    }
    try {
      byte[] payload = Base64.getDecoder().decode(ciphertext);
      if (payload.length <= IV_LENGTH) {
        return null;
      }
      byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
      byte[] data = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(
        Cipher.DECRYPT_MODE,
        key,
        new GCMParameterSpec(TAG_LENGTH_BITS, iv)
      );
      return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      return null;
    }
  }
}
