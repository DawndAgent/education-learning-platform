<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createEditor, createToolbar, type IDomEditor, type IEditorConfig, type IToolbarConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import { fileApi } from '@/api/file'
import { validateImageFile } from '@/utils/file-upload'
import { bindImageFreeMove, registerWangEditorImagePosition } from '@/utils/wangeditor-image-position'

const model = defineModel<string>({ required: true })

const toolbarBox = ref<HTMLDivElement>()
const editorBox = ref<HTMLDivElement>()
const editorRef = shallowRef<IDomEditor>()
let unbindImageMove: (() => void) | undefined

const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['group-video', 'insertVideo', 'uploadVideo', 'fullScreen'],
}

onMounted(() => {
  if (!editorBox.value || !toolbarBox.value) {
    return
  }
  registerWangEditorImagePosition()
  const config: Partial<IEditorConfig> = {
    placeholder: '请输入正文',
    MENU_CONF: {
      uploadImage: {
        allowedFileTypes: ['image/*'],
        maxFileSize: 10 * 1024 * 1024,
        async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
          const message = validateImageFile(file)
          if (message) {
            ElMessage.error(message)
            return
          }
          try {
            const response = await fileApi.uploadImage(file, { scene: 'ARTICLE' })
            const url = response.data.data.url
            if (url.startsWith('data:')) {
              ElMessage.error('禁止使用 Base64 图片')
              return
            }
            insertFn(url, file.name, url)
          } catch {
            // 请求拦截器已用 ElMessage.error 展示后端错误
          }
        },
      },
    },
    onChange(editor) {
      model.value = editor.getHtml()
    },
  }
  const editor = createEditor({
    selector: editorBox.value,
    html: model.value || '<p><br></p>',
    config,
  })
  createToolbar({
    editor,
    selector: toolbarBox.value,
    config: toolbarConfig,
  })
  editorRef.value = editor
  unbindImageMove = bindImageFreeMove(editor)
})

watch(model, (value) => {
  const editor = editorRef.value
  if (!editor) {
    return
  }
  const next = value || '<p><br></p>'
  if (editor.getHtml() !== next) {
    editor.setHtml(next)
  }
})

onBeforeUnmount(() => {
  unbindImageMove?.()
  unbindImageMove = undefined
  editorRef.value?.destroy()
})

function insertHtml(html: string) {
  const editor = editorRef.value
  if (!editor || !html.trim()) {
    return
  }
  editor.focus()
  editor.dangerouslyInsertHtml(html)
}

defineExpose({ insertHtml })
</script>

<template>
  <div class="rich-editor">
    <div ref="toolbarBox" class="rich-toolbar" />
    <div ref="editorBox" class="rich-body" />
  </div>
</template>

<style scoped>
.rich-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.rich-body {
  min-height: 360px;
}

.rich-body :deep(.w-e-image-container) {
  cursor: grab;
  touch-action: none;
}

.rich-body :deep(.w-e-image-container:active) {
  cursor: grabbing;
}

.rich-body :deep(.w-e-image-container img) {
  -webkit-user-drag: none;
  user-select: none;
  pointer-events: none;
}
</style>
