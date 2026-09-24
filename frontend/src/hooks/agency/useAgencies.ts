// Hook React pour interagir avec les donnees des agences.

import { useQuery } from "@tanstack/react-query";
import { agencyApi } from "../../api/user";
import { QUERY_KEYS } from "../../utils/constants";

// Charge les agences lorsque l'ecran a besoin du referentiel.
// L'option `enabled` evite un appel API tant que la page n'a pas besoin des agences.
export const useAgencies = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: QUERY_KEYS.AGENCIES.ALL,
    queryFn: ({ signal }) => agencyApi.getAll(signal),
    staleTime: 5 * 60 * 1000,
    enabled: options?.enabled ?? true,
  });
};
