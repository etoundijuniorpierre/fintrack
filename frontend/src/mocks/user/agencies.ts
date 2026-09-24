// Donnees simulees pour les agences.

import type { Agency } from "../../api/user/types";

// Fabrique une fixture de test pour agences.
export const makeAgency = (overrides: Partial<Agency> = {}): Agency => ({
  id: "agency-1",
  name: "Agence Principale",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

// Fabrique une fixture de test pour agences.
export const makeAgencies = (count: number = 2): Agency[] => {
  return Array.from({ length: count }, (_, index) =>
    makeAgency({
      id: `agency-${index + 1}`,
      name: index === 0 ? "Agence Principale" : "Agence Nord",
    }),
  );
};
