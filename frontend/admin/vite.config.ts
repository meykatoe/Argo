import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    fs: { allow: ['..'] },
    port: 5174,
    proxy: {
      // 開發時轉發後端
      // 轉送時帶上用戶端 IP，後端才分得出是誰
      '/api': { target: 'http://localhost:8080', xfwd: true },
    },
  },
  // 圖示等共用資源放在 shared
  publicDir: '../shared/public',
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@brand': fileURLToPath(new URL('../shared/public', import.meta.url)),
    },
  },
})
