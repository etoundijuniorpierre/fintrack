// Teste l'indicateur de presence du header : gating par permission et listes.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import PresenceIndicator from "./PresenceIndicator";
import { useOnlinePresence } from "../../../hooks/user/useOnlinePresence/useOnlinePresence";
import { useUsers } from "../../../hooks/user/useUsers/useUsers";
import type { User } from "../../../api/user/types";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../hooks/auth/useAuth", () => ({
  useAuth: () => ({
    user: { id: "me", username: "current.user" },
    isAuthenticated: true,
    hasRole: () => false,
    hasPermission: () => true,
  }),
}));

vi.mock("../../../hooks/user/useOnlinePresence/useOnlinePresence", () => ({
  useOnlinePresence: vi.fn(),
}));

vi.mock("../../../hooks/user/useUsers/useUsers", () => ({
  useUsers: vi.fn(),
}));

const makeUser = (
  username: string,
  isActive = true,
  lastLogin?: string,
): User =>
  ({
    id: `id-${username}`,
    username,
    firstName: username,
    lastName: "Test",
    isActive,
    lastLogin,
  }) as unknown as User;

describe("PresenceIndicator", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useUsers).mockReturnValue({
      data: [
        makeUser("alice", true, "2026-07-24T14:32:00Z"),
        makeUser("bob", true),
        makeUser("carol", false),
      ],
      isLoading: false,
    } as unknown as ReturnType<typeof useUsers>);
  });

  it("should render nothing without a presence permission", () => {
    vi.mocked(useOnlinePresence).mockReturnValue({
      online: [],
      states: {},
      isOnline: () => false,
      getPresence: () => undefined,
      canViewPresence: false,
      isLoading: false,
    });

    const { container } = render(<PresenceIndicator />);
    expect(container).toBeEmptyDOMElement();
  });

  it("should show the online count and split connected/disconnected users", async () => {
    vi.mocked(useOnlinePresence).mockReturnValue({
      online: ["alice"],
      states: {
        alice: {
          online: true,
          connectedAt: "2026-07-29T08:27:00",
        },
      },
      isOnline: (username?: string) => username === "alice",
      getPresence: () => undefined,
      canViewPresence: true,
      isLoading: false,
    });

    render(<PresenceIndicator />);

    const button = screen.getByRole("button", {
      name: "layout.header.presence.title",
    });
    fireEvent.click(button);

    expect(
      await screen.findByText("layout.header.presence.connected (1)"),
    ).toBeInTheDocument();
    // Les comptes inactifs ne sont pas listes comme deconnectes.
    expect(
      screen.getByText("layout.header.presence.disconnected (1)"),
    ).toBeInTheDocument();
  });

  it("should scope the online counter to the caller's user list", () => {
    // "external.user" est connecte mais hors du perimetre retourne par l'API.
    vi.mocked(useOnlinePresence).mockReturnValue({
      online: ["alice", "external.user"],
      states: {},
      isOnline: () => false,
      getPresence: () => undefined,
      canViewPresence: true,
      isLoading: false,
    });

    render(<PresenceIndicator />);

    // Badge = alice uniquement : le connecte hors perimetre n'est pas compte.
    expect(screen.getByTitle("1")).toBeInTheDocument();
  });

  it("should render the presence status string on the same line as username", async () => {
    vi.mocked(useOnlinePresence).mockReturnValue({
      online: ["alice"],
      states: {
        alice: {
          online: true,
          connectedAt: "2026-07-29T08:27:00",
        },
      },
      isOnline: (username?: string) => username === "alice",
      getPresence: () => undefined,
      canViewPresence: true,
      isLoading: false,
    });

    render(<PresenceIndicator />);

    const button = screen.getByRole("button", {
      name: "layout.header.presence.title",
    });
    fireEvent.click(button);

    expect(
      await screen.findByText(/layout\.header\.presence\.status\.online_since/),
    ).toBeInTheDocument();
  });
});
