import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ArcoVue from '@arco-design/web-vue'
import ArcoVueIcon from '@arco-design/web-vue/es/icon'
import '@arco-design/web-vue/dist/arco.css'
import App from './App.vue'
import router from './router'
import './router/guard'
import './styles/index.css'
import { vPermission, vPermissionAny } from './directives/permission'
import { i18n } from './i18n'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ArcoVue)
app.use(ArcoVueIcon)
app.use(i18n)

// 注册权限指令
app.directive('permission', vPermission)
app.directive('permission-any', vPermissionAny)

app.mount('#app')
