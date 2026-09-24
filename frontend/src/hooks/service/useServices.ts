// Hook React pour la gestion des departements internes.

import { useQuery } from "@tanstack/react-query";
import { serviceApi } from "../../api/user";
import { QUERY_KEYS } from "../../utils/constants";

// Charge le referentiel des services utilisateurs lorsque l'ecran en a besoin.
export const useServices = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: QUERY_KEYS.SERVICES.ALL,
    queryFn: ({ signal }) => serviceApi.getAll(signal),
    staleTime: 5 * 60 * 1000,
    enabled: options?.enabled ?? true,
  });
};
