// Routage performance : centralise les imports lazy et leur prechargement opportuniste.
import { APP_ROUTES } from "../utils/constants";

type RouteLoader = () => Promise<unknown>;

// Convertit un export nomme en module compatible avec React.lazy.
const toDefault =
  <T extends Record<string, unknown>, K extends keyof T>(
    loader: () => Promise<T>,
    key: K,
  ): (() => Promise<{ default: T[K] }>) =>
  () =>
    loader().then((module) => ({ default: module[key] }));

export const routeLoaders = {
  login: () => import("../pages/Login/Login"),
  dashboard: () => import("../pages/Dashboard/Dashboard"),
  superAdmin: toDefault(() => import("../pages/SuperAdmin"), "SuperAdminPage"),
  users: () => import("../pages/Users/ViewList/ViewListUsers"),
  createUser: () => import("../pages/Users/Create/CreateUser"),
  viewUser: () => import("../pages/Users/View/ViewUser"),
  incidents: () => import("../pages/Incidents/ViewList/ViewListIncidents"),
  createIncident: () => import("../pages/Incidents/Create/CreateIncident"),
  viewIncident: () => import("../pages/Incidents/View/ViewIncident"),
  myProfile: () => import("../pages/MyProfile/MyProfile"),
  settings: () => import("../pages/Settings/Settings"),
  audit: () => import("../pages/Audit/AuditPage"),
  reports: () => import("../pages/Reports/ReportPage"),
  notifications: () => import("../pages/Notifications/NotificationPage"),
  help: toDefault(() => import("../pages/Help"), "Help"),
  roleForm: () => import("../pages/Settings/pages/Roles/Form/RoleFormPage"),
  roleDetails: () =>
    import("../pages/Settings/pages/Roles/View/RoleDetailsPage"),
  agencyForm: () =>
    import("../pages/Settings/pages/Agencies/Form/AgencyFormPage"),
  agencyDetails: () =>
    import("../pages/Settings/pages/Agencies/View/AgencyDetailsPage"),
  serviceForm: () =>
    import("../pages/Settings/pages/Services/Form/ServiceFormPage"),
  serviceDetails: () =>
    import("../pages/Settings/pages/Services/View/ServiceDetailsPage"),
  incidentTypeForm: () =>
    import("../pages/Settings/pages/IncidentTypes/Form/IncidentTypeFormPage"),
  incidentTypeDetails: () =>
    import("../pages/Settings/pages/IncidentTypes/View/IncidentTypeDetailsPage"),
  reportScheduleForm: () =>
    import("../pages/Settings/pages/ReportSchedules/Form/ReportScheduleFormPage"),
  reportScheduleDetails: () =>
    import("../pages/Settings/pages/ReportSchedules/View/ReportScheduleDetailsPage"),
};

const loadedRoutes = new Map<RouteLoader, Promise<unknown>>();

// Charge un module de route une seule fois et ignore les echecs de prechargement.
const preload = (loader: RouteLoader) => {
  if (!loadedRoutes.has(loader)) {
    loadedRoutes.set(
      loader,
      loader().catch((error) => {
        loadedRoutes.delete(loader);
        if (import.meta.env.DEV) {
          console.warn("Route preload failed", error);
        }
        return undefined;
      }),
    );
  }
  return loadedRoutes.get(loader);
};

// Retrouve le module lazy correspondant a une URL applicative.
const resolveLoader = (pathname: string): RouteLoader | undefined => {
  if (pathname === APP_ROUTES.LOGIN) return routeLoaders.login;
  if (pathname === APP_ROUTES.DASHBOARD) return routeLoaders.dashboard;
  if (pathname === APP_ROUTES.SUPER_ADMIN) return routeLoaders.superAdmin;
  if (pathname === APP_ROUTES.USERS) return routeLoaders.users;
  if (pathname === `${APP_ROUTES.USERS}/create`) return routeLoaders.createUser;
  if (pathname.startsWith(`${APP_ROUTES.USERS}/`)) return routeLoaders.viewUser;
  if (pathname === APP_ROUTES.INCIDENTS) return routeLoaders.incidents;
  if (pathname === `${APP_ROUTES.INCIDENTS}/create`)
    return routeLoaders.createIncident;
  if (pathname.startsWith(`${APP_ROUTES.INCIDENTS}/`))
    return routeLoaders.viewIncident;
  if (pathname === APP_ROUTES.REPORTS) return routeLoaders.reports;
  if (pathname === APP_ROUTES.AUDIT) return routeLoaders.audit;
  if (pathname === APP_ROUTES.NOTIFICATIONS) return routeLoaders.notifications;
  if (pathname === APP_ROUTES.HELP) return routeLoaders.help;
  if (pathname === APP_ROUTES.PROFILE) return routeLoaders.myProfile;
  if (pathname === APP_ROUTES.SETTINGS) return routeLoaders.settings;
  if (
    pathname.includes("/settings/roles/new") ||
    pathname.includes("/settings/roles/edit/")
  )
    return routeLoaders.roleForm;
  if (pathname.includes("/settings/roles/")) return routeLoaders.roleDetails;
  if (
    pathname.includes("/settings/agencies/new") ||
    pathname.includes("/settings/agencies/edit/")
  )
    return routeLoaders.agencyForm;
  if (pathname.includes("/settings/agencies/"))
    return routeLoaders.agencyDetails;
  if (
    pathname.includes("/settings/services/new") ||
    pathname.includes("/settings/services/edit/")
  )
    return routeLoaders.serviceForm;
  if (pathname.includes("/settings/services/"))
    return routeLoaders.serviceDetails;
  if (
    pathname.includes("/settings/incident-types/new") ||
    pathname.includes("/settings/incident-types/edit/")
  )
    return routeLoaders.incidentTypeForm;
  if (pathname.includes("/settings/incident-types/"))
    return routeLoaders.incidentTypeDetails;
  if (
    pathname.includes("/settings/report-schedules/new") ||
    pathname.includes("/settings/report-schedules/edit/")
  )
    return routeLoaders.reportScheduleForm;
  if (pathname.includes("/settings/report-schedules/"))
    return routeLoaders.reportScheduleDetails;
  return undefined;
};

// Precharge le chunk frontend associe a une route.
export const preloadRouteChunk = (pathname: string) => {
  const loader = resolveLoader(pathname);
  if (!loader) return undefined;
  return preload(loader);
};

// Precharge plusieurs chunks de navigation en arriere-plan.
export const preloadRouteChunks = (pathnames: string[]) => {
  pathnames.forEach((pathname) => {
    void preloadRouteChunk(pathname);
  });
};
