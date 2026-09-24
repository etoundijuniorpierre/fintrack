// Hook React pour l'administration et la lecture des roles.

import { useQuery } from "@tanstack/react-query";
import { roleApi } from "../../api/user";
import { QUERY_KEYS } from "../../utils/constants";

// Charge le referentiel des roles utilisateurs.
export const useRoles = (enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.ROLES.ALL,
    queryFn: ({ signal }) => roleApi.getAll(signal),
    staleTime: 5 * 60 * 1000,
    enabled,
  });
};
