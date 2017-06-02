<template>
  <div class="ui container divided items">
    <div v-for="movie in movies" class="item movie" style="min-height: 225px;">
      <router-link :to="'/movies/' + movie.id" class="ui small image">
        <img :src="movie.thumb">
      </router-link>
      <div class="content">
        <router-link :to="'/movies/' + movie.id" class="header">
          {{ movie.title }}
        </router-link>
        <div class="description">
          <p>{{ movie.synopsis || '暂无介绍' }}</p>
        </div>
        <div class="extra">
          <div>
            <span class="date">{{ movie.createdTime | date }}</span>
            <a :href="movie.imdbUrl" target="_blank" class="imdb">IMDB：{{ movie.imdbScore || '0.0' }}</a>
            <a :href="movie.dbUrl" target="_blank" class="dou">豆瓣：{{ movie.dbScore || '0.0' }}</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<style>
  div.description {
    min-height: 150px;
    text-align: left;
  }
  span.date {
    color: #8f8f8f;
    position: absolute;
    left: 5px;
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

</style>
<script>
import movieService from '@/services/MovieService'

export default {
  name: 'MovieListView',
  data () {
    return {
      loading: false,
      error: '',
      page: this.$route.query.page || 0,
      pagination: null,
      movies: []
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.error = this.movies = null
      this.loading = true
      movieService.getAll(this.page, (success, data) => {
        this.loading = false
        if (success) {
          this.movies = data._embedded.movies
          this.pagination = data.page
        } else {
          this.error = data.message || 'Bad Request'
        }
      })
    }
  }
}

</script>
