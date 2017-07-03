<template>
  <div id="app">
    <header class="header">
      <div class="ui menu">
        <div class="ui container">
          <router-link class="item header" to="/" exact>首页</router-link>
          <router-link class="item" to="/movies">电影</router-link>
          <router-link class="item" to="/episodes">剧集</router-link>
          <router-link class="item" to="/resources">资源</router-link>
          <div class="right menu">
            <div class="item" v-if="!user.authenticated">
              <router-link to="/login">登录</router-link>
            </div>
            <div class="item" v-if="!user.authenticated">
              <router-link to="/signup">注册</router-link>
            </div>
            <div class="item" v-if="user.authenticated">
              <router-link :to="'/users/' + user.name">
                {{ user.name }}
              </router-link>
            </div>
            <div class="item" v-if="user.authenticated">
              <a href="#" @click.prevent="logout">注销</a>
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

export default {
  name: 'app',
  mounted () {
    this.user = auth.user
  },
  data () {
    return {
      user: {}
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
