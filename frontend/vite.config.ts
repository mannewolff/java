/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
    // Performance-Budget: DashboardPage als Widget-schwerer Lazy-Chunk ist legitim > 500 kB.
    // Vendor-Bundle (index.js) enthält React + MUI + Router + OIDC — Floor ca. 485 kB.
    // Grenzwert 600 kB suppressiert Warning für bekannte Ausnahmen; neue Chunks > 600 kB
    // müssen geprüft werden. Gzip-Werte sind der relevante Maßstab (< 200 kB initial).
    chunkSizeWarningLimit: 600,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.ts'],
  },
});
