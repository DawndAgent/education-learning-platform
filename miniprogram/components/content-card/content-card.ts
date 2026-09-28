Component({
  properties: {
    title: {
      type: String,
      value: ''
    },
    coverUrl: {
      type: String,
      value: ''
    },
    typeLabel: {
      type: String,
      value: ''
    },
    meta: {
      type: String,
      value: ''
    },
    dateText: {
      type: String,
      value: ''
    },
    summary: {
      type: String,
      value: ''
    },
    url: {
      type: String,
      value: ''
    },
    layout: {
      type: String,
      value: 'row'
    }
  },

  data: {
    coverFailed: false
  },

  observers: {
    coverUrl() {
      this.setData({ coverFailed: false })
    }
  },

  methods: {
    onCoverError() {
      this.setData({ coverFailed: true })
    }
  }
})
