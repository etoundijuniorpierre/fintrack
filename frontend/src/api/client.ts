// Client Axios configure : injection du token, refresh automatique sur 401.
import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { tokenManager } from "../utils/tokenManager/tokenManager";
import { API_BASE_URL, API_ROUTES } from "./routes";
import i18n from "../i18n";

// Normalise la langue de l'interface pour l'en-tete HTTP compris par les API.
export const resolveApiLanguage = (): "fr" | "en" => {
  const currentLanguage = i18n.resolvedLanguage || i18n.language || "fr";
  return currentLanguage.startsWith("en") ? "en" : "fr";
};

// Configure l instance Axios partagee par les appels API.
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  timeout: 30000, // 30s — évite les requêtes bloquées indéfiniment
  headers: {
    "Content-Type": "application/json",
  },
});
// Intercepteur de requete : ajoute le token Bearer et la langue courante.
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenManager.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    config.headers["Accept-Language"] = resolveApiLanguage();

    // Pour un envoi multipart (FormData), on retire le Content-Type fixe par
    // defaut (`application/json`) sur l'instance axios : sans cela, la requete
    // partirait sans boundary multipart, et Spring (@RequestPart) ne pourrait
    // pas lire le fichier 500. En supprimant l'en-tete, le navigateur le
    // recalcule lui-meme avec la boundary correcte.
    if (typeof FormData !== "undefined" && config.data instanceof FormData) {
      delete config.headers["Content-Type"];
    }

    return config;
  },
  (error: AxiosError) => Promise.reject(error),
);

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

// Rejoue (ou rejette) les requetes mises en attente pendant un refresh en cours.
const processQueue = (
  error: AxiosError | null,
  token: string | null = null,
) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  failedQueue = [];
};

// Intercepteur de reponse : rafraichit le token sur 401 et rejoue la requete.
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    const isAuthEndpoint =
      originalRequest.url?.includes("/auth/login") ||
      originalRequest.url?.includes("/auth/refresh") ||
      originalRequest.url?.includes("/auth/logout");

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !isAuthEndpoint
    ) {
      // Un refresh est deja en cours : on met la requete en file d'attente.
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          // Marque la requete comme deja rejouee : si elle echoue de nouveau
          // en 401, elle ne relancera pas un nouveau cycle de refresh.
          originalRequest._retry = true;
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return apiClient(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { data } = await apiClient.post(API_ROUTES.AUTH.REFRESH);
        const newToken: string = data.token;
        tokenManager.setToken(newToken);
        processQueue(null, newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        tokenManager.removeToken();
        processQueue(refreshError as AxiosError, null);
        window.location.href = "/login";
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

export default apiClient;
