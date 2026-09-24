// Contrat metier : expose les operations du domaine email.

package com.fintrack.user.service;

import com.fintrack.user.client.notification.BilingualText;
import com.fintrack.user.model.entity.User;
import java.util.List;

// Definit le contrat email attendu par les autres couches.

public interface EmailService {
  // Diffuse l'information du domaine email aux destinataires concernes.
  void sendTemporaryPassword(
    String email,
    String username,
    String lastName,
    String firstName,
    String tempPassword,
    long validityMinutes
  );
  // Realise l'intention metier contact admin.
  void contactAdmin(
    String senderEmail,
    String senderName,
    String senderUsername,
    String subject,
    String message,
    List<User> targetAdmins
  );
  // Diffuse l'information du domaine email aux destinataires concernes.
  void notifyAdmins(
    BilingualText subject,
    BilingualText message,
    List<String> adminEmails);
  // Diffuse l'information avec une cle d'idempotence commune a l'evenement.
  void notifyAdmins(
    BilingualText subject,
    BilingualText message,
    List<String> adminEmails,
    String idempotencyGroupKey
  );
}
