// Utilitaires de graphes : prepare les series, couleurs et exports du tableau de bord.

import type { TFunction } from "i18next";
import type { MonthlyMetric } from "../../../types/dashboard";
import { toConstantCase } from "../../formatters/formatters";
import {
  CHART_SERIES,
  CRITICALITY_COLORS,
  STATUS_COLORS,
} from "../../../theme/chartColors";

// Decrit une donnee affichee dans un graphe.
export interface ChartDatum {
  name: string;
  rawName: string;
  value: number;
}

// Decrit une ligne exportable depuis un graphe.
export interface ChartExportRow {
  label: string;
  value: number;
  color?: string;
}

// Palette partagee par les graphes du tableau de bord.
export const COLORS = CHART_SERIES;

// Reconnait les identifiants techniques affiches comme valeurs inconnues.
export const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// Rattache chaque statut incident a une phase de workflow lisible.
export const STATUS_PHASE: Record<string, string> = {
  OPEN: "BACKLOG",
  PENDING_VALIDATION: "BACKLOG",
  VALIDATED: "BACKLOG",
  REOPENED: "BACKLOG",
  TRANSFERRED: "BACKLOG",
  ASSIGNED: "IN_PROGRESS",
  IN_PROGRESS: "IN_PROGRESS",
  BLOCKED: "BLOCKED",
  RESOLVED: "DONE",
  CLOSED: "DONE",
  REJECTED: "REJECTED",
};

// Liste les segments du graphe de cohorte incident.
export const COHORT_SEGMENTS = [
  "closedOnTime",
  "closedLate",
  "openInTime",
  "openLate",
  "rejected",
  "cancelled",
] as const;

// Associe chaque segment de cohorte a une couleur stable.
export const COHORT_COLORS: Record<(typeof COHORT_SEGMENTS)[number], string> = {
  closedOnTime: STATUS_COLORS.RESOLVED,
  closedLate: STATUS_COLORS.IN_PROGRESS,
  openInTime: STATUS_COLORS.NEW,
  openLate: STATUS_COLORS.BLOCKED,
  rejected: STATUS_COLORS.REJECTED,
  cancelled: STATUS_COLORS.CANCELLED,
};

// Choisit la couleur d'une criticite incident.
export const getCriticalityColor = (rawName: string, index: number) => {
  return (
    CRITICALITY_COLORS[rawName as keyof typeof CRITICALITY_COLORS] ||
    CHART_SERIES.at(index % CHART_SERIES.length) ||
    CHART_SERIES[0]
  );
};

// Choisit la couleur d'une phase de workflow incident.
export const getPhaseColor = (rawName: string, index: number) => {
  return (
    STATUS_COLORS[rawName as keyof typeof STATUS_COLORS] ||
    CHART_SERIES.at(index % CHART_SERIES.length) ||
    CHART_SERIES[0]
  );
};

// Transforme une distribution cle-valeur en donnees de graphe.
export const toChartData = (
  distribution: Record<string, number>,
): ChartDatum[] =>
  Object.entries(distribution).map(([name, value]) => ({
    name,
    rawName: name,
    value,
  }));

// Construit la distribution des types d'incident pour affichage.
export const buildTypeData = (
  distribution: Record<string, number>,
  t: TFunction,
): ChartDatum[] =>
  toChartData(distribution).map((item) => ({
    ...item,
    name: UUID_PATTERN.test(item.name)
      ? t("dashboard.graphs.unknown_type", { id: item.name.slice(0, 8) })
      : item.name,
  }));

// Construit la distribution des criticites pour affichage.
export const buildCriticalityData = (
  distribution: Record<string, number>,
  t: TFunction,
): ChartDatum[] =>
  toChartData(distribution).map((item) => {
    const key = toConstantCase(item.name);
    return {
      ...item,
      rawName: key,
      name: t(`incidents.criticality.${key}`),
    };
  });

// Regroupe les statuts incident par phase de workflow.
export const buildStatusPhaseData = (
  distribution: Record<string, number>,
  t: TFunction,
): ChartDatum[] => {
  const totals = new Map<string, number>();
  Object.entries(distribution).forEach(([status, count]) => {
    const phase = STATUS_PHASE[toConstantCase(status)] ?? "OTHER";
    totals.set(phase, (totals.get(phase) ?? 0) + count);
  });

  return Array.from(totals.entries())
    .filter(([, value]) => value > 0)
    .map(([phase, value]) => ({
      name: t(`dashboard.graphs.phase.${phase}`),
      rawName: phase,
      value,
    }));
};

// Construit la distribution d'anciennete pour affichage.
export const buildAgingData = (distribution: Record<string, number>) =>
  Object.entries(distribution).map(([name, value]) => ({ name, value }));

// Construit la cohorte incident pour le graphe empile.
export const buildCohortData = (cohortOutcome: Record<string, number>) => [
  COHORT_SEGMENTS.reduce(
    (row, key) => ({ ...row, [key]: cohortOutcome[key] ?? 0 }),
    { name: "cohort" } as Record<string, number | string>,
  ),
];

// Indique si une cohorte ne contient aucune valeur utile.
export const isCohortEmpty = (cohortOutcome: Record<string, number>) =>
  COHORT_SEGMENTS.every((key) => (cohortOutcome[key] ?? 0) === 0);

// Complete une serie mensuelle avec les mois absents.
export const buildMonthlySeries = (series: MonthlyMetric[], t: TFunction) => {
  const byMonth = new Map(series.map((m) => [m.month, m.value]));
  return Array.from({ length: 12 }, (_, i) => ({
    month: t(`dashboard.months.${i + 1}`),
    value: Number((byMonth.get(i + 1) ?? 0).toFixed(2)),
  }));
};

// Calcule la largeur d'un graphe dans une paire responsive.
export const getPairedGraphColSpan = (index: number, total: number) => {
  if (total <= 1) {
    return 24;
  }
  return total % 2 === 1 && index === total - 1 ? 24 : 12;
};
