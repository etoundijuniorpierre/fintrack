// Composant racine : configure le theme, le routage et les gardes de permissions.
import React, { Suspense } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useParams,
} from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
// NB : `dayjs.locale('fr')` et `import 'dayjs/locale/fr'` sont desormais
// effectues dans `main.tsx` avant l'import de ce module, pour garantir que le
// locale dayjs est actif au moment ou antd/rc-picker evaluent leurs modules.
import MainLayout from "./components/Layout/MainLayout";
import ErrorBoundary from "./components/ErrorBoundary/ErrorBoundary";
import { PageSkeleton } from "./components/Loading/PageSkeleton";
import { APP_ROUTES } from "./utils/constants";
import { useAuthStore } from "./store/authStore/authStore";
import { useSessionBootstrap } from "./hooks/auth/useAuth";
import {
  USER_CREATE_PERMISSIONS,
  USER_VIEW_PERMISSIONS,
  INCIDENT_ACCESS_PERMISSIONS,
  PERMISSIONS,
} from "./utils/permissions/permissions";
import { themeConfig } from "./theme";
import "./index.scss";
import "./App.scss";
import { ConfigProvider, App as AntdApp, theme } from "antd";
import frFR from "antd/locale/fr_FR";
import enUS from "antd/locale/en_US";
import { useThemeStore } from "./store/themeStore";
import { useTranslation } from "react-i18next";
import dayjs from "dayjs";
import "dayjs/locale/en";
import { routeLoaders } from "./routes/routePreloading";

// Centralise la logique d'interface liee a connexion.
const Login = React.lazy(routeLoaders.login);
// Centralise la logique d'interface liee a dashboard.
const Dashboard = React.lazy(routeLoaders.dashboard);
// Centralise la logique d'interface liee a super administrateur page.
const SuperAdminPage = React.lazy(routeLoaders.superAdmin);
// Charge la page de gestion des utilisateurs de facon differee.
const Users = React.lazy(routeLoaders.users);
// Prepare utilisateur pour le flux courant.
const CreateUser = React.lazy(routeLoaders.createUser);
// Centralise la logique d'interface liee a view utilisateur.
const ViewUser = React.lazy(routeLoaders.viewUser);
// Centralise la logique d'interface liee a liste incidents.
const ViewListIncidents = React.lazy(routeLoaders.incidents);
// Prepare incident pour le flux courant.
const CreateIncident = React.lazy(routeLoaders.createIncident);
// Centralise la logique d'interface liee a view incident.
const ViewIncident = React.lazy(routeLoaders.viewIncident);
// Centralise la logique d'interface liee a mon profil.
const MyProfile = React.lazy(routeLoaders.myProfile);
// Centralise la logique d'interface liee a settings.
const Settings = React.lazy(routeLoaders.settings);
// Centralise la logique d'interface liee a audit page.
const AuditPage = React.lazy(routeLoaders.audit);
// Centralise la logique d'interface liee a rapport page.
const ReportPage = React.lazy(routeLoaders.reports);
// Centralise la logique d'interface liee a notification page.
const NotificationPage = React.lazy(routeLoaders.notifications);
// Centralise la logique d'interface liee a help.
const Help = React.lazy(routeLoaders.help);
// Centralise la logique d'interface liee a role form page.
const RoleFormPage = React.lazy(routeLoaders.roleForm);
// Centralise la logique d'interface liee a role details page.
const RoleDetailsPage = React.lazy(routeLoaders.roleDetails);
// Centralise la logique d'interface liee a agence form page.
const AgencyFormPage = React.lazy(routeLoaders.agencyForm);
// Centralise la logique d'interface liee a agence details page.
const AgencyDetailsPage = React.lazy(routeLoaders.agencyDetails);
// Centralise la logique d'interface liee a service form page.
const ServiceFormPage = React.lazy(routeLoaders.serviceForm);
// Centralise la logique d'interface liee a service details page.
const ServiceDetailsPage = React.lazy(routeLoaders.serviceDetails);
// Centralise la logique d'interface liee a incident type form page.
const IncidentTypeFormPage = React.lazy(routeLoaders.incidentTypeForm);
// Centralise la logique d'interface liee a incident type details page.
const IncidentTypeDetailsPage = React.lazy(routeLoaders.incidentTypeDetails);
// Centralise la logique d'interface liee a rapport planification form page.
const ReportScheduleFormPage = React.lazy(routeLoaders.reportScheduleForm);
// Centralise la logique d'interface liee a rapport planification details page.
const ReportScheduleDetailsPage = React.lazy(
  routeLoaders.reportScheduleDetails,
);

// Client React Query partage : un seul reessai, donnees fraiches pendant 5 min.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 5 * 60 * 1000,
      gcTime: 30 * 60 * 1000,
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
    },
  },
});

// Redirige l'ancienne route d'edition vers la fiche utilisateur en mode edition.
const EditUserRedirect = () => {
  const { id } = useParams<{ id: string }>();
  return (
    <Navigate to={`/dashboard/users/${id}`} state={{ edit: true }} replace />
  );
};

// Definit les proprietes attendues par le composant PrivateRoute.
interface PrivateRouteProps {
  children: React.ReactNode;
}

// Definit les proprietes attendues par le composant PermissionGuard.
interface PermissionGuardProps {
  permission?: string;
  permissions?: readonly string[];
  fallback: React.ReactNode;
  children: React.ReactNode;
}

// Affiche les enfants si l'utilisateur detient la permission requise, sinon la vue de repli.
const PermissionGuard = ({
  permission,
  permissions,
  fallback,
  children,
}: PermissionGuardProps) => {
  const { hasPermission } = useAuthStore();
  const allowed = permission
    ? hasPermission(permission)
    : (permissions?.some((candidate) => hasPermission(candidate)) ?? false);

  return allowed ? <>{children}</> : <>{fallback}</>;
};

// Reserve aux titulaires de SETTINGS_SYSTEM, comme la page Parametres parente :
// evite qu'un utilisateur non autorise n'ouvre une sous-page par URL directe.
const settingsGuard = (element: React.ReactNode) => (
  <PermissionGuard
    permission="SETTINGS_SYSTEM"
    fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
  >
    {element}
  </PermissionGuard>
);

// Protege une route : redirige vers la connexion si l'utilisateur n'est pas authentifie.
const PrivateRoute = ({ children }: PrivateRouteProps) => {
  const { isAuthenticated } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to={APP_ROUTES.LOGIN} replace />;
  }

  return <>{children}</>;
};

// Restaure le token d'acces (refresh une fois) avant de monter les routes :
// tant que le bootstrap n'est pas termine, on affiche le loader pour eviter
// que les pages protegees ne declenchent des requetes sans token (storm de 401).
const AppRoutes = () => {
  const ready = useSessionBootstrap();

  if (!ready) {
    return <PageSkeleton />;
  }

  return (
    <Routes>
      <Route path={APP_ROUTES.LOGIN} element={<Login />} />

      <Route
        path="/"
        element={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
      />

      <Route
        path={APP_ROUTES.DASHBOARD}
        element={
          <PrivateRoute>
            <MainLayout />
          </PrivateRoute>
        }
      >
        <Route index element={<Dashboard />} />

        <Route
          path="super-admin"
          element={
            <PermissionGuard
              permission={PERMISSIONS.USER.CREATE_ADMIN}
              fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
            >
              <SuperAdminPage />
            </PermissionGuard>
          }
        />

        <Route
          path="users"
          element={
            <PermissionGuard
              permissions={USER_VIEW_PERMISSIONS}
              fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
            >
              <Users />
            </PermissionGuard>
          }
        />
        <Route
          path="users/create"
          element={
            <PermissionGuard
              permissions={USER_CREATE_PERMISSIONS}
              fallback={<Navigate to={APP_ROUTES.USERS} replace />}
            >
              <CreateUser />
            </PermissionGuard>
          }
        />
        <Route
          path="users/:id"
          element={
            <PermissionGuard
              permissions={USER_VIEW_PERMISSIONS}
              fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
            >
              <ViewUser />
            </PermissionGuard>
          }
        />
        <Route path="users/edit/:id" element={<EditUserRedirect />} />

        <Route
          path="incidents"
          element={
            <PermissionGuard
              permissions={INCIDENT_ACCESS_PERMISSIONS}
              fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
            >
              <ViewListIncidents />
            </PermissionGuard>
          }
        />
        <Route
          path="incidents/create"
          element={
            <PermissionGuard
              permission="INCIDENT_CREATE"
              fallback={<Navigate to={APP_ROUTES.INCIDENTS} replace />}
            >
              <CreateIncident />
            </PermissionGuard>
          }
        />
        <Route
          path="incidents/:id"
          element={
            <PermissionGuard
              permissions={INCIDENT_ACCESS_PERMISSIONS}
              fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
            >
              <ViewIncident />
            </PermissionGuard>
          }
        />
        <Route
          path="reports"
          element={
            <PermissionGuard
              permission="REPORT_VIEW_OWN"
              fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
            >
              <ReportPage />
            </PermissionGuard>
          }
        />
        <Route
          path="audit"
          element={
            <PermissionGuard
              permission="AUDIT_VIEW"
              fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
            >
              <AuditPage />
            </PermissionGuard>
          }
        />
        <Route
          path="notifications"
          element={
            <PermissionGuard
              permissions={[
                PERMISSIONS.NOTIFICATION.VIEW_OWN,
                PERMISSIONS.NOTIFICATION.VIEW_ALL,
                PERMISSIONS.NOTIFICATION.MANAGE,
              ]}
              fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
            >
              <NotificationPage />
            </PermissionGuard>
          }
        />
        <Route path="help" element={<Help />} />
        <Route
          path="settings"
          element={
            <PermissionGuard
              permission="SETTINGS_SYSTEM"
              fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
            >
              <Settings />
            </PermissionGuard>
          }
        />
        <Route
          path="settings/roles/new"
          element={settingsGuard(<RoleFormPage />)}
        />
        <Route
          path="settings/roles/edit/:id"
          element={settingsGuard(<RoleFormPage />)}
        />
        <Route
          path="settings/roles/:id"
          element={settingsGuard(<RoleDetailsPage />)}
        />

        <Route
          path="settings/agencies/new"
          element={settingsGuard(<AgencyFormPage />)}
        />
        <Route
          path="settings/agencies/edit/:id"
          element={settingsGuard(<AgencyFormPage />)}
        />
        <Route
          path="settings/agencies/:id"
          element={settingsGuard(<AgencyDetailsPage />)}
        />

        <Route
          path="settings/services/new"
          element={settingsGuard(<ServiceFormPage />)}
        />
        <Route
          path="settings/services/edit/:id"
          element={settingsGuard(<ServiceFormPage />)}
        />
        <Route
          path="settings/services/:id"
          element={settingsGuard(<ServiceDetailsPage />)}
        />

        <Route
          path="settings/incident-types/new"
          element={settingsGuard(<IncidentTypeFormPage />)}
        />
        <Route
          path="settings/incident-types/edit/:id"
          element={settingsGuard(<IncidentTypeFormPage />)}
        />
        <Route
          path="settings/incident-types/:id"
          element={settingsGuard(<IncidentTypeDetailsPage />)}
        />

        <Route
          path="settings/report-schedules/new"
          element={settingsGuard(<ReportScheduleFormPage />)}
        />
        <Route
          path="settings/report-schedules/edit/:id"
          element={settingsGuard(<ReportScheduleFormPage />)}
        />
        <Route
          path="settings/report-schedules/:id"
          element={settingsGuard(<ReportScheduleDetailsPage />)}
        />
        <Route
          path="profile"
          element={
            <PermissionGuard
              permission="USER_MANAGE_PROFILE"
              fallback={<Navigate to={APP_ROUTES.DASHBOARD} replace />}
            >
              <MyProfile />
            </PermissionGuard>
          }
        />
      </Route>

      <Route path="*" element={<Navigate to={APP_ROUTES.LOGIN} replace />} />
    </Routes>
  );
};

// Rend le composant App pour l'interface app.
const App = () => {
  const { i18n } = useTranslation();
  const { mode } = useThemeStore();

  // Choix dynamique du locale Ant Design en fonction de la langue actuelle
  const antdLocale = i18n.language === "en" ? enUS : frFR;

  // Synchronisation dynamique du locale Day.js lors du changement de langue
  React.useEffect(() => {
    dayjs.locale(i18n.language);
  }, [i18n.language]);

  // Synchronize SCSS theme attribute
  React.useEffect(() => {
    document.documentElement.setAttribute("data-theme", mode);
  }, [mode]);

  return (
    <ConfigProvider
      theme={{
        ...themeConfig,
        token:
          mode === "dark"
            ? {
                ...themeConfig.token,
                colorPrimary: "#475569",
                colorPrimaryHover: "#64748B",
                colorPrimaryActive: "#334155",
              }
            : themeConfig.token,
        algorithm:
          mode === "dark" ? theme.darkAlgorithm : theme.defaultAlgorithm,
      }}
      locale={antdLocale}
    >
      <AntdApp>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            {/* Filet de sécurité global : capture toute erreur de rendu, y
                compris hors du tableau de bord (page de connexion, routage). */}
            <ErrorBoundary>
              <Suspense fallback={<PageSkeleton />}>
                <AppRoutes />
              </Suspense>
            </ErrorBoundary>
          </BrowserRouter>
        </QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  );
};

export default App;
