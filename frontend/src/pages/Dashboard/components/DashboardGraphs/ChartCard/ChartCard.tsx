// Composant React : porte l'interface de chart carte.

import { useCallback, useRef } from "react";
import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { SectionCard } from "../../../../../components/Layout";
import { IconOnlyButton } from "../../../../../components/ui";
import {
  COLORS,
  type ChartExportRow,
} from "../../../../../utils/dashboard/graphUtils/graphUtils";
import { actionIcons } from "../../../../../utils/icons/appIcons";
import styles from "../DashboardGraphs.module.scss";
import { downloadFile } from "../../../../../utils/download/downloadFile";

// Definit les proprietes attendues par le composant ChartCard.
interface ChartCardProps {
  title: string;
  filename: string;
  rows: ChartExportRow[];
  toolbar?: ReactNode;
  children: ReactNode;
}

// Securise une valeur exportee pour chart carte.
const escapeCsv = (value: string | number) => {
  const text = String(value);
  return /[",\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
};

// Prepare l'affichage lisible de chart carte.
const formatExportValue = (value: number) =>
  Number.isInteger(value) ? String(value) : value.toFixed(2);

// Normalise les valeurs de chart carte avant utilisation.
const sanitizeFilename = (value: string) =>
  value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-zA-Z0-9-_]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase();

// Ajuste le rendu visuel pour chart carte.
const fitCanvasText = (
  context: CanvasRenderingContext2D,
  value: string,
  maxWidth: number,
) => {
  if (context.measureText(value).width <= maxWidth) {
    return value;
  }

  let truncated = value;
  while (
    truncated.length > 3 &&
    context.measureText(`${truncated}...`).width > maxWidth
  ) {
    truncated = truncated.slice(0, -1);
  }
  return `${truncated}...`;
};

// Centralise la logique d'interface liee a draw legend.
const drawLegend = (
  context: CanvasRenderingContext2D,
  rows: ChartExportRow[],
  x: number,
  y: number,
  width: number,
  legendTitle: string,
) => {
  if (rows.length === 0) {
    return y;
  }

  const columns = width >= 760 ? 2 : 1;
  const columnWidth = Math.floor(width / columns);
  const rowHeight = 24;

  context.fillStyle = "#111827";
  context.font = "600 15px Arial, sans-serif";
  context.fillText(legendTitle, x, y);

  context.font = "12px Arial, sans-serif";
  rows.forEach((row, index) => {
    const col = index % columns;
    const line = Math.floor(index / columns);
    const itemX = x + col * columnWidth;
    const itemY = y + 22 + line * rowHeight;
    const label = fitCanvasText(
      context,
      `${row.label} : ${formatExportValue(row.value)}`,
      columnWidth - 26,
    );

    context.fillStyle = row.color || COLORS[index % COLORS.length];
    context.fillRect(itemX, itemY - 10, 10, 10);
    context.fillStyle = "#374151";
    context.fillText(label, itemX + 16, itemY);
  });

  return y + 22 + Math.ceil(rows.length / columns) * rowHeight;
};

// Centralise la logique d'interface liee a load svg image.
const loadSvgImage = (
  svg: SVGSVGElement,
): Promise<{ image: HTMLImageElement; width: number; height: number }> => {
  const clone = svg.cloneNode(true) as SVGSVGElement;
  if (!clone.getAttribute("xmlns")) {
    clone.setAttribute("xmlns", "http://www.w3.org/2000/svg");
  }
  const rect = svg.getBoundingClientRect();
  const width = Math.max(Math.ceil(rect.width), 320);
  const height = Math.max(Math.ceil(rect.height), 220);
  clone.setAttribute("width", String(width));
  clone.setAttribute("height", String(height));

  const svgBlob = new Blob([new XMLSerializer().serializeToString(clone)], {
    type: "image/svg+xml;charset=utf-8",
  });
  const url = window.URL.createObjectURL(svgBlob);

  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => {
      window.URL.revokeObjectURL(url);
      resolve({ image, width, height });
    };
    image.onerror = () => {
      window.URL.revokeObjectURL(url);
      reject(new Error("Unable to render chart SVG"));
    };
    image.src = url;
  });
};

// Centralise la logique d'interface liee a chart card.
const ChartCard = ({
  title,
  filename,
  rows,
  toolbar,
  children,
}: ChartCardProps) => {
  const { t } = useTranslation();
  const chartRef = useRef<HTMLDivElement>(null);
  const canExport = rows.length > 0;
  const filenameBase = sanitizeFilename(filename || title) || "dashboard-chart";

  // Traite l'export CSV.
  const handleExportCsv = useCallback(() => {
    const header = ["label", "value"];
    const body = rows.map((row) =>
      [row.label, formatExportValue(row.value)].map(escapeCsv).join(","),
    );
    downloadFile(
      [header.join(","), ...body].join("\n"),
      `${filenameBase}-${new Date().toISOString().slice(0, 10)}.csv`,
      "text/csv;charset=utf-8",
    );
  }, [filenameBase, rows]);

  // Traite l'export PNG.
  const handleExportPng = useCallback(async () => {
    const svg = chartRef.current?.querySelector("svg");
    if (!svg) {
      return;
    }

    const renderedChart = await loadSvgImage(svg);
    const padding = 28;
    const titleHeight = 42;
    const legendWidth = Math.max(renderedChart.width, 640);
    const columns = legendWidth >= 760 ? 2 : 1;
    const legendHeight =
      rows.length > 0 ? 30 + Math.ceil(rows.length / columns) * 24 : 0;
    const width = Math.max(renderedChart.width, legendWidth) + padding * 2;
    const height =
      padding + titleHeight + renderedChart.height + legendHeight + padding;
    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext("2d");
    if (!context) {
      return;
    }

    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, width, height);
    context.fillStyle = "#111827";
    context.font = "700 18px Arial, sans-serif";
    context.fillText(title, padding, padding + 4);
    context.strokeStyle = "#caa22e";
    context.lineWidth = 2;
    context.beginPath();
    context.moveTo(padding, padding + 18);
    context.lineTo(width - padding, padding + 18);
    context.stroke();

    const chartX =
      padding + Math.max(0, (width - padding * 2 - renderedChart.width) / 2);
    const chartY = padding + titleHeight;
    context.drawImage(
      renderedChart.image,
      chartX,
      chartY,
      renderedChart.width,
      renderedChart.height,
    );
    drawLegend(
      context,
      rows,
      padding,
      chartY + renderedChart.height + 24,
      width - padding * 2,
      t("dashboard.graphs.legend"),
    );

    canvas.toBlob((blob) => {
      if (blob) {
        downloadFile(
          blob,
          `${filenameBase}-${new Date().toISOString().slice(0, 10)}.png`,
          "image/png",
        );
      }
    }, "image/png");
  }, [filenameBase, rows, t, title]);

  const exportToolbar = (
    <div className={styles.chartCardToolbar}>
      {toolbar && <div className={styles.chartToolbarSlot}>{toolbar}</div>}
      <IconOnlyButton
        variant="secondary"
        size="sm"
        label={t("dashboard.graphs.export_chart_csv", { chart: title })}
        icon={actionIcons.download}
        disabled={!canExport}
        onClick={handleExportCsv}
      />
      <IconOnlyButton
        variant="secondary"
        size="sm"
        label={t("dashboard.graphs.export_chart_png", { chart: title })}
        icon={actionIcons.download}
        disabled={!canExport}
        onClick={handleExportPng}
      />
    </div>
  );

  return (
    <SectionCard title={title} toolbar={exportToolbar}>
      <div ref={chartRef}>{children}</div>
    </SectionCard>
  );
};

export default ChartCard;
