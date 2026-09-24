// Client API pour la gestion du profil utilisateur connecte.

import apiClient from "../../client";
import { USER_SERVICE_ENDPOINTS } from "../endpoints/endpoints";
import type { User, ProfileUpdateRequest } from "../types";

// Prepare profile api pour profil API.
export const profileApi = {
  getProfile: async (id: string, signal?: AbortSignal): Promise<User> => {
    const { data } = await apiClient.get<User>(
      `${USER_SERVICE_ENDPOINTS.USERS.BASE}/${id}`,
      { signal },
    );
    return data;
  },

  updateProfile: async (
    id: string,
    profileData: ProfileUpdateRequest,
  ): Promise<User> => {
    const { data } = await apiClient.patch<User>(
      USER_SERVICE_ENDPOINTS.USERS.PROFILE(id),
      profileData,
    );
    return data;
  },
};
