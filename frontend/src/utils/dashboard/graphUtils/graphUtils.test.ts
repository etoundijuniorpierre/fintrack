// Tests frontend : verifie le comportement de graphique utils.test.

import { describe, expect, it } from "vitest";
import {
  buildCriticalityData,
  buildMonthlySeries,
  buildStatusPhaseData,
  buildTypeData,
  getPairedGraphColSpan,
  isCohortEmpty,
} from "./graphUtils";

// Simule la fonction de traduction pour les libelles de graphe.
const t = (key: string, params?: Record<string, unknown>) =>
  params?.id ? `${key}:${params.id}` : key;

describe("dashboard graph utils", () => {
  it("groups incident statuses into readable lifecycle phases", () => {
    expect(
      buildStatusPhaseData(
        { OPEN: 2, VALIDATED: 1, IN_PROGRESS: 3, CLOSED: 4 },
        t as never,
      ),
    ).toEqual([
      { name: "dashboard.graphs.phase.BACKLOG", rawName: "BACKLOG", value: 3 },
      {
        name: "dashboard.graphs.phase.IN_PROGRESS",
        rawName: "IN_PROGRESS",
        value: 3,
      },
      { name: "dashboard.graphs.phase.DONE", rawName: "DONE", value: 4 },
    ]);
  });

  it("normalizes criticality keys before translating labels", () => {
    expect(buildCriticalityData({ high: 2 }, t as never)).toEqual([
      { name: "incidents.criticality.HIGH", rawName: "HIGH", value: 2 },
    ]);
  });

  it("uses a short unknown type label when the type key is a UUID", () => {
    expect(
      buildTypeData({ "123e4567-e89b-12d3-a456-426614174000": 1 }, t as never),
    ).toEqual([
      {
        name: "dashboard.graphs.unknown_type:123e4567",
        rawName: "123e4567-e89b-12d3-a456-426614174000",
        value: 1,
      },
    ]);
  });

  it("builds a complete 12 month series", () => {
    const result = buildMonthlySeries([{ month: 6, value: 2.456 }], t as never);

    expect(result).toHaveLength(12);
    expect(result[5]).toEqual({ month: "dashboard.months.6", value: 2.46 });
  });

  it("keeps paired graph spans at two columns and expands a lonely last item", () => {
    expect(
      [0, 1, 2, 3].map((index) => getPairedGraphColSpan(index, 4)),
    ).toEqual([12, 12, 12, 12]);
    expect([0, 1, 2].map((index) => getPairedGraphColSpan(index, 3))).toEqual([
      12, 12, 24,
    ]);
    expect(getPairedGraphColSpan(0, 1)).toBe(24);
  });

  it("detects empty cohort outcome data", () => {
    expect(isCohortEmpty({})).toBe(true);
    expect(isCohortEmpty({ openLate: 1 })).toBe(false);
  });
});
