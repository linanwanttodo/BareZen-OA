import { createApp } from 'vue'
import { createPinia } from 'pinia'
// Arco 按需导入：组件由 vite 构建时 ArcoResolver 自动引入（见 vite.config.ts），
// 此处只保留：①实际使用的图标全局注册（仅 11 种，替代全量 ~200 种图标）
// ②命令式 API（Message/Modal）的样式导入（resolver 不覆盖命令式调用）
import {
  IconCheck,
  IconCheckCircle,
  IconDelete,
  IconEdit,
  IconHome,
  IconLeft,
  IconMinus,
  IconNotification,
  IconPlus,
  IconRight,
  IconSearch,
} from '@arco-design/web-vue/es/icon'
import '@arco-design/web-vue/es/message/style/index.css'
import '@arco-design/web-vue/es/modal/style/index.css'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import './styles/index.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)

// 全局注册实际使用的图标（按需，非全量）
const icons = {
  IconCheck,
  IconCheckCircle,
  IconDelete,
  IconEdit,
  IconHome,
  IconLeft,
  IconMinus,
  IconNotification,
  IconPlus,
  IconRight,
  IconSearch,
}
for (const [name, comp] of Object.entries(icons)) {
  app.component(name, comp)
}

app.mount('#app')
