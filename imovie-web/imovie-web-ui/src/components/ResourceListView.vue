<template>
  <div class="ui container" id="resources">
    <div class="ui hidden divider"></div>
    <div class="ui active dimmer" v-if="loading">
      <div class="ui loader"></div>
    </div>
    <div class="ui error message" v-show="error" transition="fade">
      {{ error }}
    </div>

    <div class="ui form">
      <div class="inline fields">
        <div class="field">
          <label>{{ $t("token.search") }}</label>
          <div class="ui icon input">
            <input type="search" v-model="text" @change="search" placeholder="Search...">
            <i class="circular search link icon" @click="search"></i>
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

    <div class="ui divided items resource-list">
      <div v-for="resource in resources" class="item resource" :data-id="resource.id">
        <i class="middle aligned icon" :class="getIconClass(resource.uri)"></i>
        <div class="content">
          <div class="header">
            <a :href="resource.uri" target="_blank" :title="'点击下载资源 ' + resource.id">{{ resource.title || resource.uri }}</a>
            <a v-if="resource.original" :href="fixBtbtt(resource.original)" title="资源原始地址" target="_blank">
              &nbsp;&nbsp;<i class="small external icon"></i>
            </a>
            <a v-if="$auth.user.isAdmin" @click="deleteResource(resource.id)"><i class="small red remove icon"></i></a>
          </div>
          <div class="extra" v-if="resource._embedded && resource._embedded.movies">
            <router-link :to="getLink(movie)" v-for="movie in resource._embedded.movies" class="ui right floated">
              {{ movie.title }}
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <div class="vue-pagination ui basic segment grid" v-if="resources && resources.length">
      <vue-pagination-info></vue-pagination-info>
      <vue-pagination @vue-pagination:change-page="changePage"></vue-pagination>
    </div>

    <div class="ui hidden divider"></div>
  </div>
</template>
<style>
  .vue-pagination input.page {
    width: 100px;
  }
</style>
<script>
import resourceService from '@/services/ResourceService'
import storageService from '@/services/StorageService'
import VuePagination from './pagination/VuePagination'
import VuePaginationInfo from './pagination/VuePaginationInfo'
import {PaginationEvent} from './pagination/PaginationEvent'
import $ from 'jquery'

export default {
  name: 'ResourceListView',
  components: {
    VuePagination,
    VuePaginationInfo
  },
  data () {
    return {
      loading: false,
      error: '',
      text: this.$route.query.search || storageService.getItem('searchResource') || '',
      currentPage: this.$route.query.v || storageService.getItem('resourcePage') || 0,
      pagination: null,
      resources: []
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
      this.error = this.resources = null
      this.loading = true
      storageService.setItem('resourcePage', this.currentPage)
      let params = { text: this.text, page: this.currentPage, sort: 'id,desc', size: 50 }
      resourceService.getResource(params, (success, data) => {
        this.loading = false
        if (success) {
          this.fireEvent('load-success', data)
          this.resources = data._embedded.resources
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
      storageService.setItem('searchResource', this.text)
      this.loadData()
    },
    getIconClass: function (uri) {
      if (uri.startsWith('magnet:?')) {
        return 'magnet'
      }
      if (uri.startsWith('http://pan.baidu.com/')) {
        return 'cloud download'
      }
      return 'download'
    },
    fixBtbtt: function (uri) {
      if (uri.startsWith('http://btbtt.co/')) {
        return uri.replace('http://btbtt.co/', 'http://btbtt.pw/')
      }
      return uri
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
    deleteResource: function (id) {
      resourceService.deleteResource(id, (success, data) => {
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
