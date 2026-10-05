<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Package, Clock, Truck, Eye, CheckCircle2, AlertTriangle, FileText,
  ChevronRight, Search, Filter, MessageSquare, ShieldCheck, RefreshCw,
  Star, ShoppingBag, BarChart3, ExternalLink, Tag, CreditCard
} from 'lucide-vue-next'
import {
  getOrdersApi, getOrderDetailApi, cancelOrderApi, confirmReceivedApi,
  remindShipApi, createPaymentApi
} from '@/api'

const router = useRouter()
const route = useRoute()
const role = ref('buyer')
const activeStatus = ref('all')
const kw = ref('')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 后端 status 数字 → 前端状态字符串（后端 Order 常量：0=待付款 1=待发货 2=待收货 3=已完成 4=已取消 5=售后中）
const STATUS_NUM_TO_STR = {
  0: 'PENDING_PAYMENT', 1: 'PENDING_SHIPMENT', 2: 'SHIPPED',
  3: 'RECEIVED', 4: 'CANCELLED', 5: 'REFUNDING'
}

const statusTabs = ref([
  { id: 'all', name: '全部订单', count: 0, icon: FileText },
  { id: 'pending_payment', name: '待付款', count: 0, icon: CreditCard },
  { id: 'pending_shipment', name: '待发货', count: 0, icon: Package },
  { id: 'shipped', name: '待收货', count: 0, icon: Truck },
  { id: 'received', name: '待评价', count: 0, icon: Star },
  { id: 'aftersales', name: '退款/售后', count: 0, icon: AlertTriangle }
])

const statusBadgeMap = {
  PENDING_PAYMENT: 'bg-brand-warning/15 text-brand-warning border-brand-warning/30',
  PENDING_SHIPMENT: 'bg-brand-info/15 text-brand-info border-brand-info/30',
  SHIPPED: 'bg-brand-primary/15 text-brand-primary border-brand-primary/30',
  RECEIVED: 'bg-brand-success/15 text-brand-success border-brand-success/30',
  COMPLETED: 'bg-brand-ink-2/15 text-brand-ink-2 border-brand-ink-2/30',
  CANCELLED: 'bg-brand-ink-2/15 text-brand-ink-3 border-brand-ink-2/30',
  REFUNDING: 'bg-brand-error/15 text-brand-error border-brand-error/30'
}
const statusTextMap = {
  PENDING_PAYMENT: '等待买家付款',
  PENDING_SHIPMENT: '等待卖家发货',
  SHIPPED: '运输中 / 待收货',
  RECEIVED: '已收货 / 待评价',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '售后处理中'
}

const orders = ref([])

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)
const filtered = computed(() => orders.value)

// 解析商品快照
function parseSnapshot(snapshot) {
  if (!snapshot) return {}
  if (typeof snapshot === 'object') return snapshot
  try { return JSON.parse(snapshot) } catch { return {} }
}

// 解析并格式化收货地址
function formatAddress(addressSnapshot) {
  if (!addressSnapshot) return ''
  let addr = addressSnapshot
  if (typeof addr === 'string') {
    try { addr = JSON.parse(addr) } catch { return addr }
  }
  if (typeof addr !== 'object' || addr === null) return ''
  const name = addr.receiverName || addr.name || ''
  const phone = addr.phone || addr.mobile || ''
  const full = addr.fullAddress || `${addr.province || ''}${addr.city || ''}${addr.district || ''}${addr.detail || addr.address || ''}`
  const parts = []
  if (name) parts.push(name)
  if (phone) parts.push(phone)
  if (full) parts.push(full)
  return parts.join('  ')
}

// 映射单个订单到前端格式
function mapOrder(o) {
  const statusStr = STATUS_NUM_TO_STR[o.status] || 'COMPLETED'
  const items = (o.items || []).map(it => {
    const snap = parseSnapshot(it.productSnapshot)
    return {
      id: it.productId || it.id,
      title: snap.title || '商品',
      cover: snap.image || '',
      price: it.unitPrice || snap.price || 0,
      qty: it.quantity || 1,
      sku: snap.sku || '',
      spec: snap.spec || ''
    }
  })
  const amountInfo = []
  amountInfo.push({ label: '商品总价', value: '¥' + Number(o.totalAmount || 0).toFixed(2) })
  amountInfo.push({ label: '运费', value: Number(o.shippingFee || 0) > 0 ? '¥' + Number(o.shippingFee).toFixed(2) : '包邮' })
  if (o.discountAmount > 0) amountInfo.push({ label: '优惠', value: '-¥' + Number(o.discountAmount).toFixed(2) })
  amountInfo.push({ label: '实付金额', value: '¥' + Number(o.payableAmount || o.totalAmount || 0).toFixed(2), bold: true })

  return {
    id: o.orderNo || o.id,
    rawId: o.id,
    status: statusStr,
    total: o.totalAmount || 0,
    createdAt: o.createdAt,
    paidAt: o.paidAt,
    shippedAt: o.shippedAt,
    receivedAt: o.receivedAt,
    trackingNo: o.trackingNo || '',
    shop: { id: o.shopId || o.sellerId, name: '店铺 ' + (o.shopId || o.sellerId || ''), logo: '店', rating: 4.8 },
    items,
    amountInfo,
    role: role.value,
    address: formatAddress(o.addressSnapshot)
  }
}

async function loadOrders() {
  loading.value = true
  try {
    let statusNum = null
    if (activeStatus.value === 'pending_payment') statusNum = 0
    else if (activeStatus.value === 'pending_shipment') statusNum = 1
    else if (activeStatus.value === 'shipped') statusNum = 2
    else if (activeStatus.value === 'received') statusNum = 3
    else if (activeStatus.value === 'aftersales') statusNum = 5

    const params = {
      role: role.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (statusNum !== null) params.status = statusNum
    if (kw.value) params.keyword = kw.value

    const data = await getOrdersApi(params)
    const records = data.records || data.list || []
    total.value = data.total || records.length

    // 并发获取每个订单的详情（补全 items）
    const detailResults = await Promise.allSettled(
      records.map(o => getOrderDetailApi(o.id))
    )

    orders.value = records.map((o, i) => {
      const detail = detailResults[i].status === 'fulfilled' ? detailResults[i].value : {}
      return mapOrder({ ...o, ...detail, items: detail.items || o.items })
    })
  } catch (e) {
    console.error('加载订单失败', e)
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function handlePay(order) {
  try {
    await createPaymentApi(order.rawId, 'wallet')
    alert('支付成功')
    loadOrders()
  } catch (e) {
    alert(e.message || '支付失败')
  }
}

async function handleCancel(order) {
  if (!confirm('确认取消该订单？')) return
  try {
    await cancelOrderApi(order.rawId, '用户取消')
    alert('已取消')
    loadOrders()
  } catch (e) {
    alert(e.message || '取消失败')
  }
}

async function handleConfirm(order) {
  try {
    await confirmReceivedApi(order.rawId)
    alert('已确认收货')
    loadOrders()
  } catch (e) {
    alert(e.message || '确认失败')
  }
}

async function handleRemind(order) {
  try {
    await remindShipApi(order.rawId)
    alert('已提醒卖家发货')
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

function switchStatus(id) {
  activeStatus.value = id
  currentPage.value = 1
}

function switchRole(r) {
  role.value = r
  currentPage.value = 1
}

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === currentPage.value) return
  currentPage.value = p
}

onMounted(() => {
  if (route.query.status) {
    const q = String(route.query.status)
    const n = Number(q)
    if (!isNaN(n) && STATUS_NUM_TO_STR[n]) {
      activeStatus.value = STATUS_NUM_TO_STR[n].toLowerCase()
    }
  }
  loadOrders()
})

// 监听路由 query.status 变化（从个人中心点击状态跳转时）
watch(() => route.query.status, (newStatus) => {
  if (newStatus !== undefined && newStatus !== null) {
    const n = Number(newStatus)
    if (!isNaN(n) && STATUS_NUM_TO_STR[n]) {
      activeStatus.value = STATUS_NUM_TO_STR[n].toLowerCase()
      currentPage.value = 1
    }
  }
})

watch([role, activeStatus, currentPage], () => {
  loadOrders()
})
</script>

<template>
  <div class="container-app space-y-6">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
      <div>
        <div class="text-xs text-brand-ink-3">交易中心</div>
        <h1 class="text-2xl md:text-3xl font-bold text-brand-ink mt-1 flex items-center gap-3">
          <ShoppingBag class="w-7 h-7 text-brand-primary" />我的订单
        </h1>
        <p class="text-sm text-brand-ink-3 mt-1">跟踪订单状态，查看物流进度，进行售后处理</p>
      </div>
      <div class="inline-flex p-1 rounded-xl bg-brand-surface border border-brand-line">
        <button @click="switchRole('buyer')" :class="['px-5 py-2 rounded-lg text-sm font-medium transition', role === 'buyer' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">
          我是买家
        </button>
        <button @click="switchRole('seller')" :class="['px-5 py-2 rounded-lg text-sm font-medium transition', role === 'seller' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">
          我是卖家
        </button>
      </div>
    </div>

    <!-- 状态快速入口 -->
    <div class="grid grid-cols-3 md:grid-cols-6 gap-2 md:gap-3">
      <button v-for="s in statusTabs" :key="s.id" @click="switchStatus(s.id)"
        :class="['p-4 rounded-xl border transition text-left',
                 activeStatus === s.id ? 'bg-brand-primary/10 border-brand-primary/40 ring-1 ring-brand-primary/20' : 'bg-brand-surface border-brand-line hover:border-brand-primary/40']">
        <div class="flex items-center justify-between">
          <div :class="['w-8 h-8 rounded-lg flex items-center justify-center', activeStatus === s.id ? 'bg-brand-primary/20 text-brand-primary' : 'bg-brand-bg text-brand-ink-3']">
            <component :is="s.icon" class="w-4 h-4" />
          </div>
          <span v-if="s.count" :class="['min-w-5 h-5 px-1 rounded-full text-[10px] font-bold flex items-center justify-center',
                                         ['pending_payment','aftersales'].includes(s.id) ? 'bg-brand-error text-white' : 'bg-brand-primary text-brand-primary-ink']">{{ s.count }}</span>
        </div>
        <div class="text-xs text-brand-ink-2 mt-3 leading-snug">{{ s.name }}</div>
      </button>
    </div>

    <!-- 筛选+搜索 -->
    <div class="flex flex-col md:flex-row items-center gap-3 rounded-xl bg-brand-surface border border-brand-line p-4">
      <div class="flex items-center gap-2 flex-1 w-full">
        <div class="flex h-10 rounded-lg bg-brand-bg border border-brand-line focus-within:border-brand-primary transition max-w-xl w-full md:w-96">
          <div class="pl-3 flex items-center text-brand-ink-3"><Search class="w-4 h-4" /></div>
          <input v-model="kw" @keyup.enter="() => { currentPage = 1; loadOrders() }" placeholder="搜索订单号或商品名称..." class="flex-1 px-3 text-sm bg-transparent placeholder:text-brand-ink-3 focus:outline-none" />
        </div>
        <button @click="() => { currentPage = 1; loadOrders() }" class="px-4 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition flex items-center gap-2">
          <Search class="w-4 h-4" />搜索
        </button>
      </div>
      <div class="flex items-center gap-2 w-full md:w-auto justify-end">
        <button @click="loadOrders" class="px-4 h-10 rounded-lg bg-brand-bg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
          <RefreshCw class="w-4 h-4" />刷新
        </button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="rounded-2xl bg-brand-surface border border-brand-line p-16 text-center">
      <RefreshCw class="w-10 h-10 mx-auto text-brand-primary animate-spin" />
      <p class="text-sm text-brand-ink-3 mt-4">加载订单中...</p>
    </div>

    <!-- 订单卡片列表 -->
    <div v-else class="space-y-5">
      <div v-for="o in filtered" :key="o.id" class="rounded-2xl bg-brand-surface border border-brand-line overflow-hidden">
        <div class="flex flex-wrap items-center justify-between gap-3 px-5 py-3 bg-brand-bg/60 border-b border-brand-line">
          <div class="flex items-center gap-4 text-xs">
            <span class="text-brand-ink-3">下单时间：</span>
            <span class="text-brand-ink-2 font-medium">{{ (o.paidAt || o.createdAt || '').slice(0, 19).replace('T', ' ') }}</span>
            <span class="text-brand-ink-3">订单号：</span>
            <span class="text-brand-ink-2 font-mono font-medium">{{ o.id }}</span>
          </div>
          <div class="flex items-center gap-3">
            <span :class="['px-2.5 py-1 rounded-full text-xs font-semibold border whitespace-nowrap', statusBadgeMap[o.status]]">
              {{ statusTextMap[o.status] }}
            </span>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-[1fr_260px]">
          <div class="px-5 py-5 border-b lg:border-b-0 lg:border-r border-brand-line-subtle">
            <button @click="router.push('/seller/' + o.shop.id)" class="inline-flex items-center gap-2 mb-3 group">
              <div class="w-7 h-7 rounded-lg bg-brand-primary/20 border border-brand-primary/30 flex items-center justify-center text-brand-primary text-xs font-bold">{{ o.shop.logo }}</div>
              <span class="text-sm font-semibold text-brand-ink group-hover:text-brand-primary transition">{{ o.shop.name }}</span>
              <ChevronRight class="w-3 h-3 text-brand-ink-3" />
            </button>

            <div class="space-y-3">
              <div v-for="(it, i) in o.items" :key="it.id || i" class="flex items-start gap-4">
                <div v-if="it.cover" class="w-20 h-20 rounded-lg overflow-hidden bg-brand-bg border border-brand-line shrink-0">
                  <img :src="it.cover" class="w-full h-full object-cover" />
                </div>
                <div v-else class="w-20 h-20 rounded-lg bg-brand-bg border border-brand-line shrink-0 flex items-center justify-center text-brand-ink-3 text-xs">无图</div>
                <div class="flex-1 min-w-0">
                  <div v-if="it.title" class="text-sm text-brand-ink line-clamp-2 leading-snug hover:text-brand-primary cursor-pointer" @click="router.push('/product/' + it.id)">{{ it.title }}</div>
                  <div v-if="it.spec" class="text-[11px] text-brand-ink-3 mt-1">规格：{{ it.spec }}</div>
                  <div v-if="it.sku" class="text-[11px] text-brand-ink-3 mt-0.5 font-mono">SKU: {{ it.sku }}</div>
                </div>
                <div class="text-right shrink-0 min-w-[100px]">
                  <div class="text-sm font-semibold text-brand-ink">¥{{ Number(it.price).toFixed(2) }}</div>
                  <div class="text-[11px] text-brand-ink-3 mt-1">x {{ it.qty }}</div>
                </div>
              </div>
            </div>

            <div v-if="o.status !== 'CANCELLED'" class="mt-4 flex flex-wrap gap-2.5">
              <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-brand-info/10 text-brand-info border border-brand-info/25 text-[11px]">
                <ShieldCheck class="w-3.5 h-3.5" />平台担保交易
              </div>
              <div v-if="o.trackingNo" class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-brand-primary/10 text-brand-primary border border-brand-primary/25 text-[11px]">
                <Truck class="w-3.5 h-3.5" />快递单号：{{ o.trackingNo }}
              </div>
            </div>
          </div>

          <div class="p-5 flex flex-col">
            <div class="space-y-1.5 border-b border-brand-line pb-4">
              <div v-for="a in o.amountInfo" :key="a.label" class="flex items-center justify-between text-xs">
                <span class="text-brand-ink-3">{{ a.label }}</span>
                <span :class="['font-medium', a.bold ? 'text-brand-error text-lg font-black' : a.error ? 'text-brand-error' : 'text-brand-ink-2']">{{ a.value }}</span>
              </div>
            </div>

            <div v-if="o.address" class="py-4 border-b border-brand-line text-xs space-y-1">
              <div class="text-brand-ink-3">收货信息</div>
              <div class="text-brand-ink-2 leading-snug">{{ o.address }}</div>
            </div>

            <div class="mt-4 space-y-2 flex-1 flex flex-col justify-end">
              <template v-if="o.status === 'PENDING_PAYMENT'">
                <button @click="handlePay(o)" class="w-full h-9 rounded-lg bg-brand-error text-white text-xs font-semibold hover:bg-brand-error/90 transition flex items-center justify-center gap-1.5">
                  <CreditCard class="w-3.5 h-3.5" />立即付款
                </button>
                <button @click="handleCancel(o)" class="w-full h-8 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:border-brand-error/40 hover:text-brand-error transition">取消订单</button>
              </template>

              <template v-if="role === 'seller' && o.status === 'PENDING_SHIPMENT'">
                <button class="w-full h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-xs font-semibold hover:bg-brand-primary-hover transition">立即发货</button>
              </template>

              <template v-if="role === 'buyer' && o.status === 'SHIPPED'">
                <button @click="handleConfirm(o)" class="w-full h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-xs font-semibold hover:bg-brand-primary-hover transition flex items-center justify-center gap-1.5">
                  <CheckCircle2 class="w-3.5 h-3.5" />确认收货
                </button>
                <button @click="router.push('/messages')" class="w-full h-8 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center justify-center gap-1">
                  <MessageSquare class="w-3 h-3" />联系卖家
                </button>
              </template>

              <template v-if="o.status === 'RECEIVED'">
                <button class="w-full h-9 rounded-lg bg-brand-warning text-white text-xs font-semibold hover:bg-brand-warning/90 transition flex items-center justify-center gap-1.5">
                  <Star class="w-3.5 h-3.5" />立即评价
                </button>
                <button @click="router.push('/aftersales')" class="w-full h-8 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-error hover:border-brand-error/40 transition flex items-center justify-center gap-1">
                  <AlertTriangle class="w-3 h-3" />申请售后
                </button>
              </template>

              <template v-if="o.status === 'PENDING_SHIPMENT' && role === 'buyer'">
                <button @click="handleRemind(o)" class="w-full h-9 rounded-lg border border-brand-warning/40 text-brand-warning text-xs font-semibold hover:bg-brand-warning/10 transition flex items-center justify-center gap-1.5">
                  <MessageSquare class="w-3.5 h-3.5" />催发货
                </button>
                <button @click="router.push('/messages')" class="w-full h-8 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center justify-center gap-1">
                  <MessageSquare class="w-3 h-3" />联系卖家
                </button>
              </template>

              <template v-if="o.status === 'REFUNDING'">
                <button @click="router.push('/aftersales')" class="w-full h-9 rounded-lg bg-brand-error/15 text-brand-error text-xs font-semibold hover:bg-brand-error/25 transition border border-brand-error/30 flex items-center justify-center gap-1.5">
                  <AlertTriangle class="w-3.5 h-3.5" />查看售后进度
                </button>
              </template>

              <template v-if="o.status === 'COMPLETED'">
                <button class="w-full h-9 rounded-lg bg-brand-primary/15 text-brand-primary text-xs font-semibold hover:bg-brand-primary/25 transition flex items-center justify-center gap-1.5 border border-brand-primary/30">
                  <ShoppingBag class="w-3.5 h-3.5" />再次购买
                </button>
              </template>
            </div>
          </div>
        </div>
      </div>

      <div v-if="filtered.length === 0" class="rounded-2xl bg-brand-surface border border-brand-line p-16 text-center">
        <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-brand-primary/15 flex items-center justify-center">
          <FileText class="w-10 h-10 text-brand-primary" />
        </div>
        <h3 class="font-bold text-brand-ink text-xl">暂无匹配订单</h3>
        <p class="text-sm text-brand-ink-3 mt-2">换个筛选条件或去市场看看有没有心仪的硬件吧～</p>
        <div class="mt-6 flex items-center justify-center gap-3">
          <button @click="router.push('/browse')" class="px-6 h-10 rounded-lg bg-brand-primary text-brand-primary-ink font-medium hover:bg-brand-primary-hover transition flex items-center gap-2">
            <ShoppingBag class="w-4 h-4" />去逛逛
          </button>
          <button @click="switchStatus('all')" class="px-6 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition">
            清空筛选
          </button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="flex items-center justify-between pt-2">
      <div class="text-xs text-brand-ink-3">共 <span class="text-brand-ink font-semibold">{{ total }}</span> 条订单</div>
      <div class="flex items-center gap-1">
        <button @click="goPage(currentPage - 1)" :disabled="currentPage <= 1" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary hover:border-brand-primary/40 transition text-xs disabled:opacity-40">&lt;</button>
        <button v-for="n in Math.min(totalPages, 5)" :key="n" @click="goPage(n)" :class="['w-9 h-9 rounded-lg text-xs transition', n === currentPage ? 'bg-brand-primary text-brand-primary-ink font-semibold' : 'border border-brand-line text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40']">{{ n }}</button>
        <button @click="goPage(currentPage + 1)" :disabled="currentPage >= totalPages" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary hover:border-brand-primary/40 transition text-xs disabled:opacity-40">&gt;</button>
      </div>
    </div>
  </div>
</template>
