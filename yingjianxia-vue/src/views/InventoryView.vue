<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertTriangle, Bell, Package, Clock, TrendingDown, Plus, RefreshCw,
  Filter, Search, Edit2, BarChart3, ArrowUpRight, Zap, FileText,
  Eye, Truck, Upload, AlertCircle, CheckCircle2
} from 'lucide-vue-next'

const router = useRouter()
const kw = ref('')
const activeTab = ref('low')
const selectedIds = ref([])

const overview = ref([
  { label: '库存预警商品', value: 12, color: 'text-brand-error bg-brand-error/15 border-brand-error/30', icon: AlertTriangle },
  { label: '即将缺货（≤3）', value: 7, color: 'text-brand-warning bg-brand-warning/15 border-brand-warning/30', icon: Bell },
  { label: '完全缺货 (=0)', value: 5, color: 'text-brand-error bg-brand-error/15 border-brand-error/30', icon: Package },
  { label: '滞销商品 (>60天0动销)', value: 23, color: 'text-brand-info bg-brand-info/15 border-brand-info/30', icon: Clock },
  { label: '近7天缺货损失', value: '¥14,866', color: 'text-brand-warning bg-brand-warning/15 border-brand-warning/30', icon: TrendingDown },
  { label: '本月补货建议', value: '47 件', color: 'text-brand-success bg-brand-success/15 border-brand-success/30', icon: Plus }
])

const lowStock = ref([
  { id: 4001, title: '三星 990 PRO 2TB SSD 国行带票 质保至2029', cover: 'https://placehold.co/160x160/15161A/00D4AA?text=990PRO', price: 1299, sku: 'YJX-SSD-108', stock: 0, threshold: 5, sold30d: 62, turnover: 23, replenishCost: 77940, status: 'urgent' },
  { id: 4002, title: '拯救者 R9000P 2024 RTX4070 16G 整机', cover: 'https://placehold.co/160x160/15161A/00D4AA?text=R9000P', price: 8688, sku: 'YJX-LAP-002', stock: 0, threshold: 2, sold30d: 13, turnover: 61, replenishCost: 104256, status: 'urgent' },
  { id: 4003, title: 'RTX 3080 Ti 12G 公版 FE 成色95新', cover: 'https://placehold.co/160x160/15161A/00D4AA?text=3080Ti', price: 4588, sku: 'YJX-GPU-098', stock: 1, threshold: 3, sold30d: 8, turnover: 32, replenishCost: 9176, status: 'danger' },
  { id: 4004, title: '追风者 P600S 中塔机箱 侧透静音 黑色', cover: 'https://placehold.co/160x160/15161A/00D4AA?text=P600S', price: 699, sku: 'YJX-CASE-031', stock: 1, threshold: 5, sold30d: 9, turnover: 28, replenishCost: 2796, status: 'danger' },
  { id: 4005, title: '金士顿 FURY DDR5 6000 32GB 套条', cover: 'https://placehold.co/160x160/15161A/00D4AA?text=DDR5', price: 899, sku: 'YJX-RAM-044', stock: 2, threshold: 8, sold30d: 56, turnover: 15, replenishCost: 5394, status: 'warning' }
])

const deadStock = ref([
  { id: 5001, title: 'AMD Ryzen 7 2700X + 华硕 B450 套装', cover: 'https://placehold.co/160x160/15161A/00D4AA?text=2700X', price: 988, sku: 'YJX-CPU-SET03', stock: 8, listedAt: '2024-09-12', daysOnShelf: 125, sold30d: 0, cost: 820, suggestion: '建议降价 20% 或搭售散热' },
  { id: 5002, title: 'NVIDIA GTX 1660 Super 6G 显卡', cover: 'https://placehold.co/160x160/15161A/00D4AA?text=1660S', price: 699, sku: 'YJX-GPU-115', stock: 5, listedAt: '2024-10-03', daysOnShelf: 104, sold30d: 0, cost: 580, suggestion: '建议下架或转粉丝专享' },
  { id: 5003, title: 'Creative Sound Blaster AE-9 声卡', cover: 'https://placehold.co/160x160/15161A/00D4AA?text=AE9', price: 1899, sku: 'YJX-OTH-009', stock: 2, listedAt: '2024-08-25', daysOnShelf: 143, sold30d: 0, cost: 1600, suggestion: '建议清仓拍卖' }
])

const inboundList = ref([
  { id: 6001, title: '七彩虹 RTX 4060 8G 战斧 x5', cost: 14995, eta: '2025-01-18 14:00', status: 'shipping', statusName: '运输中' },
  { id: 6002, title: '利民 PA120 散热器 x20', cost: 5380, eta: '2025-01-19 10:30', status: 'delivering', statusName: '配送中' },
  { id: 6003, title: 'Intel i5-14600K 散片 x10', cost: 17990, eta: '2025-01-22 18:00', status: 'pending', statusName: '待出库' }
])

const lowStockFiltered = computed(() => lowStock.value.filter(p => !kw.value || p.title.includes(kw.value) || p.sku.includes(kw.value)))
const deadStockFiltered = computed(() => deadStock.value.filter(p => !kw.value || p.title.includes(kw.value)))

const totalReplenishCost = computed(() => lowStock.value.reduce((s, p) => s + p.replenishCost, 0))

function statusBadge(status) {
  return {
    urgent: 'bg-brand-error/15 text-brand-error border-brand-error/30',
    danger: 'bg-brand-warning/15 text-brand-warning border-brand-warning/30',
    warning: 'bg-brand-info/15 text-brand-info border-brand-info/30'
  }[status]
}
function statusText(status) {
  return { urgent: '紧急补货', danger: '库存告急', warning: '库存偏低' }[status]
}
</script>

<template>
  <div class="container-app space-y-6">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
      <div>
        <div class="text-xs text-brand-ink-3">卖家中心 / 库存管理</div>
        <h1 class="text-2xl font-bold text-brand-ink mt-1 flex items-center gap-2"><Package class="w-6 h-6 text-brand-primary" />库存预警中心</h1>
        <p class="text-sm text-brand-ink-3 mt-1">智能监控商品库存动销，帮助你高效补货和清理滞销品</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <button class="px-4 h-10 rounded-lg bg-brand-surface border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
          <Upload class="w-4 h-4" />批量导入库存
        </button>
        <button class="px-4 h-10 rounded-lg bg-brand-surface border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
          <FileText class="w-4 h-4" />导出库存表
        </button>
        <button class="px-4 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition flex items-center gap-2">
          <Plus class="w-4 h-4" />创建补货单
        </button>
      </div>
    </div>

    <!-- 概览卡 -->
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 md:gap-4">
      <div v-for="s in overview" :key="s.label" class="p-4 rounded-xl bg-brand-surface border border-brand-line hover:border-brand-primary/30 transition">
        <div class="flex items-center justify-between mb-3">
          <div :class="['w-9 h-9 rounded-lg border flex items-center justify-center', s.color]">
            <component :is="s.icon" class="w-4 h-4" />
          </div>
          <ArrowUpRight class="w-3.5 h-3.5 text-brand-ink-3" />
        </div>
        <div class="text-xl md:text-2xl font-black text-brand-ink">{{ s.value }}</div>
        <div class="text-[11px] text-brand-ink-3 mt-1 leading-snug">{{ s.label }}</div>
      </div>
    </div>

    <!-- 紧急提示条 -->
    <div class="rounded-xl p-5 bg-gradient-to-r from-brand-error/15 via-brand-warning/10 to-transparent border border-brand-error/30 flex flex-col md:flex-row md:items-center gap-4">
      <div class="flex items-start gap-3 flex-1">
        <div class="w-10 h-10 rounded-lg bg-brand-error/15 border border-brand-error/30 flex items-center justify-center shrink-0 mt-0.5">
          <AlertCircle class="w-5 h-5 text-brand-error" />
        </div>
        <div>
          <div class="font-bold text-brand-ink">2 件核心爆款已完全断货</div>
          <div class="text-sm text-brand-ink-2 mt-1">
            根据近 30 天日均销量推算，本周因缺货造成的销售额损失约
            <span class="text-brand-error font-semibold">¥14,866</span>，建议立即补货
            <b class="text-brand-warning">三星 990 PRO 2TB</b> 和 <b class="text-brand-warning">R9000P 2024</b>。
          </div>
        </div>
      </div>
      <button class="px-5 h-10 rounded-lg bg-brand-error text-white text-sm font-semibold hover:bg-brand-error/90 transition whitespace-nowrap">一键生成补货单</button>
    </div>

    <!-- Tabs -->
    <div class="flex gap-1 p-1 rounded-xl bg-brand-surface border border-brand-line w-fit">
      <button v-for="t in [
        { id: 'low', name: '库存预警', count: lowStock.length },
        { id: 'dead', name: '滞销商品', count: deadStock.length },
        { id: 'inbound', name: '在途补货', count: inboundList.length },
        { id: 'history', name: '出入库流水' }
      ]" :key="t.id" @click="activeTab = t.id"
        :class="['px-5 py-2.5 rounded-lg text-sm font-medium transition',
                 activeTab === t.id ? 'bg-brand-primary text-brand-primary-ink shadow' : 'text-brand-ink-2 hover:text-brand-primary']">
        {{ t.name }}<span v-if="t.count != null" class="ml-1.5 text-[11px]" :class="activeTab === t.id ? 'text-brand-primary-ink/80' : 'text-brand-ink-3'">({{ t.count }})</span>
      </button>
    </div>

    <!-- 库存预警表 -->
    <section v-if="activeTab === 'low'" class="space-y-4">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="flex flex-wrap items-center gap-2">
          <div class="flex h-10 rounded-lg bg-brand-surface border border-brand-line focus-within:border-brand-primary transition max-w-md w-full md:w-80">
            <div class="pl-3 flex items-center text-brand-ink-3"><Search class="w-4 h-4" /></div>
            <input v-model="kw" placeholder="搜索商品名 / SKU..." class="flex-1 px-3 text-sm bg-transparent placeholder:text-brand-ink-3 focus:outline-none" />
          </div>
          <select class="h-10 px-3 rounded-lg bg-brand-surface border border-brand-line text-sm text-brand-ink-2 focus:outline-none focus:border-brand-primary">
            <option>全部分类</option><option>显卡</option><option>CPU</option><option>SSD</option><option>笔记本</option>
          </select>
          <button class="px-4 h-10 rounded-lg bg-brand-surface border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
            <Filter class="w-4 h-4" />补货成本 <ChevronDown class="w-3 h-3" />
          </button>
        </div>
        <div class="flex items-center gap-2 text-xs text-brand-ink-3">
          <span>预计补货总成本：<span class="text-brand-error font-bold text-base">¥{{ totalReplenishCost.toLocaleString() }}</span></span>
          <button class="px-4 h-9 rounded-lg border border-brand-line hover:border-brand-success/40 hover:text-brand-success transition flex items-center gap-1.5"><CheckCircle2 class="w-3.5 h-3.5" />全部确认补货</button>
        </div>
      </div>

      <div class="rounded-2xl bg-brand-surface border border-brand-line overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[960px] text-sm">
            <thead class="bg-brand-bg/60 border-b border-brand-line">
              <tr>
                <th class="w-10 px-4 py-3 text-left"><input type="checkbox" class="w-4 h-4 accent-brand-primary" /></th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-brand-ink-3">商品</th>
                <th class="text-right px-4 py-3 text-xs font-semibold text-brand-ink-3 w-20">售价</th>
                <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-28">当前库存 / 阈值</th>
                <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-24">30天销量</th>
                <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-24">周转天数</th>
                <th class="text-right px-4 py-3 text-xs font-semibold text-brand-ink-3 w-28">补货成本估算</th>
                <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-24">预警级别</th>
                <th class="text-right px-4 py-3 text-xs font-semibold text-brand-ink-3 w-48 shrink-0">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in lowStockFiltered" :key="p.id" class="border-t border-brand-line-subtle hover:bg-brand-surface-2/60 transition">
                <td class="px-4 py-4"><input type="checkbox" :checked="selectedIds.includes(p.id)" class="w-4 h-4 accent-brand-primary" /></td>
                <td class="px-4 py-4">
                  <div class="flex items-start gap-3">
                    <div class="w-14 h-14 rounded-lg overflow-hidden bg-brand-bg border border-brand-line shrink-0"><img :src="p.cover" class="w-full h-full object-cover" /></div>
                    <div>
                      <div class="text-sm text-brand-ink line-clamp-2 leading-snug max-w-md">{{ p.title }}</div>
                      <div class="text-[11px] text-brand-ink-3 mt-1 font-mono">{{ p.sku }}</div>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-4 text-right font-bold text-brand-error">¥{{ p.price.toLocaleString() }}</td>
                <td class="px-4 py-4 text-center">
                  <div class="flex items-baseline justify-center gap-1">
                    <span :class="['font-black text-lg', p.stock === 0 ? 'text-brand-error' : p.stock <= p.threshold / 2 ? 'text-brand-warning' : 'text-brand-ink']">{{ p.stock }}</span>
                    <span class="text-xs text-brand-ink-3">/ {{ p.threshold }}</span>
                  </div>
                  <div class="mt-1 h-1.5 rounded-full bg-brand-line overflow-hidden w-28 mx-auto">
                    <div class="h-full rounded-full transition-all"
                         :class="p.stock / p.threshold > 0.5 ? 'bg-brand-success' : p.stock / p.threshold > 0.2 ? 'bg-brand-warning' : 'bg-brand-error'"
                         :style="{ width: Math.min(100, (p.stock / p.threshold) * 100) + '%' }"></div>
                  </div>
                </td>
                <td class="px-4 py-4 text-center font-semibold text-brand-ink">{{ p.sold30d }}</td>
                <td class="px-4 py-4 text-center text-brand-ink-2">{{ p.turnover }} <span class="text-[10px]">天</span></td>
                <td class="px-4 py-4 text-right font-bold text-brand-warning">¥{{ p.replenishCost.toLocaleString() }}</td>
                <td class="px-4 py-4 text-center">
                  <span :class="['inline-block px-2.5 py-1 rounded-full text-[11px] font-bold border', statusBadge(p.status)]">
                    <Bell class="w-3 h-3 inline mr-1 -mt-0.5" />{{ statusText(p.status) }}
                  </span>
                </td>
                <td class="px-4 py-4">
                  <div class="flex items-center justify-end gap-1.5">
                    <button class="px-3 h-8 rounded-md bg-brand-success/15 text-brand-success text-xs font-semibold hover:bg-brand-success hover:text-white transition flex items-center gap-1"><Plus class="w-3 h-3" />补货</button>
                    <button class="p-1.5 rounded-md hover:bg-brand-bg text-brand-ink-3 hover:text-brand-primary transition" title="编辑库存"><Edit2 class="w-3.5 h-3.5" /></button>
                    <button class="p-1.5 rounded-md hover:bg-brand-bg text-brand-ink-3 hover:text-brand-info transition" title="数据趋势"><BarChart3 class="w-3.5 h-3.5" /></button>
                    <button @click="router.push('/product/' + p.id)" class="p-1.5 rounded-md hover:bg-brand-bg text-brand-ink-3 hover:text-brand-primary transition" title="查看页面"><Eye class="w-3.5 h-3.5" /></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- AI 智能补货建议 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="p-5 rounded-xl bg-brand-info/10 border border-brand-info/30">
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-lg bg-brand-info/20 flex items-center justify-center shrink-0 mt-0.5">
              <Zap class="w-5 h-5 text-brand-info" />
            </div>
            <div class="text-sm text-brand-ink-2 leading-relaxed flex-1">
              <div class="font-bold text-brand-info mb-1">AI 补货建议</div>
              <p>下周预计销量高峰（学生期末+春节前），建议对 <b class="text-brand-ink">RTX 3080 Ti、三星 990 PRO 2TB、R9000P</b> 这 3 款爆款追加补货量至 <b class="text-brand-ink">日均销量 × 周转天数 × 1.3 安全系数</b>。</p>
            </div>
          </div>
        </div>
        <div class="p-5 rounded-xl bg-brand-warning/10 border border-brand-warning/30">
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-lg bg-brand-warning/20 flex items-center justify-center shrink-0 mt-0.5">
              <AlertTriangle class="w-5 h-5 text-brand-warning" />
            </div>
            <div class="text-sm text-brand-ink-2 leading-relaxed flex-1">
              <div class="font-bold text-brand-warning mb-1">滞销处理提醒</div>
              <p>检测到 <b class="text-brand-ink">23 件商品</b> 超过 60 天未成交，积压资金约 <b class="text-brand-ink">¥68,400</b>。建议查看"滞销商品"Tab，系统提供降价/搭售/清仓/捐赠 4 种处置策略。</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 滞销 -->
    <section v-if="activeTab === 'dead'" class="space-y-4">
      <div class="rounded-2xl bg-brand-surface border border-brand-line overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[900px] text-sm">
            <thead class="bg-brand-bg/60 border-b border-brand-line">
              <tr>
                <th class="text-left px-4 py-3 text-xs font-semibold text-brand-ink-3">商品</th>
                <th class="text-right px-4 py-3 text-xs font-semibold text-brand-ink-3 w-20">售价 / 成本</th>
                <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-24">当前库存</th>
                <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-28">上架天数</th>
                <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-28">30天销量</th>
                <th class="text-right px-4 py-3 text-xs font-semibold text-brand-ink-3 w-28">资金占用</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-brand-ink-3 w-48">AI 处置建议</th>
                <th class="text-right px-4 py-3 text-xs font-semibold text-brand-ink-3 w-44 shrink-0">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in deadStockFiltered" :key="p.id" class="border-t border-brand-line-subtle hover:bg-brand-surface-2/60 transition">
                <td class="px-4 py-4">
                  <div class="flex items-start gap-3">
                    <div class="w-14 h-14 rounded-lg overflow-hidden bg-brand-bg border border-brand-line shrink-0"><img :src="p.cover" class="w-full h-full object-cover" /></div>
                    <div>
                      <div class="text-sm text-brand-ink line-clamp-2 leading-snug max-w-md">{{ p.title }}</div>
                      <div class="text-[11px] text-brand-ink-3 mt-1 font-mono">{{ p.sku }}</div>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-4 text-right">
                  <div class="font-bold text-brand-error">¥{{ p.price.toLocaleString() }}</div>
                  <div class="text-[11px] text-brand-ink-3">成本 ¥{{ p.cost.toLocaleString() }}</div>
                </td>
                <td class="px-4 py-4 text-center text-brand-ink-2 font-semibold">{{ p.stock }}</td>
                <td class="px-4 py-4 text-center">
                  <div class="font-bold text-brand-warning">{{ p.daysOnShelf }}</div>
                  <div class="text-[10px] text-brand-ink-3">天</div>
                </td>
                <td class="px-4 py-4 text-center font-semibold text-brand-error">0</td>
                <td class="px-4 py-4 text-right font-bold text-brand-warning">¥{{ (p.cost * p.stock).toLocaleString() }}</td>
                <td class="px-4 py-4">
                  <div class="p-3 rounded-lg bg-brand-warning/10 border border-brand-warning/25 text-xs text-brand-ink-2 leading-relaxed">{{ p.suggestion }}</div>
                </td>
                <td class="px-4 py-4">
                  <div class="flex flex-wrap items-center justify-end gap-1.5">
                    <button class="px-3 h-7 rounded-md bg-brand-error/15 text-brand-error text-xs hover:bg-brand-error hover:text-white transition">降价</button>
                    <button class="px-3 h-7 rounded-md bg-brand-warning/15 text-brand-warning text-xs hover:bg-brand-warning hover:text-white transition">搭售</button>
                    <button class="px-3 h-7 rounded-md bg-brand-success/15 text-brand-success text-xs hover:bg-brand-success hover:text-white transition">清仓</button>
                    <button class="p-1.5 rounded-md hover:bg-brand-bg text-brand-ink-3 hover:text-brand-primary transition" title="查看详情"><Eye class="w-3.5 h-3.5" /></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- 在途 -->
    <section v-if="activeTab === 'inbound'" class="space-y-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div v-for="s in inboundList" :key="s.id" class="p-5 rounded-2xl bg-brand-surface border border-brand-line">
          <div class="flex items-center justify-between mb-4">
            <span :class="['px-2.5 py-1 rounded-full text-[11px] font-semibold border',
                          s.status === 'shipping' ? 'bg-brand-info/15 text-brand-info border-brand-info/30' :
                          s.status === 'delivering' ? 'bg-brand-warning/15 text-brand-warning border-brand-warning/30' :
                          'bg-brand-primary/15 text-brand-primary border-brand-primary/30']">
              <Truck class="w-3 h-3 inline mr-1 -mt-0.5" />{{ s.statusName }}
            </span>
            <span class="text-[11px] text-brand-ink-3">补货单 #{{ s.id }}</span>
          </div>
          <div class="font-semibold text-brand-ink text-base leading-snug mb-4">{{ s.title }}</div>
          <div class="space-y-2">
            <div class="flex justify-between text-xs">
              <span class="text-brand-ink-3">总采购价</span>
              <span class="font-semibold text-brand-ink">¥{{ s.cost.toLocaleString() }}</span>
            </div>
            <div class="flex justify-between text-xs">
              <span class="text-brand-ink-3 flex items-center gap-1"><Clock class="w-3 h-3" />预计到货</span>
              <span class="font-semibold text-brand-primary">{{ s.eta }}</span>
            </div>
            <button class="w-full h-9 mt-3 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center justify-center gap-1.5">
              <RefreshCw class="w-3 h-3" />跟踪物流
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 流水 -->
    <section v-if="activeTab === 'history'" class="rounded-2xl bg-brand-surface border border-brand-line p-12 text-center">
      <div class="w-16 h-16 mx-auto mb-4 rounded-2xl bg-brand-primary/15 flex items-center justify-center">
        <FileText class="w-8 h-8 text-brand-primary" />
      </div>
      <h3 class="font-bold text-brand-ink">出入库流水</h3>
      <p class="text-sm text-brand-ink-3 mt-2">请选择时间范围，系统将加载全部入库/出库操作明细</p>
      <div class="mt-5 flex items-center justify-center gap-2">
        <input type="date" class="h-10 px-3 rounded-lg bg-brand-bg border border-brand-line text-sm focus:outline-none focus:border-brand-primary" />
        <span class="text-brand-ink-3">至</span>
        <input type="date" class="h-10 px-3 rounded-lg bg-brand-bg border border-brand-line text-sm focus:outline-none focus:border-brand-primary" />
        <button class="px-5 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition flex items-center gap-1.5"><RefreshCw class="w-4 h-4" />生成报表</button>
      </div>
    </section>
  </div>
</template>
