export default {
  data: {},
  setItem (key, value) {
    if (typeof Storage !== 'undefined') {
      localStorage.setItem(key, value)
    } else {
      this.data[key] = value
    }
  },
  getItem (key) {
    if (typeof Storage !== 'undefined') {
      return localStorage.getItem(key)
    } else {
      return this.data[key]
    }
  },
  removeItem (key) {
    if (typeof Storage !== 'undefined') {
      localStorage.removeItem(key)
    } else {
      delete this.data[key]
    }
  }
}
