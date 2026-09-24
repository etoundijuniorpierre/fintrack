// Tests frontend : verifie le comportement de planification de rapport tab.test.

import { screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import ReportScheduleTab from "./ReportScheduleTab";
import { useAuth } from "../../../../hooks/auth/useAuth";
import type { ReportScheduleResponse } from "../../../../api/settings/types";
import { makeReportSchedule } from "../../../../mocks";
import { APP_ROUTES } from "../../../../utils/constants";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

const { mockDeleteReportSchedule } = vi.hoisted(() => ({
  mockDeleteReportSchedule: vi.fn(),
}));

vi.mock("../../../../hooks/settings", () => ({
  useReportSchedules: () => ({
    data: REPORT_SCHEDULES,
    isLoading: false,
  }),
  useDeleteReportSchedule: () => ({
    mutate: mockDeleteReportSchedule,
    isPending: false,
  }),
}));

const REPORT_SCHEDULES: ReportScheduleResponse[] = [
  makeReportSchedule({
    id: "report-1",
    name: "Daily Summary",
    type: "DAILY",
    format: "PDF",
    recipientEmails: ["admin@example.com", "manager@example.com"],
    isActive: true,
    lastGeneratedAt: "2024-06-01T08:00:00Z",
  }),
  makeReportSchedule({
    id: "report-2",
    name: "Weekly Report",
    type: "WEEKLY",
    format: "EXCEL",
    recipientEmails: [],
    isActive: false,
    lastGeneratedAt: undefined,
  }),
];

// Fabrique un mock d'authentification avec ou sans permission.
const mockAuthWithPermission = (hasPermission: boolean) => {
  vi.mocked(useAuth).mockReturnValue({
    user: null,
    isAuthenticated: true,
    hasRole: vi.fn().mockReturnValue(false),
    hasPermission: vi.fn().mockReturnValue(hasPermission),
  });
};

// Prepare l'affichage lisible de planification de rapport tab.test.
const renderTab = () => renderWithProviders(<ReportScheduleTab />);

describe("ReportScheduleTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthWithPermission(true);
  });

  it("should render the table with correct columns when component mounts", () => {
    renderTab();

    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(
      screen.getAllByText("settings.reportSchedules.table.name").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.reportSchedules.table.type").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.reportSchedules.table.format").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.reportSchedules.table.recipients").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.reportSchedules.table.sendTime").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.reportSchedules.table.scope").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.reportSchedules.table.status").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.reportSchedules.table.lastGeneratedAt")
        .length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.reportSchedules.table.actions").length,
    ).toBeGreaterThan(0);
  });

  it("should display report schedule data in table rows when data is loaded", () => {
    renderTab();

    expect(screen.getByText("Daily Summary")).toBeInTheDocument();
    expect(screen.getByText("Weekly Report")).toBeInTheDocument();
    expect(
      screen.getByText("settings.reportSchedules.reportType.DAILY"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("settings.reportSchedules.reportType.WEEKLY"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("settings.reportSchedules.reportFormat.PDF"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("settings.reportSchedules.reportFormat.EXCEL"),
    ).toBeInTheDocument();
  });

  it("should render a dash when lastGeneratedAt or recipientEmails is absent", () => {
    renderTab();
    const dashes = screen.getAllByText("-");
    expect(dashes.length).toBeGreaterThan(0);
  });

  it("should display recipient emails joined when two emails are present", () => {
    renderTab();
    expect(
      screen.getByText("admin@example.com, manager@example.com"),
    ).toBeInTheDocument();
  });

  it("should not render the add button when user lacks SETTINGS_SYSTEM permission", () => {
    mockAuthWithPermission(false);
    renderTab();

    expect(
      screen.queryByText("settings.reportSchedules.addButton"),
    ).not.toBeInTheDocument();
  });

  it("should render the add button when user has SETTINGS_SYSTEM permission", () => {
    renderTab();

    expect(
      screen.getByText("settings.reportSchedules.addButton"),
    ).toBeInTheDocument();
  });

  it("should not render action buttons when user lacks SETTINGS_SYSTEM permission", () => {
    mockAuthWithPermission(false);
    renderTab();

    expect(
      screen.queryByLabelText("settings.buttons.edit"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("settings.buttons.delete"),
    ).not.toBeInTheDocument();
  });

  it("should render edit and delete action buttons when user has SETTINGS_SYSTEM permission", () => {
    renderTab();

    expect(
      screen.getAllByLabelText("settings.buttons.edit").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByLabelText("settings.buttons.delete").length,
    ).toBeGreaterThan(0);
  });

  it("should navigate to create page when add button is clicked", () => {
    renderTab();

    fireEvent.click(screen.getByText("settings.reportSchedules.addButton"));

    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.SETTINGS_REPORT_SCHEDULES_CREATE,
    );
  });

  it("should navigate to edit page when edit button is clicked", () => {
    renderTab();

    const editButtons = screen.getAllByLabelText("settings.buttons.edit");
    fireEvent.click(editButtons[0]);

    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.SETTINGS_REPORT_SCHEDULES_EDIT("report-1"),
    );
  });

  it("should show a confirmation dialog before deleting a report schedule", async () => {
    renderTab();

    const deleteButtons = screen.getAllByLabelText("settings.buttons.delete");
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(
        screen.getByText("settings.reportSchedules.messages.delete_confirm"),
      ).toBeInTheDocument();
    });

    expect(mockDeleteReportSchedule).not.toHaveBeenCalled();
  });

  it("should call deleteReportSchedule after confirming the deletion dialog", async () => {
    renderTab();

    const deleteButtons = screen.getAllByLabelText("settings.buttons.delete");
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(
        screen.getByText("settings.reportSchedules.messages.delete_confirm"),
      ).toBeInTheDocument();
    });

    const confirmButton = screen.getByText("settings.buttons.confirm");
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockDeleteReportSchedule).toHaveBeenCalledWith("report-1");
    });
  });
});
