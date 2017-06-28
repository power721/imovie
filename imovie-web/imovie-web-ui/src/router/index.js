import Vue from 'vue'
import Router from 'vue-router'
import HomeView from '@/components/HomeView'
import MovieListView from '@/components/MovieListView'
import EpisodeListView from '@/components/EpisodeListView'
import ResourceListView from '@/components/ResourceListView'
import MovieDetail from '@/components/MovieDetail'
import NotFoundView from '@/components/NotFoundView'

Vue.use(Router)

export default new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'HomeView',
      component: HomeView
    },
    {
      path: '/movies',
      name: 'MovieListView',
      component: MovieListView
    },
    {
      path: '/movies/:id',
      alias: '/episodes/:id',
      name: 'MovieDetail',
      component: MovieDetail
    },
    {
      path: '/episodes',
      name: 'EpisodeListView',
      component: EpisodeListView
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
