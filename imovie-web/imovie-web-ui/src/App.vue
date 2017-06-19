<template>
  <div id="app">
    <header class="header">
      <div class="ui menu">
        <div class="ui container">
          <router-link class="item header" to="/" exact>电影</router-link>
          <router-link class="item" to="/resources">资源</router-link>
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
export default {
  name: 'app'
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
