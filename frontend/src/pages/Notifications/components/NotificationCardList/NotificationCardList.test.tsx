// Tests : liste de cartes de notifications (etat vide vs rendu de la liste).

import { screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import NotificationCardList from "./NotificationCardList";
import { makeNotification } from "../../../../mocks/notification/notificationFixtures";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: string) => fallback ?? key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Isole la liste de la carte enfant : on ne teste ici que la structure.
vi.mock("../NotificationCard/NotificationCard", () => ({
  default: ({ notification }: { notification: { recipient: string } }) => (
    <div data-testid="card">{notification.recipient}</div>
  ),
}));

const noop = () => {};
const baseProps = {
  onCardClick: noop,
  onMarkSent: noop,
  onMarkFailed: noop,
  canManage: false,
};

describe("NotificationCardList", () => {
  it("should render the empty state when there is no notification", () => {
    renderWithProviders(
      <NotificationCardList notifications={[]} {...baseProps} />,
    );
    expect(
      screen.getByText("notifications.empty"),
    ).toBeInTheDocument();
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });

  it("should render one card per notification inside an accessible list", () => {
    const notifications = [
      makeNotification({ id: "a", recipient: "a@finstar.com" }),
      makeNotification({ id: "b", recipient: "b@finstar.com" }),
      makeNotification({ id: "c", recipient: "c@finstar.com" }),
    ];
    renderWithProviders(
      <NotificationCardList notifications={notifications} {...baseProps} />,
    );
    expect(screen.getByRole("list")).toBeInTheDocument();
    expect(screen.getAllByRole("listitem")).toHaveLength(3);
    expect(screen.getAllByTestId("card")).toHaveLength(3);
  });
});
