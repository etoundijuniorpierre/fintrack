// Bouton de retour : ramene a la page precedente, vers une route donnee ou execute un handler custom.
import { Button } from "antd";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { memo, useCallback } from "react";
import { navigationIcons } from "../../utils/icons/appIcons";
import styles from "./BackButton.module.scss";

// Definit les proprietes attendues par le composant BackButton.
interface BackButtonProps {
  navigateTo?: string;
  onClick?: () => void;
  testId?: string;
  pageName?: string;
  "aria-label"?: string;
}

// Rend le composant BackButton pour l'interface back bouton.
const BackButton = ({
  navigateTo,
  onClick,
  testId = "back-button",
  pageName,
  "aria-label": ariaLabel,
}: BackButtonProps) => {
  const navigate = useNavigate();
  const { t } = useTranslation();

  // Gere le clic : priorite au handler custom, puis a la route cible, sinon retour arriere.
  const handleClick = useCallback(() => {
    if (onClick) {
      onClick();
    } else if (navigateTo) {
      navigate(navigateTo);
    } else {
      navigate(-1);
    }
  }, [onClick, navigateTo, navigate]);

  return (
    <Button
      icon={navigationIcons.back}
      onClick={handleClick}
      type="text"
      className={styles.backButton}
      data-testid={pageName ? `${pageName}-${testId}` : testId}
      aria-label={ariaLabel || t("common.back")}
    />
  );
};

export default memo(BackButton);
