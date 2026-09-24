// Ensemble de regles de validation pour les formulaires.

// Domaine de messagerie de la banque : seules ces adresses sont acceptees.
const COMPANY_EMAIL_DOMAIN = "finstar-cm.com";

// Valide la forme generale d'une adresse e-mail acceptée par le système.
export const isValidEmail = (email: string): boolean => {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email);
};

// Detecte une valeur vide ou composee uniquement d'espaces.
export const isBlank = (str: string | null | undefined): boolean => {
  return !str || str.trim().length === 0;
};

// Valide un numero de telephone au format international souple.
export const isValidPhone = (phone: string): boolean => {
  const re = /^[+]*[(]{0,1}[0-9]{1,4}[)]{0,1}[-\s./0-9]*$/;
  return re.test(phone);
};

// Restreint les adresses au domaine de la banque et a ses sous-domaines.
export const isValidEmailWithSubdomain = (email: string): boolean => {
  if (!email || !isValidEmail(email)) return false;
  const domain = email.split("@")[1].toLowerCase();
  return (
    domain === COMPANY_EMAIL_DOMAIN ||
    domain.endsWith(`.${COMPANY_EMAIL_DOMAIN}`)
  );
};
