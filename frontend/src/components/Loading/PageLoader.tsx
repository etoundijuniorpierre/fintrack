// Chargeur de page : affiche un spinner pendant le chargement, un message d'erreur, ou le contenu.
import { Spin, Button, Result } from "antd";
import { useTranslation } from "react-i18next";
import type { ReactNode } from "react";
import { formIcons } from "../../utils/icons/appIcons";
import styles from "./PageLoader.module.scss";

// Centralise la logique d'interface liee a page loader props.
export interface PageLoaderProps {
  isLoading: boolean;
  isError?: boolean;
  errorMessage?: string;
  onRetry?: () => void;
  children: ReactNode;
}

// Centralise la logique d'interface liee a page loader.
const PageLoader = ({
  isLoading,
  isError,
  errorMessage,
  onRetry,
  children,
}: PageLoaderProps) => {
  const { t } = useTranslation();

  if (isLoading) {
    return (
      <div
        className={styles.spinner}
        role="status"
        aria-label={t("common.loading")}
      >
        <Spin size="large" />
      </div>
    );
  }

  if (isError) {
    return (
      <Result
        icon={formIcons.warning}
        status="error"
        title={t("common.error")}
        subTitle={errorMessage || t("common.loading_error")}
        extra={
          onRetry ? (
            <Button type="primary" onClick={onRetry}>
              {t("common.retry")}
            </Button>
          ) : undefined
        }
      />
    );
  }

  return <>{children}</>;
};

export default PageLoader;
