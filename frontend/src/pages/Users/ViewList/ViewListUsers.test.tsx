// Tests frontend : verifie le comportement de view liste users.test.

import { screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import ViewListUsers from "./ViewListUsers";
import { makeUser } from "../../../mocks";
import type { User } from "../../../api/user/types";

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
  };
});

vi.mock("../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../../../hooks/user/useUsers", () => ({
  usePaginatedUsers: vi.fn(),
  useDeleteUser: () => ({ mutate: vi.fn(), isPending: false }),
  useToggleUserStatus: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("../../../hooks/user/useOnlinePresence/useOnlinePresence", () => ({
  useOnlinePresence: vi.fn(),
}));

vi.mock("../components/Table/UserTable", () => ({
  default: ({
    users,
    loading,
    onSortChange,
  }: {
    users: User[];
    loading?: boolean;
    onSortChange?: (field?: string, order?: "asc" | "desc") => void;
  }) => (
    <ul data-testid="user-table">
      {loading && <li role="status">loading</li>}
      <li>
        <button
          type="button"
          onClick={() => onSortChange?.("username", "asc")}
        >
          sort-by-username
        </button>
      </li>
      {users.map((user) => (
        <li
          key={user.id}
          data-testid="user-row"
          data-username={user.username}
          data-email={user.email}
        >
          {user.username}|{user.email}|{user.firstName} {user.lastName}
        </li>
      ))}
    </ul>
  ),
}));

import { useAuth } from "../../../hooks/auth/useAuth";
import { usePaginatedUsers } from "../../../hooks/user/useUsers";
import { useOnlinePresence } from "../../../hooks/user/useOnlinePresence/useOnlinePresence";

const USERS: User[] = [
  makeUser({
    id: "user-1",
    username: "alice_smith",
    email: "alice@company.com",
    firstName: "Alice",
    lastName: "Smith",
  }),
  makeUser({
    id: "user-2",
    username: "bob_jones",
    email: "bob@company.com",
    firstName: "Bob",
    lastName: "Jones",
  }),
  makeUser({
    id: "user-3",
    username: "charlie_doe",
    email: "charlie@example.org",
    firstName: "Charlie",
    lastName: "Doe",
  }),
  makeUser({
    id: "user-4",
    username: "diana_prince",
    email: "diana@example.org",
    firstName: "Diana",
    lastName: "Prince",
  }),
  makeUser({
    id: "user-5",
    username: "eve_admin",
    email: "eve@admin.net",
    firstName: "Eve",
    lastName: "Admin",
  }),
];

const setupMocks = ({
  permissions = ["USER_VIEW_ALL"],
  users = USERS,
  isLoading = false,
  isError = false,
  error = null,
}: {
  permissions?: string[];
  users?: User[];
  isLoading?: boolean;
  isError?: boolean;
  error?: Error | null;
} = {}) => {
  vi.mocked(useAuth).mockReturnValue({
    hasPermission: (p: string) => permissions.includes(p),
  } as ReturnType<typeof useAuth>);

  vi.mocked(usePaginatedUsers).mockReturnValue({
    data: { content: users, totalElements: users.length },
    isLoading,
    isError,
    error,
  } as ReturnType<typeof usePaginatedUsers>);
  vi.mocked(useOnlinePresence).mockReturnValue({
    online: ["alice_smith"],
    states: {},
    isOnline: (username?: string) => username === "alice_smith",
    getPresence: () => undefined,
    canViewPresence: true,
    isLoading: false,
  });
};

// Prepare l'affichage lisible de view liste users.test.
const renderComponent = () => renderWithProviders(<ViewListUsers />);

// Recupere le contenu des lignes utilisateur rendues.
const getRenderedRows = () =>
  screen.getAllByTestId("user-row").map((el) => el.textContent ?? "");

describe("ViewListUsers", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    setupMocks();
  });

  describe("initial render", () => {
    it("should render all users when no search text is entered", () => {
      renderComponent();

      const rows = getRenderedRows();
      expect(rows).toHaveLength(USERS.length);
      expect(rows.some((r) => r.includes("alice_smith"))).toBe(true);
      expect(rows.some((r) => r.includes("bob_jones"))).toBe(true);
      expect(rows.some((r) => r.includes("charlie_doe"))).toBe(true);
      expect(rows.some((r) => r.includes("diana_prince"))).toBe(true);
      expect(rows.some((r) => r.includes("eve_admin"))).toBe(true);
    });

    it("should render the search input", () => {
      renderComponent();
      expect(
        screen.getByPlaceholderText("users.search_placeholder"),
      ).toBeInTheDocument();
    });
  });

  describe("loading and error states", () => {
    it("should render a spinner when data is loading", () => {
      setupMocks({ isLoading: true, users: [] });
      renderComponent();
      expect(screen.getByRole("status")).toBeInTheDocument();
    });

    it("should render an error message when the API call fails", () => {
      setupMocks({
        isError: true,
        error: new Error("Network error"),
        users: [],
      });
      renderComponent();
      expect(screen.getByText("Network error")).toBeInTheDocument();
    });
  });

  describe("conditional rendering based on permissions", () => {
    it("should show the add user button when user has USER_CREATE_ALL_AGENT permission", () => {
      setupMocks({ permissions: ["USER_CREATE_ALL_AGENT"] });
      renderComponent();
      expect(
        screen.getByRole("button", { name: /users\.add_button/i }),
      ).toBeInTheDocument();
    });

    it("should show the add user button when user has USER_CREATE_ADMIN permission", () => {
      setupMocks({ permissions: ["USER_CREATE_ADMIN"] });
      renderComponent();
      expect(
        screen.getByRole("button", { name: /users\.add_button/i }),
      ).toBeInTheDocument();
    });

    it("should not show the add user button when user lacks create permissions", () => {
      setupMocks({ permissions: ["USER_VIEW_ALL"] });
      renderComponent();
      expect(
        screen.queryByRole("button", { name: /users\.add_button/i }),
      ).not.toBeInTheDocument();
    });
  });

  describe("user interactions", () => {
    it("should navigate to create user page when add user button is clicked", () => {
      setupMocks({ permissions: ["USER_CREATE_ALL_AGENT"] });
      renderComponent();
      fireEvent.click(
        screen.getByRole("button", { name: /users\.add_button/i }),
      );
      expect(mockNavigate).toHaveBeenCalledWith(
        expect.stringContaining("/users/create"),
      );
    });

    it("should apply server-side user order presets", async () => {
      renderComponent();

      expect(usePaginatedUsers).toHaveBeenCalledWith(
        expect.objectContaining({ sort: "lastName,asc" }),
        undefined,
      );

      fireEvent.click(
        screen.getByRole("button", { name: "users.sort.label" }),
      );
      fireEvent.click(await screen.findByText("users.sort.seniority"));

      await waitFor(() =>
        expect(usePaginatedUsers).toHaveBeenCalledWith(
          expect.objectContaining({ sort: "createdAt,desc" }),
          undefined,
        ),
      );
    });

    it("should apply the clicked column direction over the active preset", async () => {
      renderComponent();

      fireEvent.click(
        screen.getByRole("button", { name: "users.sort.label" }),
      );
      fireEvent.click(await screen.findByText("users.sort.seniority"));

      fireEvent.click(screen.getByRole("button", { name: "sort-by-username" }));

      await waitFor(() =>
        expect(usePaginatedUsers).toHaveBeenCalledWith(
          expect.objectContaining({ sort: "username,asc" }),
          undefined,
        ),
      );
    });
  });

  describe("search filter (server-side debounce)", () => {
    it("should wait for debounce before triggering API call", async () => {
      vi.useFakeTimers();
      renderComponent();

      const searchInput = screen.getByPlaceholderText(
        "users.search_placeholder",
      );
      fireEvent.change(searchInput, { target: { value: "alice" } });

      expect(
        vi
          .mocked(usePaginatedUsers)
          .mock.calls.some(([params]) => params?.keyword === "alice"),
      ).toBe(false);

      vi.advanceTimersByTime(500);
      vi.useRealTimers();

      await waitFor(() => {
        expect(usePaginatedUsers).toHaveBeenCalledWith(
          expect.objectContaining({ keyword: "alice" }),
          undefined,
        );
      });
    });
  });

  describe("presence filter", () => {
    it("should send the connected filter to the paginated endpoint", () => {
      sessionStorage.setItem(
        "users_tableFilters",
        JSON.stringify({ connected: true }),
      );

      renderComponent();

      expect(usePaginatedUsers).toHaveBeenCalledWith(
        expect.objectContaining({ connected: true }),
        ["alice_smith"],
      );
    });

    it("should refresh a filtered query when WebSocket presence changes", () => {
      sessionStorage.setItem(
        "users_tableFilters",
        JSON.stringify({ connected: false }),
      );
      const { rerender } = renderComponent();

      vi.mocked(useOnlinePresence).mockReturnValue({
        online: ["alice_smith", "bob_jones"],
        states: {},
        isOnline: (username?: string) =>
          username === "alice_smith" || username === "bob_jones",
        getPresence: () => undefined,
        canViewPresence: true,
        isLoading: false,
      });
      rerender(<ViewListUsers />);

      expect(usePaginatedUsers).toHaveBeenLastCalledWith(
        expect.objectContaining({ connected: false }),
        ["alice_smith", "bob_jones"],
      );
    });
  });
});
