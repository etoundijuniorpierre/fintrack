// Page de connexion principale de l'application.
import { memo, Suspense } from "react";
import { Spin, Button } from "antd";
import LoginBanner from "./components/Banner/LoginBanner";
import LoginForm from "./components/Form/LoginForm/LoginForm";
import LanguageSelector from "../../components/LanguageSelector/LanguageSelector";
import styles from "./Login.module.scss";
import { useThemeStore } from "../../store/themeStore";
import { headerIcons } from "../../utils/icons/appIcons";

// Rend le composant Login pour l'interface connexion.
const Login = () => {
  const { mode, toggleTheme } = useThemeStore();

  return (
    <main className={styles.page}>
      <header className={styles.topBar}>
        <Button
          type="text"
          icon={mode === "dark" ? headerIcons.sun : headerIcons.moon}
          onClick={toggleTheme}
          title={mode === "dark" ? "Mode Clair" : "Mode Sombre"}
          aria-label="Toggle Theme"
          className={styles.themeToggleBtn}
        />
        <LanguageSelector />
      </header>
      <Suspense
        fallback={
          <div className={styles.suspenseFallback}>
            <Spin size="large" />
          </div>
        }
      >
        <div className={styles.content}>
          <LoginBanner />
          <LoginForm />
        </div>
      </Suspense>
    </main>
  );
};

export default memo(Login);
