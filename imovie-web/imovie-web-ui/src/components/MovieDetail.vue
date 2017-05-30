<template>
  <div class="ui" id="movie">
    <div class="ui hidden divider"></div>
    <div class="ui active dimmer" v-if="loading">
      <div class="ui loader"></div>
    </div>
    <div class="ui error message" v-show="error" transition="fade">
      {{ error }}
    </div>
    <div v-if="movie">
      <div>{{ movie.title }}</div>
      <div>
        <img :src="movie.thumb">
      </div>
      <div>{{ movie.releaseDate }}</div>
      <div>{{ movie.runningTime }}</div>
      <div>{{ movie._embedded.directors | join }}</div>
      <div>{{ movie._embedded.editors | join }}</div>
      <div>{{ movie._embedded.actors | join }}</div>
      <div>{{ movie._embedded.categories | join }}</div>
      <div>{{ movie._embedded.languages | join }}</div>
      <div>{{ movie._embedded.regions | join }}</div>
      <div>{{ movie.aliases | join }}</div>
      <div>{{ movie.imdbUrl }}</div>
      <div>{{ movie.synopsis }}</div>
    </div>
  </div>
</template>
<style>
</style>
<script>
import movieService from '@/services/MovieService'

export default {
  name: 'MovieDetail',
  data () {
    return {
      loading: false,
      error: '',
      movie: {
        'id': 21,
        'title': '哦，我的金雨 오 마이 금비 (2016)',
        'name': '哦，我的金雨 오 마이 금비',
        'year': 2016,
        'synopsis': '　　讲述一个爸爸在照顾患有小儿痴呆症，记忆丧失的8岁女儿的过程中领悟到人生价值的故事。',
        'thumb': 'https://img1.doubanio.com/view/movie_poster_cover/lpst/public/p2398194347.jpg',
        'cover': 'https://img1.doubanio.com/view/photo/photo/public/p2398194347.jpg',
        'aliases': [ '哦，我的锦菲', 'Oh My 锦朏', 'Oh My 金雨', 'Oh My Geum Bi' ],
        'snapshots': [ 'https://img3.doubanio.com/view/photo/photo/public/p2398194371.jpg', 'https://img3.doubanio.com/view/photo/photo/public/p2398194313.jpg', 'https://img1.doubanio.com/view/photo/photo/public/p2398194347.jpg', 'https://img1.doubanio.com/view/photo/photo/public/p2398194288.jpg', 'https://img3.doubanio.com/view/photo/photo/public/p2410822950.jpg' ],
        'releaseDate': '2016-11-16(韩国)',
        'runningTime': null,
        'imdbUrl': 'http://www.imdb.com/title/tt6234398',
        'imdbScore': null,
        'dbUrl': 'https://movie.douban.com/subject/26892555/',
        'dbScore': '8.3',
        'createdTime': '2017-05-28T03:04:20.000+0000',
        '_embedded': {
          'actors': [ {
            'name': '许廷恩',
            'id': 170,
            '_links': {
              'self': {
                'href': 'http://power.oj/api/persons/170{?projection}',
                'templated': true
              },
              'actMovies': {
                'href': 'http://power.oj/api/persons/170/actMovies'
              },
              'directMovies': {
                'href': 'http://power.oj/api/persons/170/directMovies'
              },
              'editMovies': {
                'href': 'http://power.oj/api/persons/170/editMovies'
              }
            }
          }, {
            'name': '吴允儿',
            'id': 172,
            '_links': {
              'self': {
                'href': 'http://power.oj/api/persons/172{?projection}',
                'templated': true
              },
              'actMovies': {
                'href': 'http://power.oj/api/persons/172/actMovies'
              },
              'directMovies': {
                'href': 'http://power.oj/api/persons/172/directMovies'
              },
              'editMovies': {
                'href': 'http://power.oj/api/persons/172/editMovies'
              }
            }
          }, {
            'name': '朴真熙',
            'id': 174,
            '_links': {
              'self': {
                'href': 'http://power.oj/api/persons/174{?projection}',
                'templated': true
              },
              'actMovies': {
                'href': 'http://power.oj/api/persons/174/actMovies'
              },
              'directMovies': {
                'href': 'http://power.oj/api/persons/174/directMovies'
              },
              'editMovies': {
                'href': 'http://power.oj/api/persons/174/editMovies'
              }
            }
          }, {
            'name': '吴智昊',
            'id': 176,
            '_links': {
              'self': {
                'href': 'http://power.oj/api/persons/176{?projection}',
                'templated': true
              },
              'actMovies': {
                'href': 'http://power.oj/api/persons/176/actMovies'
              },
              'directMovies': {
                'href': 'http://power.oj/api/persons/176/directMovies'
              },
              'editMovies': {
                'href': 'http://power.oj/api/persons/176/editMovies'
              }
            }
          } ],
          'regions': [ {
            'name': '韩国',
            'id': 6,
            '_links': {
              'self': {
                'href': 'http://power.oj/api/regions/6{?projection}',
                'templated': true
              },
              'movies': {
                'href': 'http://power.oj/api/regions/6/movies'
              }
            }
          } ],
          'languages': [ {
            'name': '韩语',
            'id': 4,
            '_links': {
              'self': {
                'href': 'http://power.oj/api/languages/4{?projection}',
                'templated': true
              },
              'movies': {
                'href': 'http://power.oj/api/languages/4/movies'
              }
            }
          } ],
          'directors': [ {
            'name': '金英兆',
            'id': 167,
            '_links': {
              'self': {
                'href': 'http://power.oj/api/persons/167{?projection}',
                'templated': true
              },
              'actMovies': {
                'href': 'http://power.oj/api/persons/167/actMovies'
              },
              'directMovies': {
                'href': 'http://power.oj/api/persons/167/directMovies'
              },
              'editMovies': {
                'href': 'http://power.oj/api/persons/167/editMovies'
              }
            }
          } ],
          'categories': [ {
            'name': '剧情',
            'id': 3,
            '_links': {
              'self': {
                'href': 'http://power.oj/api/categories/3{?projection}',
                'templated': true
              },
              'movies': {
                'href': 'http://power.oj/api/categories/3/movies'
              }
            }
          } ],
          'editors': [ {
            'name': '金浩成',
            'id': 168,
            '_links': {
              'self': {
                'href': 'http://power.oj/api/persons/168{?projection}',
                'templated': true
              },
              'actMovies': {
                'href': 'http://power.oj/api/persons/168/actMovies'
              },
              'directMovies': {
                'href': 'http://power.oj/api/persons/168/directMovies'
              },
              'editMovies': {
                'href': 'http://power.oj/api/persons/168/editMovies'
              }
            }
          }, {
            'name': '李明熙',
            'id': 169,
            '_links': {
              'self': {
                'href': 'http://power.oj/api/persons/169{?projection}',
                'templated': true
              },
              'actMovies': {
                'href': 'http://power.oj/api/persons/169/actMovies'
              },
              'directMovies': {
                'href': 'http://power.oj/api/persons/169/directMovies'
              },
              'editMovies': {
                'href': 'http://power.oj/api/persons/169/editMovies'
              }
            }
          } ]
        }
      }
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
      // this.error = this.movie = null
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
