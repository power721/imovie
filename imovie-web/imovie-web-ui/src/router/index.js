import Vue from 'vue'
import Router from 'vue-router'
import MovieListView from '@/components/MovieListView'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'MovieListView',
      component: MovieListView
    }
  ]
})
