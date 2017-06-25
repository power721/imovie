import Vue from 'vue'

export default {
  getMovies (params, cb) {
    var uri = '/api/movies/'
    if (params.name && params.category && params.category !== 'all') {
      uri = '/api/movies/search/search/'
    } else if (params.name) {
      if (/^tt\d+$/.test(params.name)) {
        uri = '/api/movies/search/by-imdb/'
      } else {
        uri = '/api/movies/search/by-name/'
      }
    } else if (params.category && params.category !== 'all') {
      uri = '/api/movies/search/by-category/'
    }
    return Vue.http.get(uri, {params: params})
    .then(({data}) => {
      if (cb) {
        cb(true, data)
      }
    }, ({data}) => {
      if (cb) {
        cb(false, data)
      }
    })
  },

  getMovie (id, cb) {
    return Vue.http.get('/api/movies/' + id)
    .then(({data}) => {
      if (cb) {
        cb(true, data)
      }
    }, ({data}) => {
      if (cb) {
        cb(false, data)
      }
    })
  }
}
