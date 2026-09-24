// Interrupteur applicatif : Switch AntD dote par defaut des libelles d'etat
// "Active / Coupe", pour un rendu uniforme des bascules dans toute l'application.
import { Switch } from "antd";
import type { SwitchProps } from "antd";
import { useTranslation } from "react-i18next";

export type AppSwitchProps = SwitchProps;

// Rend un Switch avec libelles d'etat par defaut, surchargeable par appelant.
const AppSwitch = ({
  checkedChildren,
  unCheckedChildren,
  ...rest
}: AppSwitchProps) => {
  const { t } = useTranslation();
  return (
    <Switch
      checkedChildren={checkedChildren ?? t("common.switch.on")}
      unCheckedChildren={unCheckedChildren ?? t("common.switch.off")}
      {...rest}
    />
  );
};

export default AppSwitch;
