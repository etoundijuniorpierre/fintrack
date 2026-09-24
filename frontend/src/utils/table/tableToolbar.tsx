// Utilitaire de toolbar tableau : construit le bloc recherche, filtres et action principale.

import type { ReactNode } from "react";
import { DebouncedSearchInput } from "../../components/ui";
import styles from "./tableToolbar.module.scss";

// Decrit les options de composition d'une toolbar de tableau.
export interface TableToolbarOptions {
  searchValue: string;
  onSearch: (e: React.ChangeEvent<HTMLInputElement>) => void;
  searchPlaceholder?: string;
  leading?: ReactNode;
  addButton?: ReactNode;
  extra?: ReactNode;
  // Rangee de puces de filtres actifs, affichee sous la barre (useTableFilters).
  activeFilters?: ReactNode;
}

// Construit la toolbar standard des listes de gestion.
export function buildTableToolbar({
  searchValue,
  onSearch,
  leading,
  searchPlaceholder = "Search\u2026",
  addButton,
  extra,
  activeFilters,
}: TableToolbarOptions): ReactNode {
  return (
    <div className={styles.toolbarWrapper}>
      <div className={styles.toolbar}>
        <div className={styles.leadingSection}>
          {leading}
          <div className={styles.searchSection}>
            <DebouncedSearchInput
              initialValue={searchValue}
              placeholder={searchPlaceholder}
              onSearch={(val) => {
                onSearch({
                  target: { value: val },
                } as React.ChangeEvent<HTMLInputElement>);
              }}
            />
          </div>
        </div>
        <div className={styles.actionSection}>
          {extra}
          {addButton}
        </div>
      </div>
      {activeFilters ? (
        <div className={styles.activeFilters}>{activeFilters}</div>
      ) : null}
    </div>
  );
}
