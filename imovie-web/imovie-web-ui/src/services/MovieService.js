import Vue from 'vue'

export default {
  getMovies (params, cb) {
    var uri = '/api/movies/'
    if (params.name) {
      uri = '/api/movies/search/by-name/'
    } else if (params.category) {
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
