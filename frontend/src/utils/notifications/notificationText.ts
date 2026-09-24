// Utilitaire : choisit la langue d'affichage d'une notification bilingue.

interface BilingualNotification {
  subject?: string;
  content?: string;
  subjectEn?: string;
  contentEn?: string;
}

// La langue vient de l'appelant (useTranslation) : importer le singleton i18n
// ici forcerait son initialisation dans les tests qui mockent react-i18next.
const isEnglish = (language?: string): boolean =>
  (language ?? "fr").toLowerCase().startsWith("en");

// Sujet dans la langue du lecteur ; repli sur le francais stocke.
export const notificationSubject = (
  notification: BilingualNotification,
  language?: string,
): string =>
  (isEnglish(language) && notification.subjectEn) ||
  notification.subject ||
  notification.subjectEn ||
  "";

// Contenu dans la langue du lecteur ; repli sur le francais stocke.
export const notificationContent = (
  notification: BilingualNotification,
  language?: string,
): string =>
  (isEnglish(language) && notification.contentEn) ||
  notification.content ||
  notification.contentEn ||
  "";
