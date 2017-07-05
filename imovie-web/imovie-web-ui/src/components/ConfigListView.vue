<template>
  <div class="ui container">
    <div class="ui hidden divider"></div>
    <div class="ui active dimmer" v-if="loading">
      <div class="ui loader"></div>
    </div>
    <div class="ui error message" v-show="error" transition="fade">
      {{ error }}
    </div>

    <table class="ui celled table">
      <thead>
        <tr>
          <th>Key</th>
          <th>Value</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="config in configs">
          <td>{{ config.name }}</td>
          <td>{{ config.value }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
<style>
</style>
<script>
import configService from '@/services/ConfigService'

export default {
  name: 'ConfigListView',
  data () {
    return {
      loading: false,
      error: '',
      configs: []
    }
  },
  created () {
    this.loadData()
  },
  methods: {
    loadData: function () {
      this.error = this.configs = null
      this.loading = true
      configService.getConfigs((success, data) => {
        this.loading = false
        if (success) {
          this.configs = data._embedded.configs
        } else {
          this.error = data.message || 'Bad Request'
        }
      })
    }
  }
}
</script>
