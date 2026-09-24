// Bouton de la barre d'outils ouvrant le choix des ordres predefinis d'un tableau.

import { Dropdown } from "antd";
import type { MenuProps } from "antd";
import { IconOnlyButton } from "../IconOnlyControl/IconOnlyControl";
import { actionIcons } from "../../../utils/icons/appIcons";
import type { SortPresetConfig } from "../../../utils/table/sorting/tableSorting";

interface TableSortControlProps<T extends string> {
  presets: Record<T, SortPresetConfig>;
  // Libelles deja traduits, indexes par preset.
  labels: Record<T, string>;
  label: string;
  activePreset?: T;
  onSelect: (preset: T) => void;
}

// Rend le composant TableSortControl pour les barres d'outils de tableaux.
const TableSortControl = <T extends string>({
  presets,
  labels,
  label,
  activePreset,
  onSelect,
}: TableSortControlProps<T>) => {
  const menu: MenuProps = {
    selectable: true,
    selectedKeys: activePreset ? [activePreset] : [],
    items: (Object.keys(presets) as T[]).map((preset) => ({
      key: preset,
      label: labels[preset],
    })),
    onClick: ({ key }) => onSelect(key as T),
  };

  return (
    <Dropdown menu={menu} trigger={["click"]} placement="bottomLeft">
      <span>
        <IconOnlyButton
          variant="text"
          size="sm"
          icon={actionIcons.sort}
          label={label}
        />
      </span>
    </Dropdown>
  );
};

export default TableSortControl;
