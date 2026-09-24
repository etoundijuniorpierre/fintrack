/**
 * Le token d'accès JWT est conservé uniquement en mémoire (variable de module)
 * et jamais dans le localStorage : cela évite qu'une faille XSS puisse l'exfiltrer.
 * Au rechargement de la page, le token est perdu puis restauré de façon
 * transparente via le refresh-token (cookie httpOnly) par l'intercepteur 401.
 */
let inMemoryToken: string | null = null;

export const tokenManager = {
  getToken(): string | null {
    return inMemoryToken;
  },

  setToken(token: string): void {
    inMemoryToken = token;
  },

  removeToken(): void {
    inMemoryToken = null;
  },

  hasToken(): boolean {
    return inMemoryToken !== null;
  },
};
