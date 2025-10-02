import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const isDev = mode === 'development';
  
  return {
    plugins: [react()],
    base: isDev ? '/' : '/mini-app-react/',
    
    // Use index.dev.html for development, index.html for production
    ...(isDev && {
      root: '.',
      publicDir: 'public',
    }),
    
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: true,
      rollupOptions: {
        input: isDev ? 'index.dev.html' : 'index.html'
      }
    },
    
    server: {
      port: 3000,
      host: true,
      // Используем index.dev.html для dev режима
      ...(isDev && {
        open: '/index.dev.html'
      }),
      proxy: {
        // Проксируем API запросы на продакшн сервер
        '/api': {
          target: 'https://stickerartgallery-e13nst.amvera.io',
          changeOrigin: true,
          secure: true,
          rewrite: (path) => path,
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('proxy error', err);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              console.log('🔍 Проксируем запрос:', req.method, req.url);
              console.log('🔍 Заголовки запроса:', req.headers);
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
            });
          },
        },
        '/auth': {
          target: 'https://stickerartgallery-e13nst.amvera.io',
          changeOrigin: true,
          secure: true,
          rewrite: (path) => path,
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('proxy error', err);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              console.log('🔍 Проксируем запрос:', req.method, req.url);
              console.log('🔍 Заголовки запроса:', req.headers);
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
            });
          },
        }
      }
    }
  }
});
