// Hook React pour mettre a jour le profil de l'utilisateur connecte.

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { profileApi } from "../../../api/user/profileApi/profileApi";
import { QUERY_KEYS } from "../../../utils/constants";
import type { ProfileUpdateRequest } from "../../../api/user/types";

// Charge le profil de l'utilisateur connecte.
export const useMyProfile = (id: string) =>
  useQuery({
    queryKey: QUERY_KEYS.MY_PROFILE(id),
    queryFn: ({ signal }) => profileApi.getProfile(id, signal),
    enabled: !!id,
  });

// Declenche une mise a jour via l'API et synchronise les donnees liees.
export const useUpdateProfile = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ProfileUpdateRequest }) =>
      profileApi.updateProfile(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.MY_PROFILE(id) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.USERS.DETAIL(id) });
    },
  });
};
