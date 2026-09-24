// Tests frontend : valide les libelles de presentation des incidents.

import { describe, expect, it } from "vitest";
import { formatIncidentAge, formatIncidentSla } from "./incidentPresentation";
import type { IncidentAgeInfo, IncidentSlaInfo } from "./incidentTiming";

describe("incidentPresentation", () => {
  const t = (key: string, options?: { count?: number }) =>
    `${key}:${options?.count ?? ""}`;

  describe("formatIncidentAge", () => {
    it("should return the fallback when age is missing", () => {
      expect(formatIncidentAge(null, t)).toBe("-");
    });

    it("should translate age with its unit and value", () => {
      const age: IncidentAgeInfo = { value: 5, unit: "days" };
      expect(formatIncidentAge(age, t)).toBe("incidents.age.days:5");
    });
  });

  describe("formatIncidentSla", () => {
    it("should return the fallback when no due date exists", () => {
      const sla: IncidentSlaInfo = { state: "none" };
      expect(formatIncidentSla(sla, t, "aucun")).toBe("aucun");
    });

    it("should translate a near due date with remaining days", () => {
      const sla: IncidentSlaInfo = { state: "dueSoon", daysRemaining: 3 };
      expect(formatIncidentSla(sla, t)).toBe("incidents.sla.due_in_days:3");
    });

    it("should translate the other known SLA states", () => {
      expect(formatIncidentSla({ state: "closed" }, t)).toBe(
        "incidents.sla.closed:",
      );
      expect(formatIncidentSla({ state: "stopped" }, t)).toBe(
        "incidents.sla.stopped:",
      );
      expect(formatIncidentSla({ state: "onTrack" }, t)).toBe(
        "incidents.sla.onTrack:",
      );
    });
  });
});
