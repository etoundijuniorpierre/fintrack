// Tests : carte de notification (donnees affichees, actions selon droits/statut).

import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import NotificationCard from "./NotificationCard";
import { makeNotification } from "../../../../mocks/notification/notificationFixtures";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: "fr" },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const renderCard = ({
  notification = makeNotification(),
  canManage = false,
}: {
  notification?: ReturnType<typeof makeNotification>;
  canManage?: boolean;
} = {}) => {
  const onClick = vi.fn();
  const onMarkSent = vi.fn();
  const onMarkFailed = vi.fn();
  renderWithProviders(
    <NotificationCard
      notification={notification}
      onClick={onClick}
      onMarkSent={onMarkSent}
      onMarkFailed={onMarkFailed}
      canManage={canManage}
    />,
  );
  return { onClick, onMarkSent, onMarkFailed };
};

describe("NotificationCard", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should display the notification recipient", () => {
    renderCard({
      notification: makeNotification({ recipient: "jane@finstar.com" }),
    });
    expect(screen.getByText("jane@finstar.com")).toBeInTheDocument();
  });

  it("should display a dash when the subject is missing", () => {
    renderCard({ notification: makeNotification({ subject: undefined }) });
    expect(screen.getAllByText("-").length).toBeGreaterThan(0);
  });

  it("should call onClick with the notification when the card is clicked", async () => {
    const notification = makeNotification({ recipient: "click@finstar.com" });
    const { onClick } = renderCard({ notification });
    await userEvent.click(screen.getByText("click@finstar.com"));
    expect(onClick).toHaveBeenCalledWith(notification);
  });

  it("should show management actions when canManage and status is PENDING", () => {
    renderCard({
      notification: makeNotification({ status: "PENDING" }),
      canManage: true,
    });
    expect(
      screen.getByRole("button", { name: "notifications.actions.markSent" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "notifications.actions.markFailed" }),
    ).toBeInTheDocument();
  });

  it("should hide actions when canManage is false", () => {
    renderCard({
      notification: makeNotification({ status: "PENDING" }),
      canManage: false,
    });
    expect(
      screen.queryByRole("button", { name: "notifications.actions.markSent" }),
    ).not.toBeInTheDocument();
  });

  it("should hide actions when the status is not PENDING", () => {
    renderCard({
      notification: makeNotification({ status: "SENT" }),
      canManage: true,
    });
    expect(
      screen.queryByRole("button", { name: "notifications.actions.markSent" }),
    ).not.toBeInTheDocument();
  });

  it("should call onMarkSent with the id without triggering onClick (stopPropagation)", async () => {
    const notification = makeNotification({ id: "n-42", status: "PENDING" });
    const { onMarkSent, onClick } = renderCard({
      notification,
      canManage: true,
    });
    await userEvent.click(
      screen.getByRole("button", { name: "notifications.actions.markSent" }),
    );
    expect(onMarkSent).toHaveBeenCalledWith("n-42");
    expect(onClick).not.toHaveBeenCalled();
  });

  it("should call onMarkFailed with the id", async () => {
    const notification = makeNotification({ id: "n-7", status: "PENDING" });
    const { onMarkFailed } = renderCard({ notification, canManage: true });
    await userEvent.click(
      screen.getByRole("button", { name: "notifications.actions.markFailed" }),
    );
    expect(onMarkFailed).toHaveBeenCalledWith("n-7");
  });
});
