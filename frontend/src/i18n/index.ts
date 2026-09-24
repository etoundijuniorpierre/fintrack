// Point d'entree principal (export) du module.

import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

// Import translation files
import commonFr from "./locales/fr/common.json";
import commonEn from "./locales/en/common.json";
import loginFr from "./locales/fr/login.json";
import loginEn from "./locales/en/login.json";
import layoutFr from "./locales/fr/layout.json";
import layoutEn from "./locales/en/layout.json";
import dashboardFr from "./locales/fr/dashboard.json";
import dashboardEn from "./locales/en/dashboard.json";
import usersFr from "./locales/fr/users.json";
import usersEn from "./locales/en/users.json";
import authFr from "./locales/fr/auth.json";
import authEn from "./locales/en/auth.json";
import myProfileFr from "./locales/fr/myProfile.json";
import myProfileEn from "./locales/en/myProfile.json";
import incidentsFr from "./locales/fr/incidents.json";
import incidentsEn from "./locales/en/incidents.json";
import settingsFr from "./locales/fr/settings.json";
import settingsEn from "./locales/en/settings.json";
import auditFr from "./locales/fr/audit.json";
import auditEn from "./locales/en/audit.json";
import notificationsFr from "./locales/fr/notifications.json";
import notificationsEn from "./locales/en/notifications.json";
import reportsFr from "./locales/fr/reports.json";
import reportsEn from "./locales/en/reports.json";
import helpFr from "./locales/fr/help.json";
import helpEn from "./locales/en/help.json";
import superAdminFr from "./locales/fr/superAdmin.json";
import superAdminEn from "./locales/en/superAdmin.json";

const resources = {
  fr: {
    translation: {
      ...commonFr,
      ...loginFr,
      ...layoutFr,
      ...dashboardFr,
      ...usersFr,
      ...authFr,
      ...myProfileFr,
      ...incidentsFr,
      ...settingsFr,
      ...auditFr,
      ...notificationsFr,
      ...reportsFr,
      ...helpFr,
      ...superAdminFr,
    },
  },
  en: {
    translation: {
      ...commonEn,
      ...loginEn,
      ...layoutEn,
      ...dashboardEn,
      ...usersEn,
      ...authEn,
      ...myProfileEn,
      ...incidentsEn,
      ...settingsEn,
      ...auditEn,
      ...notificationsEn,
      ...reportsEn,
      ...helpEn,
      ...superAdminEn,
    },
  },
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    // L'UI FinTrack est livree en francais par defaut. La langue par defaut et le repli
    // sont configures sur `fr`. Le composant <App /> synchronise desormais dynamiquement
    // le locale d'Ant Design (<ConfigProvider>) et de Day.js avec la langue i18next choisie.
    // On ne force PAS `lng` : sinon LanguageDetector est court-circuite et la
    // preference de langue ne survit pas au rechargement (retour systematique en fr).
    fallbackLng: "fr",
    supportedLngs: ["fr", "en"],
    detection: {
      // Preference lue/ecrite uniquement dans localStorage (cle i18nextLng) : la
      // langue choisie persiste au rechargement. Sans valeur stockee -> fallbackLng ('fr').
      order: ["localStorage"],
      caches: ["localStorage"],
      lookupLocalStorage: "i18nextLng",
    },
    interpolation: {
      escapeValue: false,
    },
  });

// Synchronise l'attribut html[lang] au demarrage (avant tout changement).
document.documentElement.lang = i18n.language || "fr";

// Met a jour l'attribut html[lang] lors du changement de langue.
i18n.on("languageChanged", (lng: string) => {
  document.documentElement.lang = lng;
});

export default i18n;
