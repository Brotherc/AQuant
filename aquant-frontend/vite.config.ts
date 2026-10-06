import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/ai-api': {
        target: 'http://127.0.0.1:8085',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/ai-api/, '/api')
      },
      '/api': {
        target: 'http://127.0.0.1:8084',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
