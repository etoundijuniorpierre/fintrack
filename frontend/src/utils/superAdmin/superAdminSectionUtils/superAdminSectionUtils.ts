// Regroupe les helpers de lecture des sections Super Admin.

export type RowMap = Record<string, unknown>;

// Identifie les objets exploitables comme lignes de tableau.
export const isRowMap = (value: unknown): value is RowMap =>
  typeof value === "object" && value !== null && !Array.isArray(value);

// Convertit une valeur inconnue en liste de lignes.
export const asArray = (value: unknown): RowMap[] =>
  Array.isArray(value) ? value.filter(isRowMap) : [];

// Convertit une valeur inconnue en ligne exploitable.
export const asRecord = (value: unknown): RowMap =>
  isRowMap(value) ? value : {};

// Convertit une valeur inconnue en entrees cle-valeur.
export const asEntries = (value: unknown): Array<[string, unknown]> =>
  Object.entries(asRecord(value));

// Convertit une valeur inconnue en nombre avec repli.
export const asNumber = (value: unknown, fallback = 0) => {
  if (typeof value === "number") return value;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
};

// Convertit une valeur inconnue en texte avec repli.
export const asString = (value: unknown, fallback = "-") =>
  typeof value === "string" && value.trim() ? value : fallback;

// Calcule un pourcentage entier avec protection contre la division par zero.
export const asPercent = (value: number, total: number) => {
  if (total <= 0) return 0;
  return Math.round((value / total) * 100);
};

// Associe un statut technique a une couleur Ant Design.
export const statusTagColor = (status: unknown) => {
  const value = asString(status, "").toUpperCase();
  if (
    [
      "UP",
      "SENT",
      "AVAILABLE",
      "SUCCESS",
      "READY",
      "TRIGGERED",
      "CLEAN",
    ].includes(value)
  )
    return "success";
  if (["DOWN", "FAILED", "FAILURE", "DISABLED"].includes(value)) return "error";
  if (["PENDING", "GENERATING", "NEEDSAUTOMATION", "MANUAL"].includes(value))
    return "processing";
  return "default";
};

// Associe une severite de controle a une couleur d'alerte.
export const severityColor = (severity: unknown) => {
  if (severity === "high") return "red";
  if (severity === "medium") return "orange";
  return "blue";
};
