import { createApp } from 'vue'
import { createPinia } from 'pinia'
// Arco 按需导入：组件由 vite 构建时 ArcoResolver 自动引入（见 vite.config.ts），
// 此处只保留：①实际使用的图标全局注册（仅 23 种，替代全量 ~200 种图标）
// ②命令式 API（Message/Modal）的样式导入（resolver 不覆盖命令式调用）
import {
  IconApps,
  IconBranch,
  IconCheck,
  IconCheckCircle,
  IconCommand,
  IconDelete,
  IconDown,
  IconEdit,
  IconEye,
  IconFile,
  IconFolder,
  IconLeft,
  IconMenu,
  IconMinus,
  IconMore,
  IconNotification,
  IconPlus,
  IconPoweroff,
  IconRefresh,
  IconRight,
  IconUpload,
  IconUser,
  IconUserGroup,
} from '@arco-design/web-vue/es/icon'
import '@arco-design/web-vue/es/message/style/index.css'
import '@arco-design/web-vue/es/modal/style/index.css'
import App from './App.vue'
import router from './router'
import './router/guard'
import './styles/index.css'
import { vPermission, vPermissionAny } from './directives/permission'
import { i18n } from './i18n'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)

// 全局注册实际使用的图标（按需，非全量）
const icons = {
  IconApps,
  IconBranch,
  IconCheck,
  IconCheckCircle,
  IconCommand,
  IconDelete,
  IconDown,
  IconEdit,
  IconEye,
  IconFile,
  IconFolder,
  IconLeft,
  IconMenu,
  IconMinus,
  IconMore,
  IconNotification,
  IconPlus,
  IconPoweroff,
  IconRefresh,
  IconRight,
  IconUpload,
  IconUser,
  IconUserGroup,
}
for (const [name, comp] of Object.entries(icons)) {
  app.component(name, comp)
}

// 注册权限指令
app.directive('permission', vPermission)
app.directive('permission-any', vPermissionAny)

app.mount('#app')
