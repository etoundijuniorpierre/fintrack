// Page principale d'administration de la gestion des utilisateurs.

import { memo } from "react";
import { Layout } from "antd";
import { Outlet } from "react-router-dom";
import Sidebar from "../../components/Sidebar/Sidebar";
import AppHeader from "../../components/Header/AppHeader";
import styles from "./Users.module.scss";

const { Content } = Layout;

// Rend le composant UsersWithLayout pour l'interface utilisateurs.
const UsersWithLayout = memo(() => {
  return (
    <Layout className={styles.page}>
      <Sidebar />
      <Layout className={styles.mainArea}>
        <AppHeader />
        <Content className={styles.content}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
});

UsersWithLayout.displayName = "UsersWithLayout";

export default UsersWithLayout;
