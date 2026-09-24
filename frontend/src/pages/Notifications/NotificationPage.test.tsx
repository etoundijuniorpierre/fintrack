// Tests frontend : verifie le comportement de notification page.test.

import { screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../test-utils/renderWithProviders";
import NotificationPage from "./NotificationPage";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: "fr" },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({
    hasPermission: () => true,
    user: { username: "me" },
  })),
}));

const {
  mockMarkAsReadNotification,
  mockMarkAllAsReadNotification,
  mockBulkDeleteNotifications,
} = vi.hoisted(() => ({
  mockMarkAsReadNotification: vi.fn(),
  mockMarkAllAsReadNotification: vi.fn(),
  mockBulkDeleteNotifications: vi.fn(),
}));

const NOTIFICATIONS = [
  {
    id: "notif-1",
    type: "EMAIL",
    recipient: "user@example.com",
    subject: "Incident Alert",
    content: "An incident has been created.",
    incidentId: { id: "inc-1", title: "Server Down", status: "OPEN" },
    status: "PENDING",
    createdAt: "2024-01-15T10:00:00Z",
    updatedAt: "2024-01-15T10:00:00Z",
  },
  {
    id: "notif-2",
    type: "INTERNAL",
    recipient: "admin@example.com",
    subject: "System Update",
    content: "System has been updated.",
    incidentId: null,
    status: "READ",
    createdAt: "2024-01-15T09:00:00Z",
    updatedAt: "2024-01-15T11:00:00Z",
  },
  {
    id: "notif-3",
    type: "EMAIL",
    recipient: "manager@example.com",
    subject: "Failed Alert",
    content: "Notification failed.",
    incidentId: null,
    status: "FAILED",
    createdAt: "2024-01-15T08:00:00Z",
    updatedAt: "2024-01-15T08:30:00Z",
  },
];

const mockUseNotifications = vi.fn();

vi.mock("../../hooks/notification", () => ({
  useNotificationsByRecipient: (...args: unknown[]) =>
    mockUseNotifications(...args),
  useNotificationsByRecipientPage: (...args: unknown[]) =>
    mockUseNotifications(...args),
  useNotificationTypes: () => ({
    data: [
      { code: "EMAIL", name: "Email", description: "" },
      { code: "INTERNAL", name: "Internal", description: "" },
    ],
    isLoading: false,
  }),
  useNotificationStatuses: () => ({
    data: [
      { code: "PENDING", name: "Pending", description: "" },
      { code: "SENT", name: "Sent", description: "" },
      { code: "FAILED", name: "Failed", description: "" },
      { code: "READ", name: "Read", description: "" },
    ],
    isLoading: false,
  }),
  useMarkNotificationRead: () => ({
    mutate: mockMarkAsReadNotification,
    isPending: false,
  }),
  useMarkAllNotificationsRead: () => ({
    mutate: mockMarkAllAsReadNotification,
    isPending: false,
  }),
  useBulkDeleteNotifications: () => ({
    mutate: mockBulkDeleteNotifications,
    isPending: false,
  }),
}));

const defaultReturn = {
  data: { content: NOTIFICATIONS, totalElements: NOTIFICATIONS.length },
  isLoading: false,
  isError: false,
};

// Prepare l'affichage lisible de notification page.test.
const renderPage = () => renderWithProviders(<NotificationPage />);

describe("NotificationPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseNotifications.mockReturnValue(defaultReturn);
  });

  it("should render notification list when data is loaded", async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Incident Alert")).toBeInTheDocument();
      expect(screen.getByText("System Update")).toBeInTheDocument();
      expect(screen.getByText("Failed Alert")).toBeInTheDocument();
    });

    // Check that tags for statuses are rendered
    expect(
      screen.getByText("notifications.status.PENDING"),
    ).toBeInTheDocument();
    expect(screen.getByText("notifications.status.READ")).toBeInTheDocument();
    expect(screen.getByText("notifications.status.FAILED")).toBeInTheDocument();
    expect(screen.getByText(/15\/01\/2024.*11:00/)).toBeInTheDocument();
  });

  it("should request the selected server page", async () => {
    mockUseNotifications.mockReturnValue({
      data: {
        content: NOTIFICATIONS,
        totalElements: 250,
        totalPages: 25,
        size: 10,
        number: 0,
      },
      isLoading: false,
      isError: false,
    });
    renderPage();

    expect(mockUseNotifications).toHaveBeenCalledWith("me", {
      page: 0,
      size: 10,
      sort: "createdAt,desc",
    });

    fireEvent.click(screen.getByTitle("2"));

    await waitFor(() => {
      expect(mockUseNotifications).toHaveBeenLastCalledWith("me", {
        page: 1,
        size: 10,
        sort: "createdAt,desc",
      });
    });
  });

  it("should mark notification as read and navigate when clicked", async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Incident Alert")).toBeInTheDocument();
    });

    // Ouvre la premiere notification affichee.
    fireEvent.click(screen.getByText("Incident Alert"));

    await waitFor(() => {
      expect(mockMarkAsReadNotification).toHaveBeenCalledWith("notif-1");
    });
  });

  it("should handle bulk selection and deletion", async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Incident Alert")).toBeInTheDocument();
    });

    // Select all checkbox
    const selectAllCheckbox = screen.getByLabelText("notifications.selectAll");
    fireEvent.click(selectAllCheckbox);

    // Verifie l?affichage du bouton de suppression groupee.
    await waitFor(() => {
      expect(
        screen.getByText(/notifications.deleteSelected/),
      ).toBeInTheDocument();
    });

    // Confirme la suppression depuis l?interface.
    fireEvent.click(screen.getByText(/notifications.deleteSelected/));

    await waitFor(() => {
      expect(mockBulkDeleteNotifications).toHaveBeenCalledWith(
        ["notif-1", "notif-2", "notif-3"],
        expect.anything(),
      );
    });
  });

  it("should mark all visible notifications as read", async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Incident Alert")).toBeInTheDocument();
    });

    fireEvent.click(
      screen.getByRole("button", { name: "notifications.markAllRead" }),
    );

    expect(mockMarkAllAsReadNotification).toHaveBeenCalledTimes(1);
  });

  it("should display loading spinner when isLoading is true", async () => {
    mockUseNotifications.mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
    });

    const { container } = renderPage();

    await waitFor(() => {
      expect(screen.getByText("notifications.pageTitle")).toBeInTheDocument();
      expect(container.querySelector('[aria-busy="true"]')).toBeInTheDocument();
    });
  });

  it("should display error message when isError is true", async () => {
    mockUseNotifications.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText("common.loading_error")).toBeInTheDocument();
    });
  });
});
