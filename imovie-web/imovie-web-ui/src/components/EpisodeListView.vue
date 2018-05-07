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
          <select class="ui dropdown" id="category" v-model="query.category" @change="filter">
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
            <input type="search" v-model="query.text" @change="filter" placeholder="Search...">
            <i class="circular search link icon" @click="filter"></i>
          </div>
        </div>

        <a @click="refresh()"><i class="refresh link icon"></i></a>

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
          <div class="ui label" v-if="$auth.user.authenticated && movie.size">
            {{ movie.size }}
          </div>
          <a v-if="$auth.user.isAdmin" @click="refreshMovie(movie.id)"><i class="small refresh link icon"></i></a>
          <a v-if="$auth.user.isAdmin" @click="deleteMovie(movie.id)"><i class="small red remove icon"></i></a>
          <div class="description">
            <p>{{ movie.synopsis || $t("message.noIntro") | truncate }}</p>
          </div>
          <div class="extra">
            <div>
              <span class="date" :data-tooltip="getTooltip(movie.updatedTime)" data-position="top left">{{ movie.createdTime | date }}</span>
              <a class="ui teal label" v-for="c in movie.categories" @click="filterByCategory(c.name)">{{ c.name }}</a>
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
import movieService from '@/services/MovieService'
import storageService from '@/services/StorageService'
import VuePagination from './pagination/VuePagination'
import VuePaginationInfo from './pagination/VuePaginationInfo'
import {PaginationEvent} from './pagination/PaginationEvent'

export default {
  name: 'EpisodeListView',
  components: {
    VuePagination,
    VuePaginationInfo
  },
  data () {
    return {
      loading: false,
      error: '',
      sort: this.$route.query.sort || storageService.getItem('sortEpisode') || '',
      query: {
        type: 'episode',
        text: this.$route.query.search || storageService.getItem('searchEpisode') || '',
        category: this.$route.query.category || storageService.getItem('episodeCategory') || 'all',
        region: this.$route.query.region || storageService.getItem('episodeRegion') || 'all'
      },
      currentPage: this.$route.query.page || storageService.getItem('episodePage') || 0,
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
      storageService.setItem('episodePage', this.currentPage)
      let params = { q: this.query, page: this.currentPage, sort: this.sort }
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
    refresh: function () {
      this.loadData()
    },
    filter: function () {
      this.currentPage = 0
      storageService.setItem('sortEpisode', this.sort)
      storageService.setItem('searchEpisode', this.query.text)
      storageService.setItem('episodeCategory', this.query.category)
      this.loadData()
    },
    filterByCategory: function (category) {
      this.query.category = category
      this.filter()
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
      if (updatedTime) {
        return this.$t('token.updatedTime') + ': ' + updatedTime.split('T')[0]
      } else {
        return 'No update'
      }
    },
    deleteMovie: function (id) {
      this.$dialog.confirm('Are you sure?').then(() => {
        movieService.deleteMovie(id, (success, data) => {
          if (success) {
            this.movies = this.movies.filter(e => e.id !== id)
            this.$toasted.success('The movie ' + id + ' is deleted.')
          } else {
            this.$toasted.error('Delete movie ' + id + ' failed.')
            console.log(data)
          }
        })
      }).catch(() => {})
    },
    refreshMovie: function (id) {
      movieService.refreshMovie(id, (success, data) => {
        if (success) {
          this.$toasted.success('The movie ' + id + ' is refreshed.')
        } else {
          this.$toasted.error('Refresh movie ' + id + ' failed.')
          console.log(data)
        }
      })
    }
  }
}

</script>
