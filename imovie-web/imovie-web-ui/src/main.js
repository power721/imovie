// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueRouter from 'vue-router'
import VueResource from 'vue-resource'
import VueI18n from 'vue-i18n'
import NProgress from 'nprogress'
import Toasted from 'vue-toasted'
import VuejsDialog from 'vuejs-dialog'
import App from './App'
import auth from './services/Auth'
import storageService from './services/StorageService'
import messages from './services/messages'
import router from './router'
import * as filters from './filters'

require('semantic-ui/dist/semantic.css')
require('semantic-ui/dist/semantic.js')
require('nprogress/nprogress.css')

Vue.config.productionTip = false

Vue.use(VueRouter)
Vue.use(VueResource)
Vue.use(VueI18n)
Vue.use(VuejsDialog)

let options = {
  duration: 5000,
  theme: 'outline',
  action: {
    text: 'x',
    onClick: (e, toastObject) => {
      toastObject.goAway(0)
    }
  }
}
Vue.use(Toasted, options)

Object.keys(filters).forEach(key => {
  Vue.filter(key, filters[key])
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || 'imovies'
  if (to.meta.auth && !auth.loggedIn()) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (to.meta.guest && auth.loggedIn()) {
    next('/')
  } else {
    next()
  }
})

Vue.http.interceptors.push((request, next) => {
  NProgress.start()
  if (auth.getToken()) {
    request.headers.set('Authorization', 'bearer ' + auth.getToken())
  }

  next((response) => {
    NProgress.done()
    if (response.status === 401 && response.body.error === 'invalid_token') {
      auth.logout()
    }
  })
})

auth.checkAuth()

const shared = {
  user: auth.user
}
shared.install = function () {
  Object.defineProperty(Vue.prototype, '$auth', {
    get () {
      return shared
    }
  })
}
Vue.use(shared)

const i18n = new VueI18n({
  locale: storageService.getItem('locale') || 'en',
  fallbackLocale: 'en',
  messages
})

/* eslint-disable no-new */
new Vue({
  el: '#app',
  router,
  i18n,
  template: '<App/>',
  components: {App}
})
