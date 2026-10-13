import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The frontend calls the API through a same-origin `/api` path and Vite proxies
// it to the Spring Boot backend (default :8081). This avoids CORS in development
// and keeps the API base configurable for other deployments.
const apiTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8081'
const proxy = {
  '/api': {
    target: apiTarget,
    changeOrigin: true,
  },
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy,
  },
  preview: {
    port: 4173,
    proxy,
  },
})
