import { createApp } from 'vue'
import { createPinia } from 'pinia'
// 图标统一使用 tdesign-icons-vue-next：以 IconXxx 同名全局注册，
// 模板 <icon-xxx> 标签零改动；命令式提示统一走 TDesign MessagePlugin。
import {
  AddIcon,
  CheckCircleIcon,
  CheckIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  DeleteIcon,
  EditIcon,
  HomeIcon,
  NotificationIcon,
  RemoveIcon,
  SearchIcon,
} from 'tdesign-icons-vue-next'
import 'tdesign-vue-next/es/style/index.css'
import 'tdesign-vue-next/es/message/style/index.css'
import 'tdesign-vue-next/es/dialog/style/index.css'
import './styles/tdesign-theme.css'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import './styles/index.css'
import { initTheme } from './utils/theme'

// 应用暗色偏好（theme-mode='dark'），需在样式加载后、挂载前执行
initTheme()

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)

// 全局注册实际使用的图标（TDesign 实现，沿用原 IconXxx 名称，模板零改动）
const icons = {
  IconCheck: CheckIcon,
  IconCheckCircle: CheckCircleIcon,
  IconDelete: DeleteIcon,
  IconEdit: EditIcon,
  IconHome: HomeIcon,
  IconLeft: ChevronLeftIcon,
  IconMinus: RemoveIcon,
  IconNotification: NotificationIcon,
  IconPlus: AddIcon,
  IconRight: ChevronRightIcon,
  IconSearch: SearchIcon,
}
for (const [name, comp] of Object.entries(icons)) {
  app.component(name, comp)
}

app.mount('#app')
