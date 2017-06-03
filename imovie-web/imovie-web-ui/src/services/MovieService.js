import Vue from 'vue'

export default {
  getMovies (text, page, cb) {
    var uri = '/api/movies/?page=' + page
    if (text !== null && text !== '') {
      uri = '/api/movies/search/by-name?name=' + text + '&page=' + page
    }
    return Vue.http.get(uri)
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
