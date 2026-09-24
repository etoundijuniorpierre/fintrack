// Tests frontend : verifie le comportement de formatters.test.

import { describe, it, expect } from "vitest";
import type { ChangeEvent } from "react";
import {
  formatDate,
  formatDateTime,
  formatCurrency,
  truncateString,
  capitalize,
  formatPhoneNumber,
  parseNumber,
  formatHours,
  formatDurationLong,
  formatPercentage,
  phoneFormItemProps,
  toConstantCase,
  formatArrayOrString,
  EMAIL_RECIPIENT_TOKEN_SEPARATORS,
  toHyperCase,
  hyperCaseFormItemProps,
  hyperCaseAllWordsFormItemProps,
  normalizeEmailRecipients,
  buildEmailRecipientOptions,
  toSentenceCase,
  sentenceCaseFormItemProps,
  formatPresenceStatus,
} from "./formatters";

describe("formatters", () => {
  describe("formatDate", () => {
    it("should format a valid ISO date string correctly in French format", () => {
      const date = "2023-10-27T10:30:00Z";
      const result = formatDate(date);
      expect(result).toMatch(/^\d{2}\/\d{2}\/\d{4}$/);
    });

    it('should return "-" when input is an empty string', () => {
      expect(formatDate("")).toBe("-");
    });

    it('should return "-" when input is null', () => {
      expect(formatDate(null)).toBe("-");
    });

    it("should format a Date object correctly", () => {
      const date = new Date("2023-01-15T08:00:00Z");
      const result = formatDate(date);
      expect(result).toMatch(/^\d{2}\/\d{2}\/\d{4}$/);
    });

    it("should omit the time part", () => {
      expect(formatDate("2023-10-27T10:30:00Z")).not.toMatch(/\d{2}:\d{2}/);
    });
  });

  describe("formatDateTime", () => {
    it("should include both the day and the time", () => {
      const result = formatDateTime("2023-10-27T10:30:00Z");
      expect(result).toMatch(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/);
    });

    it('should return "-" when input is null', () => {
      expect(formatDateTime(null)).toBe("-");
    });

    it("should format a Date object correctly", () => {
      const result = formatDateTime(new Date("2023-01-15T08:00:00Z"));
      expect(result).toMatch(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/);
    });
  });

  describe("formatCurrency", () => {
    it("should format numbers to CFA currency (XAF)", () => {
      const result = formatCurrency(5000);
      const normalized = result
        .replace(/\s+/g, "")
        .replace(/\u202F/g, "")
        .replace(/\u00A0/g, "");
      expect(normalized).toContain("5000");
    });

    it("should handle zero amount", () => {
      expect(formatCurrency(0)).toContain("0");
    });

    it("should handle negative amounts", () => {
      expect(formatCurrency(-100)).toContain("-100");
    });
  });

  describe("formatHours", () => {
    it("should read a sub-hour duration in minutes", () => {
      // « 0.5h » ou « 0.05h » ne se lisent pas : sous l'heure on compte en minutes.
      expect(formatHours(0.5)).toBe("30 min");
      expect(formatHours(0.05)).toBe("3 min");
      expect(formatHours(0.04)).toBe("2 min");
      // L'arrondi ne doit jamais produire « 60 min ».
      expect(formatHours(0.999)).toBe("1h");
    });

    it("should split hours and minutes below two days", () => {
      expect(formatHours(12.46)).toBe("12h28");
      expect(formatHours(2)).toBe("2h");
      expect(formatHours(47.9)).toBe("47h54");
      // Les minutes sont cadrees sur deux chiffres.
      expect(formatHours(3.1)).toBe("3h06");
      // L'arrondi ne doit jamais produire « 1h60 ».
      expect(formatHours(1.999)).toBe("2h");
    });

    it("should default missing values to zero minutes", () => {
      expect(formatHours(undefined)).toBe("0 min");
      expect(formatHours(null)).toBe("0 min");
    });

    it("should switch to days beyond two days", () => {
      expect(formatHours(48)).toBe("2j");
      expect(formatHours(172.4)).toBe("7j 4h");
      // Le reste arrondi a 24 h est reporte sur le jour, jamais affiche tel quel.
      expect(formatHours(71.8)).toBe("3j");
    });
  });

  describe("formatDurationLong", () => {
    // Le format long passe par i18n : la traduction est simulee par « cle(param=valeur) ».
    const t = ((key: string, options?: Record<string, unknown>) =>
      options
        ? `${key}(${Object.entries(options)
            .map(([name, value]) => `${name}=${String(value)}`)
            .join(",")})`
        : key) as unknown as Parameters<typeof formatDurationLong>[1];

    it("should read a sub-hour duration in minutes", () => {
      expect(formatDurationLong(0.5, t)).toBe("common.duration.minutes(count=30)");
      // L'arrondi ne doit jamais produire « 60 minutes ».
      expect(formatDurationLong(0.999, t)).toBe("common.duration.hours(count=1)");
      expect(formatDurationLong(0, t)).toBe("common.duration.lessThanAMinute");
    });

    it("should stay in hours below a day", () => {
      expect(formatDurationLong(12.46, t)).toBe("common.duration.hours(count=12)");
      // L'arrondi ne doit jamais produire « 24 heures ».
      expect(formatDurationLong(23.7, t)).toBe("common.duration.days(count=1)");
    });

    it("should spell out days and hours beyond a day", () => {
      // « 36 h » obligeait le lecteur a convertir de tete.
      expect(formatDurationLong(36, t)).toBe(
        "common.duration.daysAndHours(days=common.duration.days(count=1),hours=common.duration.hours(count=12))",
      );
      expect(formatDurationLong(48, t)).toBe("common.duration.days(count=2)");
      // Le reste arrondi a 24 h est reporte sur le jour.
      expect(formatDurationLong(71.8, t)).toBe("common.duration.days(count=3)");
    });
  });

  describe("formatPercentage", () => {
    it("should round percentages to the nearest integer", () => {
      expect(formatPercentage(92.4)).toBe("92%");
      expect(formatPercentage(92.5)).toBe("93%");
    });

    it("should default missing values to zero percent", () => {
      expect(formatPercentage(undefined)).toBe("0%");
      expect(formatPercentage(null)).toBe("0%");
    });
  });

  describe("truncateString", () => {
    it("should truncate string and add ellipsis when longer than limit", () => {
      expect(truncateString("Hello World", 5)).toBe("Hello...");
    });

    it("should return original string when shorter than limit", () => {
      expect(truncateString("Hello", 10)).toBe("Hello");
    });

    it("should return original string when equal to limit", () => {
      expect(truncateString("Hello", 5)).toBe("Hello");
    });

    it("should return empty string when input is empty", () => {
      expect(truncateString("", 5)).toBe("");
    });
  });

  describe("capitalize", () => {
    it("should capitalize first letter and lowercase the rest when input is uppercase", () => {
      expect(capitalize("FINTRACK")).toBe("Fintrack");
    });

    it("should capitalize first letter and lowercase the rest when input is lowercase", () => {
      expect(capitalize("fintrack")).toBe("Fintrack");
    });

    it("should return empty string when input is empty", () => {
      expect(capitalize("")).toBe("");
    });
  });

  describe("formatPhoneNumber", () => {
    it("should format a number with spaces every 3 digits", () => {
      expect(formatPhoneNumber(770000000)).toBe("770 000 000");
    });

    it("should return empty string when value is undefined", () => {
      expect(formatPhoneNumber(undefined)).toBe("");
    });

    it("should return empty string when value is empty string", () => {
      expect(formatPhoneNumber("")).toBe("");
    });
  });

  describe("parseNumber", () => {
    it("should remove all spaces from a string", () => {
      expect(parseNumber("770 000 000")).toBe("770000000");
    });

    it("should return empty string when input is empty", () => {
      expect(parseNumber("")).toBe("");
    });

    it("should return empty string when input is undefined", () => {
      expect(parseNumber(undefined)).toBe("");
    });
  });

  describe("phoneFormItemProps", () => {
    describe("getValueProps", () => {
      it("should format a numeric phone number for display", () => {
        expect(phoneFormItemProps.getValueProps(771234567)).toEqual({
          value: "771 234 567",
        });
      });

      it("should format a string phone number for display", () => {
        expect(phoneFormItemProps.getValueProps("771234567")).toEqual({
          value: "771 234 567",
        });
      });

      it("should return empty string when value is undefined", () => {
        expect(phoneFormItemProps.getValueProps(undefined)).toEqual({
          value: "",
        });
      });

      it("should return empty string when value is empty string", () => {
        expect(phoneFormItemProps.getValueProps("")).toEqual({ value: "" });
      });
    });

    describe("getValueFromEvent", () => {
      const makeEvent = (value: string): ChangeEvent<HTMLInputElement> =>
        ({ target: { value } }) as ChangeEvent<HTMLInputElement>;

      it("should strip spaces from user input before storing", () => {
        expect(
          phoneFormItemProps.getValueFromEvent(makeEvent("771 234 567")),
        ).toBe("771234567");
      });

      it("should return raw value unchanged when no spaces present", () => {
        expect(
          phoneFormItemProps.getValueFromEvent(makeEvent("771234567")),
        ).toBe("771234567");
      });

      it("should return empty string when input is empty", () => {
        expect(phoneFormItemProps.getValueFromEvent(makeEvent(""))).toBe("");
      });
    });
  });

  describe("toConstantCase", () => {
    it("should convert spaces to underscores and uppercase everything", () => {
      expect(toConstantCase("high priority")).toBe("HIGH_PRIORITY");
    });

    it("should handle multiple spaces correctly", () => {
      expect(toConstantCase("very   high   priority")).toBe(
        "VERY_HIGH_PRIORITY",
      );
    });

    it("should return empty string when input is empty", () => {
      expect(toConstantCase("")).toBe("");
    });
  });

  describe("formatArrayOrString", () => {
    it("should join arrays with the default separator", () => {
      expect(formatArrayOrString(["A", "B"])).toBe("A - B");
    });

    it("should join arrays with a custom separator", () => {
      expect(formatArrayOrString(["A", "B"], ", ")).toBe("A, B");
    });

    it("should convert a number to a string", () => {
      expect(formatArrayOrString(123)).toBe("123");
    });

    it("should convert a string to a string", () => {
      expect(formatArrayOrString("test")).toBe("test");
    });

    it("should return the fallback when value is undefined", () => {
      expect(formatArrayOrString(undefined)).toBe("-");
    });

    it("should return the fallback when value is null", () => {
      expect(formatArrayOrString(null)).toBe("-");
    });

    it("should return a custom fallback when value is null", () => {
      expect(formatArrayOrString(null, " - ", "N/A")).toBe("N/A");
    });
  });

  describe("normalizeEmailRecipients", () => {
    it("should split comma, semicolon and whitespace separated recipients", () => {
      expect(
        normalizeEmailRecipients([
          "audit@example.com; manager@example.com",
          "audit@example.com,finance@example.com ops@example.com",
        ]),
      ).toEqual([
        "audit@example.com",
        "manager@example.com",
        "finance@example.com",
        "ops@example.com",
      ]);
    });

    it("should expose token separators for tag inputs", () => {
      expect(EMAIL_RECIPIENT_TOKEN_SEPARATORS).toEqual([
        ",",
        ";",
        " ",
        "\n",
        "\t",
      ]);
    });
  });

  describe("buildEmailRecipientOptions", () => {
    it("should label options with the person name so an email is found by name", () => {
      expect(
        buildEmailRecipientOptions([
          {
            username: "jdupont",
            firstName: "Jean",
            lastName: "Dupont",
            email: "jean.dupont@fintrack.test",
          },
        ]),
      ).toEqual([
        {
          label: "Jean Dupont (jean.dupont@fintrack.test)",
          value: "jean.dupont@fintrack.test",
        },
      ]);
    });

    it("should fall back to the username and skip users without an email", () => {
      expect(
        buildEmailRecipientOptions([
          { username: "ops", email: "ops@fintrack.test" },
          { username: "noemail" },
        ]),
      ).toEqual([
        { label: "ops (ops@fintrack.test)", value: "ops@fintrack.test" },
      ]);
    });
  });

  describe("hyperCase and sentenceCase utilities", () => {
    describe("toHyperCase", () => {
      it("should capitalize the first letter of a word and preserve the rest", () => {
        expect(toHyperCase("hello")).toBe("Hello");
        expect(toHyperCase("hello WORLD")).toBe("Hello WORLD");
      });

      it("should return empty string for empty inputs", () => {
        expect(toHyperCase("")).toBe("");
        expect(toHyperCase(undefined as unknown as string)).toBe("");
      });

      it("should capitalize only the first word if allWords is false", () => {
        expect(toHyperCase("jean-pierre", false)).toBe("Jean-pierre");
        expect(toHyperCase("jean dupont", false)).toBe("Jean dupont");
      });

      it("should capitalize all words if allWords is true", () => {
        expect(toHyperCase("jean-pierre", true)).toBe("Jean-Pierre");
        expect(toHyperCase("jean dupont", true)).toBe("Jean Dupont");
        expect(toHyperCase("very   high   priority", true)).toBe(
          "Very   High   Priority",
        );
      });
    });

    describe("hyperCaseFormItemProps", () => {
      const makeEvent = (value: string): ChangeEvent<HTMLInputElement> =>
        ({ target: { value } }) as ChangeEvent<HTMLInputElement>;

      it("should format first letter on input change event", () => {
        expect(
          hyperCaseFormItemProps.getValueFromEvent(makeEvent("hello world")),
        ).toBe("Hello world");
        expect(hyperCaseFormItemProps.getValueFromEvent(makeEvent(""))).toBe(
          "",
        );
      });
    });

    describe("hyperCaseAllWordsFormItemProps", () => {
      const makeEvent = (value: string): ChangeEvent<HTMLInputElement> =>
        ({ target: { value } }) as ChangeEvent<HTMLInputElement>;

      it("should format all words first letter on input change event", () => {
        expect(
          hyperCaseAllWordsFormItemProps.getValueFromEvent(
            makeEvent("jean dupont"),
          ),
        ).toBe("Jean Dupont");
        expect(
          hyperCaseAllWordsFormItemProps.getValueFromEvent(makeEvent("")),
        ).toBe("");
      });
    });

    describe("toSentenceCase and sentenceCaseFormItemProps", () => {
      it("should capitalize only the first letter of the first word and keep the rest unchanged", () => {
        expect(toSentenceCase("hello world")).toBe("Hello world");
        expect(toSentenceCase("hELLO WORLD")).toBe("HELLO WORLD");
        expect(toSentenceCase("")).toBe("");
      });

      it("should format first letter of first word on input change event", () => {
        const makeEvent = (value: string): ChangeEvent<HTMLInputElement> =>
          ({ target: { value } }) as ChangeEvent<HTMLInputElement>;
        expect(
          sentenceCaseFormItemProps.getValueFromEvent(
            makeEvent("lorem ipsum dolor sit amet"),
          ),
        ).toBe("Lorem ipsum dolor sit amet");
      });
    });

    describe("formatPresenceStatus", () => {
      const mockT = (key: string, options?: Record<string, unknown>) => {
        if (options?.time) return `${key}:${options.time}`;
        if (options?.date) return `${key}:${options.date}`;
        if (options?.count !== undefined) return `${key}:${options.count}`;
        return key;
      };

      it("should return online_default when connectedAt is unknown", () => {
        expect(formatPresenceStatus(null, true, mockT)).toBe(
          "layout.header.presence.status.online_default",
        );
      });

      it("should return online_since when connectedAt is known", () => {
        const date = "2026-07-24T14:32:00Z";
        const result = formatPresenceStatus(date, true, mockT);
        expect(result).toContain("layout.header.presence.status.online_since");
      });

      it("should return offline_unknown when disconnectedAt is unknown", () => {
        expect(formatPresenceStatus(null, false, mockT)).toBe(
          "layout.header.presence.status.offline_unknown",
        );
      });

      it("should return offline_ago_minutes when disconnected less than an hour ago", () => {
        const date = new Date(Date.now() - 5 * 60 * 1000).toISOString();
        const result = formatPresenceStatus(date, false, mockT);
        expect(result).toBe("layout.header.presence.status.offline_ago_minutes:5");
      });

      it("should return offline_ago_hours when disconnected between 1 and 24 hours ago", () => {
        const date = new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString();
        const result = formatPresenceStatus(date, false, mockT);
        expect(result).toBe("layout.header.presence.status.offline_ago_hours:3");
      });

      it("should return offline_date when disconnected more than 24 hours ago", () => {
        const date = "2020-01-01T10:00:00Z";
        const result = formatPresenceStatus(date, false, mockT);
        expect(result).toContain("layout.header.presence.status.offline_date");
      });
    });
  });
});
