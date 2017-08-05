import Router from 'vue-router'
import HomeView from '@/components/HomeView'
import MovieListView from '@/components/MovieListView'
import EpisodeListView from '@/components/EpisodeListView'
import ResourceListView from '@/components/ResourceListView'
import MovieDetailView from '@/components/MovieDetailView'
import ConfigListView from '@/components/ConfigListView'
import EventListView from '@/components/EventListView'
import UserView from '@/components/UserView'
import LoginView from '@/components/LoginView'
import SignupView from '@/components/SignupView'
import NotFoundView from '@/components/NotFoundView'

export default new Router({
  mode: 'history',
  scrollBehavior (to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      const position = {}
      if (to.hash) {
        position.selector = to.hash
      }
      if (to.matched.some(m => m.meta.scrollToTop)) {
        position.x = 0
        position.y = 0
      }
      return position
    }
  },
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
      path: '/configs',
      name: 'ConfigListView',
      component: ConfigListView,
      meta: {auth: true}
    },
    {
      path: '/events',
      name: 'EventListView',
      component: EventListView,
      meta: {auth: true}
    },
    {
      path: '/users',
      name: 'UserView',
      component: UserView,
      meta: {auth: true}
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
