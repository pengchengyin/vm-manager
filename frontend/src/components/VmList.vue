<template>
  <div>
    <div style="margin-bottom:12px;display:flex;gap:8px;align-items:center;">
      <button @click="load">刷新</button>
      <button @click="openCreateVm" style="background-color:#4CAF50;color:white;">创建虚拟机</button>
      <label>远程访问IP：<input v-model="targetIp" placeholder="例如 192.168.1.100" /></label>
    </div>

    <table>
      <thead>
        <tr>
          <th>名称</th>
          <th>状态</th>
          <th>CPU</th>
          <th>内存(MiB)</th>
          <th>VNC</th>
          <th class="actions">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="vm in vms" :key="vm.uuid">
          <td>{{ vm.name }}</td>
          <td>{{ vm.statusDescription }}</td>
          <td>{{ vm.cpuCount }}</td>
          <td>{{ (vm.currentMemory/1024/1024).toFixed(0) }}</td>
          <td>
            <span v-if="vm.vncPort !== null && vm.vncPort !== undefined">{{ vm.vncHost || '0.0.0.0' }}:{{ vm.vncPort }}</span>
            <span v-else>—</span>
          </td>
          <td class="actions">
            <button @click="start(vm.name)">启动</button>
            <button @click="shutdown(vm.name)">关机</button>
            <button @click="reboot(vm.name)">重启</button>
            <button @click="forceShutdown(vm.name)">强制关机</button>
            <button @click="remove(vm.name)">删除</button>
            <button @click="openVNC(vm)">VNC</button>
            <button @click="openRDP()">RDP</button>
            <button @click="openSSH()">SSH</button>
            <button @click="openChangePwd(vm.name)">改密</button>
          </td>
        </tr>
      </tbody>
    </table>

    <dialog ref="pwdDlg">
      <form @submit.prevent="submitPwd">
        <h3>修改密码 - {{ pwdVmName }}</h3>
        <div style="margin:6px 0;">
          <label>用户名：<input v-model="pwdForm.username" required /></label>
        </div>
        <div style="margin:6px 0;">
          <label>新密码：<input type="password" v-model="pwdForm.password" required /></label>
        </div>
        <div style="margin:6px 0;">
          <label><input type="checkbox" v-model="pwdForm.encrypted" /> 已加密</label>
        </div>
        <div style="margin-top:10px;">
          <button type="submit">提交</button>
          <button type="button" @click="closePwd">取消</button>
        </div>
        <p style="color:#888;margin-top:8px;">提示：需要来宾系统安装并运行 qemu-guest-agent。</p>
      </form>
    </dialog>

    <dialog ref="createVmDlg">
      <form @submit.prevent="submitCreateVm">
        <h3>创建虚拟机</h3>
        <div style="margin:6px 0;">
          <label>虚拟机名称：<input v-model="createVmForm.name" required placeholder="输入唯一名称" /></label>
        </div>
        <div style="margin:6px 0;">
          <label>内存大小(MB)：<input type="number" v-model.number="createVmForm.memoryMB" required min="512" step="512" /></label>
        </div>
        <div style="margin:6px 0;">
          <label>CPU核心数：<input type="number" v-model.number="createVmForm.cpuCount" required min="1" max="8" /></label>
        </div>
        <div style="margin:6px 0;">
          <label>磁盘镜像路径：<input v-model="createVmForm.diskImagePath" required placeholder="例如 /var/lib/libvirt/images/vm.qcow2" /></label>
        </div>
        <div style="margin:6px 0;">
          <label>网络名称：<input v-model="createVmForm.networkName" required placeholder="例如 default" /></label>
        </div>
        <div style="margin-top:10px;">
          <button type="submit">创建</button>
          <button type="button" @click="closeCreateVm">取消</button>
        </div>
        <p style="color:#888;margin-top:8px;">提示：请确保磁盘镜像文件存在且网络名称有效。</p>
      </form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

type VmInfo = {
  name: string
  uuid: string
  status: string
  statusDescription: string
  currentMemory: number
  cpuCount: number
  vncHost?: string | null
  vncPort?: number | null
}

const vms = ref<VmInfo[]>([])
const targetIp = ref('')

async function load() {
  const { data } = await axios.get('/api/v1/vms')
  vms.value = data.data || []
}

async function start(name: string) { await axios.post(`/api/v1/vms/${name}/start`); await load() }
async function shutdown(name: string) { await axios.post(`/api/v1/vms/${name}/shutdown`); await load() }
async function reboot(name: string) { await axios.post(`/api/v1/vms/${name}/reboot`); await load() }
async function forceShutdown(name: string) { await axios.post(`/api/v1/vms/${name}/force-shutdown`); await load() }
async function remove(name: string) { await axios.delete(`/api/v1/vms/${name}`); await load() }

function openVNC(vm: VmInfo) {
  // 简化：引导用户使用外部VNC客户端；如果后端提供 web VNC，可替换为 noVNC 页面
  if (vm.vncPort != null) {
    const host = vm.vncHost && vm.vncHost !== '0.0.0.0' ? vm.vncHost : location.hostname
    alert(`请使用VNC客户端连接 ${host}:${vm.vncPort}`)
  } else {
    alert('未检测到VNC端口（虚拟机需启动且启用VNC图形）')
  }
}

function openRDP() {
  if (!targetIp.value) return alert('请先填写远程访问IP')
  // RDP 协议链接（Windows 客户端会识别），其他平台请使用相应客户端
  location.href = `rdp://full%20address=s:${targetIp.value}`
}

function openSSH() {
  if (!targetIp.value) return alert('请先填写远程访问IP')
  location.href = `ssh://${targetIp.value}`
}

const pwdDlg = ref<HTMLDialogElement | null>(null)
const pwdVmName = ref('')
const pwdForm = ref({ username: 'root', password: '', encrypted: false })

// 创建虚拟机相关
const createVmDlg = ref<HTMLDialogElement | null>(null)
const createVmForm = ref({
  name: '',
  memoryMB: 2048,
  cpuCount: 2,
  diskImagePath: '',
  networkName: 'default'
})

function openChangePwd(name: string) {
  pwdVmName.value = name
  pwdForm.value = { username: 'root', password: '', encrypted: false }
  pwdDlg.value?.showModal()
}

function closePwd() { pwdDlg.value?.close() }

async function submitPwd() {
  await axios.post(`/api/v1/vms/${pwdVmName.value}/password`, pwdForm.value)
  alert('已发送修改密码指令')
  closePwd()
}

// 创建虚拟机函数
function openCreateVm() {
  createVmForm.value = {
    name: '',
    memoryMB: 2048,
    cpuCount: 2,
    diskImagePath: '',
    networkName: 'default'
  }
  createVmDlg.value?.showModal()
}

function closeCreateVm() { createVmDlg.value?.close() }

async function submitCreateVm() {
  try {
    const { data } = await axios.post('/api/v1/vms', createVmForm.value)
    if (data.success) {
      alert('虚拟机创建成功')
      closeCreateVm()
      await load() // 刷新虚拟机列表
    } else {
      alert(`创建失败: ${data.message}`)
    }
  } catch (error: any) {
    alert(`创建失败: ${error.response?.data?.message || error.message}`)
  }
}

onMounted(load)
</script>


