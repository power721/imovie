import Vue from 'vue'
import jwtDecode from 'jwt-decode'
import router from '../router'
import storageService from '@/services/StorageService'

export default {

  user: {
    name: '',
    authorities: [],
    authenticated: false,
    isAdmin: function () {
      return this.authenticated && this.authorities.length === 1 && this.authorities[0] === 'ROLE_ADMIN'
    }
  },

  loggedIn () {
    return !!storageService.getItem('jwt-token')
  },

  login (creds, cb) {
    const options = {
      emulateJSON: true,
      headers: {
        Authorization: 'Basic Y2xpZW50OnNlY3JldA=='
      }
    }
    Vue.http.post('/oauth/token', creds, options)
    .then((response) => {
      const data = response.body
      this.setToken(data.access_token)
      this.getUserInfo(data.access_token)
      if (cb) cb(true)
    }, () => {
      if (cb) cb(false)
    })
  },

  getUserInfo (token) {
    token = token || this.getToken()

    if (typeof token === 'undefined' || token === null || token === '') {
      return false
    }

    const data = jwtDecode(token)
    if (Date.now() >= data.exp * 1000) {
      this.user.name = ''
      this.user.authorities = []
      this.user.authenticated = false
      storageService.removeItem('jwt-token')
      console.log('access token expired')
    } else {
      this.user.name = data.user_name
      this.user.authorities = data.authorities
      this.user.authenticated = true
    }
  },

  checkAuth () {
    this.getUserInfo()
  },

  logout () {
    const token = this.getToken()

    if (this.user.authenticated && token) {
      this.user.name = ''
      this.user.authorities = []
      this.user.authenticated = false

      storageService.removeItem('jwt-token')
      router.push('/')
    }
  },

  getToken () {
    return storageService.getItem('jwt-token')
  },

  setToken (token) {
    storageService.setItem('jwt-token', token)
  }

}
