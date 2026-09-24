// Fonctions utilitaires de calcul et de formatage de plages de dates.

import type { Dayjs } from "dayjs";
import { PeriodType, type PeriodFilter } from "../../../types/dashboard";

// Cree le filtre immediat pour les periodes predefinies.
export function getImmediateFilter(period: PeriodType): PeriodFilter | null {
  if (period === PeriodType.CUSTOM) {
    return null;
  }
  return { period };
}

// Valide une plage personnalisee avant de l'envoyer au backend.
export function validateCustomRange(
  from: Dayjs | null,
  to: Dayjs | null,
): PeriodFilter | null {
  if (!from || !to) {
    return null;
  }
  if (from.isAfter(to)) {
    return null;
  }
  return {
    period: PeriodType.CUSTOM,
    dateFrom: from.startOf("day").format("YYYY-MM-DDTHH:mm:ss.SSS"),
    dateTo: to.endOf("day").format("YYYY-MM-DDTHH:mm:ss.SSS"),
  };
}
