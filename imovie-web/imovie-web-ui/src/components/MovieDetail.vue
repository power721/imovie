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
      <img id="thumb" class="ui image" :src="movie.thumb">
      <div class="ui items">
        <div class="item" v-if="isNotEmpty(movie._embedded.directors)">
          <div class="content">
            <div class="header">导演</div>
            <div class="description">{{ movie._embedded.directors | join }}</div>
          </div>
        </div>
        <div class="item" v-if="isNotEmpty(movie._embedded.editors)">
          <div class="content">
            <div class="header">编剧</div>
            <div class="description">{{ movie._embedded.editors | join }}</div>
          </div>
        </div>
        <div class="item" v-if="isNotEmpty(movie._embedded.actors)">
          <div class="content">
            <div class="header">主演</div>
            <div class="description">{{ movie._embedded.actors | join }}</div>
          </div>
        </div>
        <div class="item" v-if="isNotEmpty(movie._embedded.categories)">
          <div class="content">
            <div class="header">类型</div>
            <div class="description">{{ movie._embedded.categories | join }}</div>
          </div>
        </div>
        <div class="item" v-if="isNotEmpty(movie._embedded.regions)">
          <div class="content">
            <div class="header">制片国家/地区</div>
            <div class="description">{{ movie._embedded.regions | join }}</div>
          </div>
        </div>
        <div class="item" v-if="isNotEmpty(movie._embedded.languages)">
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
        <div class="item" v-if="movie.episode">
          <div class="content">
            <div class="header">集数</div>
            <div class="description">{{ movie.episode }}</div>
          </div>
        </div>
        <div class="item" v-if="isNotEmpty(movie.aliases)">
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
            <div class="header">IMDb评分</div>
            <div class="description"><a :href="movie.imdbUrl" target="_blank">{{ movie.imdbScore || '0.0' }}</a>
            </div>
          </div>
        </div>
      </div>

      <div class="ui message">
        <div class="header">剧情简介</div>
        <p>{{ movie.synopsis || '暂无介绍' }}</p>
      </div>

      <!--<div class="ui horizontal divider">剧照</div>-->
      <!--<div id="snapshots" class="ui small images">-->
      <!--<img v-for="snapshot in movie.snapshots" :src="snapshot" class="ui image">-->
      <!--</div>-->

      <template v-if="isNotEmpty(movie.res)">
        <div class="ui horizontal divider">资源</div>
        <div class="ui relaxed divided list">
          <div class="item" v-for="resource in movie.res">
            <i class="middle aligned icon" :class="getIconClass(resource.uri)"></i>
            <div class="content">
              <div class="header">
                <a :href="resource.uri" target="_blank" title="点击下载资源">{{ resource.title || resource.uri }}</a>
                <a v-if="resource.original" :href="fixBtbtt(resource.original)" title="资源原始地址" target="_blank">
                  &nbsp;&nbsp;<i class="small external icon"></i>
                </a>
              </div>
            </div>
          </div>
        </div>
      </template>
      <div class="ui hidden divider"></div>

    </div>
  </div>
</template>
<style>
  img.thumb {
    max-width: 450px;
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
    },
    getIconClass (uri) {
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
    isNotEmpty (array) {
      return typeof array !== 'undefined' && array !== null && array.length !== null && array.length > 0
    }
  }
}

</script>
