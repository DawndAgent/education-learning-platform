import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    platformName: '教育内容管理平台',
    adminName: '管理员',
  }),
})
