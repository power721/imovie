<template>
  <div id="app">
    <header class="header">
      <div class="ui menu">
        <div class="ui container">
          <router-link class="item header" to="/" exact>{{ $t("message.index") }}</router-link>
          <router-link class="item" to="/movies">{{ $t("message.movie") }}</router-link>
          <router-link class="item" to="/episodes">{{ $t("message.episode") }}</router-link>
          <router-link class="item" to="/resources">{{ $t("message.resource") }}</router-link>
          <div class="right menu">
            <div class="item" v-if="!user.authenticated">
              <router-link to="/login">{{ $t("message.login") }}</router-link>
            </div>
            <div class="item" v-if="!user.authenticated">
              <router-link to="/signup">{{ $t("message.signup") }}</router-link>
            </div>
            <div class="item" v-if="user.authenticated">
              <router-link :to="'/users/' + user.name">
                {{ user.name }}
              </router-link>
            </div>
            <div class="item" v-if="user.authenticated">
              <a href="#" @click.prevent="logout">{{ $t("message.logout") }}</a>
            </div>
            <div class="item">
              <select v-model="locale" class="ui compact dropdown" id="locale">
                <option value="en">English</option>
                <option value="zh">中文</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main>
      <transition name="fade" mode="out-in">
        <router-view></router-view>
      </transition>
    </main>
  </div>
</template>

<script>
import auth from '@/services/Auth'
import storageService from '@/services/StorageService'
import $ from 'jquery'

export default {
  name: 'app',
  mounted () {
    this.user = auth.user
    $('#locale').dropdown()
  },
  data () {
    return {
      user: {},
      locale: storageService.getItem('locale') || 'zh'
    }
  },
  watch: {
    locale (val) {
      this.$i18n.locale = val
      storageService.setItem('locale', val)
    }
  },
  methods: {
    logout () {
      auth.logout()
    }
  }
}
</script>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}
</style>
