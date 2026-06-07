import App from './App'
import { pinia } from '@/stores'
// #ifdef H5
import './pages-json-js'
import { plugin as __plugin } from '@dcloudio/uni-h5'
import { createVueApp as createH5App } from '@dcloudio/uni-h5-vue'
// #endif

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false
App.mpType = 'app'
const app = new Vue({
  pinia,
  ...App
})
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp as createNativeApp } from 'vue'
export function createApp() {
  // #ifdef H5
  const app = createH5App(App)
  // #endif
  // #ifndef H5
  const app = createNativeApp(App)
  // #endif
  app.use(pinia)
  return {
    app,
    pinia
  }
}

// #ifdef H5
createApp().app.use(__plugin).mount('#app')
// #endif
// #endif
