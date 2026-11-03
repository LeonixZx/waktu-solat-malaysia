import React from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { initialize, db, runDownloads } from "./data";
import "./style.css";
void initialize().then(() => {
  createRoot(document.getElementById("root")!).render(
    <React.StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </React.StrictMode>,
  );
  if (navigator.onLine)
    void db.get<number[]>("downloadQueue").then((queue) => {
      if (queue?.length) void runDownloads().catch(() => {});
    });
});
