import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    // threads : le cache bytecode V8 est réutilisé entre fichiers d'un même worker,
    // donc l'évaluation d'antd (lourd) est bien plus rapide qu'avec des process forkés,
    // tout en gardant l'isolation par fichier (pas de pollution d'état).
    pool: 'threads',
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    // Les tests d'integration de page rendent tout le tableau de bord (une trentaine
    // de widgets) puis son panneau de configuration : ~16 s isoles, davantage quand
    // plusieurs fichiers tournent en parallele. 30 s ne suffisaient plus.
    testTimeout: 60000,
    coverage: {
      provider: 'v8',
      // Produire le rapport même si un test échoue (sinon pas de coverage-summary.json).
      reportOnFailure: true,
      reporter: ['text-summary', 'json-summary', 'lcov', 'html', 'cobertura'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/test/**', 'src/**/*.d.ts', 'src/main.tsx', 'src/vite-env.d.ts',
        'src/**/*.test.{ts,tsx}',
        'src/**/index.ts', 'src/**/*.types.ts', 'src/**/types.ts',
        'src/**/constants*.ts', 'src/**/routes.ts', 'src/**/endpoints/**',
        'src/App.tsx', 'src/**/*.module.scss',
      ],
      // Plancher anti-régression, juste sous le mesuré (stmt 76.4 / br 67.1 / fn 70.3 / ln 77.2).
      // À remonter progressivement à mesure que la couverture augmente.
      thresholds: { branches: 66, lines: 76, functions: 69, statements: 75 },
    },
  },
});
