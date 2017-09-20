<template>
  <div id="app">
    <header class="header">
      <div class="ui menu">
        <div class="ui container">
          <router-link class="item header" to="/" exact>{{ $t("token.index") }}</router-link>
          <router-link class="item" to="/movies">{{ $tc("token.movie", 5) }}</router-link>
          <router-link class="item" to="/episodes">{{ $tc("token.episodes", 5) }}</router-link>
          <router-link class="item" to="/resources" v-if="user.authenticated">{{ $tc("token.resource", 5) }}</router-link>
          <router-link class="item" to="/configs" v-if="user.isAdmin">{{ $tc("token.config", 5) }}</router-link>
          <router-link class="item" to="/events" v-if="user.isAdmin">{{ $tc("token.event", 5) }}</router-link>
          <div class="right menu">
            <div class="item" v-if="!user.authenticated">
              <router-link :to="'/login?redirect=' + $route.path">{{ $t("token.login") }}</router-link>
            </div>
            <div class="item" v-if="!user.authenticated">
              <router-link to="/signup">{{ $t("token.signup") }}</router-link>
            </div>
            <div class="item" v-if="user.authenticated">
              <router-link to="/users">
                {{ user.name }}
              </router-link>
            </div>
            <div class="item" v-if="user.authenticated">
              <a href="#" @click.prevent="logout">{{ $t("token.logout") }}</a>
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
      locale: storageService.getItem('locale') || 'en'
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
