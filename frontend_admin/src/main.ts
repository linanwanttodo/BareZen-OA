import { createApp } from 'vue'
import { createPinia } from 'pinia'
// TDesign 按需导入：组件由 vite 构建时 TDesignResolver 自动引入（见 vite.config.ts）。
// 图标以 IconXxx 同名全局注册 tdesign-icons-vue-next 图标，模板 <icon-xxx> 标签零改动。
import {
  AddIcon,
  AppIcon,
  BrowseIcon,
  CheckCircleIcon,
  CheckIcon,
  ChevronDownIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  CommandIcon,
  DeleteIcon,
  EditIcon,
  EllipsisIcon,
  FileIcon,
  FolderIcon,
  GitBranchIcon,
  PoweroffIcon,
  RefreshIcon,
  RemoveIcon,
  UploadIcon,
  UsergroupIcon,
  UserIcon,
  ViewListIcon,
  NotificationIcon,
} from 'tdesign-icons-vue-next'
import 'tdesign-vue-next/es/style/index.css'
import 'tdesign-vue-next/es/message/style/index.css'
import 'tdesign-vue-next/es/dialog/style/index.css'
import './styles/tdesign-theme.css'
import App from './App.vue'
import router from './router'
import './router/guard'
import './styles/index.css'
import { vPermission, vPermissionAny } from './directives/permission'
import { i18n } from './i18n'
import { initTheme } from './utils/theme'

// 应用暗色偏好（theme-mode='dark'），需在样式加载后、挂载前执行
initTheme()

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)

// 全局注册实际使用的图标（TDesign 实现，沿用原 IconXxx 名称，模板零改动）
const icons = {
  IconApps: AppIcon,
  IconBranch: GitBranchIcon,
  IconCheck: CheckIcon,
  IconCheckCircle: CheckCircleIcon,
  IconCommand: CommandIcon,
  IconDelete: DeleteIcon,
  IconDown: ChevronDownIcon,
  IconEdit: EditIcon,
  IconEye: BrowseIcon,
  IconFile: FileIcon,
  IconFolder: FolderIcon,
  IconLeft: ChevronLeftIcon,
  IconMenu: ViewListIcon,
  IconMinus: RemoveIcon,
  IconMore: EllipsisIcon,
  IconNotification: NotificationIcon,
  IconPlus: AddIcon,
  IconPoweroff: PoweroffIcon,
  IconRefresh: RefreshIcon,
  IconRight: ChevronRightIcon,
  IconUpload: UploadIcon,
  IconUser: UserIcon,
  IconUserGroup: UsergroupIcon,
}
for (const [name, comp] of Object.entries(icons)) {
  app.component(name, comp)
}

// 注册权限指令
app.directive('permission', vPermission)
app.directive('permission-any', vPermissionAny)

app.mount('#app')
