import Vue from 'vue'
// import {router} from '../router'

export default {
  getAll () {
    return Vue.http.get('/api/users')
  },

  getUser (username, cb) {
    return Vue.http.get('/api/users/' + username)
    .then(({data}) => {
      if (cb) cb(true, data)
    }, ({data}) => {
      if (cb) cb(false, data)
    })
  },

  signup (userInfo, cb) {
    Vue.http.post('/api/users', userInfo)
    .then(({data}) => {
      if (cb) cb(true, data)
    }, ({data}) => {
      if (cb) cb(false, data)
    })
  },

  update (userInfo, cb) {
    Vue.http.put('/api/users', userInfo)
    .then(({data}) => {
      if (cb) cb(true, data)
    }, ({data}) => {
      if (cb) cb(false, data)
    })
  }
}
