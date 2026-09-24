// Utilitaire de tri : définit les raccourcis du tableau des utilisateurs.

import {
  resolveSortMode,
  type SortPresetConfig,
} from "../../table/sorting/tableSorting";

export type UserSortPreset = "alphabetical" | "seniority";

export const USER_SORT_PRESETS: Record<UserSortPreset, SortPresetConfig> = {
  alphabetical: { field: "lastName", order: "asc" },
  seniority: { field: "createdAt", order: "desc" },
};

/**Raccourci specifique aux presets utilisateurs.*/
export const resolveUserSortPreset = (
  field: string,
): UserSortPreset | undefined =>
  resolveSortMode(USER_SORT_PRESETS, field);
