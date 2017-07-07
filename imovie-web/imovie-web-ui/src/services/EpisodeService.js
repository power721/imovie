import Vue from 'vue'

export default {
  getEpisodes (params, cb) {
    var uri = '/api/movies/search/by-episode'
    params.episode = 0
    if (params.name && params.category && params.category !== 'all') {
      uri = '/api/movies/search/search-episode/'
    } else if (params.name) {
      if (/^tt\d+$/.test(params.name)) {
        uri = '/api/movies/search/by-episode-imdb/'
      } else {
        uri = '/api/movies/search/by-episode-name/'
      }
    } else if (params.category && params.category !== 'all') {
      uri = '/api/movies/search/by-episode-category/'
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

  deleteMovie (id, cb) {
    return Vue.http.delete('/api/movies/' + id)
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
