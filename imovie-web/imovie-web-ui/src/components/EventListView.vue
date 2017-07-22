<template>
  <div class="ui container">
    <div class="ui hidden divider"></div>
    <div class="ui active dimmer" v-if="loading">
      <div class="ui loader"></div>
    </div>
    <div class="ui error message" v-show="error" transition="fade">
      {{ error }}
    </div>

    <div class="vue-pagination ui basic segment grid">
      <vue-pagination-info></vue-pagination-info>
      <vue-pagination @vue-pagination:change-page="changePage"></vue-pagination>
    </div>

    <table class="ui celled table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Source</th>
          <th>Message</th>
          <th>Time</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="event in events" :data-id="event.id">
          <td>{{ event.id }}</td>
          <td><a :href="event.source" target="_blank">{{ event.source }}</a></td>
          <td>{{ event.message }}</td>
          <td>{{ event.createdTime }}</td>
          <td><a @click="deleteEvent(event.id)"><i class="red remove link icon"></i></a></td>
        </tr>
      </tbody>
    </table>

    <div class="vue-pagination ui basic segment grid">
      <vue-pagination-info></vue-pagination-info>
      <vue-pagination @vue-pagination:change-page="changePage"></vue-pagination>
    </div>

    <div class="ui hidden divider"></div>
  </div>
</template>
<style>
</style>
<script>
import eventService from '@/services/EventService'
import VuePagination from './pagination/VuePagination'
import VuePaginationInfo from './pagination/VuePaginationInfo'
import {PaginationEvent} from './pagination/PaginationEvent'
import $ from 'jquery'

export default {
  name: 'EventListView',
  components: {
    VuePagination,
    VuePaginationInfo
  },
  data () {
    return {
      loading: false,
      error: '',
      currentPage: this.$route.query.page || 0,
      pagination: null,
      events: []
    }
  },
  created () {
    this.loadData()
  },
  methods: {
    loadData: function () {
      this.error = this.events = null
      this.loading = true
      let params = { page: this.currentPage, sort: 'id,desc' }
      eventService.getEvents(params, (success, data) => {
        this.loading = false
        if (success) {
          this.fireEvent('load-success', data)
          this.events = data._embedded.events
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
    deleteEvent: function (id) {
      eventService.deleteEvent(id, (success, data) => {
        if (success) {
          $('tr[data-id=' + id + ']').remove()
        } else {
          console.log('delete ' + id + ' failed: ' + data)
        }
      })
    }
  }
}
</script>
