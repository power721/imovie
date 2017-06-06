<template>
  <div class="ui container divided items" id="movies">
    <div class="ui hidden divider"></div>
    <div class="ui active dimmer" v-if="loading">
      <div class="ui loader"></div>
    </div>
    <div class="ui error message" v-show="error" transition="fade">
      {{ error }}
    </div>

    <!--<div class="ui fixed menu">-->
    <!--<div class="ui container">-->
    <!--<div class="ui simple dropdown item">-->
    <!--Category <i class="dropdown icon"></i>-->
    <!--<div class="menu">-->
    <!--<a class="item" href="#">All</a>-->
    <!--<a class="item" href="#">Link Item</a>-->
    <!--</div>-->
    <!--</div>-->
    <!--<div class="right item">-->
    <!--<div class="ui icon input">-->
    <!--<input type="text" v-model="text" @change="search" placeholder="Search...">-->
    <!--<i class="search icon"></i>-->
    <!--</div>-->
    <!--</div>-->
    <!--</div>-->
    <!--</div>-->

    <div class="vue-pagination ui basic segment grid">
      <vue-pagination-info></vue-pagination-info>
      <div class="ui input">
        <input id="search" type="text" v-model="text" @change="search" placeholder="search by name">
      </div>
      <vue-pagination @vue-pagination:change-page="changePage"></vue-pagination>
    </div>

    <div v-for="movie in movies" class="item movie" style="min-height: 225px;">
      <router-link :to="'/movies/' + movie.id" class="ui small image">
        <img :src="movie.thumb">
      </router-link>
      <div class="content">
        <router-link :to="'/movies/' + movie.id" class="header">
          {{ movie.title }}
        </router-link>
        <div class="ui label" v-if="movie.resourcesSize">
          {{ movie.resourcesSize }}
        </div>
        <div class="description">
          <p>{{ movie.synopsis || '暂无介绍' | truncate }}</p>
        </div>
        <div class="extra">
          <div>
            <span class="date">{{ movie.createdTime | date }}</span>
            <span class="category">{{ movie.categories | join }}</span>
            <a :href="movie.imdbUrl" target="_blank" class="imdb">IMDB：{{ movie.imdbScore || '0.0' }}</a>
            <a :href="movie.dbUrl" target="_blank" class="dou">豆瓣：{{ movie.dbScore || '0.0' }}</a>
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
  #movies>.movie>.content>.description {
    min-height: 150px;
    text-align: left;
  }
  span.date {
    color: #8f8f8f;
    left: 5px;
  }
  span.category {
    color: #9f9f9f;
  }
  a.imdb {
    color: #f2992e;
    position: absolute;
    right: 75px;
  }
  a.dou {
    color: #56bc8a;
    position: absolute;
    right: 0px;
  }
  .vue-pagination {
    background: #f9fafb !important;
  }
  .vue-pagination-info {
    margin-top: auto;
    margin-bottom: auto;
  }
</style>
<script>
import movieService from '@/services/MovieService'
import storageService from '@/services/StorageService'
import VuePagination from './pagination/VuePagination'
import VuePaginationInfo from './pagination/VuePaginationInfo'
import {PaginationEvent} from './pagination/PaginationEvent'

export default {
  name: 'MovieListView',
  components: {
    VuePagination,
    VuePaginationInfo
  },
  data () {
    return {
      loading: false,
      error: '',
      text: this.$route.query.search || storageService.getItem('search') || '',
      category: this.$route.query.category || storageService.getItem('category') || '',
      currentPage: this.$route.query.page || storageService.getItem('currentPage') || 0,
      pagination: null,
      movies: []
    }
  },
  created () {
    this.loadData()
  },
  methods: {
    loadData: function () {
      this.error = this.movies = null
      this.loading = true
      storageService.setItem('search', this.text)
      storageService.setItem('currentPage', this.currentPage)
      let params = { name: this.text, category: this.category, page: this.currentPage }
      movieService.getMovies(params, (success, data) => {
        this.loading = false
        if (success) {
          this.fireEvent('load-success', data)
          this.movies = data._embedded.movies
          this.pagination = this.getPaginationData(data.page)

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
    search: function () {
      this.currentPage = 0
      this.loadData()
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
    }
  }
}

</script>
