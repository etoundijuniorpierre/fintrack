// Valeur : texte de notification rendu dans les deux langues de l'interface.

package com.fintrack.reporting.client.notification;

// Le lecteur choisit sa langue ; les deux variantes sont rendues a l'emission.
public record BilingualText(String fr, String en) {}
