// Utilitaires de conversion, comparaison et formatage de dates.
import dayjs from "dayjs";
import customParseFormat from "dayjs/plugin/customParseFormat";

dayjs.extend(customParseFormat);

export type { Dayjs } from "dayjs";

// Liste les formats de date utilises dans les formulaires et l'API.
export const DATE_FORMATS = {
  API: "YYYY-MM-DD",
  API_DATE_TIME: "YYYY-MM-DDTHH:mm:ss",
  DISPLAY: "DD/MM/YYYY",
  FULL: "DD/MM/YYYY HH:mm",
  TIME: "HH:mm",
} as const;

// Indique si une date est strictement anterieure au jour courant.
export const isPastDate = (date: dayjs.Dayjs | null | undefined): boolean => {
  if (!date) return false;
  return date.isBefore(dayjs().startOf("day"));
};

// Retourne le debut du jour courant.
export const getToday = () => dayjs().startOf("day");

// Convertit une date Dayjs vers le format attendu par l'API.
export const formatToApiDate = (
  date: dayjs.Dayjs | null | undefined,
): string | undefined => {
  return date?.format(DATE_FORMATS.API);
};

// Convertit une date Dayjs vers le format date-heure local attendu par les APIs Spring LocalDateTime.
export const formatToApiDateTime = (
  date: dayjs.Dayjs | null | undefined,
  boundary?: "startOfDay" | "endOfDay",
): string | undefined => {
  if (!date) return undefined;
  const normalized =
    boundary === "startOfDay"
      ? date.startOf("day")
      : boundary === "endOfDay"
        ? date.endOf("day")
        : date;
  return normalized.format(DATE_FORMATS.API_DATE_TIME);
};

// Convertit une heure textuelle en objet Dayjs.
export const parseTime = (
  timeString: string | undefined | null,
): dayjs.Dayjs | null => {
  if (!timeString) return null;
  return dayjs(timeString, DATE_FORMATS.TIME);
};

// Retourne le nom localise d'un jour de semaine.
export const getLocalizedDayName = (dayIndex: number): string => {
  return dayjs().day(dayIndex).format("dddd");
};

// Convertit de maniere securisee une valeur (string, Date, Dayjs) en objet Dayjs.
export const toDayjs = (
  value: dayjs.Dayjs | string | number | Date | null | undefined,
): dayjs.Dayjs | undefined => {
  if (!value) return undefined;
  if (dayjs.isDayjs(value)) return value;
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed : undefined;
};
