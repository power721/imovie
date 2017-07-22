import Vue from 'vue'

export default {
  getEvents (params, cb) {
    const uri = '/api/events/'
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
  deleteEvent (id, cb) {
    return Vue.http.delete('/api/events/' + id)
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
