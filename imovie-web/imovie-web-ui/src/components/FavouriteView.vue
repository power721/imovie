<template>
  <div class="ui container" id="favourites">
    <div class="ui hidden divider"></div>
    <div class="ui active dimmer" v-if="loading">
      <div class="ui loader"></div>
    </div>
    <div class="ui error message" v-show="error" transition="fade">
      {{ error }}
    </div>

    <div class="vue-pagination ui basic segment grid">
      <vue-pagination-info></vue-pagination-info>
      <div class="ui input">
        <input type="number" min="1" class="page" v-model="page" @change="loadData">
      </div>
      <vue-pagination @vue-pagination:change-page="changePage"></vue-pagination>
    </div>

    <div class="ui divided items movie-list">
      <div v-for="movie in movies" class="item movie" :data-id="movie.id" style="min-height: 225px;">
        <router-link :to="getLink(movie)" class="ui small image" :title="movie.title">
          <img :src="movie.thumb">
        </router-link>
        <div class="content">
          <router-link :to="getLink(movie)" class="header">
            {{ movie.title }}
          </router-link>
          <div class="ui blue circular label" v-if="movie.episode">
            {{ movie.episode }}
          </div>
          <div class="ui label" v-if="$auth.user.authenticated && movie.size">
            {{ movie.size }}
          </div>
          <a v-if="$auth.user.isAdmin" @click="deleteFavourite(movie.id)"><i class="small red remove icon"></i></a>
          <div class="description">
            <p>{{ movie.synopsis || $t("message.noIntro") | truncate }}</p>
          </div>
          <div class="extra">
            <div>
              <span class="date" :data-tooltip="getTooltip(movie.updatedTime)" data-position="top left">{{ movie.createdTime | date }}</span>
              <span class="category">{{ movie.categories | join }}</span>
              <a :href="movie.imdbUrl" target="_blank" class="imdb">IMDB: {{ movie.imdbScore || '0.0' }}</a>
              <a :href="movie.dbUrl" target="_blank" class="dou">{{ $t("token.db") }}: {{ movie.dbScore || '0.0' }}</a>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="vue-pagination ui basic segment grid" v-if="movies && movies.length">
      <vue-pagination-info></vue-pagination-info>
      <vue-pagination @vue-pagination:change-page="changePage"></vue-pagination>
    </div>

    <div class="ui hidden divider"></div>
  </div>
</template>
<style>
  #movies>.movie-list>.movie>.content>.description {
    min-height: 150px;
    text-align: left;
  }
  span.date {
    color: #8f8f8f;
  }
  span.category {
    color: #9f9f9f;
  }
  a.imdb {
    color: #f2992e;
    position: absolute;
    right: 87px;
  }
  a.dou {
    color: #56bc8a;
    position: absolute;
    right: 0px;
  }
  .vue-pagination {
    background: #f9fafb !important;
  }
  .vue-pagination input.page {
    width: 100px;
  }
  .vue-pagination-info {
    margin-top: auto;
    margin-bottom: auto;
  }
</style>
<script>
import userService from '@/services/UserService'
import VuePagination from './pagination/VuePagination'
import VuePaginationInfo from './pagination/VuePaginationInfo'
import {PaginationEvent} from './pagination/PaginationEvent'

export default {
  name: 'FavouriteView',
  components: {
    VuePagination,
    VuePaginationInfo
  },
  data () {
    return {
      loading: false,
      error: '',
      pagination: null,
      currentPage: this.$route.query.page || 0,
      movies: []
    }
  },
  created () {
    this.loadData()
  },
  computed: {
    page: {
      get: function () {
        return parseInt(this.currentPage) + 1
      },
      set: function (newValue) {
        this.currentPage = newValue - 1
      }
    }
  },
  methods: {
    loadData: function () {
      this.error = this.movies = null
      this.loading = true
      let params = { q: this.query, page: this.currentPage, sort: this.sort }
      userService.getFavourite(params, (success, data) => {
        this.loading = false
        if (success) {
          this.fireEvent('load-success', data)
          this.movies = data.content
          this.pagination = this.getPaginationData(data)

          this.$nextTick(function () {
            this.fireEvent('pagination-data', this.pagination)
            this.fireEvent('loaded')
          })
        } else {
          this.error = data.message || 'Bad Request'
          this.fireEvent('load-error', data)
          this.fireEvent('loaded')
        }
      })
    },
    getPaginationData: function (pagination) {
      let number = pagination.numberOfElements || pagination.size
      pagination.from = pagination.number * pagination.size + 1
      pagination.to = pagination.from + number - 1
      if (pagination.to > pagination.totalElements) {
        pagination.to = pagination.totalElements
      }
      return pagination
    },
    fireEvent: function (eventName, args) {
      PaginationEvent.$emit('vue-pagination:' + eventName, args)
    },
    changePage: function (page) {
      if (page === 'prev') {
        this.gotoPreviousPage()
      } else if (page === 'next') {
        this.gotoNextPage()
      } else {
        this.gotoPage(page)
      }
    },
    gotoPreviousPage: function () {
      if (this.currentPage > 0) {
        this.currentPage--
        this.loadData()
      }
    },
    gotoNextPage: function () {
      if (this.currentPage + 1 < this.pagination.totalPages) {
        this.currentPage++
        this.loadData()
      }
    },
    gotoPage: function (page) {
      if (page !== this.currentPage && (page >= 0 && page < this.pagination.totalPages)) {
        this.currentPage = page
        this.loadData()
      }
    },
    getLink: function (movie) {
      if (movie.episode) {
        return '/episodes/' + movie.id
      } else {
        return '/movies/' + movie.id
      }
    },
    getTooltip: function (updatedTime) {
      let d = new Date(updatedTime)
      return this.$t('token.updatedTime') + ': ' + d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate()
    },
    deleteFavourite: function (id) {
      this.$dialog.confirm('Are you sure?').then(() => {
        userService.deleteFavourite(id, (success, data) => {
          if (success) {
            this.movies = this.movies.filter(e => e.id !== id)
          } else {
            console.log('delete ' + id + ' failed: ')
            console.log(data)
          }
        })
      }).catch(() => {})
    }
  }
}

</script>
