// Utilitaire de presentation des incidents : transforme l'age et l'echeance en libelles traduits.

import type { IncidentAgeInfo, IncidentSlaInfo } from "./incidentTiming";

// Formate l'age de l'incident pour l'interface.
export const formatIncidentAge = (
  age: IncidentAgeInfo | null,
  t: (key: string, options?: { count?: number }) => string,
): string => {
  if (!age) return "-";
  return t(`incidents.age.${age.unit}`, { count: age.value });
};

// Formate l'echeance de l'incident pour l'interface.
export const formatIncidentSla = (
  sla: IncidentSlaInfo,
  t: (key: string, options?: { count?: number }) => string,
  fallback: string = "-",
): string => {
  if (sla.state === "none") return fallback;
  if (sla.state === "dueSoon") {
    return t("incidents.sla.due_in_days", { count: sla.daysRemaining ?? 0 });
  }
  return t(`incidents.sla.${sla.state}`);
};
