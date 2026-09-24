// Tableau virtualise : affiche de grandes listes avec react-window pour ne rendre que les lignes visibles.
import { memo, useMemo, useCallback } from "react";
import { FixedSizeList as List } from "react-window";
import { Table } from "antd";
import type { ColumnsType } from "antd/es/table";

// Definit les proprietes attendues par le composant VirtualizedTable.
interface VirtualizedTableProps<
  T extends Record<string, unknown> = Record<string, unknown>,
> {
  data: T[];
  columns: ColumnsType<T>;
  height?: number;
  itemHeight?: number;
  loading?: boolean;
}

// Rend le composant VirtualizedTable.
const VirtualizedTable = memo(
  <T extends Record<string, unknown> = Record<string, unknown>>({
    data,
    columns,
    height = 400,
    itemHeight = 54,
    loading = false,
  }: VirtualizedTableProps<T>) => {
    // Rendu d'une ligne individuelle de la liste virtualisee.
    const Row = useCallback(
      ({ index, style }: { index: number; style: React.CSSProperties }) => {
        const item = data[index];

        return (
          <div style={style}>
            <Table
              dataSource={[item]}
              columns={columns}
              pagination={false}
              showHeader={false}
              size="small"
              rowKey={(record) =>
                String((record as Record<string, unknown>).id ?? index)
              }
            />
          </div>
        );
      },
      [data, columns],
    );

    // Liste virtualisee memorisee pour eviter les recreations inutiles.
    const memoizedList = useMemo(
      () => (
        <List
          height={height}
          itemCount={data.length}
          itemSize={itemHeight}
          width="100%"
        >
          {Row}
        </List>
      ),
      [data.length, height, itemHeight, Row],
    );

    if (loading) {
      return (
        <Table
          dataSource={[]}
          columns={columns}
          loading={true}
          pagination={false}
        />
      );
    }

    if (data.length === 0) {
      return <Table dataSource={[]} columns={columns} pagination={false} />;
    }

    return (
      <div>
        <Table
          dataSource={[]}
          columns={columns}
          pagination={false}
          showHeader={true}
          size="small"
        />
        {memoizedList}
      </div>
    );
  },
);

VirtualizedTable.displayName = "VirtualizedTable";

export default VirtualizedTable;
