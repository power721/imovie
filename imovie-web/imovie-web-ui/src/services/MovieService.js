import Vue from 'vue'

export default {
  getMovies (params, cb) {
    var q = []
    var query = params.q
    if (query.text) {
      if (/^tt\d+$/.test(query.text)) {
        q.push('imdbUrl=="*' + query.text + '"')
      } else {
        q.push('title=="*' + query.text + '*"')
      }
    }
    if (query.type) {
      if (query.type === 'episode') {
        q.push('episode!=NULL')
      } else if (query.type === 'movie') {
        q.push('episode==NULL')
      }
    }
    if (query.category && query.category !== 'all') {
      q.push('categories.name==' + query.category)
    }
    if (query.search) {
      q.push(query.search)
    }
    params.q = q.join(';')
    return Vue.http.get('/api/movies/', {params: params})
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
