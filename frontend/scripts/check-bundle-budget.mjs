// Controle les budgets de bundle frontend apres un build Vite.
import { readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

const assetsDir = join(process.cwd(), 'dist', 'assets');
const maxChunkKb = Number(process.env.BUNDLE_MAX_CHUNK_KB ?? 500);
const maxTotalKb = Number(process.env.BUNDLE_MAX_TOTAL_KB ?? 3500);

// Convertit les octets en kilo-octets pour un rapport lisible.
const toKb = (bytes) => Math.round((bytes / 1024) * 10) / 10;

// Recupere les fichiers JavaScript produits par le build.
const files = readdirSync(assetsDir)
  .filter((file) => file.endsWith('.js'))
  .map((file) => {
    const path = join(assetsDir, file);
    return { file, sizeKb: toKb(statSync(path).size) };
  })
  .sort((a, b) => b.sizeKb - a.sizeKb);

const totalKb = files.reduce((sum, file) => sum + file.sizeKb, 0);
const oversized = files.filter((file) => file.sizeKb > maxChunkKb);

console.log(`Bundle JS total: ${totalKb} KB (budget ${maxTotalKb} KB)`);
files.slice(0, 10).forEach((file) => console.log(`${file.sizeKb} KB  ${file.file}`));

if (totalKb > maxTotalKb || oversized.length > 0) {
  console.error('Bundle budget exceeded.');
  process.exit(1);
}
