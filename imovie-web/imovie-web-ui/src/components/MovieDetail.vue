<template>
  <div class="ui container">
    <div class="ui hidden divider"></div>
    <div class="ui active dimmer" v-if="loading">
      <div class="ui loader"></div>
    </div>
    <div class="ui error message" v-show="error" transition="fade">
      {{ error }}
    </div>
    <div v-if="movie" id="movie">
      <h2>{{ movie.title }}</h2>
      <div>
        <img :src="movie.thumb">
      </div>
      <div class="ui items" v-if="movie._embedded.directors.length">
        <div class="item">
          <div class="content">
            <div class="header">导演</div>
            <div class="description">{{ movie._embedded.directors | join }}</div>
          </div>
        </div>
        <div class="item" v-if="movie._embedded.editors.length">
          <div class="content">
            <div class="header">编剧</div>
            <div class="description">{{ movie._embedded.editors | join }}</div>
          </div>
        </div>
        <div class="item" v-if="movie._embedded.actors.length">
          <div class="content">
            <div class="header">主演</div>
            <div class="description">{{ movie._embedded.actors | join }}</div>
          </div>
        </div>
        <div class="item" v-if="movie._embedded.categories.length">
          <div class="content">
            <div class="header">类型</div>
            <div class="description">{{ movie._embedded.categories | join }}</div>
          </div>
        </div>
        <div class="item" v-if="movie._embedded.regions.length">
          <div class="content">
            <div class="header">制片国家/地区</div>
            <div class="description">{{ movie._embedded.regions | join }}</div>
          </div>
        </div>
        <div class="item" v-if="movie._embedded.languages.length">
          <div class="content">
            <div class="header">语言</div>
            <div class="description">{{ movie._embedded.languages | join }}</div>
          </div>
        </div>
        <div class="item" v-if="movie.releaseDate">
          <div class="content">
            <div class="header">上映日期</div>
            <div class="description">{{ movie.releaseDate }}</div>
          </div>
        </div>
        <div class="item" v-if="movie.runningTime">
          <div class="content">
            <div class="header">片长</div>
            <div class="description">{{ movie.runningTime }}</div>
          </div>
        </div>
        <div class="item" v-if="movie.aliases.length">
          <div class="content">
            <div class="header">又名</div>
            <div class="description">{{ movie.aliases | join }}</div>
          </div>
        </div>
        <div class="item">
          <div class="content">
            <div class="header">豆瓣评分</div>
            <div class="description"><a :href="movie.dbUrl" target="_blank">{{ movie.dbScore || '0.0' }}</a></div>
          </div>
        </div>
        <div class="item" v-if="movie.imdbUrl">
          <div class="content">
            <div class="header">IMDb链接</div>
            <div class="description"><a :href="movie.imdbUrl" target="_blank">{{ movie.imdbUrl | imdb }}</a>
            </div>
          </div>
        </div>
      </div>

      <div class="ui message">
        <div class="header">剧情简介</div>
        <p>{{ movie.synopsis }}</p>
      </div>

      <div class="ui horizontal divider">
        资源
      </div>
      <div class="ui relaxed divided list">
        <div class="item" v-for="resource in movie.res">
          <i class="magnet middle aligned icon"></i>
          <div class="content">
            <div class="header">
              <a :href="resource.uri" target="_blank" title="点击下载资源">{{ resource.title }}</a>
              <a v-if="resource.original" :href="resource.original" title="资源原始地址" target="_blank">
                &nbsp;&nbsp;<i class="small external icon"></i>
              </a>
            </div>
          </div>
        </div>
      </div>
      <div class="ui hidden divider"></div>

    </div>
  </div>
</template>
<style>
  div.description {
    min-height: 10px;
  }
</style>
<script>
import movieService from '@/services/MovieService'

export default {
  name: 'MovieDetail',
  data () {
    return {
      loading: false,
      error: '',
      movie: null
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    '$route': 'fetchData'
  },
  methods: {
    fetchData () {
      this.error = this.movie = null
      this.loading = true
      movieService.getMovie(this.$route.params.id, (success, data) => {
        this.loading = false
        if (success) {
          this.movie = data
        } else {
          this.error = data.message || 'Bad Request'
        }
      })
    }
  }
}

</script>
