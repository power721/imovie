import Vue from 'vue'

export default {
  getConfigs (cb) {
    const uri = '/api/configs/'
    return Vue.http.get(uri, {params: {size: 100}})
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
