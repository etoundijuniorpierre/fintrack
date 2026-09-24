// Modele metier : represente les donnees physiques d'un fichier stocke.

package com.fintrack.document.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Resultat de stockage contenant la cle objet, la taille et le checksum SHA-256.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

  private String objectKey;
  private long size;
  private String checksum;
}
