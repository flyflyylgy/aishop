import { createApp } from 'vue'
import Vant from 'vant'
import 'vant/lib/index.css'
import '@vant/touch-emulator'
import App from './App.vue'
import router from './router'
import './styles/main.css'

createApp(App).use(router).use(Vant).mount('#app')
