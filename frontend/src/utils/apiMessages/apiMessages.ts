// Constantes et dictionnaires de messages traduits pour les retours API.

import axios, { AxiosError } from "axios";
import i18n from "../../i18n";

// Cle de repli utilisee quand aucune information d'erreur exploitable n'est disponible.
const DEFAULT_ERROR_KEY = "common.unknown_error";

// Recupere la traduction associee a la cle donnee, ou une cle de repli si i18n n'est pas initialise.
const safeTranslate = (key: string): string => {
  const translated = i18n.t(key);
  if (translated !== key) {
    return translated;
  }
  const fallback = i18n.t(DEFAULT_ERROR_KEY);
  return fallback === DEFAULT_ERROR_KEY ? DEFAULT_ERROR_KEY : fallback;
};

// Decrit le contrat d'erreur renvoye par le backend.
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  correlationId: string;
  details?: Record<string, unknown>;
}

// Decrit les details metier associes a une erreur de connexion.
export interface LoginErrorDetails {
  failed_login_attempts?: number;
  max_failed_attempts?: number;
}

// Decrit une reponse API standard contenant un message de succes.
export interface ApiSuccessResponse<T = unknown> {
  timestamp?: string;
  status?: number;
  message?: string;
  data?: T;
}

// Extrait le message d'erreur le plus utile pour l'interface.
export const getApiErrorMessage = (error: unknown): string => {
  // Le backend localise deja ses messages (header Accept-Language) : on les utilise tels quels.
  if (isApiError(error)) {
    const msg = error.response?.data?.message || error.message || "";
    if (
      msg === "Bad credentials" ||
      msg.includes("401") ||
      msg.includes("403")
    ) {
      return safeTranslate("auth.invalid_credentials");
    }
    return msg || safeTranslate("common.unknown_error");
  }

  // Erreur Axios sans reponse = panne reseau ou delai d'attente depasse.
  if (axios.isAxiosError(error) && !error.response) {
    return safeTranslate("common.network_error");
  }

  if (axios.isAxiosError(error) && error.response) {
    if (error.response.status === 401 || error.response.status === 403) {
      return safeTranslate("auth.invalid_credentials");
    }
  }

  if (error instanceof Error) {
    if (
      error.message === "Bad credentials" ||
      error.message.includes("401") ||
      error.message.includes("403")
    ) {
      return safeTranslate("auth.invalid_credentials");
    }
    return error.message;
  }

  return safeTranslate("common.unknown_error");
};

// Extrait le message de succes optionnel d'une reponse API.
export const getApiSuccessMessage = (response: unknown): string | null => {
  if (response && typeof response === "object" && "message" in response) {
    return (response as { message?: string }).message || null;
  }
  return null;
};

// Identifie les erreurs Axios qui respectent le format d'erreur backend.
export const isApiError = (error: unknown): error is AxiosError<ApiError> => {
  return axios.isAxiosError(error) && !!error.response?.data?.message;
};

// Extrait le code metier associe a une erreur API.
export const getApiErrorCode = (error: unknown): string | null => {
  if (isApiError(error)) {
    return error.response?.data?.code || null;
  }
  return null;
};

// Extrait le statut HTTP associe a une erreur API.
export const getApiErrorStatus = (error: unknown): number | null => {
  if (isApiError(error)) {
    return error.response?.status || null;
  }
  return null;
};

// Extrait le nombre de tentatives echouees apres un refus de connexion.
export const getFailedLoginAttempts = (error: unknown): number | null => {
  if (isApiError(error)) {
    const details = error.response?.data?.details;
    if (details && typeof details.failed_login_attempts === "number") {
      return details.failed_login_attempts;
    }
  }
  return null;
};

// Extrait la limite maximale de tentatives avant verrouillage.
export const getMaxFailedAttempts = (error: unknown): number | null => {
  if (isApiError(error)) {
    const details = error.response?.data?.details;
    if (details && typeof details.max_failed_attempts === "number") {
      return details.max_failed_attempts;
    }
  }
  return null;
};
