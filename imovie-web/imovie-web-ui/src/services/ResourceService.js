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
  }
}