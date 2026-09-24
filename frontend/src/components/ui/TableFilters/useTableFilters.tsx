// Filtres de tableau centralises : un bouton unique ouvrant un popover qui regroupe
// tous les filtres, plus une rangee de puces actives retirables. Reutilisable par
// toutes les listes (buildTableToolbar) pour une coherence generale.
import { useMemo, useState, type ReactNode } from "react";
import { Badge, Button, DatePicker, Popover, Select, Tag } from "antd";
import { FilterOutlined } from "@ant-design/icons";
import type { Dayjs } from "dayjs";
import { useTranslation } from "react-i18next";
import { formatDate } from "../../../utils/formatters/formatters";
import AppSwitch from "../AppSwitch/AppSwitch";
import styles from "./useTableFilters.module.scss";

type DateRange = [Dayjs | null, Dayjs | null] | null;

// Champ multi-selection (type, statut, criticite…).
export interface MultiSelectFilterField {
  key: string;
  type: "multiselect";
  label: string;
  options: { label: string; value: string }[];
  value: string[];
  onChange: (value: string[]) => void;
  placeholder?: string;
}

// Champ mono-selection (role, agence, statut unique…).
export interface SelectFilterField {
  key: string;
  type: "select";
  label: string;
  options: { label: string; value: string }[];
  value?: string;
  onChange: (value?: string) => void;
  placeholder?: string;
}

// Champ plage de dates.
export interface DateRangeFilterField {
  key: string;
  type: "daterange";
  label: string;
  value: DateRange;
  onChange: (value: DateRange) => void;
}

// Champ bascule (oui/non).
export interface ToggleFilterField {
  key: string;
  type: "toggle";
  label: string;
  value: boolean;
  onChange: (value: boolean) => void;
}

export type TableFilterField =
  | MultiSelectFilterField
  | SelectFilterField
  | DateRangeFilterField
  | ToggleFilterField;

export interface TableFiltersResult {
  // A placer dans la zone d'actions de la toolbar (buildTableToolbar `extra`).
  trigger: ReactNode;
  // A placer sous la toolbar (buildTableToolbar `activeFilters`).
  chips: ReactNode;
  activeCount: number;
}

const isActive = (field: TableFilterField): boolean => {
  if (field.type === "toggle") return field.value;
  if (field.type === "select") return field.value != null && field.value !== "";
  if (field.type === "daterange")
    return field.value != null && (field.value[0] != null || field.value[1] != null);
  return field.value.length > 0;
};

// Construit le bouton « Filtres » (avec compteur), son popover et les puces actives.
export function useTableFilters(
  fields: TableFilterField[],
): TableFiltersResult {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  const activeCount = useMemo(
    () => fields.reduce((count, field) => count + (isActive(field) ? 1 : 0), 0),
    [fields],
  );

  const reset = () =>
    fields.forEach((field) => {
      if (field.type === "toggle") field.onChange(false);
      else if (field.type === "select") field.onChange(undefined);
      else if (field.type === "daterange") field.onChange(null);
      else field.onChange([]);
    });

  // Les bascules sont regroupees en deux colonnes, separees des champs de saisie.
  const inputFields = fields.filter((field) => field.type !== "toggle");
  const toggleFields = fields.filter(
    (field): field is ToggleFilterField => field.type === "toggle",
  );

  const panel = (
    <div className={styles.panel}>
      {inputFields.map((field) => (
        <div key={field.key} className={styles.field}>
          <span className={styles.fieldLabel}>{field.label}</span>
          {field.type === "multiselect" ? (
            <Select
              mode="multiple"
              allowClear
              value={field.value}
              onChange={field.onChange}
              options={field.options}
              placeholder={field.placeholder ?? t("common.filters.select")}
              maxTagCount="responsive"
              className={styles.select}
            />
          ) : field.type === "select" ? (
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              value={field.value}
              onChange={(value) => field.onChange(value)}
              options={field.options}
              placeholder={field.placeholder ?? t("common.filters.select")}
              className={styles.select}
            />
          ) : field.type === "daterange" ? (
            <DatePicker.RangePicker
              value={field.value}
              onChange={(value) => field.onChange(value as DateRange)}
              className={styles.select}
            />
          ) : null}
        </div>
      ))}
      {toggleFields.length > 0 && (
        <div className={styles.toggles}>
          {toggleFields.map((field) => (
            <label
              key={field.key}
              className={styles.toggleField}
              title={field.label}
            >
              <AppSwitch
                size="small"
                checked={field.value}
                onChange={field.onChange}
                aria-label={field.label}
              />
              <span>{field.label}</span>
            </label>
          ))}
        </div>
      )}
      <div className={styles.footer}>
        <Button
          type="link"
          size="small"
          onClick={reset}
          disabled={activeCount === 0}
        >
          {t("common.reset")}
        </Button>
      </div>
    </div>
  );

  const trigger = (
    <Popover
      open={open}
      onOpenChange={setOpen}
      trigger="click"
      placement="bottomRight"
      content={panel}
    >
      <Badge count={activeCount} size="small" offset={[-4, 4]}>
        <Button icon={<FilterOutlined />}>{t("common.filters.button")}</Button>
      </Badge>
    </Popover>
  );

  const chipItems = fields
    .filter(isActive)
    .map((field) => {
      if (field.type === "toggle") {
        return {
          key: field.key,
          text: field.label,
          onRemove: () => field.onChange(false),
        };
      }
      if (field.type === "select") {
        const label =
          field.options.find((option) => option.value === field.value)?.label ??
          field.value;
        return {
          key: field.key,
          text: `${field.label} : ${label}`,
          onRemove: () => field.onChange(undefined),
        };
      }
      if (field.type === "daterange") {
        const from = field.value?.[0] ? formatDate(field.value[0].toDate()).split(" ")[0] : "…";
        const to = field.value?.[1] ? formatDate(field.value[1].toDate()).split(" ")[0] : "…";
        return {
          key: field.key,
          text: `${field.label} : ${from} – ${to}`,
          onRemove: () => field.onChange(null),
        };
      }
      const labels = field.value
        .map(
          (value) =>
            field.options.find((option) => option.value === value)?.label ??
            value,
        )
        .join(", ");
      return {
        key: field.key,
        text: `${field.label} : ${labels}`,
        onRemove: () => field.onChange([]),
      };
    });

  const chips =
    chipItems.length > 0 ? (
      <>
        <span className={styles.chipsLabel}>{t("common.filters.active")}</span>
        {chipItems.map((chip) => (
          <Tag
            key={chip.key}
            closable
            onClose={chip.onRemove}
            className={styles.chip}
          >
            {chip.text}
          </Tag>
        ))}
        <Button type="link" size="small" onClick={reset}>
          {t("common.reset")}
        </Button>
      </>
    ) : null;

  return { trigger, chips, activeCount };
}
