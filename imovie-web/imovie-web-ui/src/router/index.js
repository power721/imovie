import Vue from 'vue'
import Router from 'vue-router'
import MovieListView from '@/components/MovieListView'
import MovieDetail from '@/components/MovieDetail'
import NotFoundView from '@/components/NotFoundView'

Vue.use(Router)

export default new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'MovieListView',
      component: MovieListView
    },
    {
      path: '/movies/:id',
      name: 'MovieDetail',
      component: MovieDetail
    },
    {
      path: '*',
      name: 'NotFoundView',
      component: NotFoundView
    }
  ]
})
