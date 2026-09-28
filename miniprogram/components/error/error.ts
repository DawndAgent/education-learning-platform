Component({
  properties: {
    message: {
      type: String,
      value: '加载失败'
    },
    buttonText: {
      type: String,
      value: '重试'
    }
  },
  methods: {
    onRetry() {
      this.triggerEvent('retry')
    }
  }
})
