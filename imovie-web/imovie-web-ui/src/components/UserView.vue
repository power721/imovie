<template>
  <div class="ui middle center aligned grid" id="editUser">
    <div class="column">
      <h2 class="ui teal image header">
        <div class="content">
          Update account
        </div>
      </h2>
      <form id='edit-form' class="ui large form">
        <div class="ui stacked segment">
          <div class="field">
            <div class="ui left icon input">
              <i class="user icon"></i>
              <input type="text" name="username" v-model="user.username" placeholder="Username" readonly>
            </div>
          </div>
          <div class="field">
            <div class="ui left icon input">
              <i class="mail icon"></i>
              <input type="text" name="email" v-model="user.email" placeholder="Email">
            </div>
          </div>
          <div class="field">
            <div class="ui left icon input">
              <i class="lock icon"></i>
              <input type="password" name="oldPassword" v-model="user.oldPassword" placeholder="Old Password">
            </div>
          </div>
          <div class="field">
            <div class="ui left icon input">
              <i class="lock icon"></i>
              <input type="password" name="password" v-model="user.password" placeholder="New Password">
            </div>
          </div>
          <div class="field">
            <div class="ui left icon input">
              <i class="lock icon"></i>
              <input type="password" name="confirmPassword" v-model="user.confirmPassword" placeholder="Confirm New Password">
            </div>
          </div>
          <div class="ui fluid large teal submit button">{{ $t("token.update") }}</div>
        </div>
        <div class="ui error message">
        </div>
      </form>
    </div>
  </div>
</template>
<style>
.column {
  max-width: 480px;
}
#editUser {
  margin-top: 200px;
}
</style>
<script>
import auth from '@/services/Auth'
import userService from '@/services/UserService'
import $ from 'jquery'

export default {
  name: 'UserView',
  data () {
    return {
      loading: false,
      error: '',
      user: {
        username: '',
        email: '',
        oldPassword: '',
        password: '',
        confirmPassword: ''
      }
    }
  },
  mounted () {
    this.loadUser()
    $('#edit-form')
      .form({
        fields: {
          email: {
            rules: [{
              type: 'email'
            }]
          },
          oldPassword: {
            rules: [
              {
                type: 'minLength[8]'
              },
              {
                type: 'regExp[/^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9]).{8,}$/]',
                prompt: 'Password must contain 1 letter, 1 digital, 1 special char'
              }
            ]
          },
          password: {
            optional: true,
            rules: [
              {
                type: 'minLength[8]'
              },
              {
                type: 'regExp[/^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9]).{8,}$/]',
                prompt: 'Password must contain 1 letter, 1 digital, 1 special char'
              }
            ]
          },
          confirmPassword: {
            optional: true,
            rules: [{
              type: 'match[password]'
            }]
          }
        },
        onSuccess: this.update
      })
  },
  methods: {
    loadUser () {
      this.user.username = auth.user.name
      this.error = this.configs = null
      this.loading = true
      userService.getUser(this.user.username, (success, data) => {
        this.loading = false
        if (success) {
          this.user.email = data.email
        } else {
          this.error = data.message || 'Bad Request'
        }
      })
    },
    update () {
      userService.update(this.user, (success, data) => {
        if (success) {
          this.$router.push('/')
        } else {
          var errors = {}
          data.errors.forEach(entry => {
            var text = entry.error || entry.defaultMessage
            if (entry.field && !text.includes(entry.field)) {
              text = entry.field + ' ' + text
            }
            errors[entry.field] = text
            $('#edit-form').form('add prompt', entry.field)
          })
          $('#edit-form').form('add errors', errors)
        }
      })

      return false
    }
  }
}
</script>
