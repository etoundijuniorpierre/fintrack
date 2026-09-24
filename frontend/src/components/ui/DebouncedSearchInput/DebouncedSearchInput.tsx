import React, { useState, useEffect } from "react";
import { Input } from "antd";
import { actionIcons } from "../../../utils/icons/appIcons";
import styles from "../../../utils/table/tableToolbar.module.scss";

interface DebouncedSearchInputProps {
  initialValue?: string;
  onSearch: (value: string) => void;
  placeholder?: string;
  delay?: number;
}

export const DebouncedSearchInput: React.FC<DebouncedSearchInputProps> = ({
  initialValue = "",
  onSearch,
  placeholder = "Search...",
  delay = 500,
}) => {
  const [value, setValue] = useState(initialValue);

  useEffect(() => {
    const handler = setTimeout(() => {
      onSearch(value);
    }, delay);

    return () => clearTimeout(handler);
  }, [value, delay, onSearch]);

  return (
    <Input
      size="small"
      placeholder={placeholder}
      prefix={actionIcons.search}
      value={value}
      onChange={(e) => setValue(e.target.value)}
      allowClear
      className={styles.searchInput}
    />
  );
};
