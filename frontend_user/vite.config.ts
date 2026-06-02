import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ArcoResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    // Arco 按需导入：模板中 <a-xxx> 自动引入对应组件与样式，替代全量注册
    Components({
      resolvers: [ArcoResolver({ sideEffect: true })],
      dts: false,
    }),
  ],
  define: {
    // sockjs-client 等依赖在浏览器环境引用 Node.js 的 global，
    // 提供 polyfill 避免 ReferenceError: global is not defined 导致白屏
    global: 'globalThis',
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5174,
    // 端口被占用时直接报错而非静默换端口：后端 CORS 白名单按端口匹配，
    // 静默换端口会导致所有请求 403，报错更直观
    strictPort: true,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
    headers: {
      'Content-Security-Policy':
        "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self' data:; connect-src 'self' ws: wss:;",
    },
  },
  esbuild: {
    // 生产构建时移除 debugger，并将 console.log/debug/info 标记为无副作用供 tree-shake；
    // 保留 console.error/warn。比手写正则替换安全（正则无法处理嵌套括号）
    drop: process.env.NODE_ENV === 'production' ? ['debugger'] : [],
    pure: process.env.NODE_ENV === 'production'
      ? ['console.log', 'console.debug', 'console.info']
      : [],
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-arco': ['@arco-design/web-vue'],
        },
      },
    },
  },
})
