import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'nprogress/nprogress.css'
import App from './App.vue'
import router from './router'
import perm from './directives/perm'
import './styles/index.scss'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
Object.entries(ElementPlusIconsVue).forEach(([name, component]) => {
  app.component(name, component)
})
app.directive('perm', perm)
app.mount('#app')
