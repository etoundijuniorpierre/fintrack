// Tests frontend : verifie le comportement de view liste incidents.test.

import { screen, fireEvent, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import ViewListIncidents from "./ViewListIncidents";
import { makeIncidentSummary } from "../../../mocks/incident/incidentFixtures";
import {
  IncidentStatus,
  type IncidentSummaryResponse,
} from "../../../api/incident/types";
import { useAuth } from "../../../hooks/auth/useAuth";
import {
  useIncidents,
  useIncidentStatuses,
  useCriticalities,
} from "../../../hooks/incident/useIncidents/useIncidents";
import { usePaginatedUsers } from "../../../hooks/user/useUsers/useUsers";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const { mockNavigate } = vi.hoisted(() => ({ mockNavigate: vi.fn() }));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    Navigate: ({ to }: { to: string }) => (
      <div data-testid="navigate" data-to={to} />
    ),
  };
});

vi.mock("../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../../../hooks/incident/useIncidents/useIncidents", () => ({
  useIncidents: vi.fn(),
  useIncidentStatuses: vi.fn(),
  useCriticalities: vi.fn(),
  useIncidentTypes: vi.fn(() => ({ data: [] })),
  useDeleteIncident: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("../../../hooks/user/useUsers/useUsers", () => ({
  usePaginatedUsers: vi.fn(),
}));

vi.mock("../../../hooks/agency/useAgencies", () => ({
  useAgencies: () => ({
    data: [
      { id: "agency-2", name: "Douala" },
      { id: "agency-1", name: "Yaounde" },
    ],
    isLoading: false,
  }),
}));

vi.mock("../components/Table/IncidentTable", () => ({
  default: ({ incidents }: { incidents: IncidentSummaryResponse[] }) => (
    <ul data-testid="incident-table">
      {incidents.map((inc) => (
        <li key={inc.id} data-testid="incident-row">
          {inc.title}
        </li>
      ))}
    </ul>
  ),
}));

const INCIDENTS: IncidentSummaryResponse[] = [
  makeIncidentSummary({
    id: "incident-1",
    title: "Alpha Incident",
    status: "OPEN",
    criticality: "LOW",
  }),
  makeIncidentSummary({
    id: "incident-2",
    title: "Beta Incident",
    status: "OPEN",
    criticality: "HIGH",
  }),
  makeIncidentSummary({
    id: "incident-3",
    title: "Gamma Incident",
    status: "RESOLVED",
    criticality: "LOW",
  }),
  makeIncidentSummary({
    id: "incident-4",
    title: "Delta Incident",
    status: "RESOLVED",
    criticality: "MEDIUM",
  }),
  makeIncidentSummary({
    id: "incident-5",
    title: "Epsilon Incident",
    status: "IN_PROGRESS",
    criticality: "HIGH",
  }),
];

const setupMocks = ({
  permissions = ["INCIDENT_VIEW_ALL", "INCIDENT_CREATE"],
  roles = [],
  userId = "current-user",
  incidents = INCIDENTS,
  isLoading = false,
  isError = false,
  error = null as Error | null,
}: {
  permissions?: string[];
  roles?: string[];
  userId?: string;
  incidents?: IncidentSummaryResponse[];
  isLoading?: boolean;
  isError?: boolean;
  error?: Error | null;
} = {}) => {
  vi.mocked(useAuth).mockReturnValue({
    user: { id: userId },
    hasPermission: (p: string) => permissions.includes(p),
    hasRole: (r: string) => roles.includes(r),
  } as ReturnType<typeof useAuth>);

  vi.mocked(useIncidents).mockImplementation((query) => {
    let filtered = incidents;
    if (query?.keyword) {
      filtered = filtered.filter((i) =>
        i.title.includes(query.keyword as string),
      );
    }
    return {
      data: isError ? undefined : { content: filtered },
      isLoading,
      isError,
      error,
    } as ReturnType<typeof useIncidents>;
  });

  vi.mocked(useIncidentStatuses).mockReturnValue({
    data: Object.values(IncidentStatus).map((code) => ({
      code,
      name: code,
      description: "",
    })),
  } as ReturnType<typeof useIncidentStatuses>);

  vi.mocked(useCriticalities).mockReturnValue({
    data: [
      { code: "LOW", name: "Low", description: "" },
      { code: "MEDIUM", name: "Medium", description: "" },
      { code: "HIGH", name: "High", description: "" },
    ],
  } as ReturnType<typeof useCriticalities>);

  vi.mocked(usePaginatedUsers).mockReturnValue({
    data: {
      content: [
        {
          id: userId,
          username: "current.user",
          firstName: "Current",
          lastName: "User",
          isActive: true,
          createdAt: "2026-01-01T00:00:00Z",
          updatedAt: "2026-01-01T00:00:00Z",
        },
        {
          id: "other-user",
          username: "other.user",
          firstName: "Other",
          lastName: "User",
          isActive: true,
          createdAt: "2026-01-01T00:00:00Z",
          updatedAt: "2026-01-01T00:00:00Z",
        },
      ],
      totalElements: 2,
    },
  } as ReturnType<typeof usePaginatedUsers>);
};

// Prepare l'affichage lisible de view liste incidents.test.
const renderComponent = () => renderWithProviders(<ViewListIncidents />);

// Recupere les titres d'incidents affiches.
const getRenderedTitles = () =>
  screen.getAllByTestId("incident-row").map((el) => el.textContent);

describe("ViewListIncidents", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Les filtres persistent en session : sans ce nettoyage, un test en teinte
    // le suivant.
    sessionStorage.clear();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    window.history.pushState({}, "", "/");
  });

  describe("initial render", () => {
    it("should render the page title when the component mounts", () => {
      setupMocks();
      renderComponent();

      expect(screen.getByText("incidents.title")).toBeInTheDocument();
    });

    it("should render all incidents when no filter is active", () => {
      setupMocks();
      renderComponent();

      const titles = getRenderedTitles();
      expect(titles).toHaveLength(INCIDENTS.length);
      expect(titles).toContain("Alpha Incident");
      expect(titles).toContain("Beta Incident");
      expect(titles).toContain("Gamma Incident");
      expect(titles).toContain("Delta Incident");
      expect(titles).toContain("Epsilon Incident");
    });

    it("should exclude only closed incidents from the initial server query", () => {
      setupMocks();
      renderComponent();

      // Rejets et annulations restent visibles : ce sont des decisions rares,
      // contrairement aux clotures qui encombrent la liste de travail.
      const status = String(
        vi.mocked(useIncidents).mock.calls.at(-1)?.[0]?.status,
      ).split(",");
      expect(status).not.toContain("CLOSED");
      expect(status).toContain("REJECTED");
      expect(status).toContain("CANCELLED");
      expect(status).toContain("OPEN");
      expect(status).toContain("VALIDATED");
      expect(status).toContain("TRANSFERRED");
    });
  });

  describe("conditional rendering based on permissions", () => {
    it("should redirect to dashboard when user has no view permission", () => {
      setupMocks({ permissions: [] });
      renderComponent();

      expect(screen.getByTestId("navigate")).toHaveAttribute(
        "data-to",
        "/dashboard",
      );
    });

    it("should not render the create button when user lacks INCIDENT_CREATE permission", () => {
      setupMocks({ permissions: ["INCIDENT_VIEW_ALL"] });
      renderComponent();

      expect(
        screen.queryByText("incidents.add_button"),
      ).not.toBeInTheDocument();
    });

    it("should render the create button when user has INCIDENT_CREATE permission", () => {
      setupMocks({ permissions: ["INCIDENT_VIEW_ALL", "INCIDENT_CREATE"] });
      renderComponent();

      expect(screen.getByText("incidents.add_button")).toBeInTheDocument();
    });
  });

  describe("loading and error states", () => {
    it("should display an error state when the incidents API call fails", () => {
      setupMocks({
        isError: true,
        error: new Error("Network error"),
        incidents: [],
      });
      renderComponent();

      expect(screen.queryByTestId("incident-table")).not.toBeInTheDocument();
    });
  });

  describe("user interactions", () => {
    it("should navigate to the create incident page when the create button is clicked", () => {
      setupMocks({ permissions: ["INCIDENT_VIEW_ALL", "INCIDENT_CREATE"] });
      renderComponent();

      fireEvent.click(screen.getByText("incidents.add_button"));

      expect(mockNavigate).toHaveBeenCalled();
    });

    it("should remove the server status restriction when closed incidents are shown", () => {
      setupMocks();
      renderComponent();

      // Les filtres sont regroupes dans un popover : ouvrir puis basculer le switch.
      fireEvent.click(
        screen.getByRole("button", { name: /common.filters.button/ }),
      );
      fireEvent.click(
        screen.getByRole("switch", {
          name: "incidents.controls.hide.CLOSED",
        }),
      );

      const latestQuery = vi.mocked(useIncidents).mock.calls.at(-1)?.[0];
      expect(latestQuery).not.toHaveProperty("status");
    });

    it("should narrow the query further when another exit status is hidden", () => {
      setupMocks();
      renderComponent();

      fireEvent.click(
        screen.getByRole("button", { name: /common.filters.button/ }),
      );
      fireEvent.click(
        screen.getByRole("switch", {
          name: "incidents.controls.hide.REJECTED",
        }),
      );

      const status = String(
        vi.mocked(useIncidents).mock.calls.at(-1)?.[0]?.status,
      ).split(",");
      expect(status).not.toContain("CLOSED");
      expect(status).not.toContain("REJECTED");
      expect(status).toContain("CANCELLED");
    });

    it("should exclude the connected user from the user filter options", () => {
      setupMocks({
        permissions: ["INCIDENT_VIEW_ALL", "USER_VIEW_ALL"],
        userId: "current-user",
      });
      renderComponent();

      fireEvent.click(
        screen.getByRole("button", { name: /common.filters.button/ }),
      );
      const placeholder = screen.getByText(
        "incidents.controls.userFilterPlaceholder",
      );
      fireEvent.mouseDown(placeholder);

      expect(
        screen.queryByText("Current User (current.user)"),
      ).not.toBeInTheDocument();
      expect(screen.getByText("Other User (other.user)")).toBeInTheDocument();
    });

    it("should send a persisted period to the server as plain dates", () => {
      // La plage est stockee en ISO et non en Dayjs : sessionStorage passe par JSON,
      // et une valeur rehydratee en chaine ferait echouer le rendu de la puce.
      sessionStorage.setItem(
        "incidents_selectedPeriod",
        JSON.stringify(["2026-03-01", "2026-03-31"]),
      );
      setupMocks({ permissions: ["INCIDENT_VIEW_ALL"] });
      renderComponent();

      const latestQuery = vi.mocked(useIncidents).mock.calls.at(-1)?.[0];
      expect(latestQuery).toMatchObject({
        startDate: "2026-03-01",
        endDate: "2026-03-31",
      });
      expect(
        screen.getByText(/incidents.controls.periodDeclared/),
      ).toBeInTheDocument();
    });

    it("should send the chosen agency to the server when an admin filters by agency", () => {
      setupMocks({ permissions: ["INCIDENT_VIEW_ALL"] });
      renderComponent();

      fireEvent.click(
        screen.getByRole("button", { name: /common.filters.button/ }),
      );
      fireEvent.mouseDown(
        screen.getByText("incidents.controls.agencyFilterPlaceholder"),
      );
      fireEvent.click(screen.getByText("Douala"));

      const latestQuery = vi.mocked(useIncidents).mock.calls.at(-1)?.[0];
      expect(latestQuery).toMatchObject({ agencyId: "agency-2" });
    });

    it("should hide the agency filter from a user who only sees one agency", () => {
      // Sans la permission transverse, tout ce que l'utilisateur voit est deja
      // dans son perimetre : le filtre n'aurait rien a restreindre.
      setupMocks({ permissions: ["INCIDENT_VIEW_SERVICE"] });
      renderComponent();

      fireEvent.click(
        screen.getByRole("button", { name: /common.filters.button/ }),
      );

      expect(
        screen.queryByText("incidents.controls.agencyFilterPlaceholder"),
      ).not.toBeInTheDocument();
    });

    it("should offer waiting and lasting statuses without nominal transit steps", () => {
      setupMocks({ permissions: ["INCIDENT_VIEW_ALL"] });
      renderComponent();

      fireEvent.click(
        screen.getByRole("button", { name: /common.filters.button/ }),
      );
      fireEvent.mouseDown(
        screen.getByText("incidents.controls.statusPlaceholder"),
      );

      for (const status of ["OPEN", "VALIDATED", "TRANSFERRED"]) {
        expect(
          screen.queryByText(`incidents.status.${status}`),
        ).not.toBeInTheDocument();
      }
      for (const status of [
        "DRAFT",
        "PENDING_VALIDATION",
        "ASSIGNED",
        "IN_PROGRESS",
        "TREATED",
        "BLOCKED",
      ]) {
        expect(screen.getByText(`incidents.status.${status}`)).toBeInTheDocument();
      }
      expect(screen.getByText("incidents.status.RESOLVED")).toBeInTheDocument();

      fireEvent.click(screen.getByText("incidents.status.ASSIGNED"));
      expect(vi.mocked(useIncidents).mock.calls.at(-1)?.[0]?.status).toBe(
        "ASSIGNED",
      );
    });
  });

  describe("user scope (createdBy deep-link)", () => {
    it("should query incidents created by the user when createdBy is in the URL", () => {
      window.history.pushState(
        {},
        "",
        "/dashboard/incidents?createdBy=user-9",
      );
      setupMocks();
      renderComponent();

      const latestQuery = vi.mocked(useIncidents).mock.calls.at(-1)?.[0];
      expect(latestQuery).toMatchObject({ createdBy: "user-9" });
    });
  });

  describe("search filter (server-side debounce)", () => {
    it("should filter incidents by search text when the user types in the search input", () => {
      setupMocks();
      renderComponent();

      const searchInput = screen.getByPlaceholderText(
        "incidents.search_placeholder",
      );
      fireEvent.change(searchInput, { target: { value: "Alpha" } });

      act(() => {
        vi.advanceTimersByTime(500);
      });

      act(() => {
        vi.advanceTimersByTime(500);
      });

      const titles = getRenderedTitles();
      expect(titles).toHaveLength(1);
      expect(titles).toContain("Alpha Incident");
    });

    it("should show only matching incidents when search text is applied", () => {
      setupMocks();
      renderComponent();

      const searchInput = screen.getByPlaceholderText(
        "incidents.search_placeholder",
      );
      fireEvent.change(searchInput, { target: { value: "Beta" } });

      act(() => {
        vi.advanceTimersByTime(500); // Trigger DebouncedSearchInput timeout
      });

      act(() => {
        vi.advanceTimersByTime(500); // Trigger ViewListIncidents useEffect timeout
      });

      const titles = getRenderedTitles();
      expect(titles).toHaveLength(1);
      expect(titles).toContain("Beta Incident");
      expect(titles).not.toContain("Alpha Incident");
    });
  });
});
