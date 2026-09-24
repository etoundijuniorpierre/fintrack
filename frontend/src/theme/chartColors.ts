/**
 * Couleurs utilisées pour les graphiques (Recharts) afin d'assurer
 * une cohérence de la donnée sur tous les tableaux de bord.
 */

// Palette pour les statuts
export const STATUS_COLORS = {
  NEW: "#6366F1", // Indigo
  IN_PROGRESS: "#F59E0B", // Ambre
  BLOCKED: "#DC2626", // Rouge
  RESOLVED: "#10B981", // Vert
  CLOSED: "#6B7280", // Gris
  REJECTED: "#4B5563", // Gris Foncé
  CANCELLED: "#9CA3AF", // Gris clair
};

// Palette pour la criticité (gradient de température : Froid -> Chaud)
export const CRITICALITY_COLORS = {
  LOW: "#3B82F6", // Bleu clair
  MEDIUM: "#F59E0B", // Ambre
  HIGH: "#EF4444", // Rouge clair
  CRITICAL: "#B91C1C", // Rouge foncé
};

// Séquence de couleurs pour les graphiques à catégories multiples
export const CHART_SERIES = [
  "#6366F1", // Indigo
  "#F59E0B", // Ambre
  "#10B981", // Vert
  "#3B82F6", // Bleu
  "#EC4899", // Rose
  "#8B5CF6", // Violet
  "#14B8A6", // Teal
];
