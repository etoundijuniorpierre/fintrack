// Configuration par defaut et structure des widgets du tableau de bord.

import type { DashboardConfig } from "../../../types/dashboard";
import { WidgetType } from "../../../types/dashboard";
import { getVisibleWidgets } from "../widgetPermissions/widgetPermissions";

// Construit la cle de stockage des preferences d'un utilisateur.
const getStorageKey = (userId: string): string => `dashboard_config_${userId}`;

// Charge les preferences de widgets du tableau de bord.
export const loadDashboardConfig = (userId: string): DashboardConfig | null => {
  try {
    const raw = localStorage.getItem(getStorageKey(userId));
    if (raw === null) return null;
    return JSON.parse(raw) as DashboardConfig;
  } catch {
    return null;
  }
};

// Persiste les preferences de widgets du tableau de bord.
export const saveDashboardConfig = (config: DashboardConfig): void => {
  localStorage.setItem(getStorageKey(config.userId), JSON.stringify(config));
};

// Supprime les preferences de widgets d'un utilisateur.
export const resetDashboardConfig = (userId: string): void => {
  localStorage.removeItem(getStorageKey(userId));
};

// Filtre les widgets persistants selon les permissions courantes.
export const getEffectiveWidgets = (
  userId: string,
  userPermissions: string[],
): WidgetType[] => {
  const config = loadDashboardConfig(userId);

  if (config === null) {
    return getVisibleWidgets(userPermissions);
  }

  return config.visibleWidgets.filter((widget) =>
    getVisibleWidgets(userPermissions).includes(widget),
  );
};
