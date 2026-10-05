<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  RefreshCw, Package, ChevronLeft, ChevronRight, Search,
  TrendingUp, Clock, CheckCircle2, XCircle, Truck, AlertCircle
} from 'lucide-vue-next'
import { getOrdersApi } from '@/api'

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const statusFilter = ref('')
const keyword = ref('')

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

// 统计卡片
const stats = computed(() => {
  const all = orders.value
  return {
    total: total.value,
    pending: all.filter(o => o.status === 0).length,
    paid: all.filter(o => o.status === 1).length,
    shipped: all.filter(o => o.status === 2).length,
    completed: all.filter(o => o.status === 3).length,
    cancelled: all.filter(o => o.status === 4).length
  }
})

const statusLabels = {
  0: { text: '待支付', color: 'text-brand-warning bg-brand-warning/15' },
  1: { text: '已支付', color: 'text-brand-info bg-brand-info/15' },
  2: { text: '已发货', color: 'text-brand-primary bg-brand-primary/15' },
  3: { text: '已完成', color: 'text-brand-success bg-brand-success/15' },
  4: { text: '已取消', color: 'text-brand-ink-3 bg-brand-surface-2' }
}

async function loadOrders() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (statusFilter.value !== '') params.status = statusFilter.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const res = await getOrdersApi(params)
    const data = res || {}
    orders.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (e) {
    console.error('加载订单列表失败', e)
    orders.value = []
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  if (p < 1 || p > totalPages.value || p === pageNum.value) return
  pageNum.value = p
  loadOrders()
}

function onFilterChange() {
  pageNum.value = 1
  loadOrders()
}

onMounted(() => {
  loadOrders()
})
</script>

<template>
  <div class="container-app space-y-6">
    <!-- 标题 -->
    <div>
      <h1 class="text-2xl md:text-3xl font-bold text-brand-ink flex items-center gap-3">
        <Package class="w-7 h-7 text-brand-primary" />订单管理
      </h1>
      <p class="text-sm text-brand-ink-3 mt-1">查看和管理平台所有订单</p>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
      <div class="rounded-xl bg-brand-surface border border-brand-line p-4 text-center">
        <div class="text-2xl font-bold text-brand-ink">{{ stats.total }}</div>
        <div class="text-xs text-brand-ink-3 mt-1">总订单数</div>
      </div>
      <div class="rounded-xl bg-brand-surface border border-brand-line p-4 text-center">
        <div class="text-2xl font-bold text-brand-warning">{{ stats.pending }}</div>
        <div class="text-xs text-brand-ink-3 mt-1">待支付</div>
      </div>
      <div class="rounded-xl bg-brand-surface border border-brand-line p-4 text-center">
        <div class="text-2xl font-bold text-brand-info">{{ stats.paid }}</div>
        <div class="text-xs text-brand-ink-3 mt-1">已支付</div>
      </div>
      <div class="rounded-xl bg-brand-surface border border-brand-line p-4 text-center">
        <div class="text-2xl font-bold text-brand-primary">{{ stats.shipped }}</div>
        <div class="text-xs text-brand-ink-3 mt-1">已发货</div>
      </div>
      <div class="rounded-xl bg-brand-surface border border-brand-line p-4 text-center">
        <div class="text-2xl font-bold text-brand-success">{{ stats.completed }}</div>
        <div class="text-xs text-brand-ink-3 mt-1">已完成</div>
      </div>
      <div class="rounded-xl bg-brand-surface border border-brand-line p-4 text-center">
        <div class="text-2xl font-bold text-brand-ink-3">{{ stats.cancelled }}</div>
        <div class="text-xs text-brand-ink-3 mt-1">已取消</div>
      </div>
    </div>

    <!-- 筛选条 -->
    <div class="rounded-xl bg-brand-surface border border-brand-line p-4 flex flex-wrap items-center gap-3">
      <div class="flex flex-wrap items-center gap-2">
        <button v-for="s in [
          { v: '', l: '全部' },
          { v: '0', l: '待支付' },
          { v: '1', l: '已支付' },
          { v: '2', l: '已发货' },
          { v: '3', l: '已完成' },
          { v: '4', l: '已取消' }
        ]" :key="s.v" @click="statusFilter = s.v; onFilterChange()"
          :class="['inline-flex items-center h-9 px-3.5 rounded-lg text-sm font-medium transition',
                   statusFilter === s.v ? 'bg-brand-primary text-brand-primary-ink' : 'bg-brand-bg border border-brand-line text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40']">
          {{ s.l }}
        </button>
      </div>
      <div class="flex-1 min-w-[200px] relative">
        <Search class="w-4 h-4 text-brand-ink-3 absolute left-3 top-1/2 -translate-y-1/2" />
        <input v-model="keyword" type="text" placeholder="搜索订单号、商品名称..."
          class="w-full h-10 pl-10 pr-4 rounded-lg bg-brand-bg border border-brand-line text-sm text-brand-ink focus:outline-none focus:border-brand-primary"
          @keyup.enter="onFilterChange" />
      </div>
      <button @click="loadOrders" class="px-4 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
        <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />刷新
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="rounded-2xl bg-brand-surface border border-brand-line p-16 text-center">
      <div class="w-10 h-10 mx-auto rounded-full border-2 border-brand-primary border-t-transparent animate-spin"></div>
      <p class="text-sm text-brand-ink-3 mt-4">加载中...</p>
    </div>

    <!-- 空列表 -->
    <div v-else-if="orders.length === 0" class="rounded-2xl bg-brand-surface border border-brand-line p-16 text-center">
      <div class="w-16 h-16 mx-auto mb-4 rounded-xl bg-brand-surface-2 flex items-center justify-center">
        <AlertCircle class="w-8 h-8 text-brand-ink-3" />
      </div>
      <p class="text-sm text-brand-ink-3">暂无订单数据</p>
    </div>

    <!-- 订单列表 -->
    <div v-else class="space-y-3">
      <div v-for="o in orders" :key="o.id" class="rounded-xl bg-brand-surface border border-brand-line p-4 hover:border-brand-primary/30 transition">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-3">
            <span class="text-sm font-mono text-brand-ink-2">{{ o.orderNo || o.id }}</span>
            <span :class="['inline-flex items-center px-2.5 py-0.5 rounded-md text-xs font-semibold', (statusLabels[o.status] || statusLabels[0]).color]">
              {{ (statusLabels[o.status] || statusLabels[0]).text }}
            </span>
          </div>
          <span class="text-xs text-brand-ink-3">{{ o.createdAt ? String(o.createdAt).replace('T', ' ').slice(0, 16) : '-' }}</span>
        </div>
        <div class="flex items-center gap-4">
          <div class="flex-1 min-w-0">
            <div v-if="o.items && o.items.length" class="flex gap-2">
              <span v-for="(it, i) in o.items.slice(0, 3)" :key="i" class="text-xs text-brand-ink-2 truncate">
                {{ it.productSnapshot?.title || it.title || '商品' }} ×{{ it.quantity }}
              </span>
              <span v-if="o.items.length > 3" class="text-xs text-brand-ink-3">等{{ o.items.length }}件</span>
            </div>
            <div v-else class="text-xs text-brand-ink-3">无商品明细</div>
          </div>
          <div class="text-right shrink-0">
            <div class="text-lg font-bold text-brand-error">¥{{ Number(o.totalAmount || o.amount || 0).toLocaleString() }}</div>
            <div class="text-[11px] text-brand-ink-3">{{ o.buyerName || '买家' }} → {{ o.sellerName || '卖家' }}</div>
          </div>
        </div>
      </div>
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
