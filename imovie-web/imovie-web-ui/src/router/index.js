import Router from 'vue-router'
import HomeView from '@/components/HomeView'
import MovieListView from '@/components/MovieListView'
import EpisodeListView from '@/components/EpisodeListView'
import ResourceListView from '@/components/ResourceListView'
import MovieDetailView from '@/components/MovieDetailView'
import LoginView from '@/components/LoginView'
import SignupView from '@/components/SignupView'
import NotFoundView from '@/components/NotFoundView'

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
      name: 'MovieDetailView',
      component: MovieDetailView
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
      path: '/login',
      name: 'LoginView',
      component: LoginView,
      meta: {guest: true}
    },
    {
      path: '/signup',
      name: 'SignupView',
      component: SignupView,
      meta: {guest: true}
    },
    {
      path: '*',
      name: 'NotFoundView',
      component: NotFoundView
    }
  ],
  linkActiveClass: 'active'
})
