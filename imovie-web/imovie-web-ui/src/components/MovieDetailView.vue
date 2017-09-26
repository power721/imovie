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
      <h2>
        <i class="ui yellow circular label" v-if="movie.db250">{{ movie.db250 }}</i>
        {{ movie.title }}
        <a v-if="$auth.user.authenticated" @click="setFavourite(movie.id)"><i class="small star link icon" :class="{empty: !favourite, red: favourite}"></i></a>
        <a v-if="$auth.user.isAdmin" @click="refreshMovie(movie.id)"><i class="small refresh link icon"></i></a>
        <a v-if="$auth.user.isAdmin" @click="deleteMovie(movie.id)"><i class="small red remove link icon"></i></a>
      </h2>
      <img id="thumb" class="ui image" :src="movie.thumb">
      <div class="ui items">
        <div class="item directors" v-if="isNotEmpty(movie._embedded.directors)">
          <div class="content">
            <div class="header">{{ $tc("token.director", movie._embedded.directors.length) }}</div>
            <div class="description">{{ movie._embedded.directors | join }}</div>
          </div>
        </div>
        <div class="item editors" v-if="isNotEmpty(movie._embedded.editors)">
          <div class="content">
            <div class="header">{{ $tc("token.editor", movie._embedded.editors.length) }}</div>
            <div class="description">{{ movie._embedded.editors | join }}</div>
          </div>
        </div>
        <div class="item actors" v-if="isNotEmpty(movie._embedded.actors)">
          <div class="content">
            <div class="header">{{ $tc("token.actor", movie._embedded.actors.length) }}</div>
            <div class="description">{{ movie._embedded.actors | join }}</div>
          </div>
        </div>
        <div class="item categories" v-if="isNotEmpty(movie._embedded.categories)">
          <div class="content">
            <div class="header">{{ $tc("token.category", movie._embedded.categories.length) }}</div>
            <div class="description">{{ movie._embedded.categories | join }}</div>
          </div>
        </div>
        <div class="item regions" v-if="isNotEmpty(movie._embedded.regions)">
          <div class="content">
            <div class="header">{{ $tc("token.region", movie._embedded.regions.length) }}</div>
            <div class="description">{{ movie._embedded.regions | join }}</div>
          </div>
        </div>
        <div class="item languages" v-if="isNotEmpty(movie._embedded.languages)">
          <div class="content">
            <div class="header">{{ $tc("token.language", movie._embedded.languages.length) }}</div>
            <div class="description">{{ movie._embedded.languages | join }}</div>
          </div>
        </div>
        <div class="item releaseDate" v-if="movie.releaseDate">
          <div class="content">
            <div class="header">{{ $t("token.releaseDate") }}</div>
            <div class="description">{{ movie.releaseDate }}</div>
          </div>
        </div>
        <div class="item runningTime" v-if="movie.runningTime">
          <div class="content">
            <div class="header">{{ $t("token.runningTime") }}</div>
            <div class="description">{{ movie.runningTime }}</div>
          </div>
        </div>
        <div class="item episode" v-if="movie.episode">
          <div class="content">
            <div class="header">{{ $t("token.episode") }}</div>
            <div class="description">{{ movie.episode }}</div>
          </div>
        </div>
        <div class="item aliases" v-if="isNotEmpty(movie.aliases)">
          <div class="content">
            <div class="header">{{ $tc("token.alias", movie.aliases.length) }}</div>
            <div class="description">{{ movie.aliases | join }}</div>
          </div>
        </div>
        <div class="item db">
          <div class="content">
            <div class="header">{{ $t("token.dbScore") }}</div>
            <div class="description">
              <a :href="movie.dbUrl" target="_blank">{{ movie.dbScore || '0.0' }}</a>
              <template v-if="movie.votes">({{ movie.votes }})</template>
            </div>
          </div>
        </div>
        <div class="item imdb" v-if="movie.imdbUrl">
          <div class="content">
            <div class="header">{{ $t("token.imdbScore") }}</div>
            <div class="description"><a :href="movie.imdbUrl" target="_blank">{{ movie.imdbScore || '0.0' }}</a>
            </div>
          </div>
        </div>
      </div>

      <div class="ui message synopsis">
        <div class="header">{{ $t("token.synopsis") }}</div>
        <p>{{ movie.synopsis || $t("message.noIntro") }}</p>
      </div>

      <!--<div class="ui horizontal divider">剧照</div>-->
      <!--<div id="snapshots" class="ui small images">-->
      <!--<img v-for="snapshot in movie.snapshots" :src="snapshot" class="ui image">-->
      <!--</div>-->

      <template v-if="isAccessible(movie)">
        <div class="ui horizontal divider">{{ $tc("token.resource", movie.res.length) }}</div>
        <div class="ui relaxed divided list" id="resources">
          <div class="item resource" v-for="resource in movie.res" :key="resource.id">
            <i class="middle aligned icon" :class="getIconClass(resource.uri)"></i>
            <div class="content">
              <div class="header">
                <a :href="resource.uri" target="_blank" :title="'点击下载资源 ' + resource.id">{{ resource.title || resource.uri }}</a>
                <a v-if="resource.original" :href="fixBtbtt(resource.original)" title="资源原始地址" target="_blank">
                  &nbsp;&nbsp;<i class="small external icon"></i>
                </a>
                <a v-if="$auth.user.isAdmin" @click="deleteResource(resource.id)"><i class="small red remove icon"></i></a>
              </div>
            </div>
            <div class="ui right floated child checkbox" v-if="$auth.user.isAdmin">
              <input type="checkbox" name="resourceIds" v-model="resourceIds" :value="resource.id">
              <label></label>
            </div>
          </div>
          <button class="ui right floated primary button" id="move" @click="transferResources" v-if="$auth.user.isAdmin">
            Move
          </button>
        </div>

        <div class="ui modal" id="transfer">
          <i class="close icon"></i>
          <div class="header">
            Transfer Resources
          </div>
          <div class="content">
            <div class="ui form">
              <div class="field">
                <label>Movie ID</label>
                <input type="number" name="movieId">
              </div>
            </div>
          </div>
          <div class="actions">
            <div class="ui cancel button">Cancel</div>
            <div class="ui ok green button">OK</div>
          </div>
        </div>
      </template>
      <div class="ui hidden divider"></div>

      <template v-if="$auth.user.isAdmin">
        <div class="ui">
          <button class="ui right floated primary button" id="add" @click="showAddModal=true">
            Add
          </button>
        </div>
        <vue-semantic-modal v-model="showAddModal" show-close-icon="true">
          <template slot="header">
            Add Resource
          </template>
          <template slot="content">
            <div class="ui form">
              <div class="required field">
                <label>Title</label>
                <input v-model="resourceDTO.title" placeholder="title" required>
              </div>
              <div class="required field">
                <label>URI</label>
                <input type="url" v-model="resourceDTO.uri" placeholder="URI" required>
              </div>
            </div>
          </template>
          <template slot="actions">
            <div class="ui cancel button" @click="showAddModal=false">Cancel</div>
            <div class="ui ok green button" @click="addResource">Add</div>
          </template>
        </vue-semantic-modal>
      </template>
      <div class="ui hidden divider"></div>

    </div>
  </div>
</template>
<style>
  img.thumb {
    max-width: 450px;
  }
  button#add {
    margin-top: -14px;
  }
</style>
<script>
import movieService from '@/services/MovieService'
import userService from '@/services/UserService'
import resourceService from '@/services/ResourceService'
import VueSemanticModal from 'vue-semantic-modal'
import $ from 'jquery'

export default {
  name: 'MovieDetail',
  components: {
    VueSemanticModal
  },
  data () {
    return {
      loading: false,
      error: '',
      showAddModal: false,
      resourceDTO: {
        uri: null,
        title: null
      },
      favourite: false,
      movieId: null,
      resourceIds: [],
      movie: null
    }
  },
  created () {
    this.loadData()
    if (this.$auth.user.authenticated) {
      this.isFavourite(this.$route.params.id)
    }
  },
  watch: {
    '$route': 'loadData'
  },
  methods: {
    loadData () {
      this.error = this.movie = null
      this.loading = true
      movieService.getMovie(this.$route.params.id, (success, data) => {
        this.loading = false
        if (success) {
          this.movie = data
          document.title = this.movie.title
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
      // if (uri.startsWith('http://btbtt.co/')) {
      //  return uri.replace('http://btbtt.co/', 'http://btbtt.pw/')
      // }
      return uri
    },
    isNotEmpty (array) {
      return typeof array !== 'undefined' && array !== null && array.length !== null && array.length > 0
    },
    deleteResource: function (id) {
      resourceService.deleteResource(id, (success, data) => {
        if (success) {
          $('div[key=' + id + ']').remove()
        } else {
          console.log('delete ' + id + ' failed: ' + data)
        }
      })
    },
    deleteMovie: function (id) {
      movieService.deleteMovie(id, (success, data) => {
        if (success) {
          this.$router.push('/')
        } else {
          console.log('delete ' + id + ' failed: ')
          console.log(data)
        }
      })
    },
    refreshMovie: function (id) {
      movieService.refreshMovie(id, (success, data) => {
        if (success) {
          this.$router.go(this.$router.currentRoute)
        } else {
          console.log('refresh ' + id + ' failed: ')
          console.log(data)
        }
      })
    },
    setFavourite: function (id) {
      var func
      if (this.favourite) {
        func = userService.deleteFavourite
      } else {
        func = userService.addFavourite
      }
      func(id, (success, data) => {
        if (success) {
          this.favourite = data
        } else {
          console.log('set favourite ' + id + ' failed: ')
          console.log(data)
        }
      })
    },
    isFavourite: function (id) {
      userService.isFavourite(id, (success, data) => {
        if (success) {
          this.favourite = data
        } else {
          console.log('get favourite ' + id + ' failed: ')
          console.log(data)
        }
      })
    },
    isAccessible: function (movie) {
      if (!this.$auth.user.authenticated && this.isAdult(movie)) {
        return false
      }
      return this.isNotEmpty(movie.res)
    },
    isAdult: function (movie) {
      if (!movie._embedded.categories) {
        return false
      }
      for (var entry of movie._embedded.categories) {
        if (entry.name === '情色' || entry.name === '同性') {
          return true
        }
      }
      return false
    },
    transferResources: function () {
      var that = this
      $('#transfer').modal({
        onApprove: function () {
          const movieId = $('input[name=movieId]').val()
          const data = {resourceIds: that.resourceIds, movieId: movieId}
          resourceService.transferResources(data, (success, data) => {
            if (success) {
              if (movieId) {
                that.$router.push('/movies/' + movieId)
              } else {
                that.$router.go(that.$router.currentRoute)
              }
            } else {
              console.log('transfer resources failed: ')
              console.log(data)
            }
          })
        }
      }).modal('show')
    },
    addResource: function () {
      this.showAddModal = false
      movieService.addResource(this.movie.id, this.resourceDTO, (success, data) => {
        if (success) {
          this.$router.go(this.$router.currentRoute)
        } else {
          console.log('Add resources failed: ')
          console.log(data)
        }
      })
    }
  }
}

</script>
