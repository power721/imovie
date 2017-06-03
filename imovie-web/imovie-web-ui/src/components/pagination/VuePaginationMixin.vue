<script>
import {PaginationEvent} from './PaginationEvent'

export default {
  props: {
    css: {
      type: Object,
      default () {
        return {
          wrapperClass: 'ui right floated pagination menu',
          activeClass: 'active large',
          disabledClass: 'disabled',
          pageClass: 'item',
          linkClass: 'icon item',
          paginationClass: 'ui bottom attached segment grid',
          paginationInfoClass: 'left floated left aligned five wide column'
        }
      }
    },
    icons: {
      type: Object,
      default () {
        return {
          first: 'angle double left icon',
          prev: 'left chevron icon',
          next: 'right chevron icon',
          last: 'angle double right icon'
        }
      }
    },
    onEachSide: {
      type: Number,
      default () {
        return 2
      }
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
  computed: {
    totalPage () {
      return this.pagination === null
        ? 0
        : this.pagination.totalPages
    },
    isOnFirstPage () {
      return this.pagination === null
        ? false
        : this.pagination.number === 0
    },
    isOnLastPage () {
      return this.pagination === null
        ? false
        : this.pagination.number + 1 === this.pagination.totalPages
    },
    notEnoughPages () {
      return this.totalPage < (this.onEachSide * 2) + 4
    },
    windowSize () {
      return this.onEachSide * 2 + 1
    },
    windowStart () {
      if (!this.pagination || this.pagination.number <= this.onEachSide) {
        return 0
      } else if (this.pagination.number >= (this.totalPage - this.onEachSide)) {
        return this.totalPage - 1 - this.onEachSide * 2
      }

      return this.pagination.number - this.onEachSide
    }
  },
  methods: {
    loadPage (page) {
      this.$emit('vue-pagination:change-page', page)
    },
    isCurrentPage (page) {
      return page === this.pagination.number
    },
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
