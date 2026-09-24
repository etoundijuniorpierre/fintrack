// Utilitaire pour la gestion des statuts actifs/inactifs.

import { Form, type FormInstance } from "antd";

// Nomme le champ de statut actif partage par les formulaires.
export const ACTIVE_STATUS_FIELD = "isActive" as const;

// Type les valeurs de formulaire qui portent un statut actif.
type ActiveStatusValues = Partial<Record<typeof ACTIVE_STATUS_FIELD, boolean>>;

// Lit le champ actif d'un formulaire avec une valeur de repli.
export const useActiveStatusField = <T extends ActiveStatusValues>(
  form: FormInstance<T>,
  fallback = true,
): boolean => {
  const value = Form.useWatch(ACTIVE_STATUS_FIELD, form);
  return typeof value === "boolean" ? value : fallback;
};
