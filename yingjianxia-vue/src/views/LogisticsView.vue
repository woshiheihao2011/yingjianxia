<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft, Truck, Copy, CheckCircle, PackageCheck, MapPin,
  User, Phone, Package, MessageSquare, Clock
} from 'lucide-vue-next'

const router = useRouter()

// 订单基本信息
const order = {
  orderNo: 'ORD20260826003',
  product: {
    title: '芝奇 DDR5 6000 32GB(16×2) 套装 焰锋戟 银色散热片',
    seller: '装机小队',
    cover: 'https://placehold.co/192x192/15161A/00D4AA?text=DDR5+6000',
    unitPrice: 620,
    qty: 2,
    total: 1240
  },
  carrier: '顺丰速运',
  trackingNo: 'SF1234567890123'
}

const fmt = (n) => '¥' + Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2 })

// 物流时间轴
const timeline = [
  {
    status: 'completed', title: '已揽收',
    icon: PackageCheck,
    time: '2026-08-26 18:20',
    location: '上海浦东集散中心',
    desc: '包裹已由顺丰速运揽收，准备发往下一站'
  },
  {
    status: 'completed', title: '运输中',
    icon: Truck,
    time: '2026-08-26 22:45',
    location: '上海浦东 → 杭州萧山',
    desc: '快件已离开上海浦东集散中心，发往杭州萧山转运中心'
  },
  {
    status: 'completed', title: '到达分拣中心',
    icon: MapPin,
    time: '2026-08-27 04:10',
    location: '杭州萧山转运中心',
    desc: '快件已到达转运中心，正在进行分拣'
  },
  {
    status: 'current', title: '派送中',
    icon: User,
    time: '2026-08-27 08:35',
    location: '杭州市西湖区',
    desc: '快递员正在派件，请保持电话畅通，预计今日送达，快递员电话：138****6666'
  },
  {
    status: 'future', title: '已签收',
    icon: CheckCircle,
    time: '预计 2026-08-27 12:00',
    location: '杭州市西湖区',
    desc: '确认收货后，平台将放款给卖家，交易完成'
  }
]

// 收货地址
const delivery = {
  name: '张硬件',
  phone: '138 **** 8888',
  address: '浙江省杭州市西湖区文三路 369 号数码港大厦 A 座 1202 室'
}

// 复制运单号
const copied = ref(false)
const copyTracking = async () => {
  try {
    await navigator.clipboard.writeText(order.trackingNo)
    copied.value = true
    setTimeout(() => copied.value = false, 2000)
  } catch {
    alert('运单号：' + order.trackingNo)
  }
}

// 确认收货
const confirmReceipt = () => {
  if (confirm('确认已收到商品？确认后平台将放款给卖家，交易完成。')) {
    alert('已确认收货，感谢使用硬件侠')
    router.push('/orders')
  }
}
</script>

<template>
  <div class="mx-auto max-w-3xl px-4 pb-40 pt-6 md:px-6 md:pt-8 space-y-5">
    <!-- 页面标题 + 返回入口 -->
    <div class="flex items-center gap-3">
      <button @click="router.back()"
        class="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-brand-line bg-brand-surface text-brand-ink-2 transition-colors hover:border-brand-primary hover:text-brand-primary"
        aria-label="返回订单中心">
        <ArrowLeft class="h-4 w-4" />
      </button>
      <div>
        <h1 class="text-2xl font-bold text-brand-ink">物流跟踪</h1>
        <p class="text-sm text-brand-ink-2">订单号 <span class="font-mono text-brand-ink">{{ order.orderNo }}</span></p>
      </div>
    </div>

    <!-- 订单商品卡片 -->
    <article class="rounded-2xl border border-brand-line bg-brand-surface p-4 md:p-5 transition hover:border-brand-primary/40">
      <div class="flex items-start gap-4">
        <img :src="order.product.cover" :alt="order.product.title"
          class="h-20 w-20 md:h-24 md:w-24 rounded-lg object-cover shrink-0 bg-brand-surface-2" />
        <div class="min-w-0 flex-1">
          <h2 class="font-medium text-brand-ink line-clamp-2">{{ order.product.title }}</h2>
          <p class="mt-1 text-sm text-brand-ink-2">卖家：{{ order.product.seller }}</p>
          <p class="mt-1 text-sm text-brand-ink-3">
            单价 <span class="font-mono text-brand-primary">{{ fmt(order.product.unitPrice) }}</span> × {{ order.product.qty }}
          </p>
          <p class="mt-2 font-mono text-lg font-bold text-brand-primary">{{ fmt(order.product.total) }}</p>
        </div>
      </div>
    </article>

    <!-- 物流承运商信息 -->
    <section class="rounded-2xl border border-brand-line bg-brand-surface p-4 md:p-5">
      <div class="flex items-center gap-2 mb-4">
        <Truck class="h-5 w-5 text-brand-primary" />
        <h2 class="font-semibold text-brand-ink">物流信息</h2>
      </div>
      <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p class="text-sm text-brand-ink-2">承运商</p>
          <p class="text-base font-medium text-brand-ink">{{ order.carrier }}</p>
        </div>
        <div class="min-w-0">
          <p class="text-sm text-brand-ink-2">运单号</p>
          <div class="flex items-center gap-2">
            <span class="font-mono text-base text-brand-ink">{{ order.trackingNo }}</span>
            <button @click="copyTracking"
              class="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-brand-primary bg-brand-primary/12 border border-brand-primary/25 transition-colors hover:bg-brand-primary/18">
              <Copy v-if="!copied" class="h-3.5 w-3.5" />
              <CheckCircle v-else class="h-3.5 w-3.5" />
              {{ copied ? '已复制' : '复制' }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 物流时间轴 -->
    <section class="rounded-2xl border border-brand-line bg-brand-surface p-4 md:p-5">
      <h2 class="mb-5 font-semibold text-brand-ink">运输进度</h2>
      <div class="relative pl-2">
        <div v-for="(node, i) in timeline" :key="i" class="relative flex gap-4 pb-8 last:pb-0">
          <!-- 连线 -->
          <div v-if="i !== timeline.length - 1"
            :class="['absolute left-[15px] top-[30px] w-0.5 bottom-0',
                     node.status === 'completed' ? 'bg-brand-success' :
                     node.status === 'current'   ? 'bg-gradient-to-b from-brand-primary to-brand-line' : 'bg-brand-line']"></div>
          <!-- 图标节点 -->
          <div :class="['relative z-10 flex h-[30px] w-[30px] items-center justify-center rounded-full shrink-0',
            node.status === 'completed' ? 'bg-brand-success text-brand-primary-ink ring-4 ring-brand-success/20' :
            node.status === 'current'   ? 'bg-brand-primary text-brand-primary-ink ring-4 ring-brand-primary/20 animate-pulse' :
                                          'bg-brand-surface-2 text-brand-ink-3 ring-2 ring-brand-line']">
            <component :is="node.icon" class="h-3.5 w-3.5" />
          </div>
          <!-- 内容 -->
          <div class="flex-1 min-w-0 pt-0.5">
            <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-1">
              <h3 :class="['font-medium', node.status === 'future' ? 'text-brand-ink-3' : 'text-brand-ink']">{{ node.title }}</h3>
              <div class="flex items-center gap-2 text-xs">
                <Clock class="h-3 w-3 text-brand-ink-3" />
                <span :class="node.status === 'future' ? 'text-brand-ink-3' : 'text-brand-ink-2'">{{ node.time }}</span>
                <span v-if="node.location" class="text-brand-ink-3">· {{ node.location }}</span>
              </div>
            </div>
            <p :class="['mt-1 text-sm leading-relaxed',
              node.status === 'future' ? 'text-brand-ink-3' : 'text-brand-ink-2']">{{ node.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 配送信息 -->
    <section class="rounded-2xl border border-brand-line bg-brand-surface p-4 md:p-5">
      <h2 class="mb-4 font-semibold text-brand-ink">配送信息</h2>
      <div class="space-y-3">
        <div class="flex items-start gap-3">
          <User class="mt-0.5 h-4 w-4 shrink-0 text-brand-ink-3" />
          <div>
            <p class="text-sm text-brand-ink-2">收件人</p>
            <p class="text-brand-ink">{{ delivery.name }}</p>
          </div>
        </div>
        <div class="flex items-start gap-3">
          <Phone class="mt-0.5 h-4 w-4 shrink-0 text-brand-ink-3" />
          <div>
            <p class="text-sm text-brand-ink-2">联系电话</p>
            <p class="text-brand-ink font-mono">{{ delivery.phone }}</p>
          </div>
        </div>
        <div class="flex items-start gap-3">
          <MapPin class="mt-0.5 h-4 w-4 shrink-0 text-brand-ink-3" />
          <div>
            <p class="text-sm text-brand-ink-2">收货地址</p>
            <p class="text-brand-ink">{{ delivery.address }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 底部操作栏 -->
    <div class="fixed bottom-0 left-0 right-0 z-30 border-t border-brand-line bg-brand-surface/95 backdrop-blur px-4 py-4 md:py-5">
      <div class="mx-auto max-w-3xl flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-end">
        <button @click="router.push('/messages')"
          class="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-brand-line bg-brand-surface px-4 text-sm font-medium text-brand-ink transition hover:border-brand-primary hover:text-brand-primary">
          <Phone class="h-3.5 w-3.5" /> 联系快递员
        </button>
        <button @click="confirmReceipt"
          class="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 text-sm font-semibold text-brand-primary-ink transition hover:bg-brand-primary/90">
          <CheckCircle class="h-3.5 w-3.5" /> 确认收货
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@reference "../style.css";
.line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>
