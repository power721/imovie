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
            <i class="circular search link icon" :class="{inverted: query.search}" @click="showModal=true"></i>
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

    <vue-semantic-modal v-model="showModal">
      <template slot="header">
        Advance Search
      </template>
      <template slot="content">
        <div class="ui form">
          <div class="inline fields">
            <div class="field">
              <select class="ui dropdown" v-model="search.text.name">
                <option value="name">name</option>
                <option value="title">title</option>
                <option value="imdbUrl">imdb</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.text.op1">
                <option value="is"></option>
                <option value="not">not</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.text.op2">
                <option value="contains">contains</option>
                <option value="equals" v-if="search.text.name != 'imdbUrl'">equals</option>
                <option value="startsWith" v-if="search.text.name != 'imdbUrl'">starts with</option>
                <option value="endsWith" v-if="search.text.name != 'imdbUrl'">ends with</option>
              </select>
            </div>
            <div class="field">
              <input type="search" v-model="search.text.val" placeholder="Search text">
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $tc("token.category") }}</label>
              <select class="ui dropdown" v-model="query.category">
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
              <label>{{ $tc("token.area") }}</label>
              <select class="ui dropdown" v-model="query.region">
                <option value="all">{{ $t("token.all") }}</option>
                <option value="中国大陆">中国大陆</option>
                <option value="美国">美国</option>
                <option value="日本">日本</option>
                <option value="英国">英国</option>
                <option value="香港">香港</option>
                <option value="法国">法国</option>
                <option value="韩国">韩国</option>
                <option value="德国">德国</option>
                <option value="加拿大">加拿大</option>
                <option value="台湾">台湾</option>
                <option value="意大利">意大利</option>
                <option value="西班牙">西班牙</option>
                <option value="澳大利亚">澳大利亚</option>
                <option value="印度">印度</option>
                <option value="泰国">泰国</option>
                <option value="比利时">比利时</option>
                <option value="瑞典">瑞典</option>
                <option value="俄罗斯">俄罗斯</option>
                <option value="西德">西德</option>
                <option value="丹麦">丹麦</option>
                <option value="荷兰">荷兰</option>
                <option value="苏联">苏联</option>
                <option value="瑞士">瑞士</option>
              </select>
            </div>

            <div class="field">
              <label>{{ $tc("token.language") }}</label>
              <select class="ui dropdown" v-model="query.language">
                <option value="all">{{ $t("token.all") }}</option>
                <option value="汉语普通话">汉语普通话</option>
                <option value="英语">英语</option>
                <option value="日语">日语</option>
                <option value="法语">法语</option>
                <option value="粤语">粤语</option>
                <option value="韩语">韩语</option>
                <option value="德语">德语</option>
                <option value="西班牙语">西班牙语</option>
                <option value="意大利语">意大利语</option>
                <option value="俄语">俄语</option>
                <option value="泰语">泰语</option>
                <option value="北印度语">北印度语</option>
                <option value="葡萄牙语">葡萄牙语</option>
                <option value="瑞典语">瑞典语</option>
                <option value="阿拉伯语">阿拉伯语</option>
                <option value="波兰语">波兰语</option>
                <option value="印地语">印地语</option>
                <option value="丹麦语">丹麦语</option>
                <option value="荷兰语">荷兰语</option>
                <option value="芬兰语">芬兰语</option>
                <option value="希伯来语">希伯来语</option>
                <option value="土耳其语">土耳其语</option>
                <option value="无对白">无对白</option>
              </select>
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $t("token.year") }}</label>
              <select class="ui dropdown" v-model="search.year.op">
                <option value="==">==</option>
                <option value="!=">!=</option>
                <option value=">">></option>
                <option value=">=">>=</option>
                <option value="<"><</option>
                <option value="<="><=</option>
              </select>
            </div>
            <div class="field">
              <input type="number" min="1900" max="2050" v-model="search.year.val">
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $t("token.dbScore") }}</label>
              <select class="ui dropdown" v-model="search.db.op">
                <option value="==">==</option>
                <option value="!=">!=</option>
                <option value=">">></option>
                <option value=">=">>=</option>
                <option value="<"><</option>
                <option value="<="><=</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.db.val">
                <option value="all">{{ $t("token.all") }}</option>
                <option value="9.5">9.5</option>
                <option value="9.0">9.0</option>
                <option value="8.5">8.5</option>
                <option value="8.0">8.0</option>
                <option value="7.5">7.5</option>
                <option value="7.0">7.0</option>
                <option value="6.5">6.5</option>
                <option value="6.0">6.0</option>
                <option value="5.5">5.5</option>
                <option value="5.0">5.0</option>
                <option value="4.5">4.5</option>
                <option value="4.0">4.0</option>
                <option value="3.0">3.0</option>
                <option value="2.0">2.0</option>
                <option value="1.0">1.0</option>
                <option value="0.0">0.0</option>
              </select>
            </div>

            <div class="field">
              <label>{{ $t("token.imdbScore") }}</label>
              <select class="ui dropdown" v-model="search.imdb.op">
                <option value="==">==</option>
                <option value="!=">!=</option>
                <option value=">">></option>
                <option value=">=">>=</option>
                <option value="<"><</option>
                <option value="<="><=</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.imdb.val">
                <option value="all">{{ $t("token.all") }}</option>
                <option value="9.5">9.5</option>
                <option value="9.0">9.0</option>
                <option value="8.5">8.5</option>
                <option value="8.0">8.0</option>
                <option value="7.5">7.5</option>
                <option value="7.0">7.0</option>
                <option value="6.5">6.5</option>
                <option value="6.0">6.0</option>
                <option value="5.5">5.5</option>
                <option value="5.0">5.0</option>
                <option value="4.5">4.5</option>
                <option value="4.0">4.0</option>
                <option value="3.0">3.0</option>
                <option value="2.0">2.0</option>
                <option value="1.0">1.0</option>
                <option value="0.0">0.0</option>
              </select>
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $tc("token.director") }}</label>
              <select class="ui dropdown">
                <option value="contains">contains</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.director.op">
                <option value="all">{{ $t("token.all") }}</option>
                <option value="any">{{ $t("token.any") }}</option>
              </select>
            </div>
            <div class="field">
              <input type="text" v-model="search.director.val">
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $tc("token.editor") }}</label>
              <select class="ui dropdown">
                <option value="contains">contains</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.editor.op">
                <option value="all">{{ $t("token.all") }}</option>
                <option value="any">{{ $t("token.any") }}</option>
              </select>
            </div>
            <div class="field">
              <input type="text" v-model="search.editor.val">
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $tc("token.actor") }}</label>
              <select class="ui dropdown">
                <option value="contains">contains</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.actor.op">
                <option value="all">{{ $t("token.all") }}</option>
                <option value="any">{{ $t("token.any") }}</option>
              </select>
            </div>
            <div class="field">
              <input type="text" v-model="search.actor.val">
            </div>
          </div>

          <div class="inline fields">
            <label>Type</label>
            <div class="field">
              <div class="ui radio checkbox">
                <input type="radio" name="type" value="all" v-model="search.type">
                <label>{{ $t("token.all") }}</label>
              </div>
            </div>
            <div class="field">
              <div class="ui radio checkbox">
                <input type="radio" name="type" value="movie" v-model="search.type">
                <label>{{ $tc("token.movie") }}</label>
              </div>
            </div>
            <div class="field">
              <div class="ui radio checkbox">
                <input type="radio" name="type" value="episode" v-model="search.type">
                <label>{{ $tc("token.episodes") }}</label>
              </div>
            </div>
          </div>

          <div class="inline fields" v-if="$auth.user.authenticated">
            <label>{{ $tc("token.resource") }}?</label>
            <div class="field">
              <div class="ui radio checkbox">
                <input type="radio" name="resources" value="all" v-model="search.resources">
                <label>不限</label>
              </div>
            </div>
            <div class="field">
              <div class="ui radio checkbox">
                <input type="radio" name="resources" value="notempty" v-model="search.resources">
                <label>有</label>
              </div>
            </div>
            <div class="field">
              <div class="ui radio checkbox">
                <input type="radio" name="resources" value="empty" v-model="search.resources">
                <label>无</label>
              </div>
            </div>
          </div>

        </div>
      </template>
      <template slot="actions">
        <div class="ui cancel button" @click="showModal=false">Cancel</div>
        <div class="ui ok green button" @click="advanceSearch">Search</div>
      </template>
    </vue-semantic-modal>
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
import VueSemanticModal from 'vue-semantic-modal'
import $ from 'jquery'

export default {
  name: 'MovieListView',
  components: {
    VuePagination,
    VuePaginationInfo,
    VueSemanticModal
  },
  data () {
    return {
      loading: false,
      error: '',
      showModal: false,
      sort: this.$route.query.sort || storageService.getItem('sort') || '',
      search: {
        text: {
          name: 'title',
          op1: '',
          op2: 'contains',
          val: ''
        },
        year: {
          op: '==',
          val: ''
        },
        db: {
          op: '==',
          val: 'all'
        },
        imdb: {
          op: '==',
          val: 'all'
        },
        director: {
          op: 'all',
          val: ''
        },
        editor: {
          op: 'all',
          val: ''
        },
        actor: {
          op: 'all',
          val: ''
        },
        type: 'all',
        resources: 'all'
      },
      query: {
        search: '',
        text: this.$route.query.search || storageService.getItem('search') || '',
        category: this.$route.query.category || storageService.getItem('category') || 'all',
        region: this.$route.query.region || storageService.getItem('region') || 'all',
        language: this.$route.query.language || storageService.getItem('language') || 'all'
      },
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
    filter: function () {
      this.currentPage = 0
      storageService.setItem('sort', this.sort)
      storageService.setItem('search', this.query.text)
      storageService.setItem('category', this.query.category)
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
    getTooltip: function (updatedTime) {
      return this.$t('token.updatedTime') + ': ' + updatedTime.split('T')[0]
    },
    deleteMovie: function (id) {
      movieService.deleteMovie(id, (success, data) => {
        if (success) {
          $('div[data-id=' + id + ']').remove()
        } else {
          console.log('delete ' + id + ' failed: ' + data)
        }
      })
    },
    advanceSearch: function () {
      this.currentPage = 0
      var q = []
      if (this.search.text.val) {
        var op = '=='
        var val = ''
        if (this.search.text.op1 === 'not') {
          op = '!='
        }
        if (this.search.text.op2 === 'contains') {
          val = '*' + this.search.text.val + '*'
        } else if (this.search.text.op2 === 'startsWith') {
          val = '*' + this.search.text.val
        } else if (this.search.text.op2 === 'endsWith') {
          val = this.search.text.val + '*'
        } else if (this.search.text.op2 === 'equals') {
          val = this.search.text.val
        }
        q.push(this.search.text.name + op + '"' + val.trim() + '"')
      }

      if (this.search.year.val) {
        q.push('year' + this.search.year.op + this.search.year.val)
      }

      if (this.query.region && this.query.region !== 'all') {
        q.push('regions.name==' + this.query.region)
      }

      if (this.query.language && this.query.language !== 'all') {
        q.push('languages.name==' + this.query.language)
      }

      if (this.search.db.val && this.search.db.val !== 'all') {
        if (this.search.db.val === '0.0' && (this.search.db.op === '==' || this.search.db.op === '!=')) {
          q.push('dbScore' + this.search.db.op + '""')
        } else {
          q.push('dbScore' + this.search.db.op + this.search.db.val)
        }
      }

      if (this.search.imdb.val && this.search.imdb.val !== 'all') {
        if (this.search.imdb.val === '0.0' && (this.search.imdb.op === '==' || this.search.imdb.op === '!=')) {
          q.push('imdbScore' + this.search.imdb.op + '""')
        } else {
          q.push('imdbScore' + this.search.imdb.op + this.search.imdb.val)
        }
      }

      if (this.search.director.val) {
        if (this.search.director.op === 'any') {
          q.push('directors.name=in=(' + this.search.director.val.split(/[,;]/).join(',') + ')')
        } else {
          this.search.director.val.split(/[,;]/).forEach(e => q.push('directors.name=="' + e.trim() + '"'))
        }
      }

      if (this.search.editor.val) {
        if (this.search.editor.op === 'any') {
          q.push('editors.name=in=(' + this.search.editor.val.split(/[,;]/).join(',') + ')')
        } else {
          this.search.editor.val.split(/[,;]/).forEach(e => q.push('editors.name=="' + e.trim() + '"'))
        }
      }

      if (this.search.actor.val) {
        if (this.search.actor.op === 'any') {
          q.push('actors.name=in=(' + this.search.actor.val.split(/[,;]/).join(',') + ')')
        } else {
          this.search.actor.val.split(/[,;]/).forEach(e => q.push('actors.name=="' + e.trim() + '"'))
        }
      }

      if (this.search.type === 'episode') {
        q.push('episode!=NULL')
      } else if (this.search.type === 'movie') {
        q.push('episode==NULL')
      }

      if (this.search.resources === 'notempty') {
        q.push('resources=n=0')
      } else if (this.search.resources === 'empty') {
        q.push('resources=e=0')
      }

      this.query.search = q.join(';')
      this.showModal = false
      this.loadData()
    }
  }
}

</script>
