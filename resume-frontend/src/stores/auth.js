import { reactive } from 'vue'

export const useAuthStore = (() => {
  let instance
  return () => {
    if (!instance) {
      instance = reactive({
        token: localStorage.getItem('token') || '',
        username: localStorage.getItem('username') || '',
        login(token, username) {
          this.token = token
          this.username = username
          localStorage.setItem('token', token)
          localStorage.setItem('username', username)
        },
        logout() {
          this.token = ''
          this.username = ''
          localStorage.clear()
        }
      })
    }
    return instance
  }
})()
