// Tests frontend : verifie le comportement de planification de rapport formulaire page.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import ReportScheduleFormPage from "./ReportScheduleFormPage";

const mockNavigate = vi.fn();
const mockUseParams = vi.fn<() => { id?: string }>();

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: "fr" },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => mockUseParams(),
  };
});

vi.mock("../../../../../hooks/settings", () => ({
  useCreateReportSchedule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useUpdateReportSchedule: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useReportSchedule: vi.fn(() => ({ data: undefined, isLoading: false })),
}));

describe("ReportScheduleFormPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseParams.mockReturnValue({});
  });

  it("should render create title when route has no id", () => {
    renderWithProviders(<ReportScheduleFormPage />);
    expect(
      screen.getByText("settings.reportSchedules.page.createTitle"),
    ).toBeInTheDocument();
    expect(screen.getByText("reports.form.contentType")).toBeInTheDocument();
  });

  it("should render edit title when route has id", () => {
    mockUseParams.mockReturnValue({ id: "report-1" });
    renderWithProviders(<ReportScheduleFormPage />);
    expect(
      screen.getByText("settings.reportSchedules.page.editTitle"),
    ).toBeInTheDocument();
  });

  it("should navigate back when cancel button is clicked", () => {
    renderWithProviders(<ReportScheduleFormPage />);
    const cancel = screen
      .getByText("settings.buttons.cancel")
      .closest("button");
    expect(cancel).not.toBeNull();
    fireEvent.click(cancel!);
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });
});
