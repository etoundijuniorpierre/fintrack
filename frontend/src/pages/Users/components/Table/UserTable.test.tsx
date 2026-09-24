// Tests frontend : verifie le comportement de utilisateur table.test.

import { screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import UserTable from "./UserTable";
import { makeUsers } from "../../../../mocks";
import { userNavigation } from "../../../../utils/navigation/users/users";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";

vi.mock("react-window", () => ({
  FixedSizeList: ({
    children,
    itemCount,
    itemSize,
  }: {
    children: (props: {
      index: number;
      style: React.CSSProperties;
    }) => React.ReactNode;
    itemCount: number;
    itemSize: number;
  }) => (
    <div data-testid="virtualized-list">
      {Array.from({ length: Math.min(itemCount, 5) }, (_, i) =>
        children({ index: i, style: { height: itemSize, top: i * itemSize } }),
      )}
    </div>
  ),
}));

const { mockNavigate } = vi.hoisted(() => ({ mockNavigate: vi.fn() }));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useParams: vi.fn().mockReturnValue({ id: "user-1" }),
    useNavigate: () => mockNavigate,
  };
});

vi.mock("../../../../hooks/user/useUsers", () => ({
  useDeleteUser: () => ({ mutate: vi.fn() }),
  useToggleUserStatus: () => ({ mutate: vi.fn() }),
}));

vi.mock("../../../../hooks/user/useOnlinePresence/useOnlinePresence", () => ({
  useOnlinePresence: () => ({
    online: ["user1"],
    isOnline: (username?: string) => username === TEST_USERS[0]?.username,
    canViewPresence: true,
    isLoading: false,
  }),
}));

vi.mock("../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({
    hasPermission: () => true,
    user: { id: "user-1", roles: ["ROLE_ADMIN"] },
  })),
}));

vi.mock("../../../../utils/navigation/users/users", () => ({
  userNavigation: {
    navigateToUserDetail: vi.fn(),
    navigateToUserDetailEdit: vi.fn(),
  },
}));

vi.mock("../../../../utils/formatters/formatters", () => ({
  formatDate: () => "2024-01-01",
  formatUserName: (
    u: { firstName?: string; lastName?: string; username?: string } | null,
  ) =>
    u
      ? `${u.lastName ?? ""} ${u.firstName ?? ""}`.trim() || u.username || "—"
      : "—",
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, opts?: { defaultValue?: string }) =>
      opts?.defaultValue ?? key,
  }),
  I18nextProvider: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Definit les donnees de test test users.
const TEST_USERS = makeUsers(2).map((user, index) => ({
  ...user,
  id: `user-${index + 1}`,
  isActive: index === 0,
  lastLogin: index === 0 ? "2099-01-01T00:00:00" : "2020-01-01T00:00:00",
  agency:
    index === 0
      ? {
          id: "agency-1",
          name: "HQ",
          createdAt: "",
          updatedAt: "",
        }
      : undefined,
  service:
    index === 0
      ? {
          id: "service-1",
          name: "IT",
          createdAt: "",
          updatedAt: "",
        }
      : undefined,
}));

const EXPECTED_RESULTS = {
  FORMATTED_DATE: "2024-01-01",
  USER_COUNT: 2,
  ROLE_COUNT: 2,
  ACTION_BUTTON_COUNT: 2,
} as const;

const TRANSLATION_KEYS = {
  USERNAME: "users.table.username",
  FULLNAME: "users.table.fullname",
  EMAIL: "users.table.email",
  ROLES: "users.table.roles",
  AGENCY: "users.table.agency",
  SERVICE: "users.table.service",
  STATUS: "users.table.status",
  CREATED: "users.table.created",
  ACTIONS: "users.table.actions",
} as const;

describe("UserTable", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render the table when users are provided", () => {
    renderWithProviders(<UserTable users={TEST_USERS} loading={false} />);

    const table = screen.getByRole("table");
    expect(table).toBeInTheDocument();
  }, 30000);

  it("should render the table when loading is true", () => {
    renderWithProviders(<UserTable users={[]} loading={true} />);

    const table = screen.getByRole("table");
    expect(table).toBeInTheDocument();
  });

  it("should render all expected column headers when users are provided", () => {
    renderWithProviders(<UserTable users={TEST_USERS} loading={false} />);

    expect(
      screen.getAllByText(TRANSLATION_KEYS.USERNAME).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText(TRANSLATION_KEYS.FULLNAME).length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText(TRANSLATION_KEYS.EMAIL).length).toBeGreaterThan(
      0,
    );
    expect(screen.getAllByText(TRANSLATION_KEYS.ROLES).length).toBeGreaterThan(
      0,
    );
    expect(screen.getAllByText(TRANSLATION_KEYS.AGENCY).length).toBeGreaterThan(
      0,
    );
    expect(
      screen.getAllByText(TRANSLATION_KEYS.SERVICE).length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText(TRANSLATION_KEYS.STATUS).length).toBeGreaterThan(
      0,
    );
    expect(
      screen.getAllByText(TRANSLATION_KEYS.CREATED).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText(TRANSLATION_KEYS.ACTIONS).length,
    ).toBeGreaterThan(0);
    expect(screen.queryByText("users.table.connection")).not.toBeInTheDocument();
  });

  it("should display compact presence indicators beside full names", () => {
    renderWithProviders(<UserTable users={TEST_USERS} loading={false} />);

    expect(
      screen.getByRole("img", { name: "users.table.connected" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("img", { name: "users.table.disconnected" }),
    ).toBeInTheDocument();
  });

  it("should navigate to user detail when a row is clicked", () => {
    renderWithProviders(<UserTable users={TEST_USERS} loading={false} />);

    const firstRow = screen.getByText(TEST_USERS[0].username).closest("tr");
    fireEvent.click(firstRow!);

    expect(userNavigation.navigateToUserDetail).toHaveBeenCalledWith(
      expect.anything(),
      TEST_USERS[0].id,
    );
  });

  it("should display user data correctly when users are provided", () => {
    renderWithProviders(<UserTable users={TEST_USERS} loading={false} />);

    TEST_USERS.forEach((user) => {
      expect(screen.getByText(user.username)).toBeInTheDocument();
      expect(screen.getByText(user.email!)).toBeInTheDocument();
      expect(
        screen.getByText(`${user.lastName} ${user.firstName}`),
      ).toBeInTheDocument();
    });

    expect(screen.getByText("IT")).toBeInTheDocument();
  });

  it("should display role tags correctly when users have roles", () => {
    renderWithProviders(<UserTable users={TEST_USERS} loading={false} />);

    const firstUserRoles = TEST_USERS[0].roles!.map((r) =>
      r.name.toUpperCase(),
    );
    const secondUserRoles = TEST_USERS[1].roles!.map((r) =>
      r.name.toUpperCase(),
    );
    const allExpectedRoles = [...firstUserRoles, ...secondUserRoles];

    allExpectedRoles.forEach((roleName) => {
      expect(
        screen.getAllByText(new RegExp(roleName, "i")).length,
      ).toBeGreaterThan(0);
    });
  });

  it("should display agency name when user has an agency and dash when user has none", () => {
    renderWithProviders(<UserTable users={TEST_USERS} loading={false} />);

    expect(screen.getByText("HQ")).toBeInTheDocument();
    const dashes = screen.getAllByText("-");
    expect(dashes.length).toBeGreaterThan(0);
  });

  it("should display active and inactive status tags correctly", () => {
    renderWithProviders(<UserTable users={TEST_USERS} loading={false} />);

    const activeTags = screen.getAllByText("users.table.status_active");
    const inactiveTags = screen.getAllByText("users.table.status_inactive");
    expect(activeTags.length + inactiveTags.length).toBe(
      EXPECTED_RESULTS.USER_COUNT,
    );
  });

  it("should display formatted creation dates for each user", () => {
    renderWithProviders(<UserTable users={TEST_USERS} loading={false} />);

    const dates = screen.getAllByText(EXPECTED_RESULTS.FORMATTED_DATE);
    expect(dates.length).toBeGreaterThan(0);
  });

  it("should keep destructive actions unavailable for the current user", () => {
    renderWithProviders(<UserTable users={TEST_USERS} loading={false} />);

    const editButtons = screen.getAllByLabelText("common.edit");
    const activateButtons = screen.queryAllByLabelText(
      "users.form.actions.activate",
    );
    const deactivateButtons = screen.queryAllByLabelText(
      "users.form.actions.deactivate",
    );
    const deleteButtons = screen.getAllByLabelText("common.delete");

    expect(editButtons).toHaveLength(EXPECTED_RESULTS.ACTION_BUTTON_COUNT);
    expect(activateButtons.length + deactivateButtons.length).toBe(
      EXPECTED_RESULTS.ACTION_BUTTON_COUNT - 1,
    );
    expect(deleteButtons).toHaveLength(EXPECTED_RESULTS.ACTION_BUTTON_COUNT - 1);
  });

  it("should render an empty table when no users are provided", () => {
    renderWithProviders(<UserTable users={[]} loading={false} />);

    const table = screen.getByRole("table");
    expect(table).toBeInTheDocument();

    TEST_USERS.forEach((user) => {
      expect(screen.queryByText(user.username)).not.toBeInTheDocument();
    });
  });

  it("should toggle asc/desc on every header click without a dead state", () => {
    const onSortChange = vi.fn();
    const { rerender } = renderWithProviders(
      <UserTable
        users={TEST_USERS}
        loading={false}
        sort={{ field: "username", order: "asc" }}
        onSortChange={onSortChange}
      />,
    );

    const clickHeader = () =>
      fireEvent.click(screen.getAllByText("users.table.username")[0]);

    // Clic 1 : asc -> desc.
    clickHeader();
    expect(onSortChange).toHaveBeenLastCalledWith("username", "desc");

    // Le parent applique le nouvel etat controle, puis clic 2 : desc -> asc.
    rerender(
      <UserTable
        users={TEST_USERS}
        loading={false}
        sort={{ field: "username", order: "desc" }}
        onSortChange={onSortChange}
      />,
    );
    clickHeader();
    expect(onSortChange).toHaveBeenLastCalledWith("username", "asc");

    // Clic 3 : le cycle repart en desc, jamais d'ordre vide.
    rerender(
      <UserTable
        users={TEST_USERS}
        loading={false}
        sort={{ field: "username", order: "asc" }}
        onSortChange={onSortChange}
      />,
    );
    clickHeader();
    expect(onSortChange).toHaveBeenLastCalledWith("username", "desc");
    expect(onSortChange).toHaveBeenCalledTimes(3);
  });
});
