// Tests frontend : verifie le comportement de super-administration section utils.test.

import { describe, expect, it } from "vitest";
import {
  asArray,
  asEntries,
  asNumber,
  asPercent,
  asRecord,
  asString,
  isRowMap,
  severityColor,
  statusTagColor,
} from "./superAdminSectionUtils";

describe("superAdminSectionUtils", () => {
  it("normalizes unknown values safely", () => {
    expect(isRowMap({ id: 1 })).toBe(true);
    expect(isRowMap(null)).toBe(false);
    expect(asArray([{ id: 1 }, null, "x"])).toEqual([{ id: 1 }]);
    expect(asRecord(null)).toEqual({});
    expect(asEntries({ a: 1 })).toEqual([["a", 1]]);
  });

  it("formats primitive values with fallbacks", () => {
    expect(asNumber("12")).toBe(12);
    expect(asNumber("bad", 7)).toBe(7);
    expect(asString("", "fallback")).toBe("fallback");
    expect(asPercent(2, 4)).toBe(50);
    expect(asPercent(2, 0)).toBe(0);
  });

  it("maps statuses and severities to Ant Design colors", () => {
    expect(statusTagColor("UP")).toBe("success");
    expect(statusTagColor("FAILED")).toBe("error");
    expect(statusTagColor("GENERATING")).toBe("processing");
    expect(statusTagColor("OTHER")).toBe("default");
    expect(severityColor("high")).toBe("red");
    expect(severityColor("medium")).toBe("orange");
    expect(severityColor("low")).toBe("blue");
  });
});
