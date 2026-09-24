// Configuration par defaut des formats et tailles de pagination.

import type { TablePaginationConfig } from "antd";

// Fixe la taille de page initiale des tableaux.
export const DEFAULT_PAGE_SIZE = 10;

export const paginationConfig: TablePaginationConfig = {
  defaultPageSize: DEFAULT_PAGE_SIZE,
  pageSizeOptions: [10, 20, 50, 100],
  placement: ["bottomCenter"],
  showSizeChanger: true,
  showTotal: (total: number, range: [number, number]) =>
    `${range[0]}-${range[1]} / ${total}`,
};

// Fusionne la pagination standard avec les besoins d'un tableau.
export const getPaginationConfig = (
  overrides: TablePaginationConfig = {},
): TablePaginationConfig => {
  const config: TablePaginationConfig = {
    ...paginationConfig,
    ...overrides,
  };

  if (overrides.pageSize !== undefined) {
    delete config.defaultPageSize;
  }

  return config;
};
