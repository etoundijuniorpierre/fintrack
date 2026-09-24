// Banniere de connexion : surface de marque sobre (sans illustration),
// motif geometrique evoquant la tracabilite, logo et propositions de valeur.

import { useTranslation } from "react-i18next";
import { memo, useMemo } from "react";
import {
  ThunderboltOutlined,
  FileTextOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons";
import styles from "./LoginBanner.module.scss";

// Rend le composant LoginBanner pour l'interface connexion banner.
const LoginBanner = () => {
  const { t } = useTranslation();

  const features = useMemo(
    () => [
      {
        icon: <ThunderboltOutlined />,
        label: t("login.banner.features.realtime"),
      },
      { icon: <FileTextOutlined />, label: t("login.banner.features.reports") },
      {
        icon: <SafetyCertificateOutlined />,
        label: t("login.banner.features.audit"),
      },
    ],
    [t],
  );

  return (
    <aside className={styles.banner} aria-label={t("login.title")}>
      <div className={styles.motif} aria-hidden="true" />

      <img src="/Img/logo-dark.svg" alt="FinTrack" className={styles.logo} />

      <div className={styles.content}>
        <h1 className={styles.tagline}>
          {t("login.banner.tagline")}{" "}
          <span className={styles.highlight}>
            {t("login.banner.tagline_highlight")}
          </span>
        </h1>

        <p className={styles.description}>{t("login.banner.description")}</p>

        <ul
          className={styles.featureList}
          aria-label={t("login.banner.features_label")}
        >
          {features.map((feature) => (
            <li key={feature.label} className={styles.featureItem}>
              <span className={styles.featureIcon} aria-hidden="true">
                {feature.icon}
              </span>
              {feature.label}
            </li>
          ))}
        </ul>
      </div>
    </aside>
  );
};

export default memo(LoginBanner);
