// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueRouter from 'vue-router'
import VueResource from 'vue-resource'
import NProgress from 'nprogress'
import App from './App'
import auth from './services/Auth'
import router from './router'
import * as filters from './filters'

require('semantic-ui/dist/semantic.css')
require('semantic-ui/dist/semantic.js')

Vue.config.productionTip = false

Vue.use(VueRouter)
Vue.use(VueResource)

Object.keys(filters).forEach(key => {
  Vue.filter(key, filters[key])
})

router.beforeEach((to, from, next) => {
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
  })
})

auth.checkAuth()

/* eslint-disable no-new */
new Vue({
  el: '#app',
  router,
  template: '<App/>',
  components: {App}
})
