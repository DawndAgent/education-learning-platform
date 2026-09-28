import { onBeforeUnmount, onMounted, type Ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessageBox } from 'element-plus'

export function useUnsavedLeave(dirty: Ref<boolean>) {
  function onBeforeUnload(event: BeforeUnloadEvent) {
    if (!dirty.value) {
      return
    }
    event.preventDefault()
    event.returnValue = ''
  }

  onMounted(() => {
    window.addEventListener('beforeunload', onBeforeUnload)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', onBeforeUnload)
  })

  onBeforeRouteLeave(async () => {
    if (!dirty.value) {
      return true
    }
    try {
      await ElMessageBox.confirm('当前内容尚未保存，确定离开吗？', '离开页面', {
        type: 'warning',
        confirmButtonText: '离开',
        cancelButtonText: '留在此页',
      })
      return true
    } catch {
      return false
    }
  })
}
