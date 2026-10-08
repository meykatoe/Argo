import '@shared/assets/main.css'

import { createApp } from 'vue'
import { createStaffRouter } from '@shared/router'

import App from './App.vue'

createApp(App).use(createStaffRouter()).mount('#app')
