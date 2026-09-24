// Mise en page principale : assemble la barre laterale, l'en-tete et la zone de contenu routee.
import { Layout, Drawer } from "antd";
import { Outlet } from "react-router-dom";
import Sidebar from "../Sidebar/Sidebar";
import AppHeader from "../Header/AppHeader";
import FirstLoginModal from "../FirstLoginModal/FirstLoginModal";
import { PageContainer } from "../Layout";
import ErrorBoundary from "../ErrorBoundary/ErrorBoundary";
import { useAuthStore } from "../../store/authStore/authStore";
import styles from "./MainLayout.module.scss";
import { memo, Suspense, useState, useCallback, useEffect } from "react";
import PageLoader from "../Loading/PageLoader";

const { Content } = Layout;

// Expose la constante MOBILE_BREAKPOINT utilisee par main layout.
const MOBILE_BREAKPOINT = 768;

// Rend le composant MainLayout pour l'interface main layout.
const MainLayout = () => {
  const { user, completeFirstLogin } = useAuthStore();
  // La modale de changement de mot de passe reste affichee tant que la
  // premiere connexion n'est pas finalisee (mot de passe definitif defini).
  const firstLoginOpen = Boolean(user?.isFirstLogin);
  const [isMobile, setIsMobile] = useState(
    () => window.innerWidth < MOBILE_BREAKPOINT,
  );
  const [drawerOpen, setDrawerOpen] = useState(false);

  // Surveille la taille de la fenetre pour basculer entre les modes mobile et bureau.
  useEffect(() => {
    // Traite le redimensionnement de la fenetre.
    const handleResize = () => {
      const mobile = window.innerWidth < MOBILE_BREAKPOINT;
      setIsMobile(mobile);
      if (!mobile) {
        setDrawerOpen(false);
      }
    };
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  // Traite la fermeture de l'accueil premiere connexion.
  const handleFirstLoginClose = useCallback(() => {
    completeFirstLogin();
  }, [completeFirstLogin]);

  // Traite la fermeture du menu lateral.
  const handleDrawerClose = useCallback(() => {
    setDrawerOpen(false);
  }, []);

  if (firstLoginOpen) {
    return (
      <Layout className={styles.page} data-testid="main-layout">
        <FirstLoginModal open={true} onClose={handleFirstLoginClose} />
      </Layout>
    );
  }

  return (
    <Layout className={styles.page} data-testid="main-layout">
      {!isMobile && <Sidebar />}

      {isMobile && (
        <Drawer
          placement="left"
          open={drawerOpen}
          onClose={handleDrawerClose}
          size={250}
          styles={{ body: { padding: 0 } }}
          className={styles.mobileDrawer}
          data-testid="mobile-sidebar-drawer"
        >
          <Sidebar />
        </Drawer>
      )}

      <Layout className={styles.mainArea}>
        <AppHeader
          onMobileMenuClick={isMobile ? () => setDrawerOpen(true) : undefined}
        />
        <Content className={styles.content} data-testid="app-content">
          <PageContainer className={styles.innerContent}>
            <ErrorBoundary>
              {/* Tant que la première connexion n'est pas finalisée, les pages
                  ne sont pas montées : l'utilisateur ne peut ni les consulter
                  ni déclencher leurs appels API derrière la modale. */}
              <Suspense
                fallback={
                  <PageLoader isLoading={true}>
                    <span />
                  </PageLoader>
                }
              >
                <Outlet />
              </Suspense>
            </ErrorBoundary>
          </PageContainer>
        </Content>
      </Layout>

      <FirstLoginModal open={firstLoginOpen} onClose={handleFirstLoginClose} />
    </Layout>
  );
};

export default memo(MainLayout);
