<script setup lang="ts">
defineProps<{
  title: string
}>()
</script>

<template>
  <el-card shadow="never">
    <el-empty :description="`${title}将在后续阶段实现`" />
  </el-card>
</template>
