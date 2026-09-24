// Selecteur de langue : permet de changer la langue de l'interface via i18next.
import { Select, Space } from "antd";
import { useTranslation } from "react-i18next";
import { useCallback, memo, useMemo } from "react";
import { headerIcons } from "../../utils/icons/appIcons";
import styles from "./LanguageSelector.module.scss";

// Rend le composant LanguageSelector pour l'interface language selecteur.
const LanguageSelector = () => {
  const { i18n } = useTranslation();

  // Change la langue active de l'application.
  const handleChange = useCallback(
    (value: string) => {
      i18n.changeLanguage(value);
    },
    [i18n],
  );

  const options = useMemo(
    () => [
      {
        value: "fr",
        label: (
          <Space>
            <span role="img" aria-label="French">
              🇫🇷
            </span>
            <span>Français</span>
          </Space>
        ),
      },
      {
        value: "en",
        label: (
          <Space>
            <span role="img" aria-label="English">
              🇺🇸
            </span>
            <span>English</span>
          </Space>
        ),
      },
    ],
    [],
  );

  return (
    <div className={styles.container} aria-label="language-selector">
      <Select
        value={i18n.language.split("-")[0]}
        onChange={handleChange}
        variant="borderless"
        className={styles.select}
        classNames={{
          popup: {
            root: styles.dropdown,
          },
        }}
        options={options}
        suffixIcon={<span className={styles.icon}>{headerIcons.global}</span>}
      />
    </div>
  );
};

export default memo(LanguageSelector);
