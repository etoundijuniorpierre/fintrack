// Tests frontend : verifie le comportement de audit detail modal.test.

import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import AuditDetailModal from "./AuditDetailModal";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal<typeof import("react-router-dom")>();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const FULL_RECORD = {
  id: "log-1",
  timestamp: "2024-01-15T10:30:00Z",
  user: { id: "u1", username: "jdoe", firstName: "John", lastName: "Doe" },
  username: "jdoe",
  roles: ["ROLE_ADMIN", "ROLE_AGENT"],
  action: "LOGIN_SUCCESS" as const,
  resourceType: "USER",
  resourceId: "u1",
  ipAddress: "192.168.1.1",
  userAgent: "Mozilla/5.0",
  status: "SUCCESS" as const,
  details: { reason: "test", code: 42 },
  requestBody: { endpoint: "/api/login", method: "POST" },
  responseBody: { token: "abc123", expiresIn: 3600 },
};

const MINIMAL_RECORD = {
  id: "log-2",
  timestamp: "2024-01-15T11:00:00Z",
  user: undefined,
  username: "system",
  roles: [],
  action: "INCIDENT_CREATE" as const,
  resourceType: "INCIDENT",
  resourceId: undefined,
  ipAddress: undefined,
  userAgent: undefined,
  status: "FAILURE" as const,
  details: undefined,
  requestBody: undefined,
  responseBody: undefined,
};

// Traite l'evenement utilisateur lie a audit detail modal.test.
const onClose = vi.fn();

// Prepare l'affichage lisible de audit detail modal.test.
const renderModal = (
  record: typeof FULL_RECORD | typeof MINIMAL_RECORD | null,
  open = true,
) =>
  renderWithProviders(
    <AuditDetailModal open={open} record={record} onClose={onClose} />,
  );

describe("AuditDetailModal", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render nothing inside the modal body when record is null", async () => {
    renderModal(null);
    expect(screen.queryByText("log-1")).not.toBeInTheDocument();
  });

  it("should not render when open is false", async () => {
    renderModal(FULL_RECORD, false);
    expect(screen.queryByText("audit.detail.title")).not.toBeInTheDocument();
  });

  it("should render the modal title when open is true", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("audit.detail.title")).toBeInTheDocument();
  });

  it("should display the record id when record is provided", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("log-1")).toBeInTheDocument();
  });

  it("should display the audit day and time", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText(/15\/01\/2024.*11:30/)).toBeInTheDocument();
  });

  it("should display the username when record is provided", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("jdoe")).toBeInTheDocument();
  });

  it("should display the user full name when user object is present", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("Doe John")).toBeInTheDocument();
  });

  it("should display roles joined by comma when roles are present", async () => {
    renderModal(FULL_RECORD);
    expect(
      screen.getByText("users.roles.ADMIN, users.roles.AGENT"),
    ).toBeInTheDocument();
  });

  it("should display the action when record is provided", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("audit.action.LOGIN_SUCCESS")).toBeInTheDocument();
  });

  it("should display the resourceType when record is provided", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("USER")).toBeInTheDocument();
  });

  it("should display the resourceId when resourceId is present", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getAllByText("u1").length).toBeGreaterThan(0);
  });

  it("should display the ipAddress when record is provided", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("192.168.1.1")).toBeInTheDocument();
  });

  it("should display the userAgent when record is provided", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("Mozilla/5.0")).toBeInTheDocument();
  });

  it("should render the status tag with translated label when status is SUCCESS", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("audit.status.SUCCESS")).toBeInTheDocument();
  });

  it("should render the details block as formatted list when details is present", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("Reason :")).toBeInTheDocument();
    expect(screen.getByText("test")).toBeInTheDocument();
    expect(screen.getByText("Code :")).toBeInTheDocument();
    expect(screen.getByText("42")).toBeInTheDocument();
  });

  it("should not render requestBody label (field removed from component)", async () => {
    renderModal(FULL_RECORD);
    expect(
      screen.queryByText("audit.detail.requestBody"),
    ).not.toBeInTheDocument();
  });

  it("should not render responseBody label (field removed from component)", async () => {
    renderModal(FULL_RECORD);
    expect(
      screen.queryByText("audit.detail.responseBody"),
    ).not.toBeInTheDocument();
  });

  it("should apply styles to the detail values when details is present", async () => {
    renderModal(FULL_RECORD);
    expect(screen.getByText("test")).toBeInTheDocument();
    expect(screen.getByText("42")).toBeInTheDocument();
  });

  it("should not render the user row when user is absent", async () => {
    renderModal(MINIMAL_RECORD);
    expect(screen.queryByText("audit.detail.user")).not.toBeInTheDocument();
  });

  it("should render a dash for absent resourceId when resourceId is undefined", async () => {
    renderModal(MINIMAL_RECORD);
    expect(screen.getAllByText("-").length).toBeGreaterThan(0);
  });

  it("should not render the details block when details is absent", async () => {
    renderModal(MINIMAL_RECORD);
    expect(screen.queryByText("Reason :")).not.toBeInTheDocument();
  });

  it("should not render requestBody label when requestBody is absent", async () => {
    renderModal(MINIMAL_RECORD);
    expect(
      screen.queryByText("audit.detail.requestBody"),
    ).not.toBeInTheDocument();
  });

  it("should not render responseBody label when responseBody is absent", async () => {
    renderModal(MINIMAL_RECORD);
    expect(
      screen.queryByText("audit.detail.responseBody"),
    ).not.toBeInTheDocument();
  });

  it("should render the FAILURE status tag when status is FAILURE", async () => {
    renderModal(MINIMAL_RECORD);
    expect(screen.getByText("audit.status.FAILURE")).toBeInTheDocument();
  });

  it("should call onClose when the cancel button is clicked", async () => {
    renderModal(FULL_RECORD);
    await userEvent.click(screen.getByText("audit.close"));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("should call onClose when the modal X button is clicked", async () => {
    renderModal(FULL_RECORD);
    const closeBtn = screen.getByRole("button", { name: "Close" });
    await userEvent.click(closeBtn);
    expect(onClose).toHaveBeenCalled();
  });

  it("should navigate to resource detail and close modal when resource redirect button is clicked", async () => {
    renderModal(FULL_RECORD);

    const redirectBtn = screen.getByTitle("audit.table.goToResource");
    await userEvent.click(redirectBtn);

    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users/u1");
    expect(onClose).toHaveBeenCalled();
  });

  it("should navigate to user profile and close modal when user redirect button is clicked", async () => {
    renderModal(FULL_RECORD);

    const redirectBtn = screen.getByTitle("audit.table.goToUser");
    await userEvent.click(redirectBtn);

    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/users/u1");
    expect(onClose).toHaveBeenCalled();
  });

  it("should name the changed setting and both its values on a threshold change", () => {
    // Le back enveloppe le diff des seuils : { applied: <etat complet>, delta: {...} }.
    // Sans deballage, la modale n'affichait ni le parametre ni ses valeurs.
    const record = {
      ...FULL_RECORD,
      action: "SETTINGS_CHANGE" as const,
      resourceType: "system_settings",
      resourceId: "thresholds",
      details: {
        applied: { defaultSlaHours: 72, maxReopenCount: 3 },
        delta: { defaultSlaHours: { from: 48, to: 72 } },
      },
    };

    renderModal(record as unknown as typeof FULL_RECORD);

    expect(
      screen.getByText("superAdmin.config.thresholdLabels.defaultSlaHours"),
    ).toBeInTheDocument();
    expect(screen.getByText("48")).toBeInTheDocument();
    expect(screen.getByText("72")).toBeInTheDocument();
    // L'etat complet resultant est redondant : il ne doit pas polluer l'affichage.
    expect(screen.queryByText(/object Object/)).not.toBeInTheDocument();
  });

  it("should never render a raw object placeholder in the details list", () => {
    const record = {
      ...FULL_RECORD,
      details: { nested: { a: 1 }, plain: "lisible" },
    };

    renderModal(record as unknown as typeof FULL_RECORD);

    expect(screen.getByText("lisible")).toBeInTheDocument();
    expect(screen.queryByText(/object Object/)).not.toBeInTheDocument();
  });
});
