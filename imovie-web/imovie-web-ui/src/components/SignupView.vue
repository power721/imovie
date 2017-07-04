<template>
  <div class="ui middle aligned center aligned grid" id="signup">
    <div class="column">
      <h2 class="ui teal image header">
        <div class="content">
          Create a new account
        </div>
      </h2>
      <form id='signup-form' class="ui large form">
        <div class="ui stacked segment">
          <div class="field">
            <div class="ui left icon input">
              <i class="mail icon"></i>
              <input type="text" name="email" v-model="user.email" placeholder="Email">
            </div>
          </div>
          <div class="field">
            <div class="ui left icon input">
              <i class="user icon"></i>
              <input type="text" name="username" v-model="user.username" placeholder="Username">
            </div>
          </div>
          <div class="field">
            <div class="ui left icon input">
              <i class="lock icon"></i>
              <input type="password" name="password" v-model="user.password" placeholder="Password">
            </div>
          </div>
          <div class="field">
            <div class="ui left icon input">
              <i class="lock icon"></i>
              <input type="password" name="confirmPassword" v-model="user.confirmPassword" placeholder="Confirm Password">
            </div>
          </div>
          <div class="ui fluid large teal submit button">{{ $t("token.signup") }}</div>
        </div>
        <div class="ui error message">
        </div>
      </form>

      <div class="ui horizontal divider">
        Or
      </div>
      <div class="ui message">
        Have an account? <router-link to="/login">{{ $t("token.login") }}</router-link>
      </div>
    </div>
  </div>
</template>
<style>
.column {
  max-width: 480px;
}
#signup {
  margin-top: 200px;
}
</style>
<script>
import userService from '@/services/UserService'
import $ from 'jquery'

export default {
  name: 'sign-up',
  data () {
    return {
      user: {
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      }
    }
  },
  mounted () {
    $('#signup-form')
      .form({
        fields: {
          email: {
            rules: [{
              type: 'email'
            }]
          },
          username: {
            rules: [
              {
                type: 'minLength[5]'
              },
              {
                type: 'empty'
              }
            ]
          },
          password: {
            rules: [
              {
                type: 'minLength[6]'
              },
              {
                type: 'regExp[/^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9]).{6,}$/]',
                prompt: 'Password must contain 1 letter, 1 digital, 1 special char'
              }
            ]
          },
          confirmPassword: {
            rules: [{
              type: 'match[password]'
            }]
          }
        },
        onSuccess: this.signup
      })
  },
  methods: {
    signup () {
      userService.signup(this.user, (success, data) => {
        if (success) {
          this.$router.push('/login')
        } else {
          var errors = {}
          data.errors.forEach(entry => {
            errors[entry.field] = entry.field + ' ' + entry.error
            $('#signup-form').form('add prompt', entry.field)
          })
          $('#signup-form').form('add errors', errors)
        }
      })

      return false
    }
  }
}
</script>
