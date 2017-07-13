<template>
  <div v-if="pagination && pagination.totalPages > 0" :class="css.wrapperClass">
    <a @click="loadPage(0)"
       :class="['btn-nav', css.linkClass, isOnFirstPage ? css.disabledClass : '']">
      <i v-if="icons.first != ''" :class="[icons.first]"></i>
      <span v-else>&laquo;</span>
    </a>
    <!--<a @click="loadPage(prevWindow)" :class="[css.pageClass]" v-if="prevWindow >= 0">&#8672;</a>-->
    <a @click="loadPage('prev')"
       :class="['btn-nav', css.linkClass, isOnFirstPage ? css.disabledClass : '']">
      <i v-if="icons.next != ''" :class="[icons.prev]"></i>
      <span v-else>&nbsp;&lsaquo;</span>
    </a>
    <template v-if="notEnoughPages">
      <template v-for="n in totalPage">
        <a @click="loadPage(n-1)"
           :class="[css.pageClass, isCurrentPage(n-1) ? css.activeClass : '']"
           v-html="n">
        </a>
      </template>
    </template>
    <template v-else>
      <template v-for="n in windowSize">
        <a @click="loadPage(windowStart+n-1)"
           :class="[css.pageClass, isCurrentPage(windowStart+n-1) ? css.activeClass : '']"
           v-html="windowStart+n">
        </a>
      </template>
    </template>
    <a @click="loadPage('next')"
       :class="['btn-nav', css.linkClass, isOnLastPage ? css.disabledClass : '']">
      <i v-if="icons.next != ''" :class="[icons.next]"></i>
      <span v-else>&rsaquo;&nbsp;</span>
    </a>
    <!--<a @click="loadPage(nextWindow)" :class="[css.pageClass]" v-if="nextWindow < totalPage">&#8674;</a>-->
    <a @click="loadPage(totalPage-1)"
       :class="['btn-nav', css.linkClass, isOnLastPage ? css.disabledClass : '']">
      <i v-if="icons.last != ''" :class="[icons.last]"></i>
      <span v-else>&raquo;</span>
    </a>
  </div>
</template>

<script>
import VuePaginationMixin from './VuePaginationMixin.vue'

export default {
  mixins: [VuePaginationMixin]
}

</script>
