// Tests frontend : verifie le comportement de tableau columns.test.

import { describe, it, expect } from "vitest";
import { commonColumnProps, actionsColumnProps } from "./tableColumns";

describe("tableColumns properties", () => {
  it("should have standard commonColumnProps", () => {
    expect(commonColumnProps).toEqual({
      align: "center",
      filterOnClose: false,
    });
  });

  it("should return correct actionsColumnProps with default width", () => {
    const props = actionsColumnProps();
    expect(props).toEqual({
      align: "center",
      filterOnClose: false,
      key: "actions",
      fixed: "right",
      width: 100,
    });
  });

  it("should return correct actionsColumnProps with custom width", () => {
    const props = actionsColumnProps(150);
    expect(props.width).toBe(150);
  });
});
