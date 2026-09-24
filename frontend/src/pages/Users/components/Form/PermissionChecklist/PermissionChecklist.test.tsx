// Tests frontend : verifie le comportement de permission checklist.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import PermissionChecklist, {
  type PermissionOption,
} from "./PermissionChecklist";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

describe("PermissionChecklist", () => {
  const mockOptions: PermissionOption[] = [
    {
      value: "perm-1",
      label: "Create incident",
      desc: "Can create incidents",
      name: "INCIDENT_CREATE",
    },
    {
      value: "perm-2",
      label: "View incidents",
      desc: "Can view incidents",
      name: "INCIDENT_VIEW_ALL",
    },
    {
      value: "perm-3",
      label: "Create user",
      desc: "Can create users",
      name: "USER_CREATE_ALL_AGENT",
    },
    { value: "perm-4", label: "View users", desc: "", name: "USER_VIEW_ALL" },
  ];

  const optionsWithRole: PermissionOption[] = [
    { ...mockOptions[0], fromRole: true },
    { ...mockOptions[1], fromRole: false },
  ];

  const mockOnChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render a loading spinner when loading is true", async () => {
    renderWithProviders(<PermissionChecklist options={[]} loading={true} />);
    expect(document.querySelector('[aria-busy="true"]')).toBeInTheDocument();
  });

  it("should not render options when loading is true", async () => {
    renderWithProviders(
      <PermissionChecklist options={mockOptions} loading={true} />,
    );
    expect(screen.queryByText("Create incident")).not.toBeInTheDocument();
  });

  it("should group permissions by domain prefix when rendered", async () => {
    renderWithProviders(
      <PermissionChecklist options={mockOptions} value={[]} />,
    );
    expect(
      screen.getByText("users.permission_groups.INCIDENT"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("users.permission_groups.USER"),
    ).toBeInTheDocument();
  });

  it("should render all permissions within their groups when panels are expanded", async () => {
    renderWithProviders(
      <PermissionChecklist options={mockOptions} value={[]} />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    expect(screen.getByText("Create incident")).toBeInTheDocument();
    expect(screen.getByText("View incidents")).toBeInTheDocument();
  });

  it("should render one checkbox per permission plus one group select-all checkbox", async () => {
    renderWithProviders(
      <PermissionChecklist options={mockOptions} value={[]} />,
    );
    const panels = screen.getAllByRole("button");
    for (const p of panels) await userEvent.click(p);
    const checkboxes = screen.getAllByRole("checkbox");
    expect(checkboxes).toHaveLength(mockOptions.length + 2);
  });

  it("should render permission descriptions when they are non-empty", async () => {
    renderWithProviders(
      <PermissionChecklist options={mockOptions} value={[]} />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    expect(screen.getByText("Can create incidents")).toBeInTheDocument();
    expect(screen.getByText("Can view incidents")).toBeInTheDocument();
  });

  it("should not render a description element when the description is empty", async () => {
    renderWithProviders(
      <PermissionChecklist options={mockOptions} value={[]} />,
    );
    const userPanel = screen.getByRole("button", {
      name: /users.permission_groups.USER/i,
    });
    await userEvent.click(userPanel);
    const viewUsersCheckbox = screen.getByText("View users");
    expect(viewUsersCheckbox.parentElement?.textContent).not.toContain("Can");
  });

  it("should pre-check permissions whose ids are in the value array", async () => {
    renderWithProviders(
      <PermissionChecklist
        options={mockOptions}
        value={["perm-1", "perm-3"]}
      />,
    );
    const panels = screen.getAllByRole("button");
    for (const p of panels) await userEvent.click(p);
    expect(
      screen.getByRole("checkbox", { name: /Create incident/i }),
    ).toBeChecked();
    expect(
      screen.getByRole("checkbox", { name: /Create user/i }),
    ).toBeChecked();
  });

  it("should apply rowChecked class to checked permissions", async () => {
    const { container } = renderWithProviders(
      <PermissionChecklist options={mockOptions} value={["perm-1"]} />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    const checkedRows = container.querySelectorAll('[class*="rowChecked"]');
    expect(checkedRows.length).toBeGreaterThan(0);
    expect(checkedRows[0].textContent).toContain("Create incident");
  });

  it("should call onChange with the added id when an unchecked permission is clicked", async () => {
    renderWithProviders(
      <PermissionChecklist
        options={mockOptions}
        value={[]}
        onChange={mockOnChange}
      />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    const checkbox = screen.getByRole("checkbox", { name: /Create incident/i });
    await userEvent.click(checkbox);
    expect(mockOnChange).toHaveBeenCalledWith(["perm-1"]);
  });

  it("should call onChange with the removed id when a checked permission is clicked", async () => {
    renderWithProviders(
      <PermissionChecklist
        options={mockOptions}
        value={["perm-1", "perm-2"]}
        onChange={mockOnChange}
      />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    const checkbox = screen.getByRole("checkbox", { name: /Create incident/i });
    await userEvent.click(checkbox);
    expect(mockOnChange).toHaveBeenCalledWith(["perm-2"]);
  });

  it("should return to the original state when a checkbox is clicked twice", async () => {
    const { rerender } = renderWithProviders(
      <PermissionChecklist
        options={mockOptions}
        value={[]}
        onChange={mockOnChange}
      />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    const checkbox = screen.getByRole("checkbox", { name: /Create incident/i });
    await userEvent.click(checkbox);
    expect(mockOnChange).toHaveBeenCalledWith(["perm-1"]);
    rerender(
      <PermissionChecklist
        options={mockOptions}
        value={["perm-1"]}
        onChange={mockOnChange}
      />,
    );
    mockOnChange.mockClear();
    await userEvent.click(checkbox!);
    expect(mockOnChange).toHaveBeenCalledWith([]);
  });

  it("should disable all checkboxes when disabled is true", async () => {
    renderWithProviders(
      <PermissionChecklist options={mockOptions} value={[]} disabled={true} />,
    );
    const panels = screen.getAllByRole("button");
    for (const p of panels) await userEvent.click(p);
    const checkboxes = screen.getAllByRole("checkbox") as HTMLInputElement[];
    checkboxes.forEach((cb) => expect(cb).toBeDisabled());
  });

  it("should not call onChange when a disabled checkbox is clicked", async () => {
    renderWithProviders(
      <PermissionChecklist
        options={mockOptions}
        value={[]}
        disabled={true}
        onChange={mockOnChange}
      />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    const checkbox = screen.getByRole("checkbox", { name: /Create incident/i });
    fireEvent.click(checkbox);
    expect(mockOnChange).not.toHaveBeenCalled();
  });

  it("should call onChange with every permission in a group when select all is clicked", async () => {
    renderWithProviders(
      <PermissionChecklist
        options={mockOptions}
        value={[]}
        onChange={mockOnChange}
      />,
    );

    const selectAll = screen.getAllByRole("checkbox", {
      name: "users.permission_actions.select_all",
    })[0];
    await userEvent.click(selectAll);

    expect(mockOnChange).toHaveBeenCalledWith(["perm-1", "perm-2"]);
  });

  it("should call onChange without group permissions when a fully selected group is unchecked", async () => {
    renderWithProviders(
      <PermissionChecklist
        options={mockOptions}
        value={["perm-1", "perm-2", "perm-3"]}
        onChange={mockOnChange}
      />,
    );

    const selectAll = screen.getAllByRole("checkbox", {
      name: "users.permission_actions.select_all",
    })[0];
    await userEvent.click(selectAll);

    expect(mockOnChange).toHaveBeenCalledWith(["perm-3"]);
  });

  it('should display a "(from role)" tag for permissions with fromRole set to true', async () => {
    renderWithProviders(
      <PermissionChecklist options={optionsWithRole} value={[]} />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    expect(
      screen.getByText("(users.permission_actions.from_role)"),
    ).toBeInTheDocument();
  });

  it("should apply rowFromRole class to permissions that come from a role", async () => {
    const { container } = renderWithProviders(
      <PermissionChecklist options={optionsWithRole} value={[]} />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    const roleRows = container.querySelectorAll('[class*="rowFromRole"]');
    expect(roleRows.length).toBeGreaterThan(0);
  });

  it("should display a badge with the count of checked permissions in each group", async () => {
    renderWithProviders(
      <PermissionChecklist
        options={mockOptions}
        value={["perm-1", "perm-2"]}
      />,
    );
    const incidentHeader = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    expect(incidentHeader?.textContent).toContain("2");
  });

  it("should not display a badge when no permissions are checked in a group", async () => {
    renderWithProviders(
      <PermissionChecklist options={mockOptions} value={[]} />,
    );
    const incidentHeader = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    expect(incidentHeader?.querySelector(".ant-badge")).not.toBeInTheDocument();
  });

  it("should revoke a role-granted permission when its checked box is clicked", async () => {
    const mockOnRevoke = vi.fn();
    renderWithProviders(
      <PermissionChecklist
        options={optionsWithRole}
        value={["perm-1", "perm-2"]}
        revokedValue={[]}
        onChange={mockOnChange}
        onRevokeChange={mockOnRevoke}
      />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    const checkbox = screen.getByRole("checkbox", { name: /Create incident/i });
    expect(checkbox).toBeChecked();
    await userEvent.click(checkbox);
    expect(mockOnRevoke).toHaveBeenCalledWith(["perm-1"]);
    expect(mockOnChange).not.toHaveBeenCalled();
  });

  it("should show a revoked role permission as unchecked with a revoked tag", async () => {
    renderWithProviders(
      <PermissionChecklist
        options={optionsWithRole}
        value={["perm-1"]}
        revokedValue={["perm-1"]}
      />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    expect(
      screen.getByRole("checkbox", { name: /Create incident/i }),
    ).not.toBeChecked();
    expect(
      screen.getByText("(users.permission_actions.revoked)"),
    ).toBeInTheDocument();
  });

  it("should un-revoke a role permission when its unchecked box is clicked again", async () => {
    const mockOnRevoke = vi.fn();
    renderWithProviders(
      <PermissionChecklist
        options={optionsWithRole}
        value={["perm-1"]}
        revokedValue={["perm-1"]}
        onRevokeChange={mockOnRevoke}
      />,
    );
    const incidentPanel = screen.getByRole("button", {
      name: /users.permission_groups.INCIDENT/i,
    });
    await userEvent.click(incidentPanel);
    await userEvent.click(
      screen.getByRole("checkbox", { name: /Create incident/i }),
    );
    expect(mockOnRevoke).toHaveBeenCalledWith([]);
  });
});
