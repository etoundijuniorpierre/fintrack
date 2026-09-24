// Tests frontend : verifie le comportement de planification de rapport details page.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import ReportScheduleDetailsPage from "./ReportScheduleDetailsPage";

const mockNavigate = vi.fn();
const mockDeleteSchedule = vi.fn();
const mockUpdateSchedule = vi.fn();
const mockToggleSchedule = vi.fn();

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: "report-1" }),
  };
});

vi.mock("../../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({
    hasPermission: () => true,
    user: null,
    isAuthenticated: true,
  })),
}));

vi.mock("../../../../../hooks/settings", () => ({
  useReportSchedule: vi.fn(() => ({
    data: {
      id: "report-1",
      name: "Daily Activity Report",
      type: "DAILY",
      contentType: "INCIDENT_TYPE_ANALYSIS",
      format: "PDF",
      recipientEmails: ["test@example.com"],
      scope: "all",
      sendTime: "08:30:00",
      weekDay: 2,
      lastGeneratedAt: "2023-01-01T00:00:00Z",
      isActive: true,
      createdBy: {
        id: "user-1",
        username: "scheduler",
        firstName: "Report",
        lastName: "Owner",
      },
      createdAt: "2026-07-20T08:00:00",
      updatedAt: "2026-07-28T09:30:00",
    },
    isLoading: false,
  })),
  useDeleteReportSchedule: vi.fn(() => ({ mutate: mockDeleteSchedule })),
  useUpdateReportSchedule: vi.fn(() => ({
    mutate: mockUpdateSchedule,
    isPending: false,
  })),
  useToggleReportSchedule: vi.fn(() => ({
    mutate: mockToggleSchedule,
    isPending: false,
  })),
}));

describe("ReportScheduleDetailsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render report schedule details", () => {
    renderWithProviders(<ReportScheduleDetailsPage />);
    expect(screen.getAllByText("Daily Activity Report")[0]).toBeInTheDocument();
    expect(
      screen.getByText("settings.reportSchedules.reportType.DAILY"),
    ).toBeInTheDocument();
    expect(screen.getByText("PDF")).toBeInTheDocument();
    expect(
      screen.getByText("reports.contentType.INCIDENT_TYPE_ANALYSIS"),
    ).toBeInTheDocument();
    expect(screen.getByText("test@example.com")).toBeInTheDocument();
    expect(screen.getByText("08:30")).toBeInTheDocument();
    expect(screen.getByText("Report Owner")).toBeInTheDocument();
    expect(screen.getByText("report-1")).toBeInTheDocument();
    expect(
      screen.getByText("settings.reportSchedules.details.createdAt"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("settings.reportSchedules.details.updatedAt"),
    ).toBeInTheDocument();
  });

  it("should navigate to the canonical edit page when edit is clicked", () => {
    renderWithProviders(<ReportScheduleDetailsPage />);
    const edit = screen.getByText("settings.buttons.edit").closest("button");
    expect(edit).not.toBeNull();
    fireEvent.click(edit!);
    expect(mockNavigate).toHaveBeenCalledWith(
      "/dashboard/settings/report-schedules/edit/report-1",
    );
  });

  it("should call delete mutation after confirmation", () => {
    renderWithProviders(<ReportScheduleDetailsPage />);
    const deleteBtn = screen
      .getByText("settings.buttons.delete")
      .closest("button");
    expect(deleteBtn).not.toBeNull();
    fireEvent.click(deleteBtn!);
    fireEvent.click(screen.getByText("Confirm"));
    expect(mockDeleteSchedule).toHaveBeenCalledWith(
      "report-1",
      expect.any(Object),
    );
  });

  it("should call toggle mutation when deactivate button is clicked", () => {
    renderWithProviders(<ReportScheduleDetailsPage />);
    const toggleButton = screen
      .getByText("common.deactivate")
      .closest("button")!;
    fireEvent.click(toggleButton);
    expect(mockToggleSchedule).toHaveBeenCalledWith("report-1");
  });

  it("should navigate back when back button is clicked", () => {
    renderWithProviders(<ReportScheduleDetailsPage />);
    const backBtn = screen.getByRole("button", { name: "Return" });
    fireEvent.click(backBtn);
    expect(mockNavigate).toHaveBeenCalled();
  });

});
