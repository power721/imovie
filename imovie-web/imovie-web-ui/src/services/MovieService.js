import Vue from 'vue'

export default {
  getMovies (params, cb) {
    var q = []
    if (params.name) {
      if (/^tt\d+$/.test(params.name)) {
        q.push('imdbUrl==*' + params.name)
      } else {
        q.push('title==*' + params.name + '*')
      }
      delete params.name
    }
    if (params.episode) {
      q.push('episode!=NULL')
      delete params.episode
    }
    if (params.movie) {
      q.push('episode==NULL')
      delete params.movie
    }
    if (params.category && params.category !== 'all') {
      q.push('categories.name==' + params.category)
      delete params.category
    }
    if (params.region && params.region !== 'all') {
      q.push('regions.name==' + params.region)
      delete params.region
    }
    if (params.language && params.language !== 'all') {
      q.push('languages.name==' + params.language)
      delete params.language
    }
    if (params.year && params.year !== 'all') {
      q.push('year==' + params.year)
      delete params.year
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
