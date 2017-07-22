<template>
  <div class="ui container" id="movies">
    <div class="ui hidden divider"></div>
    <div class="ui active dimmer" v-if="loading">
      <div class="ui loader"></div>
    </div>
    <div class="ui error message" v-show="error" transition="fade">
      {{ error }}
    </div>

    <div class="ui form">
      <div class="inline fields">
        <label>{{ $t("token.sort") }}</label>
        <div class="field">
          <div class="ui radio checkbox">
            <input type="radio" name="sort" v-model="sort" value="createdTime,desc" @change="filter">
            <label>{{ $t("token.createdTime") }}</label>
          </div>
        </div>
        <div class="field">
          <div class="ui radio checkbox">
            <input type="radio" name="sort" v-model="sort" value="updatedTime,desc" @change="filter">
            <label>{{ $t("token.updatedTime") }}</label>
          </div>
        </div>
        <div class="field">
          <div class="ui radio checkbox">
            <input type="radio" name="sort" v-model="sort" value="releaseDate,desc,year,desc" @change="filter">
            <label>{{ $t("token.releaseDate") }}</label>
          </div>
        </div>
        <div class="field">
          <div class="ui radio checkbox">
            <input type="radio" name="sort" v-model="sort" value="dbScore,desc" @change="filter">
            <label>{{ $t("token.dbScore") }}</label>
          </div>
        </div>
        <div class="field">
          <div class="ui radio checkbox">
            <input type="radio" name="sort" v-model="sort" value="imdbScore,desc,dbScore,desc" @change="filter">
            <label>{{ $t("token.imdbScore") }}</label>
          </div>
        </div>

        <div class="field">
          <label>{{ $tc("token.category") }}</label>
          <select class="ui dropdown" id="category" v-model="category" @change="filter">
            <option value="all">{{ $t("token.all") }}</option>
            <option value="剧情">剧情</option>
            <option value="爱情">爱情</option>
            <option value="喜剧">喜剧</option>
            <option value="动作">动作</option>
            <option value="科幻">科幻</option>
            <option value="奇幻">奇幻</option>
            <option value="冒险">冒险</option>
            <option value="动画">动画</option>
            <option value="战争">战争</option>
            <option value="悬疑">悬疑</option>
            <option value="惊悚">惊悚</option>
            <option value="恐怖">恐怖</option>
            <option value="犯罪">犯罪</option>
            <option value="音乐">音乐</option>
            <option value="歌舞">歌舞</option>
            <option value="历史">历史</option>
            <option value="传记">传记</option>
            <option value="家庭">家庭</option>
            <option value="短片">短片</option>
            <option value="情色" v-if="$auth.user.authenticated">情色</option>
            <option value="纪录片">纪录片</option>
          </select>
        </div>

        <div class="field">
          <label>{{ $t("token.search") }}</label>
          <div class="ui icon input">
            <input type="search" v-model="text" @change="filter" placeholder="Search...">
            <i class="circular search link icon" @click="filter"></i>
          </div>
        </div>
      </div>
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
          <div class="ui label" v-if="$auth.user.authenticated && movie.resourcesSize">
            {{ movie.resourcesSize }}
          </div>
          <a v-if="$auth.user.isAdmin" @click="deleteMovie(movie.id)"><i class="small red remove icon"></i></a>
          <div class="description">
            <p>{{ movie.synopsis || $t("message.noIntro") | truncate }}</p>
          </div>
          <div class="extra">
            <div>
              <span class="date" data-tooltip="movie.updatedTime | date">{{ movie.createdTime | date }}</span>
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
    left: 5px;
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
import movieService from '@/services/MovieService'
import storageService from '@/services/StorageService'
import VuePagination from './pagination/VuePagination'
import VuePaginationInfo from './pagination/VuePaginationInfo'
import {PaginationEvent} from './pagination/PaginationEvent'
import $ from 'jquery'

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
      sort: this.$route.query.sort || storageService.getItem('sort') || '',
      category: this.$route.query.category || storageService.getItem('category') || 'all',
      currentPage: this.$route.query.page || storageService.getItem('page') || 0,
      pagination: null,
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
      storageService.setItem('page', this.currentPage)
      let params = { name: this.text, category: this.category, page: this.currentPage, sort: this.sort }
      movieService.getAllMovies(params, (success, data) => {
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
    filter: function () {
      this.currentPage = 0
      storageService.setItem('sort', this.sort)
      storageService.setItem('search', this.text)
      storageService.setItem('category', this.category)
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
    },
    getLink: function (movie) {
      if (movie.episode) {
        return '/episodes/' + movie.id
      } else {
        return '/movies/' + movie.id
      }
    },
    deleteMovie: function (id) {
      movieService.deleteMovie(id, (success, data) => {
        if (success) {
          $('div[data-id=' + id + ']').remove()
        } else {
          console.log('delete ' + id + ' failed: ' + data)
        }
      })
    }
  }
}

</script>
