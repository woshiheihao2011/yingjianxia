<script setup>
import { ref, computed, reactive } from 'vue'
import {
  Plus, Eye, Ticket, CheckCircle, TrendingUp, Layers, Zap, MinusCircle, Truck,
  Search, Pencil, Pause, BarChart2, Trash2, Copy, Play, CalendarRange, ChevronDown,
  Filter
} from 'lucide-vue-next'

// 活动类型标签
const promoTabs = [
  { key: 'all', label: '全部', icon: Layers, color: 'bg-primary text-primary-foreground' },
  { key: 'coupon', label: '优惠券', icon: Ticket, color: 'bg-primary/15 text-primary' },
  { key: 'flash', label: '限时折扣', icon: Zap, color: 'bg-amber-500/15 text-amber-400' },
  { key: 'threshold', label: '满减', icon: MinusCircle, color: 'bg-blue-500/15 text-blue-400' },
  { key: 'shipping', label: '包邮', icon: Truck, color: 'bg-emerald-500/15 text-emerald-400' },
]
const activeTab = ref('all')
const searchQuery = ref('')

// 滚动到创建区域
const createSectionRef = ref(null)
const scrollToCreate = () => {
  createSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 优惠券表单
const couponForm = reactive({
  name: '',
  amount: '',
  threshold: '0',
  total: '',
  limit: '1',
  scope: '全场通用',
  start: '',
  end: '',
})

const createCoupon = () => {
  if (!couponForm.name || !couponForm.amount || !couponForm.total || !couponForm.start || !couponForm.end) {
    alert('请完整填写优惠券信息')
    return
  }
  alert(`优惠券「${couponForm.name}」创建成功！`)
  Object.keys(couponForm).forEach((k) => {
    couponForm[k] = k === 'threshold' ? '0' : k === 'limit' ? '1' : k === 'scope' ? '全场通用' : ''
  })
}

// 限时折扣表单
const flashForm = reactive({
  product: '影驰 RTX 4070 星曜 OC',
  discount: '9 折',
  stock: '',
  start: '',
  end: '',
})
const originalPrice = 3299
const discountMap = { '9 折': 0.9, '8.5 折': 0.85, '8 折': 0.8, '7.5 折': 0.75, '7 折': 0.7 }
const discountedPrice = computed(() => Math.round(originalPrice * (discountMap[flashForm.discount] || 1)))

const createFlash = () => {
  if (!flashForm.stock || !flashForm.start || !flashForm.end) {
    alert('请完整填写限时折扣信息')
    return
  }
  alert(`限时折扣「${flashForm.product}」创建成功！`)
  flashForm.stock = ''
  flashForm.start = ''
  flashForm.end = ''
}

// 活动列表
const promoList = [
  {
    id: 1,
    name: '显卡满 2000 减 150',
    type: 'coupon',
    typeLabel: '优惠券',
    typeIcon: Ticket,
    typeColor: 'bg-primary/15 text-primary',
    createDate: '2026-08-20',
    period: '2026-08-25 至 2026-09-10',
    status: 'active',
    statusText: '进行中',
    statusColor: 'bg-emerald-500/15 text-emerald-400',
    data1: '领取 1,248',
    data2: '使用 356',
    amount: '¥42,380',
  },
  {
    id: 2,
    name: 'RTX 4070 限时 9 折',
    type: 'flash',
    typeLabel: '限时折扣',
    typeIcon: Zap,
    typeColor: 'bg-amber-500/15 text-amber-400',
    createDate: '2026-08-22',
    period: '2026-08-28 至 2026-08-30',
    status: 'active',
    statusText: '进行中',
    statusColor: 'bg-emerald-500/15 text-emerald-400',
    data1: '曝光 5,632',
    data2: '成交 28 单',
    amount: '¥28,910',
  },
  {
    id: 3,
    name: '全场满 500 减 50',
    type: 'threshold',
    typeLabel: '满减',
    typeIcon: MinusCircle,
    typeColor: 'bg-blue-500/15 text-blue-400',
    createDate: '2026-08-15',
    period: '2026-09-01 至 2026-09-07',
    status: 'upcoming',
    statusText: '未开始',
    statusColor: 'bg-blue-500/15 text-blue-400',
    data1: '领取 0',
    data2: '使用 0',
    amount: '-',
  },
  {
    id: 4,
    name: '主板品类包邮',
    type: 'shipping',
    typeLabel: '包邮',
    typeIcon: Truck,
    typeColor: 'bg-emerald-500/15 text-emerald-400',
    createDate: '2026-07-28',
    period: '2026-08-01 至 2026-08-15',
    status: 'ended',
    statusText: '已结束',
    statusColor: 'bg-zinc-500/15 text-zinc-400',
    data1: '领取 892',
    data2: '使用 412',
    amount: '¥15,130',
  },
  {
    id: 5,
    name: 'CPU 品类满 1000 减 100',
    type: 'coupon',
    typeLabel: '优惠券',
    typeIcon: Ticket,
    typeColor: 'bg-primary/15 text-primary',
    createDate: '2026-08-05',
    period: '2026-08-10 至 2026-08-20',
    status: 'ended',
    statusText: '已结束',
    statusColor: 'bg-zinc-500/15 text-zinc-400',
    data1: '领取 624',
    data2: '使用 198',
    amount: '¥18,560',
  },
]

// 过滤后的活动列表
const filteredList = computed(() => {
  let list = promoList
  if (activeTab.value !== 'all') {
    list = list.filter((p) => p.type === activeTab.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter((p) => p.name.toLowerCase().includes(q))
  }
  return list
})

// 暂停/恢复切换
const togglePause = (item) => {
  if (item.status === 'active') {
    item.status = 'paused'
    item.statusText = '已暂停'
    item.statusColor = 'bg-amber-500/15 text-amber-400'
  } else if (item.status === 'paused') {
    item.status = 'active'
    item.statusText = '进行中'
    item.statusColor = 'bg-emerald-500/15 text-emerald-400'
  }
}

const deletePromo = (item) => {
  if (confirm(`确认删除活动「${item.name}」吗？`)) {
    const idx = promoList.findIndex((p) => p.id === item.id)
    if (idx > -1) promoList.splice(idx, 1)
  }
}

const clonePromo = (item) => {
  const copy = { ...item, id: Date.now(), name: item.name + ' (副本)' }
  promoList.unshift(copy)
  alert(`已复制活动：${item.name}`)
}

// 活动效果指标
const metrics = [
  {
    label: '曝光量',
    value: '12,458',
    trend: '+18.2%',
    trendUp: true,
    icon: Eye,
    iconBg: 'bg-primary/15',
    iconColor: 'text-primary',
  },
  {
    label: '领取量',
    value: '3,206',
    trend: '+12.5%',
    trendUp: true,
    icon: Ticket,
    iconBg: 'bg-blue-500/15',
    iconColor: 'text-blue-400',
  },
  {
    label: '使用量',
    value: '892',
    trend: '+8.7%',
    trendUp: true,
    icon: CheckCircle,
    iconBg: 'bg-emerald-500/15',
    iconColor: 'text-emerald-400',
  },
  {
    label: '带动成交额',
    value: '¥86,420',
    trend: '+24.3%',
    trendUp: true,
    icon: TrendingUp,
    iconBg: 'bg-amber-500/15',
    iconColor: 'text-amber-400',
    valueColor: 'text-primary',
  },
]
</script>

<template>
  <div class="min-h-screen bg-background pb-20">
    <!-- 页面标题 -->
    <section class="container-app px-4 pt-6 md:px-6 md:pt-8">
      <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 class="text-2xl md:text-3xl font-bold text-foreground">营销活动</h1>
          <p class="mt-1 text-sm text-muted-foreground">创建优惠券、限时折扣等营销活动，提升商品曝光与转化</p>
        </div>
        <button
          type="button"
          @click="scrollToCreate"
          class="inline-flex items-center justify-center gap-2 h-10 px-5 rounded-lg bg-primary text-primary-foreground font-semibold transition hover:bg-primary/90 active:translate-y-px"
        >
          <Plus class="h-4 w-4" />
          创建活动
        </button>
      </div>
    </section>

    <!-- 活动效果数据 -->
    <section class="container-app px-4 pt-6 md:px-6 mt-2">
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div
          v-for="m in metrics"
          :key="m.label"
          class="rounded-2xl bg-card border border-border p-5 transition hover:border-primary/40"
        >
          <div class="flex items-center gap-3">
            <div
              :class="[
                'inline-flex h-10 w-10 items-center justify-center rounded-full',
                m.iconBg,
                m.iconColor,
              ]"
            >
              <component :is="m.icon" class="h-5 w-5" />
            </div>
            <div>
              <p class="text-xs text-muted-foreground">{{ m.label }}</p>
              <p :class="['font-mono text-xl font-bold', m.valueColor || 'text-foreground']">
                {{ m.value }}
              </p>
            </div>
          </div>
          <p :class="['mt-2 text-xs', m.trendUp ? 'text-emerald-400' : 'text-red-400']">
            {{ m.trend }} 较上周
          </p>
        </div>
      </div>
    </section>

    <!-- 活动类型标签 -->
    <section class="container-app px-4 pt-6 md:px-6 mt-2" aria-label="活动类型筛选">
      <div class="flex gap-2 overflow-x-auto no-scrollbar border-b border-border pb-3">
        <button
          v-for="tab in promoTabs"
          :key="tab.key"
          role="tab"
          :aria-selected="activeTab === tab.key"
          @click="activeTab = tab.key"
          :class="[
            'inline-flex items-center gap-1.5 h-9 px-4 rounded-lg text-sm font-medium whitespace-nowrap transition-all border',
            activeTab === tab.key
              ? 'bg-primary text-primary-foreground border-transparent'
              : 'bg-surface-2 text-muted-foreground border-border hover:text-foreground hover:bg-surface-3',
          ]"
        >
          <component :is="tab.icon" class="h-4 w-4" />
          {{ tab.label }}
        </button>
      </div>
    </section>

    <!-- 创建活动区域 -->
    <section ref="createSectionRef" class="container-app px-4 pt-6 md:px-6 mt-4">
      <h2 class="text-lg md:text-xl font-semibold text-foreground mb-4 flex items-center gap-2">
        <Plus class="w-5 h-5 text-primary" />
        创建新活动
      </h2>
      <div class="grid gap-5 lg:grid-cols-2">
        <!-- 创建优惠券 -->
        <div class="rounded-2xl bg-card border border-border p-5 md:p-6 transition focus-within:border-primary focus-within:shadow-xl">
          <div class="mb-5 flex items-center gap-3">
            <div class="inline-flex h-10 w-10 items-center justify-center rounded-full bg-primary/15 text-primary">
              <Ticket class="h-5 w-5" />
            </div>
            <div>
              <h3 class="font-semibold text-foreground">创建优惠券</h3>
              <p class="text-xs text-muted-foreground mt-0.5">吸引买家领取，提升下单转化</p>
            </div>
          </div>
          <form @submit.prevent="createCoupon" class="space-y-4">
            <div>
              <label class="block text-xs font-medium text-muted-foreground mb-1.5">券名称</label>
              <input
                v-model="couponForm.name"
                type="text"
                placeholder="如：显卡品类满减券"
                class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 placeholder:text-muted-foreground"
              />
            </div>
            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label class="block text-xs font-medium text-muted-foreground mb-1.5">面额（元）</label>
                <input
                  v-model="couponForm.amount"
                  type="number"
                  placeholder="50"
                  min="1"
                  class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 placeholder:text-muted-foreground"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-muted-foreground mb-1.5">使用门槛（元）</label>
                <input
                  v-model="couponForm.threshold"
                  type="number"
                  placeholder="满多少可用，0 为无门槛"
                  min="0"
                  class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 placeholder:text-muted-foreground"
                />
              </div>
            </div>
            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label class="block text-xs font-medium text-muted-foreground mb-1.5">发放总量</label>
                <input
                  v-model="couponForm.total"
                  type="number"
                  placeholder="1000"
                  min="1"
                  class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 placeholder:text-muted-foreground"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-muted-foreground mb-1.5">每人限领</label>
                <input
                  v-model="couponForm.limit"
                  type="number"
                  placeholder="1"
                  min="1"
                  class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 placeholder:text-muted-foreground"
                />
              </div>
            </div>
            <div>
              <label class="block text-xs font-medium text-muted-foreground mb-1.5">适用商品</label>
              <select
                v-model="couponForm.scope"
                class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 appearance-none bg-no-repeat bg-[right_12px_center]"
                style="background-image: url(&quot;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%236B7280' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E&quot;);"
              >
                <option>全场通用</option>
                <option>指定品类</option>
                <option>指定商品</option>
              </select>
            </div>
            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label class="block text-xs font-medium text-muted-foreground mb-1.5 flex items-center gap-1">
                  <CalendarRange class="w-3.5 h-3.5" />
                  开始时间
                </label>
                <input
                  v-model="couponForm.start"
                  type="datetime-local"
                  class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-muted-foreground mb-1.5 flex items-center gap-1">
                  <CalendarRange class="w-3.5 h-3.5" />
                  结束时间
                </label>
                <input
                  v-model="couponForm.end"
                  type="datetime-local"
                  class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>
            <button
              type="submit"
              class="inline-flex items-center justify-center gap-1.5 h-9 px-4 rounded-lg bg-primary text-primary-foreground text-sm font-semibold transition hover:bg-primary/90 w-full"
            >
              <Plus class="h-4 w-4" />
              创建优惠券
            </button>
          </form>
        </div>

        <!-- 创建限时折扣 -->
        <div class="rounded-2xl bg-card border border-border p-5 md:p-6 transition focus-within:border-primary focus-within:shadow-xl">
          <div class="mb-5 flex items-center gap-3">
            <div class="inline-flex h-10 w-10 items-center justify-center rounded-full bg-amber-500/15 text-amber-400">
              <Zap class="h-5 w-5" />
            </div>
            <div>
              <h3 class="font-semibold text-foreground">创建限时折扣</h3>
              <p class="text-xs text-muted-foreground mt-0.5">营造紧迫感，快速清库存</p>
            </div>
          </div>
          <form @submit.prevent="createFlash" class="space-y-4">
            <div>
              <label class="block text-xs font-medium text-muted-foreground mb-1.5">选择商品</label>
              <select
                v-model="flashForm.product"
                class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 appearance-none bg-no-repeat bg-[right_12px_center]"
                style="background-image: url(&quot;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%236B7280' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E&quot;);"
              >
                <option>影驰 RTX 4070 星曜 OC</option>
                <option>芝奇幻锋戟 DDR5 32GB 6000MHz</option>
                <option>Intel i5-13600K 散片</option>
                <option>华硕 TUF GAMING B760M 主板</option>
                <option>三星 980 Pro 1TB NVMe</option>
                <option>猫头鹰 NH-D15 双塔散热器</option>
              </select>
            </div>
            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label class="block text-xs font-medium text-muted-foreground mb-1.5">折扣率</label>
                <select
                  v-model="flashForm.discount"
                  class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 appearance-none bg-no-repeat bg-[right_12px_center]"
                  style="background-image: url(&quot;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%236B7280' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E&quot;);"
                >
                  <option>9 折</option>
                  <option>8.5 折</option>
                  <option>8 折</option>
                  <option>7.5 折</option>
                  <option>7 折</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-medium text-muted-foreground mb-1.5">活动库存</label>
                <input
                  v-model="flashForm.stock"
                  type="number"
                  placeholder="参与活动库存数量"
                  min="1"
                  class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 placeholder:text-muted-foreground"
                />
              </div>
            </div>
            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label class="block text-xs font-medium text-muted-foreground mb-1.5 flex items-center gap-1">
                  <CalendarRange class="w-3.5 h-3.5" />
                  开始时间
                </label>
                <input
                  v-model="flashForm.start"
                  type="datetime-local"
                  class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-muted-foreground mb-1.5 flex items-center gap-1">
                  <CalendarRange class="w-3.5 h-3.5" />
                  结束时间
                </label>
                <input
                  v-model="flashForm.end"
                  type="datetime-local"
                  class="w-full h-10 px-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>
            <div class="rounded-lg border border-border bg-background p-4">
              <div class="flex items-center justify-between text-sm">
                <span class="text-muted-foreground">原价</span>
                <span class="font-mono text-foreground">¥{{ originalPrice.toLocaleString() }}</span>
              </div>
              <div class="mt-2 flex items-center justify-between text-sm">
                <span class="text-muted-foreground">折后价</span>
                <span class="font-mono text-lg font-bold text-primary">¥{{ discountedPrice.toLocaleString() }}</span>
              </div>
            </div>
            <button
              type="submit"
              class="inline-flex items-center justify-center gap-1.5 h-9 px-4 rounded-lg bg-primary text-primary-foreground text-sm font-semibold transition hover:bg-primary/90 w-full"
            >
              <Plus class="h-4 w-4" />
              创建限时折扣
            </button>
          </form>
        </div>
      </div>
    </section>

    <!-- 活动列表 -->
    <section class="container-app px-4 py-8 md:px-6">
      <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 class="text-lg md:text-xl font-semibold text-foreground flex items-center gap-2">
          <Filter class="w-5 h-5 text-primary" />
          活动列表
        </h2>
        <div class="relative max-w-xs">
          <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            v-model="searchQuery"
            type="search"
            placeholder="搜索活动名称..."
            class="w-full h-10 pl-10 pr-3 rounded-lg border border-border bg-background text-foreground text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 placeholder:text-muted-foreground"
          />
        </div>
      </div>

      <div class="rounded-2xl bg-card border border-border overflow-hidden">
        <!-- 桌面表格 -->
        <div class="hidden md:block overflow-x-auto">
          <table class="w-full text-left text-sm">
            <thead class="bg-surface-2 text-muted-foreground">
              <tr>
                <th class="px-5 py-3 font-medium whitespace-nowrap">活动名称</th>
                <th class="px-5 py-3 font-medium whitespace-nowrap">类型</th>
                <th class="px-5 py-3 font-medium whitespace-nowrap">有效期</th>
                <th class="px-5 py-3 font-medium whitespace-nowrap">状态</th>
                <th class="px-5 py-3 font-medium whitespace-nowrap">数据</th>
                <th class="px-5 py-3 font-medium whitespace-nowrap text-right">操作</th>
              </tr>
            </thead>
            <tbody v-if="filteredList.length" class="divide-y divide-border">
              <tr
                v-for="item in filteredList"
                :key="item.id"
                class="transition hover:bg-surface-2"
              >
                <td class="px-5 py-4">
                  <div class="font-medium text-foreground">{{ item.name }}</div>
                  <div class="text-xs text-muted-foreground mt-0.5">创建于 {{ item.createDate }}</div>
                </td>
                <td class="px-5 py-4">
                  <span
                    :class="[
                      'inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium',
                      item.typeColor,
                    ]"
                  >
                    <component :is="item.typeIcon" class="h-3 w-3" />
                    {{ item.typeLabel }}
                  </span>
                </td>
                <td class="px-5 py-4 text-muted-foreground whitespace-nowrap">{{ item.period }}</td>
                <td class="px-5 py-4">
                  <span
                    :class="[
                      'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium',
                      item.statusColor,
                    ]"
                  >
                    <span class="h-1.5 w-1.5 rounded-full bg-current"></span>
                    {{ item.statusText }}
                  </span>
                </td>
                <td class="px-5 py-4">
                  <div class="text-xs text-muted-foreground">{{ item.data1 }} / {{ item.data2 }}</div>
                  <div :class="['text-xs mt-0.5', item.amount === '-' ? 'text-muted-foreground' : 'text-primary']">
                    带动 {{ item.amount }}
                  </div>
                </td>
                <td class="px-5 py-4">
                  <div class="flex items-center justify-end gap-2 flex-wrap">
                    <button
                      v-if="item.status !== 'ended'"
                      type="button"
                      class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-border bg-surface-2 text-muted-foreground text-xs font-medium transition hover:bg-surface-3 hover:text-foreground"
                      title="编辑"
                    >
                      <Pencil class="h-3.5 w-3.5" />编辑
                    </button>
                    <button
                      v-if="item.status === 'active' || item.status === 'paused'"
                      type="button"
                      @click="togglePause(item)"
                      class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-border bg-surface-2 text-muted-foreground text-xs font-medium transition hover:bg-surface-3 hover:text-foreground"
                      :title="item.status === 'active' ? '暂停' : '恢复'"
                    >
                      <component :is="item.status === 'active' ? Pause : Play" class="h-3.5 w-3.5" />
                      {{ item.status === 'active' ? '暂停' : '恢复' }}
                    </button>
                    <button
                      type="button"
                      class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-border bg-surface-2 text-muted-foreground text-xs font-medium transition hover:bg-surface-3 hover:text-foreground"
                      title="查看数据"
                    >
                      <BarChart2 class="h-3.5 w-3.5" />数据
                    </button>
                    <button
                      v-if="item.status === 'ended'"
                      type="button"
                      @click="clonePromo(item)"
                      class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-border bg-surface-2 text-muted-foreground text-xs font-medium transition hover:bg-surface-3 hover:text-foreground"
                      title="复制"
                    >
                      <Copy class="h-3.5 w-3.5" />复制
                    </button>
                    <button
                      type="button"
                      @click="deletePromo(item)"
                      class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-border bg-surface-2 text-muted-foreground text-xs font-medium transition hover:bg-red-500/10 hover:text-red-400 hover:border-red-500/30"
                      title="删除"
                    >
                      <Trash2 class="h-3.5 w-3.5" />删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!filteredList.length" class="py-16 text-center">
            <Filter class="w-12 h-12 text-muted-foreground mx-auto mb-4" />
            <p class="text-muted-foreground">暂无符合条件的活动</p>
          </div>
        </div>

        <!-- 移动端卡片列表 -->
        <div class="md:hidden divide-y divide-border">
          <div v-if="!filteredList.length" class="py-16 text-center">
            <Filter class="w-12 h-12 text-muted-foreground mx-auto mb-4" />
            <p class="text-muted-foreground">暂无符合条件的活动</p>
          </div>
          <article
            v-for="item in filteredList"
            :key="item.id"
            class="p-4 transition hover:bg-surface-2"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0 flex-1">
                <h3 class="font-medium text-foreground truncate">{{ item.name }}</h3>
                <p class="mt-1 text-xs text-muted-foreground">{{ item.period }}</p>
              </div>
              <span
                :class="[
                  'inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium flex-shrink-0',
                  item.statusColor,
                ]"
              >
                <span class="h-1.5 w-1.5 rounded-full bg-current"></span>
                {{ item.statusText }}
              </span>
            </div>
            <div class="mt-3 flex flex-wrap items-center gap-2">
              <span
                :class="[
                  'inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium',
                  item.typeColor,
                ]"
              >
                <component :is="item.typeIcon" class="h-3 w-3" />
                {{ item.typeLabel }}
              </span>
            </div>
            <div class="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
              <div class="rounded-md bg-background py-2 border border-border">
                <p class="text-muted-foreground text-[11px]">{{ item.data1.split(' ')[0] }}</p>
                <p class="mt-1 font-mono font-semibold text-foreground">{{ item.data1.split(' ')[1] }}</p>
              </div>
              <div class="rounded-md bg-background py-2 border border-border">
                <p class="text-muted-foreground text-[11px]">{{ item.data2.split(' ')[0] }}</p>
                <p class="mt-1 font-mono font-semibold text-foreground">{{ item.data2.split(' ')[1] }}</p>
              </div>
              <div class="rounded-md bg-background py-2 border border-border">
                <p class="text-muted-foreground text-[11px]">成交</p>
                <p :class="['mt-1 font-mono font-semibold', item.amount === '-' ? 'text-muted-foreground' : 'text-primary']">
                  {{ item.amount === '-' ? '-' : item.amount.replace('¥', '') }}
                </p>
              </div>
            </div>
            <div class="mt-3 flex flex-wrap gap-2">
              <button
                v-if="item.status !== 'ended'"
                type="button"
                class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-border bg-surface-2 text-muted-foreground text-xs font-medium"
              >
                <Pencil class="h-3.5 w-3.5" />编辑
              </button>
              <button
                v-if="item.status === 'active' || item.status === 'paused'"
                type="button"
                @click="togglePause(item)"
                class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-border bg-surface-2 text-muted-foreground text-xs font-medium"
              >
                <component :is="item.status === 'active' ? Pause : Play" class="h-3.5 w-3.5" />
                {{ item.status === 'active' ? '暂停' : '恢复' }}
              </button>
              <button
                type="button"
                class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-border bg-surface-2 text-muted-foreground text-xs font-medium"
              >
                <BarChart2 class="h-3.5 w-3.5" />数据
              </button>
              <button
                v-if="item.status === 'ended'"
                type="button"
                @click="clonePromo(item)"
                class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-border bg-surface-2 text-muted-foreground text-xs font-medium"
              >
                <Copy class="h-3.5 w-3.5" />复制
              </button>
              <button
                type="button"
                @click="deletePromo(item)"
                class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-border bg-surface-2 text-xs font-medium text-red-400 hover:bg-red-500/10"
              >
                <Trash2 class="h-3.5 w-3.5" />删除
              </button>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>
