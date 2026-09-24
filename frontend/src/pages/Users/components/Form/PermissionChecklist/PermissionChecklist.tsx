// Composant interactif pour cocher/selectionner des permissions.

import { Checkbox, Typography, Spin, Collapse, Badge } from "antd";
import { memo, useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import styles from "./PermissionChecklist.module.scss";

const { Text } = Typography;

// Centralise la logique d'interface liee a permission option.
export interface PermissionOption {
  value: string;
  label: string;
  desc?: string;
  fromRole?: boolean;
  name?: string;
}

// Definit les proprietes attendues par le composant PermissionChecklist.
interface PermissionChecklistProps {
  options: PermissionOption[];
  value?: string[];
  onChange?: (ids: string[]) => void;
  revokedValue?: string[];
  onRevokeChange?: (ids: string[]) => void;
  disabled?: boolean;
  loading?: boolean;
}

// Expose la constante KNOWN_GROUPS utilisee par permission checklist.
const KNOWN_GROUPS = [
  "INCIDENT",
  "USER",
  "ROLE",
  "REPORT",
  "DASHBOARD",
  "NOTIFICATION",
  "SETTINGS",
  "AUDIT",
] as const;

// Determine group e partir du contexte fourni.
const getGroup = (permissionName: string): string => {
  for (const g of KNOWN_GROUPS) {
    if (permissionName.startsWith(g + "_") || permissionName === g) return g;
  }
  return "OTHER";
};

// Rend le composant PermissionChecklist.
const PermissionChecklist = memo(
  ({
    options,
    value = [],
    onChange,
    revokedValue = [],
    onRevokeChange,
    disabled = false,
    loading = false,
  }: PermissionChecklistProps) => {
    const { t } = useTranslation();

    // Traite le changement de selection : une permission directe bascule dans la
    // selection ; une permission heritee d'un role bascule dans l'ensemble revoque.
    const handleToggle = useCallback(
      (option: PermissionOption) => {
        if (disabled) return;
        if (option.fromRole) {
          if (!onRevokeChange) return;
          const next = revokedValue.includes(option.value)
            ? revokedValue.filter((v) => v !== option.value)
            : [...revokedValue, option.value];
          onRevokeChange(next);
          return;
        }
        if (!onChange) return;
        const next = value.includes(option.value)
          ? value.filter((v) => v !== option.value)
          : [...value, option.value];
        onChange(next);
      },
      [value, onChange, revokedValue, onRevokeChange, disabled],
    );

    // Traite la selection d'un groupe.
    const handleToggleGroup = useCallback(
      (groupOptions: PermissionOption[]) => {
        if (!onChange || disabled) return;

        const groupIds = groupOptions
          .filter((option) => !option.fromRole)
          .map((option) => option.value);
        if (groupIds.length === 0) return;
        const allSelected = groupIds.every((id) => value.includes(id));
        const next = allSelected
          ? value.filter((id) => !groupIds.includes(id))
          : [...new Set([...value, ...groupIds])];

        onChange(next);
      },
      [value, onChange, disabled],
    );

    const groups = useMemo(() => {
      const map = new Map<string, PermissionOption[]>();
      for (const opt of options) {
        const group = opt.name ? getGroup(opt.name) : "OTHER";
        if (!map.has(group)) map.set(group, []);
        map.get(group)!.push(opt);
      }
      return map;
    }, [options]);

    const collapseItems = useMemo(
      () =>
        Array.from(groups.entries()).map(([groupKey, groupOptions]) => {
          const checkedCount = groupOptions.filter((o) =>
            o.fromRole
              ? !revokedValue.includes(o.value)
              : value.includes(o.value),
          ).length;
          const editableOptions = groupOptions.filter(
            (option) => !option.fromRole,
          );
          const selectedEditableCount = editableOptions.filter((option) =>
            value.includes(option.value),
          ).length;
          const allSelected =
            editableOptions.length > 0 &&
            selectedEditableCount === editableOptions.length;
          const partiallySelected =
            selectedEditableCount > 0 &&
            selectedEditableCount < editableOptions.length;
          const groupLabel = t(`users.permission_groups.${groupKey}`, groupKey);
          return {
            key: groupKey,
            label: (
              <span className={styles.groupHeader}>
                <span className={styles.groupTitle}>{groupLabel}</span>
                {checkedCount > 0 && (
                  <Badge count={checkedCount} className={styles.groupBadge} />
                )}
                <Checkbox
                  checked={allSelected}
                  disabled={disabled || editableOptions.length === 0}
                  indeterminate={partiallySelected}
                  className={styles.groupSelectAll}
                  onClick={(event) => event.stopPropagation()}
                  onChange={(event) => {
                    event.stopPropagation();
                    handleToggleGroup(groupOptions);
                  }}
                >
                  {t("users.permission_actions.select_all")}
                </Checkbox>
              </span>
            ),
            children: (
              <div className={styles.list} role="group" aria-label={groupLabel}>
                {groupOptions.map((option) => {
                  const isRevoked =
                    option.fromRole && revokedValue.includes(option.value);
                  const isChecked = option.fromRole
                    ? !isRevoked
                    : value.includes(option.value);
                  return (
                    <div
                      key={option.value}
                      className={`${styles.row} ${isChecked ? styles.rowChecked : styles.rowUnchecked} ${option.fromRole ? styles.rowFromRole : ""}`}
                    >
                      <Checkbox
                        checked={isChecked}
                        disabled={disabled}
                        onChange={() => handleToggle(option)}
                      >
                        <span className={styles.label}>
                          {option.label}
                          {option.fromRole && (
                            <Text type="secondary" className={styles.roleTag}>
                              {" "}
                              ({t("users.permission_actions.from_role")})
                            </Text>
                          )}
                          {isRevoked && (
                            <Text type="danger" className={styles.roleTag}>
                              {" "}
                              ({t("users.permission_actions.revoked")})
                            </Text>
                          )}
                        </span>
                        {option.desc && (
                          <Text type="secondary" className={styles.desc}>
                            {option.desc}
                          </Text>
                        )}
                      </Checkbox>
                    </div>
                  );
                })}
              </div>
            ),
          };
        }),
      [groups, value, revokedValue, disabled, handleToggle, handleToggleGroup, t],
    );

    if (loading) {
      return (
        <div className={styles.loadingWrapper}>
          <Spin size="small" />
        </div>
      );
    }

    return (
      <Collapse
        items={collapseItems}
        className={styles.collapse}
        ghost={false}
        size="small"
      />
    );
  },
);

PermissionChecklist.displayName = "PermissionChecklist";

export default PermissionChecklist;
