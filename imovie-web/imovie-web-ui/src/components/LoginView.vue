<template>
  <div class="ui middle aligned center aligned grid" id="login">
    <div class="column">
      <h2 class="ui teal image header">
        <div class="content">
          Log-in to your account
        </div>
      </h2>
      <div class="ui warning message" v-if="$route.query.redirect">
        You need to login first.
      </div>
      <form id='login-form' class="ui large form">
        <div class="ui stacked segment">
          <div class="field">
            <div class="ui left icon input">
              <i class="user icon"></i>
              <input type="text" name="username" v-model="creds.username" placeholder="Username">
            </div>
          </div>
          <div class="field">
            <div class="ui left icon input">
              <i class="lock icon"></i>
              <input type="password" name="password" v-model="creds.password" placeholder="Password">
            </div>
          </div>
          <div class="ui fluid large teal submit button">Login</div>
        </div>
        <div class="ui error message">
        </div>
      </form>

      <div class="ui horizontal divider">
        Or
      </div>
      <div class="ui message">
        New to us? <router-link to="/signup">Signup</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import auth from '@/services/Auth'
import $ from 'jquery'

export default {
  name: 'login-view',
  data () {
    return {
      creds: {
        username: '',
        password: '',
        grant_type: 'password',
        scope: 'read'
      }
    }
  },
  mounted () {
    $('#login-form')
      .form({
        fields: {
          username: 'empty',
          password: 'minLength[6]'
        },
        onSuccess: this.login
      })
  },
  methods: {
    login () {
      auth.login(this.creds, success => {
        if (!success) {
          $('#login-form').form('add errors', {password: 'Invalid Credentials'})
          $('#login-form').form('add prompt', 'password')
          this.creds.password = ''
        } else {
          this.creds.username = ''
          this.creds.password = ''
          this.$router.replace(this.$route.query.redirect || '/')
        }
      })
      return false
    }
  }
}
</script>

<style>
.column {
  max-width: 465px;
}
#login {
  margin-top: 200px;
}
</style>
