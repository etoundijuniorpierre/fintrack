// Tests frontend : verifie le comportement du hook useTableFilters (popover + puces).

import { useState } from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { useTableFilters, type TableFilterField } from "./useTableFilters";

// i18n : on renvoie la cle pour des assertions stables.
vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

// Hote de test : pilote un champ multi-selection et une bascule via le hook.
function Host() {
  const [types, setTypes] = useState<string[]>([]);
  const [hideInactive, setHideInactive] = useState(false);

  const fields: TableFilterField[] = [
    {
      key: "type",
      type: "multiselect",
      label: "Type",
      options: [
        { label: "Software", value: "soft" },
        { label: "Hardware", value: "hard" },
      ],
      value: types,
      onChange: setTypes,
    },
    {
      key: "hideInactive",
      type: "toggle",
      label: "Hide inactive",
      value: hideInactive,
      onChange: setHideInactive,
    },
  ];

  const { trigger, chips, activeCount } = useTableFilters(fields);
  return (
    <div>
      <span data-testid="count">{activeCount}</span>
      {trigger}
      <div data-testid="chips">{chips}</div>
    </div>
  );
}

describe("useTableFilters", () => {
  it("should expose no active filter and render no chips initially", () => {
    render(<Host />);
    expect(screen.getByTestId("count")).toHaveTextContent("0");
    expect(screen.queryByText("common.filters.active")).not.toBeInTheDocument();
  });

  it("should mark the toggle as active and show a removable chip when enabled", () => {
    render(<Host />);

    fireEvent.click(
      screen.getByRole("button", { name: /common.filters.button/ }),
    );
    fireEvent.click(screen.getByRole("switch", { name: "Hide inactive" }));

    expect(screen.getByTestId("count")).toHaveTextContent("1");
    const chips = screen.getByTestId("chips");
    expect(within(chips).getByText("common.filters.active")).toBeInTheDocument();
    expect(within(chips).getByText("Hide inactive")).toBeInTheDocument();
  });

  it("should clear the toggle when its chip is removed", () => {
    render(<Host />);

    fireEvent.click(
      screen.getByRole("button", { name: /common.filters.button/ }),
    );
    fireEvent.click(screen.getByRole("switch", { name: "Hide inactive" }));
    expect(screen.getByTestId("count")).toHaveTextContent("1");

    const chips = screen.getByTestId("chips");
    fireEvent.click(chips.querySelector(".ant-tag-close-icon") as HTMLElement);

    expect(screen.getByTestId("count")).toHaveTextContent("0");
  });
});

// Hote dedie au champ mono-selection.
function SelectHost() {
  const [role, setRole] = useState<string | undefined>(undefined);
  const fields: TableFilterField[] = [
    {
      key: "role",
      type: "select",
      label: "Role",
      options: [
        { label: "Admin", value: "ADMIN" },
        { label: "Agent", value: "AGENT" },
      ],
      value: role,
      onChange: setRole,
    },
  ];
  const { chips, activeCount } = useTableFilters(fields);
  return (
    <div>
      <span data-testid="count">{activeCount}</span>
      <button onClick={() => setRole("ADMIN")}>set-role</button>
      <div data-testid="chips">{chips}</div>
    </div>
  );
}

describe("useTableFilters — select field", () => {
  it("should show a chip with the option label when a single-select value is set", () => {
    render(<SelectHost />);
    expect(screen.getByTestId("count")).toHaveTextContent("0");

    fireEvent.click(screen.getByText("set-role"));

    expect(screen.getByTestId("count")).toHaveTextContent("1");
    expect(
      within(screen.getByTestId("chips")).getByText("Role : Admin"),
    ).toBeInTheDocument();
  });
});
