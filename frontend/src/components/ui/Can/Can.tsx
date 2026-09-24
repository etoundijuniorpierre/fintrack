// Garde de permission : affiche conditionnellement ses enfants selon les droits de l'utilisateur.
import { memo } from "react";
import type { ReactNode } from "react";
import { useAuth } from "../../../hooks/auth/useAuth";
import { hasAnyPermission } from "../../../utils/permissions/permissions";

// Verifie la regle d'interface liee a can props.
export interface CanProps {
  /** Affiche les enfants si l'utilisateur possède cette permission. */
  permission?: string;
  /** Affiche les enfants si l'utilisateur possède au moins une de ces permissions. */
  anyOf?: readonly string[];
  /** Contenu de repli si l'accès est refusé (par défaut : rien). */
  fallback?: ReactNode;
  children: ReactNode;
}

// Garde le rendu des enfants derriere une permission explicite.
const Can = memo(
  ({ permission, anyOf, fallback = null, children }: CanProps) => {
    const { hasPermission } = useAuth();

    const allowed = permission
      ? hasPermission(permission)
      : anyOf
        ? hasAnyPermission(hasPermission, anyOf)
        : false;

    return <>{allowed ? children : fallback}</>;
  },
);

Can.displayName = "Can";

export default Can;
