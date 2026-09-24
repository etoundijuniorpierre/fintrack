// Module frontend : regroupe la logique de main.

import dayjs from "dayjs";
import "dayjs/locale/fr";
dayjs.locale("fr");

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./index.scss";
import "./i18n";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
