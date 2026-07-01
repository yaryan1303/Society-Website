import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The frontend talks to the Spring Boot API. In dev we proxy /api -> :8080 so
// there are no CORS surprises; in production VITE_API_URL can point anywhere.
export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        // Split heavy/stable vendors into their own cacheable chunks so charts
        // don't bloat the initial load.
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          charts: ['recharts'],
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});
