<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  PackageCheck, Search, CheckSquare, ChevronLeft, ChevronRight,
  X, Printer, Truck, Package, PackageX, Eye, Copy, Clock, User,
  MapPin, ChevronDown, ChevronUp, AlertTriangle
} from 'lucide-vue-next'

const router = useRouter()

// 状态筛选 Tab
const statusTabs = [
  { id: 'pending', name: '待发货', count: 3 },
  { id: 'shipped', name: '已发货', count: 8 },
  { id: 'partial', name: '部分发货', count: 1 },
  { id: 'delivered', name: '已签收', count: 15 }
]
const activeStatus = ref('pending')

// 搜索 & 排序
const keyword = ref('')
const sortOrder = ref('desc') // desc / asc

// 数据
const ordersData = ref([
  {
    id: 'ORD202608280001', orderNo: 'ORD202608280001', createdAt: '2026-08-28 11:20',
    status: 'pending', buyer: '王先生', buyerPhone: '138****8888',
    address: '北京市海淀区知春路 111 号锦秋国际 A1801',
    items: [
      { sku: 'YJX-GPU-001', title: '七彩虹 RTX 4070 战斧豪华版 成色95新', price: 3680, qty: 1,
        cover: 'https://placehold.co/160x160/15161A/00D4AA?text=RTX4070' },
      { sku: 'YJX-ACC-012', title: '顺丰到付运费（商品发走后补差）', price: 0, qty: 1, cover: '' }
    ],
    total: 3680, paidAt: '2026-08-28 11:25'
  },
  {
    id: 'ORD2026082700012', orderNo: 'ORD2026082700012', createdAt: '2026-08-27 19:05',
    status: 'pending', buyer: '张硬件', buyerPhone: '136****6666',
    address: '浙江省杭州市西湖区文三路 369 号数码港大厦 A 座 1202',
    items: [
      { sku: 'YJX-CPU-027', title: 'Intel i7-12700K 散片 无磕碰 成色准新 原装', price: 2280, qty: 1,
        cover: 'https://placehold.co/160x160/15161A/00D4AA?text=12700K' }
    ],
    total: 2280, paidAt: '2026-08-27 19:08'
  },
  {
    id: 'ORD2026082600058', orderNo: 'ORD2026082600058', createdAt: '2026-08-26 14:20',
    status: 'pending', buyer: '李装机', buyerPhone: '139****1234',
    address: '广东省深圳市南山区科技园南区 T3 栋 2105',
    items: [
      { sku: 'YJX-RAM-044', title: '金士顿 FURY Beast DDR5 6000 32GB(16×2) 黑色 CL30', price: 899, qty: 2,
        cover: 'https://placehold.co/160x160/15161A/00D4AA?text=DDR5' }
    ],
    total: 1798, paidAt: '2026-08-26 14:22'
  },
  {
    id: 'ORD2026082500033', orderNo: 'ORD2026082500033', createdAt: '2026-08-25 10:02',
    status: 'partial', buyer: '赵二手', buyerPhone: '135****5555',
    address: '上海市浦东新区张江高科技园区博云路 2 号 10 号楼',
    items: [
      { sku: 'YJX-MB-015', title: '华硕 ROG STRIX B660-A GAMING WIFI 主板', price: 1699, qty: 1,
        cover: 'https://placehold.co/160x160/15161A/00D4AA?text=B660A', shipped: true },
      { sku: 'YJX-CASE-031', title: '追风者 P600S 中塔机箱 侧透静音款', price: 699, qty: 1,
        cover: 'https://placehold.co/160x160/15161A/00D4AA?text=P600S', shipped: false }
    ],
    total: 2398, paidAt: '2026-08-25 10:05'
  },
  {
    id: 'ORD2026082400088', orderNo: 'ORD2026082400088', createdAt: '2026-08-24 09:30',
    status: 'shipped', buyer: '陈数码', buyerPhone: '133****7777',
    address: '四川省成都市高新区天府二街 138 号蜀都中心 2 号楼',
    items: [
      { sku: 'YJX-SSD-021', title: '三星 990 PRO 2TB PCIe 4.0 固态', price: 1199, qty: 1,
        cover: 'https://placehold.co/160x160/15161A/00D4AA?text=990PRO' }
    ],
    total: 1199, paidAt: '2026-08-24 09:32', shippedAt: '2026-08-24 15:00',
    courier: '顺丰速运', trackingNo: 'SF1234567890456'
  }
])

// 过滤
const filtered = computed(() => {
  let list = ordersData.value.filter(o => o.status === activeStatus.value)
  const kw = keyword.value.trim()
  if (kw) {
    list = list.filter(o =>
      o.orderNo.includes(kw) || o.buyer.includes(kw) ||
      o.items.some(it => it.title.includes(kw))
    )
  }
  list = [...list].sort((a, b) => {
    const cmp = a.createdAt.localeCompare(b.createdAt)
    return sortOrder.value === 'desc' ? -cmp : cmp
  })
  return list
})

// 批量选择
const selectedIds = ref(new Set())
const toggleSelect = (id) => {
  const set = new Set(selectedIds.value)
  if (set.has(id)) set.delete(id); else set.add(id)
  selectedIds.value = set
}
const toggleSelectAll = () => {
  if (selectedIds.value.size === filtered.value.length) selectedIds.value = new Set()
  else selectedIds.value = new Set(filtered.value.map(o => o.id))
}
const selectedCount = computed(() => selectedIds.value.size)
const showBulkBar = computed(() => selectedCount.value > 0)
const isAllSelected = computed(() => filtered.value.length > 0 && selectedCount.value === filtered.value.length)

// 分页
const currentPage = ref(1)
const pageSize = 10
const fmt = (n) => '¥' + Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2 })

// ====== 发货弹窗 ======
const shipModalOpen = ref(false)
const shipOrderId = ref(null)
const isBatchShip = ref(false)
const shipCourier = ref('顺丰速运')
const shipTracking = ref('')
const shipItemsSelected = ref(new Set())
const couriers = ['顺丰速运', '京东物流', '中通快递', '圆通速递', '韵达快递']

const openShip = (orderId) => {
  shipOrderId.value = orderId
  isBatchShip.value = false
  shipCourier.value = '顺丰速运'
  shipTracking.value = ''
  shipItemsSelected.value = new Set()
  shipModalOpen.value = true
}
const openBatchShip = () => {
  if (selectedCount.value === 0) return alert('请先勾选要批量发货的订单')
  shipOrderId.value = null
  isBatchShip.value = true
  shipCourier.value = '顺丰速运'
  shipTracking.value = ''
  shipItemsSelected.value = new Set()
  shipModalOpen.value = true
}
const currentShipOrder = computed(() => ordersData.value.find(o => o.id === shipOrderId.value))
const confirmShip = () => {
  if (!shipTracking.value.trim()) return alert('请输入运单号')
  alert((isBatchShip.value ? `批量发货成功（${selectedCount.value} 单）` : `订单 ${shipOrderId.value} 已发货`) + `，运单号：${shipTracking.value}`)
  shipModalOpen.value = false
  selectedIds.value = new Set()
}

// ====== 电子面单弹窗 ======
const labelModalOpen = ref(false)
const labelOrder = ref(null)
const openLabel = (orderId) => {
  labelOrder.value = ordersData.value.find(o => o.id === orderId)
  labelModalOpen.value = true
}
const printLabel = () => {
  alert('电子面单已发送到打印机，请检查打印任务')
  labelModalOpen.value = false
}
</script>

<template>
  <div class="container-app space-y-6 pb-20">
    <!-- 标题 -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-brand-ink">发货管理</h1>
        <p class="mt-1 text-sm text-brand-ink-2">处理订单发货、打印电子面单与跟踪物流</p>
      </div>
      <button @click="openBatchShip"
        class="inline-flex items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 py-2 text-sm font-medium text-brand-primary-ink transition-colors hover:bg-brand-primary/90 self-start">
        <PackageCheck class="h-4 w-4" /> 批量发货
      </button>
    </div>

    <!-- 状态 Tab -->
    <div class="flex items-center gap-2 overflow-x-auto no-scrollbar border-b border-brand-line pb-3">
      <button v-for="s in statusTabs" :key="s.id" @click="activeStatus = s.id"
        :class="['h-9 px-4 rounded-full text-sm font-medium transition border',
                 activeStatus === s.id
                   ? 'bg-brand-primary/12 text-brand-primary border-brand-primary/30'
                   : 'text-brand-ink-2 border-transparent hover:bg-brand-surface hover:text-brand-ink']">
        {{ s.name }}
        <span v-if="s.count" class="ml-1.5 text-xs opacity-70">({{ s.count }})</span>
      </button>
    </div>

    <!-- 搜索筛选 -->
    <div class="flex flex-col gap-3 md:flex-row md:items-center">
      <div class="relative flex-1">
        <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-brand-ink-3" />
        <input v-model="keyword" type="search" placeholder="搜索订单号、商品名称或买家..."
          class="h-10 w-full rounded-lg border border-brand-line bg-brand-surface pl-10 pr-4 text-sm text-brand-ink placeholder:text-brand-ink-3 outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
      </div>
      <div class="flex gap-3">
        <select v-model="sortOrder"
          class="h-10 rounded-lg border border-brand-line bg-brand-surface px-3 text-sm text-brand-ink outline-none focus:border-brand-primary">
          <option value="desc">按时间倒序</option>
          <option value="asc">按时间正序</option>
        </select>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <Transition name="slide">
      <div v-if="showBulkBar" class="flex flex-col gap-3 rounded-xl border border-brand-line bg-brand-surface-2 p-4 md:flex-row md:items-center md:justify-between">
        <div class="flex items-center gap-4">
          <button @click="toggleSelectAll" class="inline-flex items-center gap-2 text-sm font-medium text-brand-primary transition-colors hover:text-brand-primary/80">
            <CheckSquare class="h-4 w-4" /> {{ isAllSelected ? '取消全选' : '全选' }}
          </button>
          <span class="text-sm text-brand-ink-2">已选 <strong class="text-brand-primary">{{ selectedCount }}</strong> 单</span>
        </div>
        <button @click="openBatchShip"
          class="inline-flex items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 py-2 text-sm font-medium text-brand-primary-ink transition-colors hover:bg-brand-primary/90 self-start md:self-auto">
          <PackageCheck class="h-4 w-4" /> 批量发货
        </button>
      </div>
    </Transition>

    <!-- 订单列表 -->
    <section v-if="filtered.length" class="space-y-4">
      <article v-for="o in filtered" :key="o.id"
        class="rounded-2xl border border-brand-line bg-brand-surface overflow-hidden transition hover:border-brand-primary/40">
        <!-- 头部 -->
        <div class="flex flex-col gap-3 px-4 py-3 md:px-5 md:flex-row md:items-center md:justify-between bg-brand-surface/60 border-b border-brand-line">
          <div class="flex items-center gap-4">
            <label class="inline-flex items-center gap-2 cursor-pointer">
              <input type="checkbox" :checked="selectedIds.has(o.id)" @change="toggleSelect(o.id)"
                class="w-4 h-4 rounded border-brand-line text-brand-primary focus:ring-brand-primary/30 bg-brand-bg" />
            </label>
            <div class="text-xs text-brand-ink-3">
              <span class="font-mono text-brand-ink-2">{{ o.orderNo }}</span>
              <span class="mx-2">·</span>
              <Clock class="inline h-3 w-3" /> 下单 {{ o.createdAt }}
            </div>
          </div>
          <div class="flex items-center gap-3 text-xs">
            <span class="inline-flex items-center gap-1 rounded-full bg-brand-warning/12 text-brand-warning px-2.5 py-0.5 font-medium">
              {
                pending: '待发货',
                shipped: '已发货',
                partial: '部分发货',
                delivered: '已签收'
              }[o.status]
            </span>
          </div>
        </div>

        <!-- 商品 -->
        <div class="p-4 md:p-5">
          <div class="flex flex-col gap-4 md:flex-row md:items-start">
            <div class="flex-1 space-y-3">
              <div v-for="(it, idx) in o.items" :key="idx" class="flex gap-3 items-start">
                <div class="h-16 w-16 rounded-lg bg-brand-surface-2 flex-shrink-0 overflow-hidden flex items-center justify-center">
                  <img v-if="it.cover" :src="it.cover" :alt="it.title" class="h-full w-full object-cover" />
                  <Package v-else class="h-7 w-7 text-brand-ink-3" />
                </div>
                <div class="min-w-0 flex-1">
                  <p class="text-sm font-medium text-brand-ink line-clamp-2">{{ it.title }}</p>
                  <p class="mt-1 text-xs text-brand-ink-3 font-mono">{{ it.sku || '—' }}</p>
                  <div class="mt-1 flex items-center justify-between text-xs">
                    <span class="text-brand-ink-2">¥{{ it.price.toLocaleString() }} × {{ it.qty }}</span>
                    <span v-if="it.shipped" class="text-brand-success">已发</span>
                    <span v-else-if="o.status === 'partial'" class="text-brand-warning">待发</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧：买家信息 + 操作 -->
            <div class="md:w-64 md:border-l md:border-brand-line md:pl-5 space-y-3">
              <div class="text-xs space-y-1">
                <div class="flex items-center gap-1.5 text-brand-ink-3"><User class="h-3.5 w-3.5" /><span class="text-brand-ink">{{ o.buyer }}</span><span class="text-brand-ink-3">{{ o.buyerPhone }}</span></div>
                <div class="flex items-start gap-1.5 text-brand-ink-3"><MapPin class="h-3.5 w-3.5 mt-0.5 flex-shrink-0" /><span class="line-clamp-2">{{ o.address }}</span></div>
                <div v-if="o.trackingNo" class="flex items-center gap-1.5 text-brand-ink-3">
                  <Truck class="h-3.5 w-3.5" />
                  <span class="text-brand-ink">{{ o.courier }} · {{ o.trackingNo }}</span>
                </div>
              </div>
              <div class="flex flex-wrap gap-2">
                <button v-if="o.status === 'pending' || o.status === 'partial'" @click="openShip(o.id)"
                  class="inline-flex items-center gap-1.5 rounded-lg bg-brand-primary px-3 py-1.5 text-xs font-medium text-brand-primary-ink transition hover:bg-brand-primary/90">
                  <PackageCheck class="h-3.5 w-3.5" /> 发货
                </button>
                <button v-if="o.trackingNo"
                  class="inline-flex items-center gap-1.5 rounded-lg border border-brand-line bg-brand-surface px-3 py-1.5 text-xs font-medium text-brand-ink transition hover:border-brand-primary hover:text-brand-primary">
                  <Printer class="h-3.5 w-3.5" @click.stop="openLabel(o.id)" /> 面单
                </button>
                <button v-if="o.trackingNo" @click="router.push('/logistics')"
                  class="inline-flex items-center gap-1.5 rounded-lg border border-brand-line bg-brand-surface px-3 py-1.5 text-xs font-medium text-brand-ink transition hover:border-brand-primary hover:text-brand-primary">
                  <Eye class="h-3.5 w-3.5" /> 跟踪
                </button>
              </div>
              <div class="text-right">
                <p class="text-xs text-brand-ink-3">合计</p>
                <p class="font-mono text-lg font-bold text-brand-primary">{{ fmt(o.total) }}</p>
              </div>
            </div>
          </div>
        </div>
      </article>
    </section>

    <!-- 空状态 -->
    <section v-else class="flex flex-col items-center justify-center rounded-2xl border border-brand-line bg-brand-surface py-16 text-center">
      <PackageX class="h-16 w-16 text-brand-ink-3" />
      <h3 class="mt-4 text-lg font-medium text-brand-ink">暂无相关订单</h3>
      <p class="mt-1 text-sm text-brand-ink-2">当前状态下没有需要处理的订单</p>
    </section>

    <!-- 分页 -->
    <div class="mt-8 flex flex-col items-center justify-between gap-4 md:flex-row">
      <p class="text-sm text-brand-ink-2">共 <span class="font-mono font-semibold text-brand-ink">{{ filtered.length }}</span> 笔订单</p>
      <div class="flex items-center gap-2">
        <button :disabled="currentPage === 1" @click="currentPage--"
          class="inline-flex items-center justify-center min-w-9 h-9 rounded-lg border border-brand-line bg-brand-surface text-brand-ink-2 text-sm transition hover:border-brand-primary hover:text-brand-primary disabled:opacity-40 disabled:cursor-not-allowed">
          <ChevronLeft class="h-4 w-4" />
        </button>
        <button class="inline-flex items-center justify-center min-w-9 h-9 rounded-lg bg-brand-primary border-brand-primary text-brand-primary-ink text-sm">1</button>
        <button class="inline-flex items-center justify-center min-w-9 h-9 rounded-lg border border-brand-line bg-brand-surface text-brand-ink-2 text-sm transition hover:border-brand-primary hover:text-brand-primary">2</button>
        <button @click="currentPage++"
          class="inline-flex items-center justify-center min-w-9 h-9 rounded-lg border border-brand-line bg-brand-surface text-brand-ink-2 text-sm transition hover:border-brand-primary hover:text-brand-primary">
          <ChevronRight class="h-4 w-4" />
        </button>
      </div>
    </div>

    <!-- ================ 发货弹窗 ================ -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="shipModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" @click.self="shipModalOpen = false">
          <div class="w-full max-w-md max-h-[90vh] overflow-y-auto rounded-2xl bg-brand-surface border border-brand-line shadow-2xl p-6">
            <div class="mb-4 flex items-center justify-between">
              <h2 class="text-lg font-semibold text-brand-ink">{{ isBatchShip ? `批量发货 (${selectedCount} 单)` : '填写发货信息' }}</h2>
              <button @click="shipModalOpen = false" class="inline-flex h-8 w-8 items-center justify-center rounded-lg text-brand-ink-2 transition-colors hover:bg-brand-surface-2 hover:text-brand-ink">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="space-y-4">
              <div>
                <label class="mb-1.5 block text-sm text-brand-ink-2">快递公司</label>
                <select v-model="shipCourier"
                  class="h-10 w-full rounded-lg border border-brand-line bg-brand-surface-2 px-3 text-sm text-brand-ink outline-none focus:border-brand-primary">
                  <option v-for="c in couriers" :key="c" :value="c">{{ c }}</option>
                </select>
              </div>
              <div>
                <label class="mb-1.5 block text-sm text-brand-ink-2">运单号</label>
                <input v-model="shipTracking" type="text" placeholder="请输入运单号"
                  class="h-10 w-full rounded-lg border border-brand-line bg-brand-surface-2 px-3 text-sm text-brand-ink placeholder:text-brand-ink-3 outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
              </div>
              <div v-if="!isBatchShip && currentShipOrder">
                <label class="mb-1.5 block text-sm text-brand-ink-2">选择发货商品</label>
                <div class="space-y-2">
                  <label v-for="(it, idx) in currentShipOrder.items" :key="idx"
                    class="flex items-center gap-3 rounded-lg border border-brand-line bg-brand-surface-2 p-3 cursor-pointer hover:border-brand-primary/60 transition">
                    <input type="checkbox" :checked="shipItemsSelected.has(idx) || it.shipped" :disabled="it.shipped"
                      @change="() => { const s = new Set(shipItemsSelected); s.has(idx)?s.delete(idx):s.add(idx); shipItemsSelected = s; }"
                      class="w-4 h-4 rounded border-brand-line text-brand-primary focus:ring-brand-primary/30 bg-brand-bg" />
                    <div class="min-w-0 flex-1">
                      <p class="text-sm text-brand-ink truncate">{{ it.title }}</p>
                      <p class="text-xs text-brand-ink-3">¥{{ it.price }} × {{ it.qty }} <span v-if="it.shipped" class="ml-2 text-brand-success">（已发）</span></p>
                    </div>
                  </label>
                </div>
              </div>
            </div>
            <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button @click="shipModalOpen = false"
                class="h-10 rounded-lg border border-brand-line bg-brand-surface px-4 text-sm font-medium text-brand-ink transition hover:border-brand-primary hover:text-brand-primary">取消</button>
              <button @click="confirmShip"
                class="h-10 rounded-lg bg-brand-primary px-4 text-sm font-semibold text-brand-primary-ink transition hover:bg-brand-primary/90 inline-flex items-center justify-center gap-2">
                <PackageCheck class="h-4 w-4" /> 确认发货
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ================ 电子面单弹窗 ================ -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="labelModalOpen && labelOrder" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" @click.self="labelModalOpen = false">
          <div class="w-full max-w-md max-h-[90vh] overflow-y-auto rounded-2xl bg-white text-slate-900 border shadow-2xl">
            <div class="p-4 bg-brand-surface text-brand-ink rounded-t-2xl flex items-center justify-between">
              <h2 class="text-lg font-semibold">电子面单预览</h2>
              <button @click="labelModalOpen = false" class="inline-flex h-8 w-8 items-center justify-center rounded-lg text-brand-ink-2 transition-colors hover:bg-brand-surface-2 hover:text-brand-ink">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="p-5 bg-white">
              <div class="flex items-center justify-between border-b border-slate-200 pb-3">
                <div>
                  <div class="text-xs text-slate-500">硬件侠 · 担保交易</div>
                  <div class="text-base font-bold">SF · 顺丰速运</div>
                </div>
                <div class="font-mono text-xs text-slate-600">{{ labelOrder.trackingNo || '待生成' }}</div>
              </div>
              <!-- 条形码 -->
              <div class="flex items-end justify-center gap-0.5 h-12 my-4 px-2">
                <div v-for="i in 40" :key="i" class="bg-slate-900" :style="{ width: (i%3===0?3:1)+'px', height: (50 + (i%5)*10)+'%' }"></div>
              </div>
              <div class="space-y-3 text-sm">
                <div class="border-b border-slate-200 pb-2">
                  <div class="text-xs text-slate-500 mb-1">收件人</div>
                  <div class="flex gap-2"><span class="font-semibold">{{ labelOrder.buyer }}</span><span>{{ labelOrder.buyerPhone }}</span></div>
                  <div class="text-slate-700 mt-0.5">{{ labelOrder.address }}</div>
                </div>
                <div class="border-b border-slate-200 pb-2">
                  <div class="text-xs text-slate-500 mb-1">寄件人</div>
                  <div class="flex gap-2"><span class="font-semibold">极客老王</span><span>138****6688</span></div>
                  <div class="text-slate-700 mt-0.5">上海市浦东新区张江路 100 号硬件仓 B2-18</div>
                </div>
                <div>
                  <div class="text-xs text-slate-500 mb-1">商品</div>
                  <ul class="list-disc list-inside text-slate-700">
                    <li v-for="(it, idx) in labelOrder.items.slice(0, 2)" :key="idx">{{ it.title.slice(0, 30) }} ×{{ it.qty }}</li>
                    <li v-if="labelOrder.items.length > 2">...共 {{ labelOrder.items.length }} 件</li>
                  </ul>
                </div>
                <div class="text-xs text-slate-500 border-t border-slate-200 pt-2">
                  订单号 {{ labelOrder.orderNo }} · 平台担保 签收后放款
                </div>
              </div>
            </div>
            <div class="p-4 bg-brand-surface rounded-b-2xl flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button @click="labelModalOpen = false"
                class="h-10 rounded-lg border border-brand-line bg-brand-surface px-4 text-sm font-medium text-brand-ink transition hover:border-brand-primary hover:text-brand-primary">关闭</button>
              <button @click="printLabel"
                class="h-10 rounded-lg bg-brand-primary px-4 text-sm font-semibold text-brand-primary-ink transition hover:bg-brand-primary/90 inline-flex items-center justify-center gap-2">
                <Printer class="h-4 w-4" /> 打印面单
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
@reference "../style.css";
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-enter-active, .slide-leave-active { transition: all 0.25s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-4px); }
.line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>
