<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { categoryApi } from '@/api/category'
import type { Category, CategoryStatus } from '@/types/category'
import {
  CODE_PATTERN,
  canAddChild,
  deleteConfirmText,
  nextStatus,
  normalizeTree,
  parentChoices,
  payloadFromCategory,
  statusConfirmText,
  statusLabel,
  toPayload,
  validateCategoryForm,
  type CategoryFormValues,
} from '@/utils/category-form'

const loading = ref(false)
const submitting = ref(false)
const actionKey = ref('')
const loadError = ref('')
const tree = ref<Category[]>([])
const dialogVisible = ref(false)
const editingId = ref('')
const parentLocked = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<CategoryFormValues>({
  name: '',
  code: '',
  parentId: '0',
  iconUrl: '',
  description: '',
  sort: 1,
  status: 'ENABLED',
})

const rules: FormRules = {
  name: [
    { required: true, message: '分类名称不能为空', trigger: 'blur' },
    { min: 1, max: 64, message: '分类名称长度不能超过64', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '分类编码不能为空', trigger: 'blur' },
    { pattern: CODE_PATTERN, message: '分类编码必须是大写字母、数字或下划线', trigger: 'blur' },
  ],
  parentId: [{ required: true, message: '请选择父级分类', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const parentOptions = computed(() => parentChoices(tree.value, editingId.value))
const dialogTitle = computed(() => (editingId.value ? '编辑分类' : '新增分类'))
const busy = computed(() => loading.value || submitting.value || actionKey.value !== '')
const emptyText = computed(() => (loadError.value ? '加载失败' : '暂无分类'))

async function loadTree() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await categoryApi.getCategoryTree()
    tree.value = normalizeTree(response.data.data ?? [])
  } catch (error) {
    tree.value = []
    loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function resetForm(values: Partial<CategoryFormValues>) {
  form.name = values.name ?? ''
  form.code = values.code ?? ''
  form.parentId = values.parentId ?? '0'
  form.iconUrl = values.iconUrl ?? ''
  form.description = values.description ?? ''
  form.sort = values.sort ?? 1
  form.status = values.status ?? 'ENABLED'
}

async function openDialog() {
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

function openCreate() {
  editingId.value = ''
  parentLocked.value = false
  resetForm({ parentId: '0', sort: 1, status: 'ENABLED' })
  void openDialog()
}

function openCreateChild(row: Category) {
  editingId.value = ''
  parentLocked.value = true
  resetForm({ parentId: row.id, sort: 1, status: 'ENABLED' })
  void openDialog()
}

function openEdit(row: Category) {
  editingId.value = row.id
  parentLocked.value = Boolean(row.children?.length)
  resetForm({
    name: row.name,
    code: row.code,
    parentId: row.parentId,
    iconUrl: row.iconUrl ?? '',
    description: row.description ?? '',
    sort: row.sort,
    status: row.status,
  })
  void openDialog()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || submitting.value) {
    return
  }
  const message = validateCategoryForm(form)
  if (message) {
    ElMessage.warning(message)
    return
  }
  submitting.value = true
  try {
    const payload = toPayload(form)
    if (editingId.value) {
      await categoryApi.updateCategory(editingId.value, payload)
      ElMessage.success('已保存分类')
    } else {
      await categoryApi.createCategory(payload)
      ElMessage.success('已新增分类')
    }
    dialogVisible.value = false
    await loadTree()
  } catch {
    // 请求拦截器已展示后端错误
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: Category) {
  try {
    await ElMessageBox.confirm(deleteConfirmText(row.name), '删除分类', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  if (actionKey.value) {
    return
  }
  actionKey.value = `${row.id}:delete`
  try {
    await categoryApi.deleteCategory(row.id)
    ElMessage.success('已删除分类')
    await loadTree()
  } catch {
    // 请求拦截器已展示后端错误
  } finally {
    actionKey.value = ''
  }
}

async function onToggleStatus(row: Category) {
  const status: CategoryStatus = nextStatus(row.status)
  try {
    await ElMessageBox.confirm(statusConfirmText(row.name, status), status === 'ENABLED' ? '启用分类' : '停用分类', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  if (actionKey.value) {
    return
  }
  actionKey.value = `${row.id}:status`
  try {
    await categoryApi.updateCategory(row.id, payloadFromCategory(row, status))
    ElMessage.success(status === 'ENABLED' ? '已启用分类' : '已停用分类')
    await loadTree()
  } catch {
    // 请求拦截器已展示后端错误
  } finally {
    actionKey.value = ''
  }
}

void loadTree()
</script>

<template>
  <section class="category-page">
    <div class="toolbar">
      <el-button type="primary" :disabled="busy" @click="openCreate">新增分类</el-button>
    </div>
    <el-alert v-if="loadError" class="load-alert" type="error" :closable="false" show-icon :title="loadError">
      <el-button link type="primary" @click="loadTree">重新加载</el-button>
    </el-alert>
    <el-table
      v-loading="loading"
      :data="tree"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
      :empty-text="emptyText"
    >
      <el-table-column prop="name" label="分类名称" min-width="220" />
      <el-table-column prop="code" label="Code" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canAddChild(row)" link type="primary" :disabled="busy" @click="openCreateChild(row)">
            新增子分类
          </el-button>
          <el-button link type="primary" :disabled="busy" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :disabled="busy" @click="onDelete(row)">删除</el-button>
          <el-button link type="primary" :disabled="busy" @click="onToggleStatus(row)">
            {{ row.status === 'ENABLED' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="分类编码" prop="code">
          <el-input v-model="form.code" maxlength="64" />
        </el-form-item>
        <el-form-item label="父级分类" prop="parentId">
          <el-select v-model="form.parentId" :disabled="parentLocked" placeholder="请选择父级分类">
            <el-option v-for="item in parentOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="图标 URL" prop="iconUrl">
          <el-input v-model="form.iconUrl" maxlength="255" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" :step="1" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="submitting" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}

.load-alert {
  margin-bottom: 16px;
}
</style>
