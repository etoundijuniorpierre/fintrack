// Barriere d'erreur : intercepte les erreurs de rendu React et affiche un ecran de secours.
import { Component } from "react";
import type { ReactNode, ErrorInfo } from "react";
import { Result, Button } from "antd";
import i18n from "../../i18n";

// Centralise les proprietes exposees par l'ErrorBoundary.
export interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
}

// Definit l'etat interne du composant ErrorBoundary.
interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

// Rend le composant de capture d'erreur React.
class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
    };
  }

  // Met a jour l'etat pour declencher l'affichage de secours apres une erreur.
  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  // Journalise l'erreur capturee et ses informations de contexte.
  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error("[ErrorBoundary] Erreur capturee :", error, errorInfo);
  }

  render(): ReactNode {
    const { hasError } = this.state;
    const { children, fallback } = this.props;

    if (hasError) {
      if (fallback) {
        return fallback;
      }

      // Message generique : le detail technique de l'erreur n'est pas expose
      // a l'utilisateur (deja journalise dans componentDidCatch).
      return (
        <Result
          status="error"
          title={i18n.t("common.boundary_title")}
          subTitle={i18n.t("common.boundary_message")}
          extra={
            <Button type="primary" onClick={() => window.location.reload()}>
              {i18n.t("common.reload")}
            </Button>
          }
        />
      );
    }

    return children;
  }
}

export default ErrorBoundary;
