// Tests frontend : verifie le comportement de roles section.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import RolesSection from "./RolesSection";
import type { Role } from "../../../../api/user/types";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, opts?: { defaultValue?: string }) =>
      opts?.defaultValue ?? key,
  }),
}));

// Fabrique une fixture de test pour roles section.test.
const makeRole = (overrides: Partial<Role> = {}): Role => ({
  id: "role-1",
  name: "AGENT",
  description: "Agent role",
  createdAt: "",
  updatedAt: "",
  ...overrides,
});

describe("RolesSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should show empty state when roles array is empty", () => {
    renderWithProviders(<RolesSection roles={[]} />);
    expect(screen.getByText("myProfile.messages.no_roles")).toBeInTheDocument();
  });

  it("should not render a list when roles array is empty", () => {
    renderWithProviders(<RolesSection roles={[]} />);
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });

  it("should render a list item for each role when roles are provided", () => {
    const roles = [
      makeRole({ id: "role-1", name: "AGENT" }),
      makeRole({ id: "role-2", name: "ADMIN" }),
    ];
    renderWithProviders(<RolesSection roles={roles} />);
    expect(screen.getByText("AGENT")).toBeInTheDocument();
    expect(screen.getByText("ADMIN")).toBeInTheDocument();
  });

  it("should render role description when description is present", () => {
    const roles = [makeRole({ name: "AGENT", description: "Field agent" })];
    renderWithProviders(<RolesSection roles={roles} />);
    expect(screen.getByText("Field agent")).toBeInTheDocument();
  });

  it("should render exactly one list item when one role is provided", () => {
    const { container } = renderWithProviders(
      <RolesSection roles={[makeRole({ name: "AGENT" })]} />,
    );
    expect(container.querySelectorAll(".ant-list-item")).toHaveLength(1);
  });

  it("should render exactly three list items when three roles are provided", () => {
    const roles = [
      makeRole({ id: "role-1", name: "AGENT" }),
      makeRole({ id: "role-2", name: "ADMIN" }),
      makeRole({ id: "role-3", name: "CHEF_AGENCE" }),
    ];
    const { container } = renderWithProviders(<RolesSection roles={roles} />);
    expect(container.querySelectorAll(".ant-list-item")).toHaveLength(3);
  });

  it("should render zero list items when roles array is empty", () => {
    const { container } = renderWithProviders(<RolesSection roles={[]} />);
    expect(container.querySelectorAll(".ant-list-item")).toHaveLength(0);
  });
});
