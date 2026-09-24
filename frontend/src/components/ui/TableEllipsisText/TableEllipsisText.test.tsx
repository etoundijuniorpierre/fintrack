// Tests frontend : verifie le rendu compact des textes longs dans les tableaux.
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import TableEllipsisText from "./TableEllipsisText";

describe("TableEllipsisText", () => {
  it("should constrain the visible text width", () => {
    render(
      <TableEllipsisText value="A long table description" maxWidth={180} />,
    );

    expect(screen.getByText("A long table description")).toHaveStyle({
      display: "inline-block",
      maxWidth: "180px",
      width: "100%",
    });
  });

  it("should render the fallback for an empty value", () => {
    render(<TableEllipsisText value="  " />);

    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("should emphasize text when requested", () => {
    render(<TableEllipsisText value="Important incident" strong />);

    expect(
      screen.getByText("Important incident").closest("strong"),
    ).toBeInTheDocument();
  });
});
