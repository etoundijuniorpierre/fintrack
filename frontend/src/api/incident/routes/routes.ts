// Routes de navigation front-end du module incidents.
import { APP_ROUTES } from "../../../utils/constants";

// Expose la constante INCIDENT_ROUTES utilisee par routes.
export const INCIDENT_ROUTES = {
  LIST: APP_ROUTES.INCIDENTS,
  CREATE: `${APP_ROUTES.INCIDENTS}/create`,
  VIEW: (id: string) => `${APP_ROUTES.INCIDENTS}/${id}`,
} as const;
