// Tests frontend : verifie le comportement de incident form.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import IncidentForm from "./IncidentForm";
import type { IncidentFormProps } from "./IncidentForm";
import dayjs from "dayjs";
import type { IncidentResponse } from "../../../../api/incident/types";
import { documentApi } from "../../../../api/document/documentApi";

const mockIncidentFormData = vi.hoisted(() => ({
  incidentTypes: [
    {
      id: "type-1",
      name: "TECHNICAL",
      displayName: "Technical Issue",
      isActive: true,
    },
  ] as Array<Record<string, unknown>>,
  services: [] as Array<Record<string, unknown>>,
}));

// Type les props minimales du formulaire d'incident simule.
vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../api/incident/incidentApi/incidentApi", () => ({
  incidentApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    validate: vi.fn(),
    reject: vi.fn(),
    transfer: vi.fn(),
    assign: vi.fn(),
    resolve: vi.fn(),
    close: vi.fn(),
    getComments: vi.fn(),
    addComment: vi.fn(),
    getHistory: vi.fn(),
    getStatuses: vi.fn(),
    getCriticalities: vi.fn(),
    getActionTypes: vi.fn(),
    getIncidentTypes: vi.fn(),
  },
}));

const mockCreateMutate = vi.fn();
const mockUpdateMutate = vi.fn();

vi.mock("../../../../hooks/incident/useIncidents/useIncidents", () => ({
  useCreateIncident: () => ({ mutate: mockCreateMutate, isPending: false }),
  useUpdateIncident: () => ({ mutate: mockUpdateMutate, isPending: false }),
  useIncidentTypes: () => ({
    data: mockIncidentFormData.incidentTypes,
    isLoading: false,
  }),
  useCriticalities: () => ({
    data: [
      { code: "LOW", name: "Low", description: "" },
      { code: "HIGH", name: "High", description: "" },
    ],
    isLoading: false,
  }),
  useIncidentCauses: () => ({
    data: [],
    isLoading: false,
  }),
}));

vi.mock("../../../../hooks/user/useUsers/useUsers", () => ({
  useUsers: () => ({ data: [], isLoading: false }),
}));

vi.mock("../../../../hooks/service/useServices", () => ({
  useServices: () => ({
    data: mockIncidentFormData.services,
    isLoading: false,
  }),
}));

vi.mock("../../../../api/document/documentApi", () => ({
  documentApi: { upload: vi.fn() },
}));

vi.mock("../Attachments/section/AttachmentsSection", () => ({
  default: ({
    incidentId,
    incidentStatus,
    creatorId,
    onAttachmentsChange,
  }: {
    incidentId: string;
    incidentStatus?: string;
    creatorId?: string | null;
    onAttachmentsChange?: () => void;
  }) => (
    <div
      data-testid="attachments-section"
      data-incident-id={incidentId}
      data-incident-status={incidentStatus}
      data-creator-id={creatorId}
    >
      AttachmentsSection
      <button type="button" onClick={onAttachmentsChange}>
        attachments-changed
      </button>
    </div>
  ),
}));

vi.mock("../Comments/sections/CommentsSection", () => ({
  default: ({ incidentId }: { incidentId: string }) => (
    <div data-testid="comments-section" data-incident-id={incidentId}>
      CommentsSection
    </div>
  ),
}));

// Definit les donnees de test submit button.
const SUBMIT_BUTTON = "incidents.form.buttons.submit_create";

// Prepare l'affichage lisible de incident form.test.
const renderForm = (props: IncidentFormProps = {}) =>
  renderWithProviders(<IncidentForm {...props} />);

// Pilote l'interaction de test liee a incident form.test.
const submitForm = () =>
  fireEvent.click(screen.getByRole("button", { name: SUBMIT_BUTTON }));

describe("IncidentForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockIncidentFormData.incidentTypes = [
      {
        id: "type-1",
        name: "TECHNICAL",
        displayName: "Technical Issue",
        isActive: true,
      },
    ];
    mockIncidentFormData.services = [];
  });

  it("should render all form fields when mounted", () => {
    renderForm();

    expect(
      screen.getByLabelText("incidents.form.labels.title"),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("incidents.form.labels.description"),
    ).toBeInTheDocument();
    expect(screen.getAllByRole("combobox").length).toBeGreaterThanOrEqual(2);
    expect(
      screen.getByPlaceholderText("incidents.form.placeholders.due_date"),
    ).toBeDisabled();
  });

  it("should default incident and observation dates to today", () => {
    renderForm();

    const today = dayjs().format("DD/MM/YYYY");
    expect(
      screen.getByLabelText("incidents.form.labels.incident_date"),
    ).toHaveValue(today);
    expect(
      screen.getByLabelText("incidents.form.labels.observation_date"),
    ).toHaveValue(today);
  });

  it("should make fields read-only when disabled prop is true", () => {
    renderForm({ disabled: true });

    expect(screen.getByLabelText("incidents.form.labels.title")).toBeDisabled();
    expect(
      screen.getByLabelText("incidents.form.labels.description"),
    ).toBeDisabled();
  });

  it("should show validation error when title is too short", async () => {
    renderForm();

    fireEvent.change(screen.getByLabelText("incidents.form.labels.title"), {
      target: { value: "ab" },
    });
    submitForm();

    await waitFor(() =>
      expect(
        screen.getByText("incidents.form.validation.title_min"),
      ).toBeInTheDocument(),
    );
  });

  it("should show validation error when title exceeds maximum length", async () => {
    renderForm();

    fireEvent.change(screen.getByLabelText("incidents.form.labels.title"), {
      target: { value: "a".repeat(201) },
    });
    submitForm();

    await waitFor(() =>
      expect(
        screen.getByText("incidents.form.validation.title_max"),
      ).toBeInTheDocument(),
    );
  });

  it("should show validation error when description exceeds maximum length", async () => {
    renderForm();

    fireEvent.change(
      screen.getByLabelText("incidents.form.labels.description"),
      {
        target: { value: "a".repeat(5001) },
      },
    );
    submitForm();

    await waitFor(() =>
      expect(
        screen.getByText("incidents.form.validation.description_max"),
      ).toBeInTheDocument(),
    );
  });

  it("should show required field errors when submitting an empty form", async () => {
    renderForm();
    submitForm();

    await waitFor(() => {
      expect(
        screen.getByText("incidents.form.validation.title_required"),
      ).toBeInTheDocument();
      expect(
        screen.getByText("incidents.form.validation.description_required"),
      ).toBeInTheDocument();
      expect(
        screen.getByText("incidents.form.validation.type_required"),
      ).toBeInTheDocument();
      expect(
        screen.getByText("incidents.form.validation.criticality_required"),
      ).toBeInTheDocument();
    });
  });

  it("should disable target service and show automatic routing info when selected type has a default service", () => {
    mockIncidentFormData.incidentTypes = [
      {
        id: "type-1",
        name: "TECHNICAL",
        displayName: "Technical Issue",
        isActive: true,
        defaultTargetService: { id: "service-1", name: "Support" },
      },
    ];
    mockIncidentFormData.services = [{ id: "service-1", name: "Support" }];

    renderForm({ initialValues: { typeId: "type-1" } });

    expect(
      screen.getByText("incidents.form.info.auto_service"),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("incidents.form.labels.target_service"),
    ).toBeDisabled();
  });
});
describe("IncidentForm — edit mode (incidentId provided)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should show submit_edit button text when incidentId is provided", () => {
    renderForm({ incidentId: "incident-1" });
    expect(
      screen.getByRole("button", {
        name: "incidents.form.buttons.submit_edit",
      }),
    ).toBeInTheDocument();
  });

  it("should hand the attachments section what unlocks adding a file", () => {
    // Sans le statut ni le createur, la section se rend en lecture seule : ni ajout,
    // ni suppression.
    renderForm({
      incidentId: "incident-1",
      incidentStatus: "OPEN",
      creatorId: "user-1",
    });

    const section = screen.getByTestId("attachments-section");
    expect(section).toHaveAttribute("data-incident-status", "OPEN");
    expect(section).toHaveAttribute("data-creator-id", "user-1");
  });

  it("should enable the submit button once an attachment has changed", async () => {
    renderForm({ incidentId: "incident-1" });

    const submit = screen.getByRole("button", {
      name: "incidents.form.buttons.submit_edit",
    });
    expect(submit).toBeDisabled();

    // Une piece jointe ajoutee ou retiree est une modification de l'incident :
    // laisser le bouton grise laissait croire qu'elle n'avait pas ete prise en compte.
    fireEvent.click(
      screen.getByRole("button", { name: "attachments-changed" }),
    );

    await waitFor(() => expect(submit).toBeEnabled());
  });

  it("should show submit_create button text when no incidentId is provided", () => {
    renderForm();
    expect(
      screen.getByRole("button", {
        name: "incidents.form.buttons.submit_create",
      }),
    ).toBeInTheDocument();
  });

  it("should pre-fill title field when initialValues and incidentId are provided", async () => {
    renderForm({
      incidentId: "incident-1",
      initialValues: { title: "Pre-filled Title" },
    });
    await waitFor(() =>
      expect(screen.getByLabelText("incidents.form.labels.title")).toHaveValue(
        "Pre-filled Title",
      ),
    );
  });

  it("should pre-fill description field when initialValues and incidentId are provided", async () => {
    renderForm({
      incidentId: "incident-1",
      initialValues: { description: "Pre-filled description text" },
    });
    await waitFor(() =>
      expect(
        screen.getByLabelText("incidents.form.labels.description"),
      ).toHaveValue("Pre-filled description text"),
    );
  });

  it("should call onCancel when cancel button is clicked", () => {
    const onCancel = vi.fn();
    renderForm({ incidentId: "incident-1", onCancel });
    const cancel = screen.getByRole("button", {
      name: "incidents.form.buttons.cancel",
    });
    fireEvent.click(cancel);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("should not render cancel button when onCancel is not provided", () => {
    renderForm({ incidentId: "incident-1" });
    expect(
      screen.queryByRole("button", { name: "incidents.form.buttons.cancel" }),
    ).not.toBeInTheDocument();
  });

  it("should not request a reason when the incident type changes", async () => {
    mockIncidentFormData.incidentTypes = [
      ...mockIncidentFormData.incidentTypes,
      {
        id: "type-2",
        name: "FUNCTIONAL",
        displayName: "Functional Issue",
        isActive: true,
      },
    ];
    renderForm({
      incidentId: "incident-1",
      initialValues: {
        title: "Initial title",
        description: "Initial description",
        typeId: "type-1",
        criticality: "LOW",
      },
    });

    fireEvent.mouseDown(screen.getByLabelText("incidents.form.labels.type"));
    fireEvent.click(await screen.findByText("Functional Issue"));

    // Corriger sa propre declaration est un geste ordinaire : aucun motif exige.
    await waitFor(() =>
      expect(
        screen.queryByLabelText("incidents.form.labels.edit_reason"),
      ).not.toBeInTheDocument(),
    );
  });

  it("should show the target service read-only in edit mode", () => {
    renderForm({
      incidentId: "incident-1",
      currentServiceName: "Comptabilité",
    });

    const field = screen.getByLabelText(
      "incidents.form.labels.target_service",
    );
    expect(field).toBeInTheDocument();
    expect(field).toBeDisabled();
    expect(field).toHaveValue("Comptabilité");
  });
});

describe("IncidentForm — P-6.A: required fields block API call", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should not call createIncident mutate when title is empty", async () => {
    renderForm();
    submitForm();

    await waitFor(() =>
      expect(
        screen.getByText("incidents.form.validation.title_required"),
      ).toBeInTheDocument(),
    );
    expect(mockCreateMutate).not.toHaveBeenCalled();
  });

  it("should not call createIncident mutate when description is empty", async () => {
    renderForm();

    fireEvent.change(screen.getByLabelText("incidents.form.labels.title"), {
      target: { value: "Valid title for testing" },
    });
    submitForm();

    await waitFor(() =>
      expect(
        screen.getByText("incidents.form.validation.description_required"),
      ).toBeInTheDocument(),
    );
    expect(mockCreateMutate).not.toHaveBeenCalled();
  });

  it("should not call createIncident mutate when typeId is empty", async () => {
    renderForm();

    fireEvent.change(screen.getByLabelText("incidents.form.labels.title"), {
      target: { value: "Valid title for testing" },
    });
    fireEvent.change(
      screen.getByLabelText("incidents.form.labels.description"),
      {
        target: { value: "Valid description for testing" },
      },
    );
    submitForm();

    await waitFor(() => {
      expect(
        screen.getByText("incidents.form.validation.type_required"),
      ).toBeInTheDocument();
    });
    expect(mockCreateMutate).not.toHaveBeenCalled();
  });

  it("should not call createIncident mutate when criticality is empty", async () => {
    renderForm();

    fireEvent.change(screen.getByLabelText("incidents.form.labels.title"), {
      target: { value: "Valid title for testing" },
    });
    fireEvent.change(
      screen.getByLabelText("incidents.form.labels.description"),
      {
        target: { value: "Valid description for testing" },
      },
    );
    submitForm();

    await waitFor(() => {
      expect(
        screen.getByText("incidents.form.validation.criticality_required"),
      ).toBeInTheDocument();
    });
    expect(mockCreateMutate).not.toHaveBeenCalled();
  });

  it("should call createIncident mutate when all required fields are filled", async () => {
    renderForm({
      initialValues: {
        title: "Valid title for testing",
        description: "Valid description for testing",
        typeId: "type-1",
        criticality: "LOW",
      },
    });

    submitForm();

    await waitFor(() => expect(mockCreateMutate).toHaveBeenCalledTimes(1));
  });

  it("should upload pending attachments after the incident is created", async () => {
    const createdIncident = { id: "incident-created-1" } as IncidentResponse;
    mockCreateMutate.mockImplementation((_payload, options) => {
      void options?.onSuccess?.(createdIncident);
    });
    vi.mocked(documentApi.upload).mockResolvedValue({} as never);

    const { container } = renderForm({
      initialValues: {
        title: "Valid title for testing",
        description: "Valid description for testing",
        typeId: "type-1",
        criticality: "LOW",
      },
    });
    const fileInput = container.querySelector('input[type="file"]');
    const file = new File(["attachment-content"], "evidence.pdf", {
      type: "application/pdf",
    });

    expect(fileInput).not.toBeNull();
    fireEvent.change(fileInput!, { target: { files: [new File([], "empty.pdf", { type: "application/pdf" }), file] } });
    await waitFor(() => expect(screen.getByText("incidents.attachments.empty_file")).toBeInTheDocument());
    expect(screen.queryByText("empty.pdf")).not.toBeInTheDocument();
    submitForm();

    await waitFor(() =>
      expect(documentApi.upload).toHaveBeenCalledWith(
        file,
        "incident-created-1",
      ),
    );
    expect(documentApi.upload).toHaveBeenCalledTimes(1);
  });
});
