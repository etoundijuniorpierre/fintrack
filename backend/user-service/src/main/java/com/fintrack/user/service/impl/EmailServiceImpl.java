// Service metier : coordonne les operations du domaine email.

package com.fintrack.user.service.impl;

import com.fintrack.user.client.notification.BilingualText;
import com.fintrack.user.client.notification.NotificationClientService;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.service.EmailService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

// Implemente les regles metier du domaine email.

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

  private final NotificationClientService notificationClientService;
  private final MessageSource messageSource;

  // Diffuse l'information du domaine email aux destinataires concernes.

  public void sendTemporaryPassword(
    String email,
    String username,
    String lastName,
    String firstName,
    String tempPassword,
    long validityMinutes
  ) {
    try {
      Map<String, Object> templateParams = new HashMap<>();
      templateParams.put("email_template", "credential");
      templateParams.put("name", lastName != null ? lastName : "");
      templateParams.put("firstName", firstName != null ? firstName : "");
      templateParams.put("userName", username != null ? username : "");
      templateParams.put("temporary_password", tempPassword);
      // Delai de validite pilote par le seuil super-admin tempPasswordValidityMinutes.
      templateParams.put("validity_minutes", String.valueOf(validityMinutes));
      templateParams.put(
        "date",
        LocalDateTime.now().format(
          DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        )
      );

      BilingualText subject = lmsg(
        "notification.user.temp_password.subject"
      );
      BilingualText content = lmsg(
        "notification.user.temp_password.content"
      );

      notificationClientService.sendEmail(
        email,
        subject,
        content,
        templateParams,
        notificationKey(
          "TEMP_PASSWORD",
          username + ":" + tempPassword,
          "EMAIL",
          email
        )
      );
      log.info(
        "System notification creation requested pour {} via notification-service",
        email
      );
    } catch (Exception e) {
      log.error(
        "Échec de creation de la notification systeme du mot de passe temporaire: {}",
        e.getMessage()
      );
    }
  }

  // Realise l'intention metier contact admin.

  @Override
  public void contactAdmin(
    String senderEmail,
    String senderName,
    String senderUsername,
    String subject,
    String message,
    List<User> targetAdmins
  ) {
    try {
      String eventKey = notificationKey(
        "CONTACT_ADMIN",
        senderUsername + ":" + subject + ":" + message,
        null,
        null
      );
      Map<String, Object> baseTemplateParams = new HashMap<>();
      baseTemplateParams.put("email_template", "generic_notification");
      baseTemplateParams.put("display_sender_details", "block");
      baseTemplateParams.put("sender_email", senderEmail);
      baseTemplateParams.put("sender_name", senderName);
      baseTemplateParams.put("sender_username", senderUsername);
      baseTemplateParams.put("message_body", message);

      BilingualText translatedSubject = lmsg(
        "notification.admin.contact.subject",
        subject
      );
      BilingualText translatedContent = lmsg(
        "notification.admin.contact.content",
        message
      );

      for (User admin : targetAdmins) {

        Map<String, Object> personalParams = new HashMap<>(baseTemplateParams);
        if (admin.getFirstName() != null && admin.getLastName() != null) {
          personalParams.put(
            "name",
            admin.getFirstName() + " " + admin.getLastName()
          );
        } else if (admin.getFirstName() != null) {
          personalParams.put("name", admin.getFirstName());
        }

        if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
          try {
            notificationClientService.sendEmail(
              admin.getEmail(),
              translatedSubject,
              translatedContent,
              personalParams,
              notificationKey(eventKey, "EMAIL", admin.getEmail())
            );
          } catch (Exception e) {
            log.error(
              "Échec d'envoi de email vers {}: {}",
              admin.getEmail(),
              e.getMessage()
            );
          }
        }
        if (admin.getUsername() != null && !admin.getUsername().isBlank()) {
          try {
            log.info(
              "Tentative d envoi de la notification interne à {}",
              admin.getUsername()
            );
            notificationClientService.sendInternal(
              admin.getUsername(),
              translatedSubject,
              translatedContent,
              personalParams,
              notificationKey(eventKey, "INTERNAL", admin.getUsername())
            );
            log.info("Notification interne envoyee à {}", admin.getUsername());
          } catch (Exception e) {
            log.error(
              "Erreur lors de l envoi de la notification interne à {}: {}",
              admin.getUsername(),
              e.getMessage()
            );
          }
        }
      }
      log.info(
        "E-mails de contact administrateur et notifications internes envoyes pour {}",
        senderEmail
      );
    } catch (Exception e) {
      log.error(
        "Erreur inattendue dans le flux contactAdmin : {}",
        e.getMessage()
      );
    }
  }

  // Diffuse l'information du domaine email aux destinataires concernes.

  @Override
  public void notifyAdmins(
    BilingualText subject,
    BilingualText message,
    List<String> adminEmails
  ) {
    notifyAdmins(subject, message, adminEmails, null);
  }

  // Diffuse l'information du domaine email aux destinataires concernes.

  @Override
  public void notifyAdmins(
    BilingualText subject,
    BilingualText message,
    List<String> adminEmails,
    String idempotencyGroupKey
  ) {
    try {
      String eventKey = idempotencyGroupKey != null
        ? idempotencyGroupKey
        : notificationKey(
          "ADMIN_NOTICE",
          subject.fr() +
          "|" +
          message.fr() +
          "|" +
          adminEmails.stream().filter(Objects::nonNull).sorted().toList(),
          null,
          null
        );
      Map<String, Object> templateParams = new HashMap<>();
      templateParams.put("message_body", message.fr());

      for (String adminEmail : adminEmails) {
        notificationClientService.sendInternal(
          adminEmail,
          subject,
          message,
          templateParams,
          notificationKey(eventKey, "INTERNAL", adminEmail)
        );
      }
      log.info("Notifications administrateur envoyees pour : {}", subject.fr());
    } catch (Exception e) {
      log.error("Échec de notify admins: {}", e.getMessage());
    }
  }

  // Rend une cle dans les deux langues de l'interface ; la cle sert de repli.
  private BilingualText lmsg(String key, Object... args) {
    return new BilingualText(
      messageSource.getMessage(key, args, key, Locale.FRENCH),
      messageSource.getMessage(key, args, key, Locale.ENGLISH)
    );
  }

  // Construit une cle de notification idempotente par evenement, canal et destinataire.
  private String notificationKey(
    String eventType,
    String eventPayload,
    String channel,
    String recipient
  ) {
    String base =
      "user-service:" +
      eventType +
      ":" +
      sha256(Objects.toString(eventPayload, ""));
    if (channel == null || recipient == null) {
      return base;
    }
    return notificationKey(base, channel, recipient);
  }

  // Complete une cle d'evenement par canal et destinataire.
  private String notificationKey(
    String eventKey,
    String channel,
    String recipient
  ) {
    return String.join(
      ":",
      eventKey,
      Objects.toString(channel, ""),
      sha256(Objects.toString(recipient, ""))
    );
  }

  // Calcule une empreinte compacte pour eviter d'exposer des donnees sensibles.
  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponible", e);
    }
  }
}
