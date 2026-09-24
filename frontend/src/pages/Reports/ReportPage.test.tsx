// Tests frontend : verifie le comportement de rapport page.test.

import { screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { useAuth } from "../../hooks/auth/useAuth";
import {
  useDeleteReport,
  useDownloadReport,
  useGenerateReport,
  useReports,
  useRetryReport,
  useSendEmailReport,
} from "../../hooks/reporting/useReports/useReports";
import { useUsers } from "../../hooks/user/useUsers/useUsers";
import { renderWithProviders } from "../../test-utils/renderWithProviders";
import ReportPage from "./ReportPage";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../../hooks/reporting/useReports/useReports", () => ({
  useReports: vi.fn(),
  useGenerateReport: vi.fn(),
  useDeleteReport: vi.fn(),
  useRetryReport: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useDownloadReport: vi.fn(() => ({ mutateAsync: vi.fn() })),
  useSendEmailReport: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
}));

vi.mock("../../hooks/user/useUsers/useUsers", () => ({
  useUsers: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual =
    await vi.importActual<typeof import("react-router-dom")>(
      "react-router-dom",
    );
  return {
    ...actual,
    Navigate: ({ to }: { to: string }) => {
      mockNavigate(to);
      return null;
    },
  };
});

const generateReport = vi.fn();
const deleteReport = vi.fn();
const sendEmailReport = vi.fn();

const REPORTS = {
  content: [
    {
      id: "report-1",
      name: "Monthly incidents",
      type: "MONTHLY",
      period: "2026-05",
      generatedAt: "2026-05-13T10:00:00Z",
      format: "PDF",
      status: "AVAILABLE",
      downloadUrl: "/download/report-1",
      recipients: ["audit@example.com"],
    },
  ],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 10,
};

const permissions = [
  "REPORT_VIEW_ALL",
  "REPORT_GENERATE",
  "REPORT_GENERATE_ALL_SCOPES",
  "REPORT_EXPORT",
  "REPORT_SEND_EMAIL",
  "INCIDENT_VIEW_ALL",
];

// Prepare l'affichage lisible de rapport page.test.
const renderPage = () => renderWithProviders(<ReportPage />);

describe("ReportPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: true,
      hasRole: () => false,
      hasPermission: (permission: string) => permissions.includes(permission),
    });
    vi.mocked(useReports).mockReturnValue({
      data: REPORTS,
      isLoading: false,
      isError: false,
    } as never);
    vi.mocked(useGenerateReport).mockReturnValue({
      mutateAsync: generateReport,
      isPending: false,
    } as never);
    vi.mocked(useDeleteReport).mockReturnValue({
      mutate: deleteReport,
    } as never);
    vi.mocked(useRetryReport).mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: false,
    } as never);
    vi.mocked(useDownloadReport).mockReturnValue({
      mutateAsync: vi.fn(),
    } as never);
    vi.mocked(useSendEmailReport).mockReturnValue({
      mutate: sendEmailReport,
      isPending: false,
    } as never);
    vi.mocked(useUsers).mockReturnValue({ data: [] } as never);
  });

  it("redirects when the user has no report view permission", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: true,
      hasRole: () => false,
      hasPermission: () => false,
    });

    renderPage();

    expect(mockNavigate).toHaveBeenCalledWith("/dashboard");
  });

  it("renders existing reports and permissioned actions", async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole("table")).toBeInTheDocument();
    });

    expect(
      screen.getByPlaceholderText("reports.filters.search"),
    ).toBeInTheDocument();
    expect(screen.getByText("reports.buttons.generate")).toBeInTheDocument();
    expect(screen.getByText("Monthly incidents")).toBeInTheDocument();
    expect(
      screen.getByLabelText("reports.buttons.download"),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("reports.buttons.send_email"),
    ).toBeInTheDocument();
  });

  it("opens the generate modal only when REPORT_GENERATE is present", () => {
    renderPage();

    fireEvent.click(screen.getByText("reports.buttons.generate"));

    expect(
      screen.getByText("reports.modal.generate_title"),
    ).toBeInTheDocument();
  });

  it("opens the email modal from the send email table action", async () => {
    renderPage();

    await waitFor(() => {
      expect(
        screen.getByLabelText("reports.buttons.send_email"),
      ).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText("reports.buttons.send_email"));

    expect(screen.getByText("reports.emailModal.title")).toBeInTheDocument();
    expect(
      screen.getByText("reports.emailModal.description"),
    ).toBeInTheDocument();
  });

  it("sends the selected report by email from the modal action", async () => {
    renderPage();

    fireEvent.click(screen.getByLabelText("reports.buttons.send_email"));
    fireEvent.click(screen.getByText("reports.emailModal.confirm"));

    await waitFor(() => {
      expect(sendEmailReport).toHaveBeenCalledWith(
        { id: "report-1", recipients: ["audit@example.com"] },
        { onSuccess: expect.any(Function) },
      );
    });
  });

  it("hides generate action when REPORT_GENERATE is missing", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: true,
      hasRole: () => false,
      hasPermission: (permission: string) => permission === "REPORT_VIEW_ALL",
    });

    renderPage();

    expect(
      screen.queryByText("reports.buttons.generate"),
    ).not.toBeInTheDocument();
  });

  it("hides email action when REPORT_SEND_EMAIL is missing", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: true,
      hasRole: () => false,
      hasPermission: (permission: string) => permission === "REPORT_VIEW_ALL",
    });

    renderPage();

    expect(
      screen.queryByLabelText("reports.buttons.send_email"),
    ).not.toBeInTheDocument();
  });
});
