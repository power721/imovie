import Vue from 'vue'
import Router from 'vue-router'
import MovieListView from '@/components/MovieListView'
import MovieDetail from '@/components/MovieDetail'

Vue.use(Router)

export default new Router({
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
    }
  ]
})
