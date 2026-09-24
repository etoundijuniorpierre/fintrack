// Ouvre une image dans une fenetre dediee et lance l'impression.
// Construit le document via l'API DOM (pas de document.write interpole) : le nom
// de fichier, potentiellement fourni par un autre utilisateur, ne peut pas
// injecter de HTML/script dans la fenetre d'impression.
export const printImage = (src: string, filename: string): void => {
  if (!src) return;
  const printWindow = window.open("", "_blank");
  if (!printWindow) return;

  const doc = printWindow.document;
  // Affectation de propriete : sans interpretation HTML, donc sans injection.
  doc.title = filename;

  const style = doc.createElement("style");
  style.textContent =
    "body{margin:0;display:flex;justify-content:center;align-items:center;" +
    "min-height:100vh;background:#ffffff}" +
    "img{max-width:100%;max-height:100vh;object-fit:contain}" +
    "@media print{body{background:none}img{max-width:100%;height:auto}}";
  doc.head.appendChild(style);

  const img = doc.createElement("img");
  img.onload = () => {
    printWindow.print();
    printWindow.close();
  };
  img.src = src;
  doc.body.appendChild(img);
};
