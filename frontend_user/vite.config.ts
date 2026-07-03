import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { TDesignResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

// 本地 8080 常被其他服务占用：允许用 VITE_API_PROXY 指定后端，默认仍为 8080
const proxyTarget = process.env.VITE_API_PROXY || 'http://localhost:8080'

export default defineConfig({
  plugins: [
    vue(),
    // 组件按需导入：模板中 t-* 组件自动引入对应实现与样式
    Components({
      resolvers: [
        // TDesign 按需导入：模板 t-* 组件与样式自动引入
        TDesignResolver({ library: 'vue-next' }),
      ],
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
        target: proxyTarget,
        changeOrigin: true,
      },
      '/ws': {
        target: proxyTarget,
        changeOrigin: true,
        ws: true,
      },
    },
    headers: {
      'Content-Security-Policy':
        "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self' data:; connect-src 'self' ws: wss:; frame-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self';",
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
        },
      },
    },
  },
})
