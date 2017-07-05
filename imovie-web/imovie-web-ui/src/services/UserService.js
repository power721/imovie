import Vue from 'vue'
// import {router} from '../router'

export default {
  getAll () {
    return Vue.http.get('/users')
  },

  getUser (username, cb) {
    return Vue.http.get('/users/' + username)
    .then(({data}) => {
      if (cb) cb(true, data)
    }, ({data}) => {
      if (cb) cb(false, data)
    })
  },

  signup (userInfo, cb) {
    Vue.http.post('/api/users/account', userInfo)
    .then(({data}) => {
      if (cb) cb(true, data)
    }, ({data}) => {
      if (cb) cb(false, data)
    })
  },

  update (userInfo, cb) {
    Vue.http.put('/api/users/account', userInfo)
    .then(({data}) => {
      if (cb) cb(true, data)
    }, ({data}) => {
      if (cb) cb(false, data)
    })
  }
}
