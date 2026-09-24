// Grille de donnees : tableau generique qui bascule en mode virtualise au-dela d'un seuil de lignes.
import React, { memo, useMemo, useCallback } from "react";
import { Table } from "antd";
import type { ColumnsType, TableProps } from "antd/es/table";
import { useTranslation } from "react-i18next";
import clsx from "clsx";
import VirtualizedTable from "../../VirtualizedTable/VirtualizedTable";
import styles from "./DataGrid.module.scss";
import type { AntdSize } from "../types";

// Expose la constante DEFAULT_VIRTUALIZATION_THRESHOLD utilisee par data grid.
const DEFAULT_VIRTUALIZATION_THRESHOLD = 100;

// Centralise la logique d'interface liee a data grid props.
export interface DataGridProps<T extends object = Record<string, unknown>> {
  data: T[];
  columns: ColumnsType<T>;
  rowKey?: keyof T | ((record: T) => string | number);
  onRowClick?: (record: T, index?: number) => void;
  emptyState?: React.ReactNode;
  virtualizationThreshold?: number;
  loading?: boolean;
  pagination?: TableProps<T>["pagination"];
  onChange?: TableProps<T>["onChange"];
  size?: AntdSize;
  className?: string;
  style?: React.CSSProperties;
  showHeader?: boolean;
  scroll?: TableProps<T>["scroll"];
}

// Rend le composant DataGrid.
const DataGrid = memo(
  <T extends object = Record<string, unknown>>({
    data,
    columns,
    rowKey = "id" as keyof T,
    onRowClick,
    emptyState,
    virtualizationThreshold = DEFAULT_VIRTUALIZATION_THRESHOLD,
    loading = false,
    pagination = false,
    onChange,
    size = "middle",
    className,
    style,
    showHeader = true,
    scroll,
  }: DataGridProps<T>) => {
    const { t } = useTranslation();
    const isEmpty = !loading && data.length === 0;
    const resolvedScroll = isEmpty ? undefined : scroll;

    // Resout la cle unique de chaque ligne, qu'elle soit fournie comme champ ou comme fonction.
    const resolvedRowKey = useCallback(
      (record: T): string => {
        if (typeof rowKey === "function") {
          return String(rowKey(record));
        }
        return String(record[rowKey]);
      },
      [rowKey],
    );

    // Attache les gestionnaires d'interaction (clic, touche Entree, focus) a chaque ligne cliquable.
    const onRow = useCallback(
      (record: T, index?: number) => ({
        onClick: onRowClick ? () => onRowClick(record, index) : undefined,
        onKeyDown: onRowClick
          ? (e: React.KeyboardEvent<HTMLElement>) => {
              if (e.key === "Enter") {
                onRowClick(record, index);
              }
            }
          : undefined,
        tabIndex: onRowClick ? 0 : undefined,
        className: onRowClick ? styles.clickableRow : undefined,
      }),
      [onRowClick],
    );

    // On fournit toujours les libelles des boutons de filtre (OK / Reinitialiser)
    // et le placeholder de recherche : passer un objet locale partiel (ex. juste
    // emptyText) ecrase l'heritage du ConfigProvider et laissait ces boutons sans
    // texte. On y ajoute emptyText seulement quand un etat vide est fourni.
    const locale = useMemo(
      () => ({
        filterConfirm: t("common.ok"),
        filterReset: t("common.reset"),
        filterSearchPlaceholder: t("common.search"),
        ...(emptyState ? { emptyText: emptyState } : {}),
      }),
      [emptyState, t],
    );

    const tableStyle = useMemo(
      (): React.CSSProperties => ({ ...style }),
      [style],
    );

    // Au-dela du seuil, delegue le rendu au tableau virtualise pour preserver les performances.
    if (!loading && data.length > virtualizationThreshold) {
      return (
        <VirtualizedTable
          data={data as Record<string, unknown>[]}
          columns={columns as ColumnsType<Record<string, unknown>>}
          loading={loading}
        />
      );
    }

    return (
      <Table<T>
        dataSource={data}
        columns={columns}
        rowKey={resolvedRowKey}
        onRow={onRow}
        locale={locale}
        loading={loading}
        pagination={pagination}
        onChange={onChange}
        size={size}
        className={clsx(styles.table, isEmpty && styles.emptyTable, className)}
        style={tableStyle}
        showHeader={showHeader}
        scroll={resolvedScroll}
      />
    );
  },
) as <T extends object = Record<string, unknown>>(
  props: DataGridProps<T>,
) => React.ReactElement;

(DataGrid as React.FC).displayName = "DataGrid";

export default DataGrid;
