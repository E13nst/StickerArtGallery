import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  base: '/mini-app-react/',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: true
  },
  server: {
    port: 3000,
    proxy: {
      // Всегда проксируем на продакшн для разработки
      '/api': {
        target: 'https://stickerartgallery-e13nst.amvera.io',
        changeOrigin: true,
        secure: true,
        rewrite: (path) => path
      },
      '/auth': {
        target: 'https://stickerartgallery-e13nst.amvera.io',
        changeOrigin: true,
        secure: true,
        rewrite: (path) => path
      }
    }
  }
}))
