import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: "prompt",
      includeAssets: [
        "favicon.svg",
        "icons/*.png",
        "assets/*.webp",
        "assets/*.ttf",
        "data/*.json",
        "licenses/*.txt",
      ],
      manifest: {
        id: "/",
        name: "Waktu Solat Malaysia",
        short_name: "Waktu Solat",
        description: "Prayer times, Quran and daily practice",
        theme_color: "#FCF9F3",
        background_color: "#FCF9F3",
        display: "standalone",
        start_url: "/",
        scope: "/",
        lang: "ms",
        icons: [
          {
            src: "/icons/icon-192.png",
            sizes: "192x192",
            type: "image/png",
            purpose: "any",
          },
          {
            src: "/icons/icon-512.png",
            sizes: "512x512",
            type: "image/png",
            purpose: "any",
          },
          {
            src: "/icons/maskable-512.png",
            sizes: "512x512",
            type: "image/png",
            purpose: "maskable",
          },
        ],
      },
      workbox: {
        navigateFallback: "/index.html",
        navigateFallbackDenylist: [/^\/api\//],
        globPatterns: ["**/*.{js,css,html}"],
        cleanupOutdatedCaches: true,
      },
    }),
  ],
  server: {
    proxy: { "/api": { target: "http://127.0.0.1:8787", changeOrigin: true } },
  },
  build: { sourcemap: true },
});
