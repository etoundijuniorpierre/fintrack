// Hook React pour charger la liste globale des permissions.

import { useQuery } from "@tanstack/react-query";
import { permissionApi } from "../../api/user/permissionApi/permissionApi";
import { QUERY_KEYS } from "../../utils/constants";

// Charge le catalogue des permissions disponibles.
export const usePermissions = (enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.PERMISSIONS.ALL,
    queryFn: ({ signal }) => permissionApi.getAll(signal),
    staleTime: 10 * 60 * 1000,
    enabled,
  });
};
