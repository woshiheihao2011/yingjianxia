<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowDownLeft, ArrowUpRight, Wallet, ChevronDown, ChevronLeft, ChevronRight,
  Download, Search, RotateCcw, Receipt, Plus, Banknote, RotateCcw as RotateBack
} from 'lucide-vue-next'

const router = useRouter()

// 交易类型筛选 Tabs
const typeTabs = [
  { id: 'all', name: '全部' },
  { id: 'income', name: '收入' },
  { id: 'expense', name: '支出' },
  { id: 'refund', name: '退款' },
  { id: 'deposit', name: '充值' },
  { id: 'withdraw', name: '提现' }
]
const activeType = ref('all')

// 时间与排序
const timeRanges = ['近 7 天', '近 30 天', '近 90 天', '自定义']
const activeTimeRange = ref('近 30 天')
const sortOptions = ['按时间倒序', '按时间正序', '按金额排序']
const activeSort = ref('按时间倒序')

// 搜索关键字
const keyword = ref('')

// 分页
const currentPage = ref(1)
const pageSize = 5
const totalPages = 8

// 汇总统计
const summary = {
  monthIncome: 5420.00,
  monthExpense: 1240.00,
  totalIncome: 42680.00
}
const fmt = (n) => '¥' + Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const fmtNum = (n) => Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

// 交易明细数据
const allTransactions = ref([
  {
    id: 'TX2026082800001', type: 'income', title: '售出：七彩虹 RTX 4070 战斧豪华版',
    orderNo: 'ORD20260828001', amount: 3680.00, direction: 'in', balance: 12580.00,
    time: '2026-08-28 10:23', fullTime: '2026-08-28 10:23:17', status: 'success',
    details: [
      { label: '交易类型', value: '销售收入' },
      { label: '买家', value: '显卡发烧友' },
      { label: '订单号', value: 'ORD20260828001', mono: true },
      { label: '交易时间', value: '2026-08-28 10:23:17' },
      { label: '交易金额', value: '+¥3,680.00', strong: true, success: true },
      { label: '手续费', value: '-¥36.80', mono: true }
    ]
  },
  {
    id: 'TX2026082700002', type: 'expense', title: '购买：Intel Core i5-13600K 盒装',
    orderNo: 'ORD20260827002', amount: 1250.00, direction: 'out', balance: 8900.00,
    time: '2026-08-27 18:45', fullTime: '2026-08-27 18:45:09', status: 'success',
    details: [
      { label: '交易类型', value: '购买支出' },
      { label: '卖家', value: '极客阿杰' },
      { label: '订单号', value: 'ORD20260827002', mono: true },
      { label: '交易时间', value: '2026-08-27 18:45:09' },
      { label: '交易金额', value: '-¥1,250.00', strong: true, error: true },
      { label: '支付方式', value: '余额支付' }
    ]
  },
  {
    id: 'TX2026082600003', type: 'refund', title: '退款：猫头鹰 NH-D15 双塔散热器',
    orderNo: 'ORD20260818005', amount: 480.00, direction: 'in', balance: 10150.00,
    time: '2026-08-26 14:10', fullTime: '2026-08-26 14:10:44', status: 'success',
    details: [
      { label: '交易类型', value: '售后退款' },
      { label: '关联售后单', value: 'AFT20260826012', mono: true },
      { label: '订单号', value: 'ORD20260818005', mono: true },
      { label: '退款原因', value: '成色与描述不符' },
      { label: '交易金额', value: '+¥480.00', strong: true, success: true }
    ]
  },
  {
    id: 'TX2026082500088', type: 'deposit', title: '账户充值',
    orderNo: 'TXN20260825088', amount: 2000.00, direction: 'in', balance: 10150.00,
    time: '2026-08-25 09:00', fullTime: '2026-08-25 09:00:22', status: 'success',
    details: [
      { label: '交易类型', value: '账户充值' },
      { label: '充值渠道', value: '支付宝' },
      { label: '流水号', value: 'TXN20260825088', mono: true },
      { label: '交易时间', value: '2026-08-25 09:00:22' },
      { label: '交易金额', value: '+¥2,000.00', strong: true, success: true }
    ]
  },
  {
    id: 'TX2026082400036', type: 'withdraw', title: '提现至银行卡',
    orderNo: 'TXN20260824036', amount: 1500.00, direction: 'out', balance: 8150.00,
    time: '2026-08-24 16:20', fullTime: '2026-08-24 16:20:51', status: 'processing',
    details: [
      { label: '交易类型', value: '余额提现' },
      { label: '到账账户', value: '建设银行（尾号 6688）' },
      { label: '流水号', value: 'TXN20260824036', mono: true },
      { label: '预计到账', value: '2026-08-25 23:59 前' },
      { label: '交易金额', value: '-¥1,500.00', strong: true, error: true }
    ]
  },
  {
    id: 'TX2026082000011', type: 'withdraw', title: '提现至银行卡',
    orderNo: 'TXN20260820011', amount: 3000.00, direction: 'out', balance: 9650.00,
    time: '2026-08-20 11:05', fullTime: '2026-08-20 11:05:02', status: 'failed',
    details: [
      { label: '交易类型', value: '余额提现' },
      { label: '失败原因', value: '银行卡信息有误', error: true },
      { label: '流水号', value: 'TXN20260820011', mono: true },
      { label: '交易时间', value: '2026-08-20 11:05:02' },
      { label: '交易金额', value: '-¥3,000.00', strong: true, error: true }
    ]
  }
])

// 展开详情控制
const expandedIds = ref(new Set())
const toggleExpand = (id) => {
  const set = new Set(expandedIds.value)
  if (set.has(id)) set.delete(id); else set.add(id)
  expandedIds.value = set
}

// 筛选后的交易列表
const filteredTx = computed(() => {
  let list = allTransactions.value
  if (activeType.value !== 'all') {
    list = list.filter(t => t.type === activeType.value)
  }
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(t =>
      t.title.toLowerCase().includes(kw) ||
      t.orderNo.toLowerCase().includes(kw) ||
      t.id.toLowerCase().includes(kw)
    )
  }
  // 排序
  if (activeSort.value === '按金额排序') {
    list = [...list].sort((a, b) => b.amount - a.amount)
  } else if (activeSort.value === '按时间正序') {
    list = [...list].sort((a, b) => a.fullTime.localeCompare(b.fullTime))
  }
  return list
})

// 图标 & 颜色分类
const iconForTx = (t) => {
  switch (t.type) {
    case 'income': return ArrowDownLeft
    case 'expense': return ArrowUpRight
    case 'refund': return RotateBack
    case 'deposit': return Plus
    case 'withdraw': return Banknote
    default: return Wallet
  }
}
const iconWrapClass = (t) => {
  const base = 'h-10 w-10 items-center justify-center rounded-lg inline-flex bg-brand-surface-2 flex-shrink-0'
  if (t.type === 'expense') return `${base} text-brand-error`
  if (t.type === 'refund') return `${base} text-brand-info`
  if (t.type === 'deposit' || t.type === 'income') return `${base} text-brand-success`
  if (t.type === 'withdraw') return `${base} text-brand-warning`
  return `${base} text-brand-primary`
}

const amountClass = (t) => {
  if (t.direction === 'in') return 'font-mono text-lg font-bold text-brand-success'
  return 'font-mono text-lg font-bold text-brand-error'
}
const signedAmount = (t) => (t.direction === 'in' ? '+' : '-') + fmtNum(t.amount)

const statusClass = (s) => {
  switch (s) {
    case 'success': return 'bg-brand-success/12 text-brand-success'
    case 'processing': return 'bg-brand-warning/12 text-brand-warning'
    case 'failed': return 'bg-brand-error/12 text-brand-error'
    default: return 'bg-brand-ink-2/12 text-brand-ink-2'
  }
}
const statusText = (s) => ({ success: '成功', processing: '处理中', failed: '失败' }[s] || s)

// 导出账单
const exportBill = () => {
  alert('账单导出已提交，稍后将通过站内信/邮箱发送下载链接')
}

// 重置筛选
const resetFilter = () => {
  activeType.value = 'all'
  keyword.value = ''
  activeTimeRange.value = '近 30 天'
  activeSort.value = '按时间倒序'
  currentPage.value = 1
}

// 分页按钮
const pages = computed(() => {
  const total = Math.min(totalPages, 8)
  const arr = []
  for (let i = 1; i <= total; i++) {
    if (i === 1 || i === total || (i >= currentPage.value - 1 && i <= currentPage.value + 1)) {
      arr.push(i)
    } else if (arr[arr.length - 1] !== '...') {
      arr.push('...')
    }
  }
  return arr
})
</script>

<template>
  <div class="container-app space-y-6 pb-20">
    <!-- 标题 -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
      <h1 class="text-2xl font-bold text-brand-ink">收支明细</h1>
      <button @click="exportBill"
        class="inline-flex items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 py-2 text-sm font-medium text-brand-primary-ink transition-colors hover:bg-brand-primary/90">
        <Download class="h-4 w-4" /> 导出账单
      </button>
    </div>

    <!-- 汇总卡片 -->
    <section class="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <div class="rounded-2xl border border-brand-line bg-brand-surface p-5 transition hover:border-brand-primary hover:shadow-lg">
        <div class="flex items-center gap-3">
          <span class="inline-flex h-10 w-10 items-center justify-center rounded-lg bg-brand-success/12 text-brand-success">
            <ArrowDownLeft class="h-5 w-5" />
          </span>
          <div>
            <p class="text-sm text-brand-ink-2">本月收入</p>
            <p class="mt-0.5 font-mono text-2xl font-bold text-brand-success">+{{ fmt(summary.monthIncome) }}</p>
          </div>
        </div>
      </div>
      <div class="rounded-2xl border border-brand-line bg-brand-surface p-5 transition hover:border-brand-primary hover:shadow-lg">
        <div class="flex items-center gap-3">
          <span class="inline-flex h-10 w-10 items-center justify-center rounded-lg bg-brand-error/12 text-brand-error">
            <ArrowUpRight class="h-5 w-5" />
          </span>
          <div>
            <p class="text-sm text-brand-ink-2">本月支出</p>
            <p class="mt-0.5 font-mono text-2xl font-bold text-brand-error">-{{ fmt(summary.monthExpense) }}</p>
          </div>
        </div>
      </div>
      <div class="rounded-2xl border border-brand-line bg-brand-surface p-5 transition hover:border-brand-primary hover:shadow-lg">
        <div class="flex items-center gap-3">
          <span class="inline-flex h-10 w-10 items-center justify-center rounded-lg bg-brand-primary/12 text-brand-primary">
            <Wallet class="h-5 w-5" />
          </span>
          <div>
            <p class="text-sm text-brand-ink-2">累计收入</p>
            <p class="mt-0.5 font-mono text-2xl font-bold text-brand-ink">{{ fmt(summary.totalIncome) }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 筛选栏 -->
    <section class="space-y-4 rounded-2xl border border-brand-line bg-brand-surface p-4 md:p-5">
      <!-- 交易类型 -->
      <div class="flex items-center gap-2 overflow-x-auto no-scrollbar">
        <button v-for="t in typeTabs" :key="t.id" @click="activeType = t.id"
          :class="['inline-flex items-center h-8.5 px-3.5 rounded-full text-[13px] font-medium whitespace-nowrap border transition',
                   activeType === t.id
                     ? 'bg-brand-primary/12 text-brand-primary border-brand-primary/25'
                     : 'bg-brand-surface-2 text-brand-ink-2 border-brand-line hover:bg-brand-surface-3 hover:text-brand-ink']">
          {{ t.name }}
        </button>
      </div>
      <!-- 搜索 / 时间 / 排序 -->
      <div class="flex flex-col gap-3 md:flex-row md:items-center">
        <div class="relative flex-1">
          <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-brand-ink-3" />
          <input v-model="keyword" type="search" placeholder="搜索交易名称、订单号..."
            class="h-10 w-full rounded-lg border border-brand-line bg-brand-bg pl-10 pr-4 text-sm text-brand-ink placeholder:text-brand-ink-3 outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
        </div>
        <div class="flex gap-3">
          <select v-model="activeTimeRange"
            class="h-10 rounded-lg border border-brand-line bg-brand-bg px-3 text-sm text-brand-ink outline-none focus:border-brand-primary">
            <option v-for="r in timeRanges" :key="r">{{ r }}</option>
          </select>
          <select v-model="activeSort"
            class="h-10 rounded-lg border border-brand-line bg-brand-bg px-3 text-sm text-brand-ink outline-none focus:border-brand-primary">
            <option v-for="s in sortOptions" :key="s">{{ s }}</option>
          </select>
        </div>
      </div>
    </section>

    <!-- 交易列表 -->
    <section v-if="filteredTx.length" class="space-y-3">
      <article v-for="tx in filteredTx" :key="tx.id"
        class="rounded-2xl border border-brand-line bg-brand-surface overflow-hidden transition hover:border-brand-primary hover:shadow-lg">
        <!-- 行头 -->
        <div @click="toggleExpand(tx.id)"
          role="button" tabindex="0"
          :aria-expanded="expandedIds.has(tx.id)"
          @keydown.enter="toggleExpand(tx.id)"
          @keydown.space.prevent="toggleExpand(tx.id)"
          class="flex items-center gap-4 p-4 md:p-5 cursor-pointer transition hover:bg-brand-surface/60">
          <div :class="iconWrapClass(tx)">
            <component :is="iconForTx(tx)" class="h-5 w-5" />
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
              <div class="min-w-0">
                <h3 class="font-medium text-brand-ink truncate">{{ tx.title }}</h3>
                <p class="mt-0.5 text-sm text-brand-ink-3 font-mono truncate">{{ tx.orderNo }}</p>
              </div>
              <div class="flex items-center gap-4 md:text-right">
                <div>
                  <p :class="amountClass(tx)">{{ signedAmount(tx) }}</p>
                  <p class="text-xs text-brand-ink-3">余额 {{ fmt(tx.balance) }}</p>
                </div>
              </div>
            </div>
            <div class="mt-2 flex flex-wrap items-center gap-3 text-sm text-brand-ink-2">
              <span>{{ tx.time }}</span>
              <span :class="['inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium', statusClass(tx.status)]">{{ statusText(tx.status) }}</span>
            </div>
          </div>
          <ChevronDown :class="['h-5 w-5 shrink-0 text-brand-ink-3 transition-transform', expandedIds.has(tx.id) && 'rotate-180']" />
        </div>

        <!-- 展开的详情 -->
        <Transition name="collapse">
          <div v-show="expandedIds.has(tx.id)" class="border-t border-brand-line bg-brand-surface-2 p-4 md:p-5">
            <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
              <div v-for="(d, i) in tx.details" :key="i">
                <p class="text-xs text-brand-ink-3">{{ d.label }}</p>
                <p :class="['mt-1 text-sm',
                  d.mono && 'font-mono',
                  d.strong && 'font-semibold',
                  d.success && 'text-brand-success',
                  d.error && 'text-brand-error',
                  !d.success && !d.error && 'text-brand-ink']">{{ d.value }}</p>
              </div>
            </div>
          </div>
        </Transition>
      </article>
    </section>

    <!-- 空状态 -->
    <section v-else class="flex flex-col items-center justify-center rounded-2xl border border-brand-line bg-brand-surface py-16 text-center">
      <Receipt class="h-16 w-16 text-brand-ink-3" />
      <h3 class="mt-4 text-lg font-medium text-brand-ink">暂无交易记录</h3>
      <p class="mt-1 text-sm text-brand-ink-2">当前筛选条件下没有资金流水，换个条件试试</p>
      <button @click="resetFilter"
        class="mt-5 inline-flex items-center gap-1.5 rounded-lg bg-brand-primary px-4 py-2 text-sm font-medium text-brand-primary-ink transition-colors hover:bg-brand-primary/90">
        <RotateCcw class="h-4 w-4" /> 重置筛选
      </button>
    </section>

    <!-- 分页与统计 -->
    <div class="mt-8 flex flex-col items-center justify-between gap-4 md:flex-row">
      <p class="text-sm text-brand-ink-2">
        共 <span class="font-mono font-semibold text-brand-ink">{{ filteredTx.length }}</span> 笔交易
      </p>
      <div class="flex items-center gap-2">
        <button @click="currentPage > 1 && currentPage--"
          :disabled="currentPage === 1"
          class="inline-flex items-center justify-center min-w-9 h-9 rounded-lg border border-brand-line bg-brand-surface text-brand-ink-2 text-sm transition hover:border-brand-primary hover:text-brand-primary disabled:opacity-40 disabled:cursor-not-allowed">
          <ChevronLeft class="h-4 w-4" />
        </button>
        <template v-for="(p, idx) in pages" :key="idx">
          <span v-if="p === '...'" class="px-1 text-brand-ink-3">...</span>
          <button v-else @click="currentPage = p"
            :class="['inline-flex items-center justify-center min-w-9 h-9 rounded-lg border text-sm transition',
                     currentPage === p ? 'bg-brand-primary border-brand-primary text-brand-primary-ink'
                                       : 'bg-brand-surface border-brand-line text-brand-ink-2 hover:border-brand-primary hover:text-brand-primary']">
            {{ p }}
          </button>
        </template>
        <button @click="currentPage < totalPages && currentPage++"
          :disabled="currentPage === totalPages"
          class="inline-flex items-center justify-center min-w-9 h-9 rounded-lg border border-brand-line bg-brand-surface text-brand-ink-2 text-sm transition hover:border-brand-primary hover:text-brand-primary disabled:opacity-40 disabled:cursor-not-allowed">
          <ChevronRight class="h-4 w-4" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@reference "../style.css";
.collapse-enter-active, .collapse-leave-active { transition: all 0.25s ease; overflow: hidden; }
.collapse-enter-from, .collapse-leave-to { max-height: 0; padding-top: 0; padding-bottom: 0; opacity: 0; }
.collapse-enter-to, .collapse-leave-from { max-height: 400px; opacity: 1; }
.h-8\.5 { height: 34px; }
</style>
