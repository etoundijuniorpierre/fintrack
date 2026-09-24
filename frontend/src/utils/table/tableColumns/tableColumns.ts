/**
 * tableColumns — utilitaires centralisés pour les colonnes Ant Design
 *
 * Toutes les tables du projet doivent importer `commonColumnProps` depuis ici
 * plutôt que de le redéfinir localement.
 */
import type { ColumnType } from "antd/es/table";

// Decrit les proprietes communes a toutes les colonnes.
// Centrage horizontal du contenu (header + cellule)
export const commonColumnProps = {
  align: "center" as const,
  filterOnClose: false,
} satisfies Partial<ColumnType<Record<string, unknown>>>;

// Configure la colonne des actions a droite du tableau.
// Largeur par defaut : 100px. Passer `width` pour surcharger.
export const actionsColumnProps = (width = 100) => ({
  ...commonColumnProps,
  key: "actions",
  fixed: "right" as const,
  width,
});
