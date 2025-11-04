<template>
  <div class="container">
    <h1>虚拟机管理</h1>
    <VmList />
  </div>
  
</template>

<script setup lang="ts">
import VmList from './components/VmList.vue'
</script>

<style>
.container { max-width: 1100px; margin: 20px auto; font-family: system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial; }
button { margin-right: 8px; }
table { width: 100%; border-collapse: collapse; }
th, td { border: 1px solid #ddd; padding: 8px; }
th { background: #f5f5f5; text-align: left; }
.actions { white-space: nowrap; }
input, select { padding: 6px; }
</style>


