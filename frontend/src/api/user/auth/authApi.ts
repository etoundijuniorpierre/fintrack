// Service API d'authentification : connexion, deconnexion, rafraichissement du token et changement de mot de passe.
import apiClient from "../../client";
import { API_ROUTES } from "../../routes";
import type { AuthResponse, LoginCredentials } from "../types";

// Modele la structure change password request manipulee par le frontend.
interface ChangePasswordRequest {
  currentPassword?: string;
  newPassword: string;
}

// Prepare auth api pour authentification API.
export const authApi = {
  login: async (credentials: LoginCredentials): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>(
      API_ROUTES.AUTH.LOGIN,
      credentials,
    );
    return data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post(API_ROUTES.AUTH.LOGOUT);
  },

  refreshToken: async (): Promise<AuthResponse> => {
    const { data } = await apiClient.post<AuthResponse>(
      API_ROUTES.AUTH.REFRESH,
    );
    return data;
  },

  changePassword: async (
    userId: string,
    request: ChangePasswordRequest,
  ): Promise<void> => {
    await apiClient.post(API_ROUTES.AUTH.CHANGE_PASSWORD(userId), request);
  },

  contactAdmin: async (request: {
    username: string;
    subject: string;
    message: string;
  }): Promise<void> => {
    await apiClient.post(API_ROUTES.AUTH.CONTACT_ADMIN, request);
  },
};
