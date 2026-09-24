// Donnees simulees pour les services.

import type { Service } from "../../api/user/types";

// Fabrique une fixture de test pour services.
export const makeService = (overrides: Partial<Service> = {}): Service => ({
  id: "service-1",
  name: "Informatique",
  description: "Service informatique",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

// Fabrique une fixture de test pour services.
export const makeServices = (count: number = 2): Service[] => {
  return Array.from({ length: count }, (_, index) =>
    makeService({
      id: `service-${index + 1}`,
      name: index === 0 ? "Informatique" : "Comptabilité",
    }),
  );
};
