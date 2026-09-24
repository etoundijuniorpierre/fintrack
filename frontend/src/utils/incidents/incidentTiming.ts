// Utilitaire de timing incident : calcule l'age et l'etat SLA a partir des dates metier.

import type { IncidentSummaryResponse } from "../../api/incident/types";
import { IncidentStatus } from "../../api/incident/types";

const ACTIVE_STATUSES = new Set<string>([
  IncidentStatus.DRAFT,
  IncidentStatus.OPEN,
  IncidentStatus.PENDING_VALIDATION,
  IncidentStatus.VALIDATED,
  IncidentStatus.TRANSFERRED,
  IncidentStatus.ASSIGNED,
  IncidentStatus.IN_PROGRESS,
  IncidentStatus.BLOCKED,
  IncidentStatus.REOPENED,
]);

const MS_PER_MINUTE = 60 * 1000;
const MS_PER_HOUR = 60 * MS_PER_MINUTE;
const MS_PER_DAY = 24 * MS_PER_HOUR;

// Decrit l'unite retenue pour afficher l'age d'un incident.
export type IncidentAgeUnit = "minutes" | "hours" | "days";

// Transporte l'age lisible d'un incident avant traduction.
export interface IncidentAgeInfo {
  value: number;
  unit: IncidentAgeUnit;
}

// Decrit l'etat d'echeance affiche pour un incident.
export type IncidentSlaState =
  | "none"
  | "closed"
  | "stopped"
  | "overdue"
  | "dueToday"
  | "dueSoon"
  | "onTrack";

// Transporte l'etat d'echeance et le delai restant avant traduction.
export interface IncidentSlaInfo {
  state: IncidentSlaState;
  daysRemaining?: number;
}

// Calcule l'age d'un incident dans l'unite la plus lisible.
export const getIncidentAgeInfo = (
  createdAt: string | Date | undefined,
  now: Date = new Date(),
): IncidentAgeInfo | null => {
  if (!createdAt) return null;

  const createdDate =
    typeof createdAt === "string" ? new Date(createdAt) : createdAt;
  const elapsedMs = Math.max(0, now.getTime() - createdDate.getTime());

  if (elapsedMs < MS_PER_HOUR) {
    return {
      value: Math.max(1, Math.floor(elapsedMs / MS_PER_MINUTE)),
      unit: "minutes",
    };
  }

  if (elapsedMs < MS_PER_DAY) {
    return { value: Math.floor(elapsedMs / MS_PER_HOUR), unit: "hours" };
  }

  return { value: Math.floor(elapsedMs / MS_PER_DAY), unit: "days" };
};

// Calcule l'etat d'echeance d'un incident selon son echeance et son statut.
export const getIncidentSlaInfo = (
  incident: Pick<IncidentSummaryResponse, "dueDate" | "status">,
  now: Date = new Date(),
): IncidentSlaInfo => {
  if (!incident.dueDate) {
    return { state: "none" };
  }

  if (!ACTIVE_STATUSES.has(incident.status)) {
    return {
      state: incident.status === IncidentStatus.CLOSED ? "closed" : "stopped",
    };
  }

  const rawDate = incident.dueDate;
  const normalizedDateStr =
    typeof rawDate === "string" ? rawDate.replace(" ", "T") : rawDate;
  const due = new Date(normalizedDateStr);

  if (isNaN(due.getTime())) {
    return { state: "none" };
  }

  const remainingMs = due.getTime() - now.getTime();
  const daysRemaining =
    remainingMs < 0
      ? Math.floor(remainingMs / MS_PER_DAY)
      : Math.ceil(remainingMs / MS_PER_DAY);

  if (remainingMs < 0) {
    return { state: "overdue", daysRemaining };
  }

  if (due.toDateString() === now.toDateString()) {
    return { state: "dueToday", daysRemaining };
  }

  if (daysRemaining <= 2) {
    return { state: "dueSoon", daysRemaining };
  }

  return { state: "onTrack", daysRemaining };
};
