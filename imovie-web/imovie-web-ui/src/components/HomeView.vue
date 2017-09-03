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
            <input type="radio" name="sort" v-model="sort" value="createdTime,desc" @change="sortMovies">
            <label>{{ $t("token.createdTime") }}</label>
          </div>
        </div>
        <div class="field">
          <div class="ui radio checkbox">
            <input type="radio" name="sort" v-model="sort" value="updatedTime,desc" @change="sortMovies">
            <label>{{ $t("token.updatedTime") }}</label>
          </div>
        </div>
        <div class="field">
          <div class="ui radio checkbox">
            <input type="radio" name="sort" v-model="sort" value="releaseDate,desc,year,desc" @change="sortMovies">
            <label>{{ $t("token.releaseDate") }}</label>
          </div>
        </div>
        <div class="field">
          <div class="ui radio checkbox">
            <input type="radio" name="sort" v-model="sort" value="dbScore,desc" @change="sortMovies">
            <label>{{ $t("token.dbScore") }}</label>
          </div>
        </div>
        <div class="field">
          <div class="ui radio checkbox">
            <input type="radio" name="sort" v-model="sort" value="imdbScore,desc,dbScore,desc" @change="sortMovies">
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

    <div v-if="query.search">
      <div class="ui blue label" v-if="search.text.val">
        {{ search.text.val }}
        <i class="delete icon" @click="resetSearch('text', search.text.val)"></i>
      </div>
      <div class="ui blue label" v-if="search.year.val">
        {{ search.year.val }}
        <i class="delete icon" @click="resetSearch('year', search.year.val)"></i>
      </div>
      <div class="ui label" v-for="category in search.category.val">
        {{ category }}
        <i class="delete icon" @click="resetSearch(search.category.val, category)"></i>
      </div>
      <div class="ui label" v-for="region in search.region.val">
        {{ region }}
        <i class="delete icon" @click="resetSearch(search.region.val, region)"></i>
      </div>
      <div class="ui label" v-for="language in search.language.val">
        {{ language }}
        <i class="delete icon" @click="resetSearch(search.language.val, language)"></i>
      </div>
      <div class="ui label" v-for="director in search.director.val">
        {{ director }}
        <i class="delete icon" @click="resetSearch(search.director.val, director)"></i>
      </div>
      <div class="ui label" v-for="editor in search.editor.val">
        {{ editor }}
        <i class="delete icon" @click="resetSearch(search.editor.val, editor)"></i>
      </div>
      <div class="ui label" v-for="actor in search.actor.val">
        {{ actor }}
        <i class="delete icon" @click="resetSearch(search.actor.val, actor)"></i>
      </div>
      <i class="red delete link icon" @click="clearAdvanceSearch"></i>
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
        <router-link :to="getLink(movie)" class="ui small image" :title="movie.title" :target="query.search ? '_blank' : ''">
          <img :src="movie.thumb">
        </router-link>
        <div class="content">
          <router-link :to="getLink(movie)" class="header" :target="query.search ? '_blank' : ''">
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

    <vue-semantic-modal v-model="showModal" show-close-icon="true">
      <template slot="header">
        {{ $t("message.AdvanceSearch") }}
      </template>
      <template slot="content">
        <div class="ui form">
          <div class="inline fields">
            <div class="field">
              <select class="ui dropdown" v-model="search.text.name">
                <option value="name">{{ $t("token.name") | lower }}</option>
                <option value="title">{{ $t("token.title") | lower }}</option>
                <option value="synopsis">{{ $t("token.synopsis") | lower }}</option>
                <option value="imdbUrl">IMDb</option>
                <option value="aliases">{{ $tc("token.alias") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.text.op1">
                <option value="is"></option>
                <option value="not" v-if="search.text.name != 'aliases'">not</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.text.op2">
                <option value="contains">{{ $t("token.contains") | lower }}</option>
                <option value="equals" v-if="search.text.name == 'name' || search.text.name == 'title'">equals</option>
                <option value="startsWith" v-if="search.text.name == 'name' || search.text.name == 'title'">starts with</option>
                <option value="endsWith" v-if="search.text.name == 'name' || search.text.name == 'title'">ends with</option>
              </select>
            </div>
            <div class="field">
              <input type="search" v-model="search.text.val" placeholder="Search text">
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $tc("token.category") }}</label>
              <select class="ui disabled dropdown">
                <option value="contains">{{ $t("token.contains") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.category.op">
                <option value="all">{{ $t("token.all") | lower }}</option>
                <option value="any">{{ $t("token.any") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <multiselect v-model="search.category.val" :multiple="true" :options="options.categories" select-label="" :max="5"></multiselect>
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $tc("token.area") }}</label>
              <select class="ui disabled dropdown">
                <option value="contains">{{ $t("token.contains") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.region.op">
                <option value="all">{{ $t("token.all") | lower }}</option>
                <option value="any">{{ $t("token.any") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <multiselect v-model="search.region.val" :multiple="true" :options="options.regions" select-label="" :max="5"></multiselect>
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $tc("token.language") }}</label>
              <select class="ui disabled dropdown">
                <option value="contains">{{ $t("token.contains") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.language.op">
                <option value="all">{{ $t("token.all") | lower }}</option>
                <option value="any">{{ $t("token.any") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <multiselect v-model="search.language.val" :multiple="true" :options="options.languages" select-label="" :max="5"></multiselect>
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $tc("token.director") }}</label>
              <select class="ui disabled dropdown">
                <option value="contains">{{ $t("token.contains") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.director.op">
                <option value="all">{{ $t("token.all") | lower }}</option>
                <option value="any">{{ $t("token.any") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <multiselect v-model="search.director.val" placeholder="Type to search" open-direction="bottom" :options="options.directors" :multiple="true" :searchable="true" :loading="options.isLoading" :internal-search="false" select-label="" :limit="5" :max="5" @search-change="findDirectorsByName"/>
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $tc("token.editor") }}</label>
              <select class="ui disabled dropdown">
                <option value="contains">{{ $t("token.contains") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.editor.op">
                <option value="all">{{ $t("token.all") | lower }}</option>
                <option value="any">{{ $t("token.any") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <multiselect v-model="search.editor.val" placeholder="Type to search" open-direction="bottom" :options="options.editors" :multiple="true" :searchable="true" :loading="options.isLoading" :internal-search="false" select-label="" :limit="5" :max="5" @search-change="findEditorsByName"/>
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $tc("token.actor") }}</label>
              <select class="ui disabled dropdown">
                <option value="contains">{{ $t("token.contains") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <select class="ui dropdown" v-model="search.actor.op">
                <option value="all">{{ $t("token.all") | lower }}</option>
                <option value="any">{{ $t("token.any") | lower }}</option>
              </select>
            </div>
            <div class="field">
              <multiselect v-model="search.actor.val" placeholder="Type to search" open-direction="bottom" :options="options.actors" :multiple="true" :searchable="true" :loading="options.isLoading" :internal-search="false" select-label="" :limit="5" :max="5" @search-change="findActorsByName"/>
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

            <div class="field">
              <label>{{ $t("token.updatedTime") }}</label>
              <select class="ui dropdown" v-model="search.updated.op">
                <option value="==">==</option>
                <option value=">">></option>
                <option value=">=">>=</option>
                <option value="<"><</option>
                <option value="<="><=</option>
              </select>
            </div>
            <div class="field">
              <datepicker v-model="search.updated.val" :language="$i18n.locale" name="updatedTime" format="yyyy-MM-dd" clear-button="true"></datepicker>
            </div>
          </div>

          <div class="inline fields">
            <div class="field">
              <label>{{ $t("token.dbScore") }}</label>
              <select class="ui dropdown" :class="{disabled: search.db.val === 'all'}" v-model="search.db.op">
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
              <select class="ui dropdown" :class="{disabled: search.imdb.val === 'all'}" v-model="search.imdb.op">
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

          <div class="inline fields" v-if="search.type === 'episode'">
            <div class="field">
              <label>{{ $t("token.episode") }}</label>
              <select class="ui dropdown" v-model="search.episode.op">
                <option value="==">==</option>
                <option value="!=">!=</option>
                <option value=">">></option>
                <option value=">=">>=</option>
                <option value="<"><</option>
                <option value="<="><=</option>
              </select>
            </div>
            <div class="field">
              <input type="number" min="0" max="1000" v-model="search.episode.val">
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
<style src="vue-multiselect/dist/vue-multiselect.min.css"></style>
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
  #app input.multiselect__input {
    border: none;
    padding: 0px;
  }
</style>
<script>
import movieService from '@/services/MovieService'
import storageService from '@/services/StorageService'
import VuePagination from './pagination/VuePagination'
import VuePaginationInfo from './pagination/VuePaginationInfo'
import {PaginationEvent} from './pagination/PaginationEvent'
import Datepicker from './datepicker/Datepicker'
import VueSemanticModal from 'vue-semantic-modal'
import Multiselect from 'vue-multiselect'
import $ from 'jquery'

export default {
  name: 'MovieListView',
  components: {
    VuePagination,
    VuePaginationInfo,
    VueSemanticModal,
    Datepicker,
    Multiselect
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
        updated: {
          op: '>',
          val: ''
        },
        episode: {
          op: '==',
          val: ''
        },
        db: {
          op: '>=',
          val: 'all'
        },
        imdb: {
          op: '>=',
          val: 'all'
        },
        director: {
          op: 'all',
          val: []
        },
        editor: {
          op: 'all',
          val: []
        },
        actor: {
          op: 'all',
          val: []
        },
        category: {
          op: 'all',
          val: []
        },
        region: {
          op: 'all',
          val: []
        },
        language: {
          op: 'all',
          val: []
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
      options: {
        isLoading: false,
        directors: [],
        editors: [],
        actors: [],
        categories: ['剧情', '爱情', '喜剧', '动作', '科幻', '奇幻', '冒险', '动画', '战争', '悬疑', '惊悚', '恐怖', '犯罪', '音乐', '歌舞', '历史', '传记', '家庭', '短片', '纪录片'],
        regions: ['中国大陆', '美国', '日本', '英国', '香港', '法国', '韩国', '德国', '加拿大', '台湾', '意大利', '西班牙', '澳大利亚', '印度', '泰国', '比利时', '瑞典', '俄罗斯', '西德', '丹麦', '荷兰', '苏联', '瑞士'],
        languages: ['汉语普通话', '英语', '日语', '法语', '粤语', '韩语', '德语', '西班牙语', '意大利语', '俄语', '泰语', '北印度语', '葡萄牙语', '瑞典语', '阿拉伯语', '波兰语', '印地语', '丹麦语', '荷兰语', '芬兰语', '希伯来语', '土耳其语', '无对白']
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
      if (this.query.text !== storageService.getItem('search') || this.query.category !== storageService.getItem('category')) {
        this.query.search = ''
      }
      storageService.setItem('sort', this.sort)
      storageService.setItem('search', this.query.text)
      storageService.setItem('category', this.query.category)
      this.loadData()
    },
    sortMovies: function () {
      this.currentPage = 0
      storageService.setItem('sort', this.sort)
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
    findDirectorsByName: function (name) {
      this.options.isLoading = true
      movieService.findPersonsByName(name, (success, data) => {
        this.options.isLoading = false
        if (success) {
          this.options.directors = data
        }
      })
    },
    findEditorsByName: function (name) {
      this.options.isLoading = true
      movieService.findPersonsByName(name, (success, data) => {
        this.options.isLoading = false
        if (success) {
          this.options.editors = data
        }
      })
    },
    findActorsByName: function (name) {
      this.options.isLoading = true
      movieService.findPersonsByName(name, (success, data) => {
        this.options.isLoading = false
        if (success) {
          this.options.actors = data
        }
      })
    },
    advanceSearch: function () {
      var q = []
      if (this.search.text.val) {
        var op = '=='
        var val = ''
        if (this.search.text.op1 === 'not') {
          op = '!='
        }
        if (this.search.text.name === 'aliases') {
          op = '=m='
          val = this.search.text.val
        } else if (this.search.text.op2 === 'contains') {
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

      if (this.search.updated.val) {
        var date = new Date(this.search.updated.val).toISOString().substring(0, 10)
        q.push('updatedTime' + this.search.updated.op + date)
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

      if (this.search.director.val && this.search.director.val.length > 0) {
        if (this.search.director.op === 'any') {
          q.push('directors.name=in=(' + this.search.director.val.map(e => '"' + e + '"').join(',') + ')')
        } else {
          this.search.director.val.forEach(e => q.push('directors.name=="' + e + '"'))
        }
      }

      if (this.search.editor.val && this.search.editor.val.length > 0) {
        if (this.search.editor.op === 'any') {
          q.push('editors.name=in=(' + this.search.editor.val.map(e => '"' + e + '"').join(',') + ')')
        } else {
          this.search.editor.val.forEach(e => q.push('editors.name=="' + e + '"'))
        }
      }

      if (this.search.actor.val && this.search.actor.val.length > 0) {
        if (this.search.actor.op === 'any') {
          q.push('actors.name=in=(' + this.search.actor.val.map(e => '"' + e + '"').join(',') + ')')
        } else {
          this.search.actor.val.forEach(e => q.push('actors.name=="' + e + '"'))
        }
      }

      if (this.search.category.val && this.search.category.val.length > 0) {
        if (this.search.category.op === 'any') {
          q.push('categories.name=in=(' + this.search.category.val.join(',') + ')')
        } else {
          this.search.category.val.forEach(e => q.push('categories.name=="' + e + '"'))
        }
        this.query.category = ''
      } else {
        this.query.category = 'all'
      }

      if (this.search.region.val && this.search.region.val.length > 0) {
        if (this.search.region.op === 'any') {
          q.push('regions.name=in=(' + this.search.region.val.join(',') + ')')
        } else {
          this.search.region.val.forEach(e => q.push('regions.name=="' + e + '"'))
        }
      }

      if (this.search.language.val && this.search.language.val.length > 0) {
        if (this.search.language.op === 'any') {
          q.push('languages.name=in=(' + this.search.language.val.join(',') + ')')
        } else {
          this.search.language.val.forEach(e => q.push('languages.name=="' + e + '"'))
        }
      }

      if (this.search.type === 'episode') {
        if (this.search.episode.val) {
          q.push('episode' + this.search.episode.op + this.search.episode.val)
        } else {
          q.push('episode!=NULL')
        }
      } else if (this.search.type === 'movie') {
        q.push('episode==NULL')
      }

      if (this.search.resources === 'notempty') {
        q.push('resources=n=0')
      } else if (this.search.resources === 'empty') {
        q.push('resources=e=0')
      }

      this.currentPage = 0
      this.query.search = q.join(';')
      this.query.text = ''
      this.showModal = false
      this.loadData()
    },
    resetSearch: function (type, value) {
      console.log(type + ': ' + value)
      if (Array.isArray(type)) {
        var index = type.indexOf(value)
        if (index > -1) {
          type.splice(index, 1)
        }
      } else {
        if (type === 'text') {
          this.search.text.val = ''
        } else if (type === 'year') {
          this.search.year.val = ''
        } else if (type === 'type') {
          this.search.type = 'all'
        }
      }
      this.advanceSearch()
    },
    clearAdvanceSearch: function () {
      this.search.text.val = ''
      this.search.year.val = ''
      this.search.updated.val = ''
      this.search.episode.val = ''
      this.search.director.val = []
      this.search.editor.val = []
      this.search.actor.val = []
      this.search.category.val = []
      this.search.region.val = []
      this.search.language.val = []
      this.search.db.val = 'all'
      this.search.imdb.val = 'all'
      this.search.type = 'all'
      this.search.resources = 'all'
      this.currentPage = 0
      this.query.search = ''
      this.loadData()
    }
  }
}

</script>
