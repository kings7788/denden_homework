import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Dev server proxies API + WebSocket to the Spring backend on :8080,
// so during development the frontend behaves as if same-origin (no CORS).
export default defineConfig({
  plugins: [vue()],
  define: { global: 'window' }, // sockjs-client references Node's `global`
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws-stomp': { target: 'http://localhost:8080', ws: true, changeOrigin: true },
    },
  },
})
