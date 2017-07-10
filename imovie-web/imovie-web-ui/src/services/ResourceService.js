import Vue from 'vue'

export default {
  getResource (params, cb) {
    var uri = '/api/resources/'
    if (params.text) {
      uri = '/api/resources/search/search/'
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
  deleteResource (id, cb) {
    return Vue.http.delete('/api/resources/' + id)
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
  transferResources (data, cb) {
    return Vue.http.post('/api/resources/transfer', data)
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
