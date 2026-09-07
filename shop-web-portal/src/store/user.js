import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    member: JSON.parse(localStorage.getItem('member') || 'null')
  }),
  actions: {
    setLogin(token, member) {
      this.token = token
      this.member = member
      localStorage.setItem('token', token)
      localStorage.setItem('member', JSON.stringify(member))
    },
    logout() {
      this.token = ''
      this.member = null
      localStorage.removeItem('token')
      localStorage.removeItem('member')
    }
  }
})
