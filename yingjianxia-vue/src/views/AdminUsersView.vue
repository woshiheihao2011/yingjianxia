<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  RefreshCw, Users, Search, ChevronLeft, ChevronRight,
  CheckCircle2, XCircle, AlertCircle, Shield, ShieldCheck, Store
} from 'lucide-vue-next'
import request from '@/api/request'

const loading = ref(false)
const keyword = ref('')
const users = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

// 统计卡片（基于当前页数据）
const stats = computed(() => {
  const all = users.value
  return {
    total: total.value,
    buyers: all.filter(u => (u.roles || []).includes('BUYER')).length,
    sellers: all.filter(u => (u.roles || []).includes('SELLER')).length,
    auditors: all.filter(u => (u.roles || []).includes('AUDITOR')).length
  }
})

// 调用管理端用户列表 API
async function getAdminUsersApi(params) {
  return request.get('/admin/users', { params })
}

async function loadUsers() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await getAdminUsersApi(params)
    users.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (e) {
    console.error('加载用户列表失败', e)
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function roleLabel(roles) {
  if (!roles || !roles.length) return '普通用户'
  const labels = []
  if (roles.includes('BUYER')) labels.push('买家')
  if (roles.includes('SELLER')) labels.push('卖家')
  if (roles.includes('AUDITOR')) labels.push('审核员')
  if (roles.includes('ADMIN')) labels.push('管理员')
  return labels.join(' / ')
}

function onSearch() {
  pageNum.value = 1
  loadUsers()
}

function changePage(p) {
  if (p < 1 || p > totalPages.value || p === pageNum.value) return
  pageNum.value = p
  loadUsers()
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="container-app space-y-6">
    <!-- 标题 -->
    <div>
      <h1 class="text-2xl md:text-3xl font-bold text-brand-ink flex items-center gap-3">
        <Users class="w-7 h-7 text-brand-primary" />用户管理
      </h1>
      <p class="text-sm text-brand-ink-3 mt-1">查看和管理平台用户</p>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <div class="rounded-xl bg-brand-surface border border-brand-line p-4 text-center">
        <div class="text-2xl font-bold text-brand-ink">{{ stats.total }}</div>
        <div class="text-xs text-brand-ink-3 mt-1">总用户数</div>
      </div>
      <div class="rounded-xl bg-brand-surface border border-brand-line p-4 text-center">
        <div class="text-2xl font-bold text-brand-info">{{ stats.buyers }}</div>
        <div class="text-xs text-brand-ink-3 mt-1">买家</div>
      </div>
      <div class="rounded-xl bg-brand-surface border border-brand-line p-4 text-center">
        <div class="text-2xl font-bold text-brand-success">{{ stats.sellers }}</div>
        <div class="text-xs text-brand-ink-3 mt-1">卖家</div>
      </div>
      <div class="rounded-xl bg-brand-surface border border-brand-line p-4 text-center">
        <div class="text-2xl font-bold text-brand-warning">{{ stats.auditors }}</div>
        <div class="text-xs text-brand-ink-3 mt-1">审核员</div>
      </div>
    </div>

    <!-- 搜索条 -->
    <div class="rounded-xl bg-brand-surface border border-brand-line p-4 flex flex-wrap items-center gap-3">
      <div class="flex-1 min-w-[200px] relative">
        <Search class="w-4 h-4 text-brand-ink-3 absolute left-3 top-1/2 -translate-y-1/2" />
        <input v-model="keyword" type="text" placeholder="搜索用户昵称、手机号..."
          class="w-full h-10 pl-10 pr-4 rounded-lg bg-brand-bg border border-brand-line text-sm text-brand-ink focus:outline-none focus:border-brand-primary"
          @keyup.enter="onSearch" />
      </div>
      <button @click="loadUsers" class="px-4 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
        <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />刷新
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="rounded-2xl bg-brand-surface border border-brand-line p-16 text-center">
      <div class="w-10 h-10 mx-auto rounded-full border-2 border-brand-primary border-t-transparent animate-spin"></div>
      <p class="text-sm text-brand-ink-3 mt-4">加载中...</p>
    </div>

    <!-- 空列表 -->
    <div v-else-if="users.length === 0" class="rounded-2xl bg-brand-surface border border-brand-line p-16 text-center">
      <div class="w-16 h-16 mx-auto mb-4 rounded-xl bg-brand-surface-2 flex items-center justify-center">
        <AlertCircle class="w-8 h-8 text-brand-ink-3" />
      </div>
      <p class="text-sm text-brand-ink-3">暂无用户数据</p>
    </div>

    <!-- 用户列表 -->
    <div v-else class="space-y-3">
      <div v-for="u in users" :key="u.id" class="rounded-xl bg-brand-surface border border-brand-line p-4 hover:border-brand-primary/30 transition">
        <div class="flex items-center gap-4">
          <!-- 头像 -->
          <div class="w-12 h-12 rounded-full bg-brand-primary/15 flex items-center justify-center text-brand-primary text-lg font-bold shrink-0">
            <img v-if="u.avatar" :src="u.avatar" class="w-full h-full rounded-full object-cover" :alt="u.nickname" />
            <span v-else>{{ (u.nickname || '?').slice(0, 1) }}</span>
          </div>
          <!-- 信息 -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="text-sm font-semibold text-brand-ink">{{ u.nickname }}</span>
              <span class="text-xs text-brand-ink-3 font-mono">{{ u.phone }}</span>
              <span v-if="u.roles?.includes('SELLER')" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-semibold text-brand-success bg-brand-success/15">
                <Store class="w-3 h-3" />卖家
              </span>
              <span v-if="u.roles?.includes('AUDITOR')" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-semibold text-brand-warning bg-brand-warning/15">
                <ShieldCheck class="w-3 h-3" />审核员
              </span>
              <span v-if="u.roles?.includes('BUYER')" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-semibold text-brand-info bg-brand-info/15">
                <Shield class="w-3 h-3" />买家
              </span>
            </div>
            <div class="text-xs text-brand-ink-3 mt-1 flex items-center gap-3">
              <span>ID: {{ u.id }}</span>
              <span v-if="u.creditScore">信用分: {{ u.creditScore }}</span>
              <span v-if="u.createdAt">注册: {{ String(u.createdAt).replace('T', ' ').slice(0, 10) }}</span>
            </div>
          </div>
          <!-- 状态 -->
          <div class="shrink-0">
            <span v-if="u.status === 1" class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-semibold text-brand-success bg-brand-success/15 border border-brand-success/30">
              <CheckCircle2 class="w-3.5 h-3.5" />正常
            </span>
            <span v-else-if="u.status === 0" class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-semibold text-brand-warning bg-brand-warning/15 border border-brand-warning/30">
              <AlertCircle class="w-3.5 h-3.5" />待激活
            </span>
            <span v-else class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-semibold text-brand-error bg-brand-error/15 border border-brand-error/30">
              <XCircle class="w-3.5 h-3.5" />已封禁
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 提示 -->
    <div v-if="!loading && users.length > 0" class="rounded-xl bg-brand-info/10 border border-brand-info/30 p-4 text-center">
      <p class="text-xs text-brand-ink-3">
        <Shield class="w-3.5 h-3.5 inline mr-1" />
        共 {{ total }} 位用户，当前第 {{ pageNum }} 页 / 共 {{ totalPages }} 页
      </p>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-1 pb-4">
      <button @click="changePage(1)" :disabled="pageNum <= 1" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition flex items-center justify-center">
        <ChevronLeft class="w-4 h-4" /><ChevronLeft class="w-4 h-4 -ml-2" />
      </button>
      <button @click="changePage(pageNum - 1)" :disabled="pageNum <= 1" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition flex items-center justify-center">
        <ChevronLeft class="w-4 h-4" />
      </button>
      <button v-for="n in totalPages" :key="n" @click="changePage(n)" v-show="n >= pageNum - 2 && n <= pageNum + 2"
        :class="['w-9 h-9 rounded-lg text-sm font-medium transition',
                 n === pageNum ? 'bg-brand-primary text-brand-primary-ink border border-brand-primary' : 'border border-brand-line text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40']">
        {{ n }}
      </button>
      <button @click="changePage(pageNum + 1)" :disabled="pageNum >= totalPages" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition flex items-center justify-center">
        <ChevronRight class="w-4 h-4" />
      </button>
      <button @click="changePage(totalPages)" :disabled="pageNum >= totalPages" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition flex items-center justify-center">
        <ChevronRight class="w-4 h-4" /><ChevronRight class="w-4 h-4 -ml-2" />
      </button>
    </div>
  </div>
</template>
