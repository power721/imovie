<template>
  <div :class="['vue-pagination-info', infoClass]"
       v-html="paginationInfo">
  </div>
</template>

<script>
import VuePaginationInfoMixin from './VuePaginationInfoMixin.vue'
import {PaginationEvent} from './PaginationEvent'

export default {
  mixins: [VuePaginationInfoMixin],
  computed: {
    paginationInfo () {
      if (this.pagination === null || this.pagination.totalElements === 0) {
        return this.noDataTemplate
      }

      return this.infoTemplate
        .replace('{from}', this.pagination.from || 0)
        .replace('{to}', this.pagination.to || 0)
        .replace('{total}', this.pagination.totalElements || 0)
    }
  },
  data: function () {
    return {
      pagination: null
    }
  },
  created () {
    this.registerEvents()
  },
  methods: {
    setPaginationData (pagination) {
      this.pagination = pagination
    },
    registerEvents () {
      let self = this

      PaginationEvent.$on('vue-pagination:pagination-data', (paginationData) => {
        self.setPaginationData(paginationData)
      })
    }
  }
}

</script>
