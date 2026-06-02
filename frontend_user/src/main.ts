import { createApp } from 'vue'
import { createPinia } from 'pinia'
// 图标已切换为 TDesign：以原 IconXxx 同名注册 tdesign-icons-vue-next 图标，
// 模板 <icon-xxx> 标签零改动；命令式 API（Message/Modal）样式仍走 Arco（P4 移除）。
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
import './styles/tdesign-theme.css'
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
