// Tests frontend : verifie le comportement de audit page.test.

import { screen, fireEvent, waitFor, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../test-utils/renderWithProviders";
import AuditPage from "./AuditPage";
import { useAuth } from "../../hooks/auth/useAuth";

// Cette page est lourde a monter en jsdom (tableau + popover de filtres antd) :
// certains tests coutent ~10 s seuls, et depassent les 30 s globales quand la suite
// complete sature les workers. La marge est accordee ici plutot qu'en relevant le
// seuil global, qui doit rester serre pour les 570 autres fichiers.
// Ce n'est pas un correctif de lenteur : alleger ce montage reste a faire.
vi.setConfig({ testTimeout: 60_000 });

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({ hasPermission: () => true })),
}));

const AUDIT_LOGS = [
  {
    id: "log-1",
    timestamp: "2024-01-15T10:30:00Z",
    user: { id: "u1", username: "jdoe", firstName: "John", lastName: "Doe" },
    username: "jdoe",
    roles: ["ADMIN"],
    action: "LOGIN_SUCCESS",
    resourceType: "USER",
    resourceId: "u1",
    ipAddress: "192.168.1.1",
    userAgent: "Mozilla/5.0",
    status: "SUCCESS",
    details: null,
  },
  {
    id: "log-2",
    timestamp: "2024-01-15T11:00:00Z",
    user: null,
    username: "system",
    roles: [],
    action: "INCIDENT_CREATE",
    resourceType: "INCIDENT",
    resourceId: "inc-1",
    ipAddress: null,
    userAgent: null,
    status: "FAILURE",
    details: { reason: "test" },
  },
];

const AUDIT_ACTIONS = [
  { code: "LOGIN_SUCCESS", name: "Login Success", description: "" },
  { code: "INCIDENT_CREATE", name: "Incident Create", description: "" },
];

const AUDIT_STATUSES = [
  { code: "SUCCESS", name: "Success", description: "" },
  { code: "FAILURE", name: "Failure", description: "" },
];

const mockUseAuditLogs = vi.fn();

vi.mock("../../hooks/audit", () => ({
  useAuditLogs: (...args: unknown[]) => mockUseAuditLogs(...args),
  useAuditActions: () => ({ data: AUDIT_ACTIONS, isLoading: false }),
  useAuditStatuses: () => ({ data: AUDIT_STATUSES, isLoading: false }),
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual =
    await vi.importActual<typeof import("react-router-dom")>(
      "react-router-dom",
    );
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    Navigate: ({ to }: { to: string }) => {
      mockNavigate(to);
      return null;
    },
  };
});

const defaultAuditLogsReturn = {
  data: { content: AUDIT_LOGS, totalElements: 2 },
  isLoading: false,
  isError: false,
};

// Recupere les derniers parametres transmis au hook d'audit.
const getLastAuditLogsParams = () => {
  const calls = mockUseAuditLogs.mock.calls;
  return calls[calls.length - 1]?.[0] ?? {};
};

// Prepare l'affichage lisible de audit page.test.
const renderPage = () => renderWithProviders(<AuditPage />);

describe("AuditPage — P-10.A: displayed logs match all active filter parameters", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAuditLogs.mockReturnValue(defaultAuditLogsReturn);
  });

  it("should redirect to /dashboard when user lacks AUDIT_VIEW permission", () => {
    vi.mocked(useAuth).mockReturnValueOnce({
      hasPermission: () => false,
      hasRole: () => false,
      isAuthenticated: false,
      user: null,
    });
    renderPage();
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard");
  });

  it("should render all logs when no filters are active", async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Doe John")).toBeInTheDocument();
      expect(screen.getByText("system")).toBeInTheDocument();
    });

    expect(getLastAuditLogsParams()).toEqual({
      page: 0,
      size: 10,
      sort: "timestamp,desc",
    });
  });

  it("should display timestamp, user, resourceType, resourceId, and status badge", async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/15\/01\/2024.*11:30/)).toBeInTheDocument();
      expect(screen.getByText("Doe John")).toBeInTheDocument();
      expect(screen.getByText("system")).toBeInTheDocument();

      // Action rendue via t('audit.action.CODE', name) : le mock identite renvoie la cle.
      expect(
        screen.getByText("audit.action.LOGIN_SUCCESS"),
      ).toBeInTheDocument();
      expect(
        screen.getByText("audit.action.INCIDENT_CREATE"),
      ).toBeInTheDocument();

      expect(screen.getByText("USER")).toBeInTheDocument();
      expect(screen.getByText("INCIDENT")).toBeInTheDocument();
    });
  });

  it("should render action and status select filters in the filters popover", async () => {
    renderPage();

    fireEvent.click(
      await waitFor(() =>
        screen.getByRole("button", { name: /common.filters.button/ }),
      ),
    );

    await waitFor(() => {
      expect(screen.getByText("audit.filters.action")).toBeInTheDocument();
      expect(screen.getByText("audit.filters.status")).toBeInTheDocument();
    });
  });

  it("should send the resourceType search as a server-side keyword", async () => {
    renderPage();

    const searchInput = await waitFor(() =>
      screen.getByPlaceholderText("audit.filters.search"),
    );

    await act(async () => {
      fireEvent.change(searchInput, { target: { value: "INCIDENT" } });
    });

    await waitFor(() => {
      expect(getLastAuditLogsParams()).toEqual(
        expect.objectContaining({ keyword: "INCIDENT" }),
      );
    });
  });

  it("should not include from/to params in useAuditLogs call when no date range is selected", async () => {
    renderPage();

    fireEvent.click(
      await waitFor(() =>
        screen.getByRole("button", { name: /common.filters.button/ }),
      ),
    );

    await waitFor(() => {
      const rangeInputs = document.querySelectorAll(".ant-picker-input input");
      expect(rangeInputs.length).toBeGreaterThanOrEqual(2);
    });

    expect(getLastAuditLogsParams()).not.toHaveProperty("from");
    expect(getLastAuditLogsParams()).not.toHaveProperty("to");
  });

  it("should send the username search as a server-side keyword", async () => {
    renderPage();

    const searchInput = await waitFor(() =>
      screen.getByPlaceholderText("audit.filters.search"),
    );

    await act(async () => {
      fireEvent.change(searchInput, { target: { value: "jdoe" } });
    });

    await waitFor(() => {
      expect(getLastAuditLogsParams()).toEqual(
        expect.objectContaining({ keyword: "jdoe" }),
      );
    });
  });

  it("should display only the logs returned by useAuditLogs when hook returns a subset", async () => {
    mockUseAuditLogs.mockReturnValue({
      data: { content: [AUDIT_LOGS[1]], totalElements: 1 },
      isLoading: false,
      isError: false,
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText("system")).toBeInTheDocument();
      expect(screen.queryByText("Doe John")).not.toBeInTheDocument();
    });
  });

  it("should remove the server-side keyword when search text is cleared", async () => {
    renderPage();

    const searchInput = await waitFor(() =>
      screen.getByPlaceholderText("audit.filters.search"),
    );

    await act(async () => {
      fireEvent.change(searchInput, { target: { value: "INCIDENT" } });
    });
    await waitFor(() => {
      expect(getLastAuditLogsParams()).toEqual(
        expect.objectContaining({ keyword: "INCIDENT" }),
      );
    });

    await act(async () => {
      fireEvent.change(searchInput, { target: { value: "" } });
    });
    await waitFor(() => {
      expect(getLastAuditLogsParams()).not.toHaveProperty("keyword");
    });
  });

  it("should not send action or status initially", async () => {
    renderPage();

    await waitFor(() => {
      expect(getLastAuditLogsParams()).not.toHaveProperty("action");
      expect(getLastAuditLogsParams()).not.toHaveProperty("status");
    });
  });

  it("should show loading state while useAuditLogs is loading", async () => {
    mockUseAuditLogs.mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
    });

    renderPage();

    await waitFor(() => {
      expect(screen.queryByText("Doe John")).not.toBeInTheDocument();
    });
  });

  it("should show empty table when useAuditLogs returns no data", async () => {
    mockUseAuditLogs.mockReturnValue({
      data: { content: [], totalElements: 0 },
      isLoading: false,
      isError: false,
    });

    renderPage();

    await waitFor(() => {
      expect(screen.queryByText("Doe John")).not.toBeInTheDocument();
      expect(screen.queryByText("system")).not.toBeInTheDocument();
    });
  });

  it("should navigate to user profile when user link button is clicked", async () => {
    renderPage();

    const userGoBtn = await waitFor(
      () => screen.getAllByTitle("audit.table.goToUser")[0],
    );
    fireEvent.click(userGoBtn);

    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users/u1");
  });

  it("should navigate to resource path when resource link button is clicked", async () => {
    renderPage();

    const resourceBtns = await waitFor(() =>
      screen.getAllByTitle("audit.table.goToResource"),
    );
    // Premier log est de type USER (resourceId: u1), le second est INCIDENT (resourceId: inc-1)
    fireEvent.click(resourceBtns[0]);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users/u1");

    fireEvent.click(resourceBtns[1]);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/incidents/inc-1");
  });
});
