import Vue from 'vue'
import Router from 'vue-router'
import MovieListView from '@/components/MovieListView'
import ResourceListView from '@/components/ResourceListView'
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
      path: '/resources',
      name: 'ResourceListView',
      component: ResourceListView
    },
    {
      path: '*',
      name: 'NotFoundView',
      component: NotFoundView
    }
  ],
  linkActiveClass: 'active'
})
