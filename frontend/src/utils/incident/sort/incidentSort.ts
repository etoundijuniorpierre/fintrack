// Utilitaire de tri : presets d'ordre du tableau des incidents.

import type { SortPresetConfig } from "../../table/sorting/tableSorting";

export type IncidentSortPreset = "alphabetical" | "seniority";

export const INCIDENT_SORT_PRESETS: Record<
  IncidentSortPreset,
  SortPresetConfig
> = {
  alphabetical: { field: "title", order: "asc" },
  seniority: { field: "createdAt", order: "desc" },
};
