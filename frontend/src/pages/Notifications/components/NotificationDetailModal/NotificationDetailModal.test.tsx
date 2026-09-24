// Tests frontend : verifie le comportement de notification detail modal.test.

import { screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import NotificationDetailModal from "./NotificationDetailModal";
import type { NotificationResponse } from "../../../../api/notification";
import { formatDateTime } from "../../../../utils/formatters/formatters";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: "fr" },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const FULL_RECORD: NotificationResponse = {
  id: "notif-abc-123",
  type: "EMAIL",
  recipient: "user@example.com",
  subject: "Incident Alert",
  content: "An incident has been created.",
  incidentId: {
    id: "inc-1",
    title: "Server Down",
    status: "OPEN",
    reference: "FT-I-2026-0001",
  },
  status: "PENDING",
  sentAt: "2024-01-15T11:00:00Z",
  retryCount: 2,
  retryAt: "2024-01-15T12:00:00Z",
  createdAt: "2024-01-15T10:00:00Z",
  updatedAt: "2024-01-15T10:30:00Z",
  modifiedBy: "admin",
  templateParams: {
    agency_name: "Agence Alpha",
    agency_name_en: "Agence Alpha",
    concerned_users: [
      {
        role: "recipient",
        username: "seed.admin.01",
        fullName: "Admin Seed",
        email: "seed.admin.01@fintrack.local",
      },
      {
        role: "creator",
        username: "seed.agent.01",
        fullName: "Agent Seed",
        email: "seed.agent.01@fintrack.local",
      },
    ],
  },
};

const MINIMAL_RECORD: NotificationResponse = {
  id: "notif-min-001",
  type: "INTERNAL",
  recipient: "ops@example.com",
  status: "SENT",
  createdAt: "2024-02-01T08:00:00Z",
  updatedAt: "2024-02-01T08:00:00Z",
};

// Prepare l'affichage lisible de notification detail modal.test.
const renderModal = (
  props: Partial<{
    open: boolean;
    notification: NotificationResponse | null;
    onClose: () => void;
  }> = {},
) =>
  renderWithProviders(
    <NotificationDetailModal
      open={props.open ?? true}
      notification={props.notification ?? FULL_RECORD}
      onClose={props.onClose ?? vi.fn()}
    />,
  );

describe("NotificationDetailModal", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should not render when open is false", () => {
    renderModal({ open: false });
    expect(
      screen.queryByText("notifications.detail.title"),
    ).not.toBeInTheDocument();
  });

  it("should render all fields when a full record is provided", async () => {
    renderModal();
    await waitFor(() =>
      expect(
        screen.getByText("notifications.detail.title"),
      ).toBeInTheDocument(),
    );

    // L'UUID technique de la notification n'est plus expose ; c'est le code
    // metier de l'incident qui identifie la notification.
    expect(screen.queryByText("notif-abc-123")).not.toBeInTheDocument();
    expect(screen.getByText("FT-I-2026-0001")).toBeInTheDocument();
    expect(screen.queryByText("user@example.com")).not.toBeInTheDocument();
    expect(screen.getByText("Incident Alert")).toBeInTheDocument();
    expect(
      screen.getByText("An incident has been created."),
    ).toBeInTheDocument();
    expect(screen.getByText("Server Down")).toBeInTheDocument();
    expect(screen.getByText("Agence Alpha")).toBeInTheDocument();
    expect(
      screen.getByText("notifications.detail.concernedUsers"),
    ).toBeInTheDocument();
    expect(screen.getAllByText("Admin Seed")).toHaveLength(2);
    expect(screen.getByText("Agent Seed")).toBeInTheDocument();
    expect(screen.queryByText(/seed\.admin\.01@fintrack\.local/)).not
      .toBeInTheDocument();
    expect(screen.queryByText(/seed\.agent\.01@fintrack\.local/)).not
      .toBeInTheDocument();
    expect(
      screen.getByText(formatDateTime(FULL_RECORD.createdAt)),
    ).toBeInTheDocument();
    expect(
      screen.getByText(formatDateTime(FULL_RECORD.updatedAt)),
    ).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("admin")).toBeInTheDocument();

    expect(screen.getByText("notifications.type.EMAIL")).toBeInTheDocument();
    expect(
      screen.getByText("notifications.status.PENDING"),
    ).toBeInTheDocument();
  });

  it("should show dashes for absent optional fields and hide modifiedBy row when record has only required fields", async () => {
    renderModal({ notification: MINIMAL_RECORD });
    await waitFor(() =>
      expect(
        screen.getByText("notifications.detail.title"),
      ).toBeInTheDocument(),
    );

    const noValues = screen.getAllByText("notifications.noValue");
    expect(noValues.length).toBeGreaterThanOrEqual(4);

    expect(screen.getByText("0")).toBeInTheDocument();

    expect(
      screen.queryByText("notifications.detail.modifiedBy"),
    ).not.toBeInTheDocument();
  });

  it("should call onClose when the cancel button is clicked", async () => {
    const onClose = vi.fn();
    renderModal({ onClose });
    await waitFor(() =>
      expect(screen.getByText("notifications.close")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByText("notifications.close"));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
