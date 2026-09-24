// Regroupe les helpers de formatage utilises par l'interface.
import dayjs from "dayjs";
import type { TFunction } from "i18next";
import { DATE_FORMATS } from "../date/dateUtils";

// Formate une date (jour uniquement, pas d'heure) pour les ecrans en locale francaise.
export const formatDate = (
  dateString: string | Date | null | undefined,
): string => {
  if (!dateString) return "-";
  const date =
    typeof dateString === "string" ? new Date(dateString) : dateString;
  return new Intl.DateTimeFormat("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
};

// Formate une date avec l'horodatage complet (jour et heure).
export const formatDateTime = (
  dateString: string | Date | null | undefined,
): string => {
  if (!dateString) return "-";
  const date =
    typeof dateString === "string" ? new Date(dateString) : dateString;
  return new Intl.DateTimeFormat("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};

// Formate un montant XAF pour l'affichage.
export const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "XAF",
  }).format(amount);
};

// Une duree se lit dans l'unite de son ordre de grandeur : « 0,5h » et « 172,4h »
// sont aussi illisibles l'un que l'autre. Sous l'heure on descend en minutes, au-dela
// de deux jours on monte en jours.
const DAY_THRESHOLD_HOURS = 48;
const MINUTES_PER_HOUR = 60;
const HOURS_PER_DAY = 24;

// Formate une duree dans l'unite adaptee a son ordre de grandeur.
export const formatHours = (value?: number | null): string => {
  const hours = Math.max(value ?? 0, 0);

  if (hours < 1) {
    const minutes = Math.round(hours * MINUTES_PER_HOUR);
    // 0,999 h arrondi a 60 min deviendrait « 60 min » : on passe a l'heure.
    return minutes === MINUTES_PER_HOUR ? "1h" : `${minutes} min`;
  }

  if (hours < DAY_THRESHOLD_HOURS) {
    const wholeHours = Math.floor(hours);
    const minutes = Math.round((hours - wholeHours) * MINUTES_PER_HOUR);
    // 1,999 h arrondi a 60 min deviendrait « 1h60 » : on reporte sur l'heure.
    if (minutes === MINUTES_PER_HOUR) return `${wholeHours + 1}h`;
    return minutes === 0
      ? `${wholeHours}h`
      : `${wholeHours}h${String(minutes).padStart(2, "0")}`;
  }

  const days = Math.floor(hours / 24);
  const remainder = Math.round(hours - days * 24);
  // 47,6 h arrondi a 24 h de reste deviendrait « 1j 24h » : on reporte sur le jour.
  return remainder === 24
    ? `${days + 1}j`
    : `${days}j ${remainder}h`.replace(" 0h", "");
};

// La meme duree en toutes lettres, pour les chiffres mis en avant : « 36h » oblige
// le lecteur a convertir de tete, « 1 jour et 12 heures » non. Le format compact
// reste celui des graphiques et des listes, ou la place manque.
export const formatDurationLong = (
  value: number | null | undefined,
  t: TFunction,
): string => {
  const hours = Math.max(value ?? 0, 0);

  if (hours < 1) {
    const minutes = Math.round(hours * MINUTES_PER_HOUR);
    if (minutes === 0) return t("common.duration.lessThanAMinute");
    // 0,999 h arrondi a 60 min deviendrait « 60 minutes » : on passe a l'heure.
    return minutes === MINUTES_PER_HOUR
      ? t("common.duration.hours", { count: 1 })
      : t("common.duration.minutes", { count: minutes });
  }

  if (hours < HOURS_PER_DAY) {
    const wholeHours = Math.round(hours);
    return wholeHours === HOURS_PER_DAY
      ? t("common.duration.days", { count: 1 })
      : t("common.duration.hours", { count: wholeHours });
  }

  const days = Math.floor(hours / HOURS_PER_DAY);
  const remainder = Math.round(hours - days * HOURS_PER_DAY);
  // 47,6 h arrondi a 24 h de reste deviendrait « 1 jour et 24 heures ».
  if (remainder === HOURS_PER_DAY) {
    return t("common.duration.days", { count: days + 1 });
  }
  const daysLabel = t("common.duration.days", { count: days });
  return remainder === 0
    ? daysLabel
    : t("common.duration.daysAndHours", {
        days: daysLabel,
        hours: t("common.duration.hours", { count: remainder }),
      });
};

// Formate un taux en pourcentage entier.
export const formatPercentage = (value?: number | null): string => {
  return `${Math.round(value ?? 0)}%`;
};

// Raccourcit un libelle trop long avec une ellipse.
export const truncateString = (str: string, length: number): string => {
  if (str.length <= length) return str;
  return str.slice(0, length) + "...";
};

// Normalise la casse d'un mot pour un libelle simple.
export const capitalize = (str: string): string => {
  if (!str) return "";
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
};

// Met en majuscule le premier caractere d'un ou plusieurs mots.
export const toHyperCase = (str: string, allWords: boolean = false): string => {
  if (!str) return "";
  if (allWords) {
    return str
      .split(/([\s-]+)/)
      .map((part) => {
        if (/^[\s-]+$/.test(part)) return part;
        return part.charAt(0).toUpperCase() + part.slice(1);
      })
      .join("");
  }
  return str.charAt(0).toUpperCase() + str.slice(1);
};

// Met uniquement le debut d'une phrase en majuscule.
export const toSentenceCase = (str: string): string => {
  if (!str) return "";
  return str.charAt(0).toUpperCase() + str.slice(1);
};

// Transforme un libelle en CONSTANT_CASE.
export const toConstantCase = (str: string): string => {
  if (!str) return "";
  return str.toUpperCase().replace(/\s+/g, "_");
};

// Affiche une valeur simple ou une liste sous forme de libelle.
export const formatArrayOrString = (
  value: unknown,
  separator: string = " - ",
  fallback: string = "-",
): string => {
  if (value === undefined || value === null) return fallback;
  return Array.isArray(value) ? value.join(separator) : String(value);
};

// Liste les separateurs acceptes dans les champs de destinataires e-mail.
export const EMAIL_RECIPIENT_TOKEN_SEPARATORS = [
  ",",
  ";",
  " ",
  "\n",
  "\t",
] as const;

// Separe les destinataires e-mail saisis en texte libre.
const EMAIL_RECIPIENT_SPLIT_PATTERN = /[,\s;]+/;

// Dedoublonne et nettoie les destinataires e-mail avant envoi.
export const normalizeEmailRecipients = (recipients?: string[]): string[] =>
  Array.from(
    new Set(
      (recipients ?? [])
        .flatMap((email) => email.split(EMAIL_RECIPIENT_SPLIT_PATTERN))
        .map((email) => email.trim())
        .filter(Boolean),
    ),
  );

// Option d'autocompletion d'un destinataire : libelle « Nom (email) », valeur = email.
export interface EmailRecipientOption {
  label: string;
  value: string;
}

// Construit les options destinataires a partir d'utilisateurs : permet de
// retrouver un e-mail en tapant simplement le nom (le libelle porte le nom).
export const buildEmailRecipientOptions = (
  users: {
    username: string;
    email?: string;
    firstName?: string;
    lastName?: string;
  }[],
): EmailRecipientOption[] =>
  users
    .filter((user) => Boolean(user.email && user.email.trim()))
    .map((user) => {
      const fullName = [user.firstName, user.lastName]
        .filter(Boolean)
        .join(" ");
      const displayName = fullName || user.username;
      return {
        label: `${displayName} (${user.email})`,
        value: user.email!.trim(),
      };
    });

// Formate un numero de telephone avec des espaces de lecture.
export const formatPhoneNumber = (
  value: string | number | undefined,
): string => {
  if (value === undefined || value === null || value === "") return "";
  return `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, " ");
};

// Nettoie une saisie numerique avant stockage.
export const parseNumber = (value: string | undefined): string => {
  if (!value) return "";
  return value.replace(/\s?|(,*)/g, "");
};

// Adapte un champ telephone Ant Design au format de lecture attendu.
export const phoneFormItemProps = {
  getValueProps: (value: string | number | undefined) => ({
    value: formatPhoneNumber(value),
  }),
  getValueFromEvent: (e: React.ChangeEvent<HTMLInputElement>) =>
    e.target.value.replace(/\s/g, ""),
} as const;

// Adapte un champ texte pour capitaliser le premier mot.
export const hyperCaseFormItemProps = {
  getValueFromEvent: (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    return toHyperCase(e.target.value);
  },
} as const;

// Adapte un champ texte pour capitaliser chaque mot.
export const hyperCaseAllWordsFormItemProps = {
  getValueFromEvent: (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    return toHyperCase(e.target.value, true);
  },
} as const;

// Adapte un champ texte pour capitaliser uniquement le debut de phrase.
export const sentenceCaseFormItemProps = {
  getValueFromEvent: (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    return toSentenceCase(e.target.value);
  },
} as const;

// Produit le meilleur libelle disponible pour un utilisateur.
export const formatUserName = (
  user:
    | {
        firstName?: string | null;
        lastName?: string | null;
        username?: string | null;
      }
    | null
    | undefined,
  fallbackUsername?: string | null,
  fallback: string = "\u2014",
): string => {
  const firstName = user?.firstName?.trim();
  const lastName = user?.lastName?.trim();
  if (lastName && firstName) return `${lastName} ${firstName}`;
  if (lastName) return lastName;
  if (firstName) return firstName;
  const username = user?.username?.trim() || fallbackUsername?.trim();
  if (username) return username;
  return fallback;
};

// Valide le format UUID utilise par les identifiants techniques.
export const isUuid = (value: unknown): boolean => {
  return (
    typeof value === "string" &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
      value,
    )
  );
};

// Transforme une cle technique en libelle humain.
export const humanizeString = (key: string): string => {
  if (!key) return "";
  const humanized = key
    .replace(/([A-Z])/g, " $1")
    .replace(/[_-]/g, " ")
    .trim();
  return humanized.charAt(0).toUpperCase() + humanized.slice(1).toLowerCase();
};

// Transforme un titre en slug propre pour les noms de fichiers (sans accents ni caracteres speciaux).
export const slugifyTitle = (
  title?: string | null,
  fallback: string = "document",
): string => {
  if (!title || !title.trim()) return fallback;
  const slug = title
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
  return slug || fallback;
};

// Formate le statut de connexion pour l'indicateur de presence.
export const formatPresenceStatus = (
  dateString: string | Date | null | undefined,
  online: boolean,
  t: (key: string, options?: Record<string, unknown>) => string,
): string => {
  if (online) {
    if (!dateString) {
      return t("layout.header.presence.status.online_default");
    }
    const connectedAt = dayjs(dateString);
    const timeStr = connectedAt.isSame(dayjs(), "day")
      ? connectedAt.format(DATE_FORMATS.TIME)
      : connectedAt.format(DATE_FORMATS.FULL);
    return t("layout.header.presence.status.online_since", { time: timeStr });
  }

  if (!dateString) {
    return t("layout.header.presence.status.offline_unknown");
  }

  const presenceDate = dayjs(dateString);
  const now = dayjs();
  const diffHours = now.diff(presenceDate, "hour");

  if (diffHours >= 24) {
    const dateStr = presenceDate.format(DATE_FORMATS.FULL);
    return t("layout.header.presence.status.offline_date", { date: dateStr });
  }

  const diffMinutes = now.diff(presenceDate, "minute");
  if (diffHours < 1) {
    const count = Math.max(1, diffMinutes);
    return t("layout.header.presence.status.offline_ago_minutes", { count });
  }

  return t("layout.header.presence.status.offline_ago_hours", {
    count: diffHours,
  });
};
