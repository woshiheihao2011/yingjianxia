<template>
  <div class="min-h-screen bg-background text-foreground">
    <!-- 页面标题 + 时间范围 -->
    <section class="py-8 border-b border-border">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 class="text-2xl md:text-3xl font-bold">卖家数据中心</h1>
            <p class="text-sm text-muted-foreground mt-1">实时掌握店铺经营动态，用数据驱动成交增长</p>
          </div>
          <div class="flex flex-wrap items-center gap-2" role="group">
            <button
              v-for="r in ranges"
              :key="r.value"
              type="button"
              class="range-btn"
              :class="{ 'active': range === r.value }"
              @click="range = r.value"
            >{{ r.label }}</button>
          </div>
        </div>
      </div>
    </section>

    <!-- 核心指标 -->
    <section class="py-8">
      <div class="max-w-7xl mx-auto px-4">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          <div v-for="m in metrics" :key="m.label" class="metric-card">
            <div class="flex items-center justify-between">
              <span class="text-sm text-muted-foreground">{{ m.label }}</span>
              <component :is="m.icon" class="w-4 h-4 text-primary" />
            </div>
            <div class="text-2xl font-bold">{{ m.value }}</div>
            <div class="flex items-center gap-1 text-xs" :class="m.up ? 'text-emerald-400' : 'text-rose-400'">
              <TrendingUp v-if="m.up" class="w-3.5 h-3.5" />
              <TrendingDown v-else class="w-3.5 h-3.5" />
              <span>{{ m.up ? '+' : '' }}{{ m.change }}%</span>
              <span class="text-muted-foreground ml-1">环比</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 趋势图表 -->
    <section class="py-8">
      <div class="max-w-7xl mx-auto px-4">
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- 趋势图 -->
          <div class="chart-card lg:col-span-2">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-lg font-bold">近 30 天成交额 / 订单趋势</h2>
              <div class="flex items-center gap-4 text-xs">
                <span class="flex items-center gap-1.5 text-muted-foreground"><span class="w-3 h-1 rounded-full bg-primary"></span>成交额</span>
                <span class="flex items-center gap-1.5 text-muted-foreground"><span class="w-3 h-1 rounded-full bg-blue-500"></span>订单数</span>
              </div>
            </div>
            <div class="relative w-full aspect-[16/7]">
              <svg viewBox="0 0 800 240" class="w-full h-full" preserveAspectRatio="none">
                <defs>
                  <linearGradient id="revGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#00D4AA" stop-opacity="0.25"/>
                    <stop offset="100%" stop-color="#00D4AA" stop-opacity="0"/>
                  </linearGradient>
                  <linearGradient id="ordGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#3B82F6" stop-opacity="0.18"/>
                    <stop offset="100%" stop-color="#3B82F6" stop-opacity="0"/>
                  </linearGradient>
                </defs>
                <g stroke="#2A2D35" stroke-width="1">
                  <line x1="0" y1="200" x2="800" y2="200"/>
                  <line x1="0" y1="150" x2="800" y2="150"/>
                  <line x1="0" y1="100" x2="800" y2="100"/>
                  <line x1="0" y1="50" x2="800" y2="50"/>
                </g>
                <path d="M0,180 L26,172 L53,160 L80,155 L106,140 L133,132 L160,125 L186,118 L213,110 L240,100 L266,95 L293,90 L320,82 L346,78 L373,70 L400,65 L426,58 L453,50 L480,48 L506,55 L533,62 L560,58 L586,50 L613,45 L640,40 L666,35 L693,30 L720,25 L746,22 L773,18 L800,15 L800,200 L0,200 Z" fill="url(#revGradient)"/>
                <path d="M0,180 L26,172 L53,160 L80,155 L106,140 L133,132 L160,125 L186,118 L213,110 L240,100 L266,95 L293,90 L320,82 L346,78 L373,70 L400,65 L426,58 L453,50 L480,48 L506,55 L533,62 L560,58 L586,50 L613,45 L640,40 L666,35 L693,30 L720,25 L746,22 L773,18 L800,15" fill="none" stroke="#00D4AA" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M0,195 L26,188 L53,182 L80,178 L106,172 L133,168 L160,162 L186,158 L213,150 L240,145 L266,140 L293,135 L320,130 L346,128 L373,122 L400,118 L426,112 L453,108 L480,105 L506,110 L533,115 L560,112 L586,108 L613,104 L640,100 L666,96 L693,92 L720,90 L746,86 L773,84 L800,80 L800,200 L0,200 Z" fill="url(#ordGradient)"/>
                <path d="M0,195 L26,188 L53,182 L80,178 L106,172 L133,168 L160,162 L186,158 L213,150 L240,145 L266,140 L293,135 L320,130 L346,128 L373,122 L400,118 L426,112 L453,108 L480,105 L506,110 L533,115 L560,112 L586,108 L613,104 L640,100 L666,96 L693,92 L720,90 L746,86 L773,84 L800,80" fill="none" stroke="#3B82F6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <g fill="#6B7280" font-size="10" font-family="Inter, system-ui, sans-serif">
                  <text x="0" y="218">7/30</text>
                  <text x="196" y="218">8/08</text>
                  <text x="392" y="218">8/18</text>
                  <text x="588" y="218">8/24</text>
                  <text x="764" y="218">8/29</text>
                </g>
              </svg>
            </div>
          </div>

          <!-- 热销商品 -->
          <div class="chart-card">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-lg font-bold">热销商品排行</h2>
              <RouterLink to="/listings" class="text-xs text-primary hover:underline">管理商品</RouterLink>
            </div>
            <div class="space-y-4">
              <div v-for="(p, idx) in topProducts" :key="idx" class="flex items-center gap-3">
                <span
                  class="w-5 h-5 rounded text-xs font-bold flex items-center justify-center"
                  :class="idx === 0 ? 'bg-primary text-primary-foreground' : idx < 3 ? 'bg-surface-3 text-foreground' : 'text-muted-foreground border border-border'"
                >{{ idx + 1 }}</span>
                <div class="w-12 h-12 rounded border border-border bg-surface-2 flex items-center justify-center text-primary">
                  <component :is="p.icon" class="w-6 h-6" />
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium truncate">{{ p.name }}</p>
                  <p class="text-xs text-muted-foreground">销量 {{ p.sales }} · ¥{{ p.revenue }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 流量来源与买家画像 -->
    <section class="py-8">
      <div class="max-w-7xl mx-auto px-4">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- 流量来源 -->
          <div class="chart-card">
            <h2 class="text-lg font-bold mb-6">流量来源分析</h2>
            <div class="space-y-5">
              <div v-for="src in trafficSources" :key="src.label">
                <div class="flex items-center justify-between text-sm mb-2">
                  <span class="flex items-center gap-2"><component :is="src.icon" class="w-4 h-4" :style="{ color: src.color }" />{{ src.label }}</span>
                  <span class="text-muted-foreground">{{ src.value }}%</span>
                </div>
                <div class="progress-track"><div class="progress-fill" :style="{ width: src.value + '%', background: src.color }"></div></div>
              </div>
            </div>
          </div>

          <!-- 买家画像 -->
          <div class="chart-card">
            <h2 class="text-lg font-bold mb-6">买家画像简览</h2>
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-6">
              <div class="text-center">
                <span class="text-sm text-muted-foreground block mb-3">性别分布</span>
                <svg viewBox="0 0 64 64" class="w-24 h-24 mx-auto">
                  <circle cx="32" cy="32" r="28" fill="none" stroke="#252830" stroke-width="10"/>
                  <circle cx="32" cy="32" r="28" fill="none" stroke="#00D4AA" stroke-width="10" stroke-dasharray="88 176" transform="rotate(-90 32 32)"/>
                  <circle cx="32" cy="32" r="28" fill="none" stroke="#3B82F6" stroke-width="10" stroke-dasharray="62 176" stroke-dashoffset="-88" transform="rotate(-90 32 32)"/>
                  <circle cx="32" cy="32" r="28" fill="none" stroke="#F59E0B" stroke-width="10" stroke-dasharray="26 176" stroke-dashoffset="-150" transform="rotate(-90 32 32)"/>
                </svg>
                <div class="mt-3 flex flex-wrap justify-center gap-2 text-xs">
                  <span><span class="inline-block w-2 h-2 rounded-full bg-primary mr-1"></span>男 50%</span>
                  <span><span class="inline-block w-2 h-2 rounded-full bg-blue-500 mr-1"></span>女 35%</span>
                  <span><span class="inline-block w-2 h-2 rounded-full bg-amber-500 mr-1"></span>未知 15%</span>
                </div>
              </div>
              <div>
                <span class="text-sm text-muted-foreground block mb-3">热门地域</span>
                <div class="space-y-3 text-sm">
                  <div v-for="r in regions" :key="r.name">
                    <div class="flex items-center justify-between mb-1">
                      <span>{{ r.name }}</span>
                      <span class="text-muted-foreground">{{ r.value }}%</span>
                    </div>
                    <div class="progress-track"><div class="progress-fill" :style="{ width: r.value + '%' }"></div></div>
                  </div>
                </div>
              </div>
              <div>
                <span class="text-sm text-muted-foreground block mb-3">常用设备</span>
                <div class="space-y-3">
                  <div v-for="d in devices" :key="d.label" class="flex items-center justify-between p-3 rounded-md bg-background border border-border">
                    <span class="flex items-center gap-2 text-sm"><component :is="d.icon" class="w-4 h-4" :style="{ color: d.color }" />{{ d.label }}</span>
                    <span class="text-sm font-medium">{{ d.value }}%</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 数据导出 -->
    <section class="py-8">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex flex-col sm:flex-row items-center justify-between gap-4 p-6 rounded-lg border border-border bg-card">
          <div class="flex items-start gap-3">
            <FileSpreadsheet class="w-8 h-8 text-primary shrink-0" />
            <div>
              <h3 class="font-semibold">导出经营报表</h3>
              <p class="text-sm text-muted-foreground mt-1">下载当前时间范围内的订单、流量与商品数据，支持 CSV / Excel 格式。</p>
            </div>
          </div>
          <button type="button" class="btn-primary shrink-0" @click="toast = true">
            <Download class="w-4 h-4" />
            导出数据
          </button>
        </div>
      </div>
    </section>

    <!-- Toast -->
    <Transition name="fade">
      <div v-if="toast" class="fixed bottom-8 left-1/2 -translate-x-1/2 z-50 px-5 py-3 rounded-lg bg-card border border-primary text-sm shadow-lg">
        <Check class="w-4 h-4 inline mr-2 text-primary" /> 已生成报表，即将开始下载
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  Banknote, ShoppingBag, Eye, Users, Percent, RotateCcw,
  TrendingUp, TrendingDown, Search, LayoutGrid, Sparkles, Link,
  Monitor, Smartphone, Tablet, FileSpreadsheet, Download, Check,
  Cpu, Gauge, MemoryStick, HardDrive, CircuitBoard
} from 'lucide-vue-next'

const range = ref('30')
const toast = ref(false)

const ranges = [
  { label: '近 7 天', value: '7' },
  { label: '近 30 天', value: '30' },
  { label: '近 90 天', value: '90' }
]

const metrics = [
  { label: '成交额', value: '¥128,450', icon: Banknote, up: true, change: 12.5 },
  { label: '订单数', value: '386', icon: ShoppingBag, up: true, change: 8.2 },
  { label: '浏览量', value: '24.3k', icon: Eye, up: true, change: 15.7 },
  { label: '访客数', value: '8,920', icon: Users, up: true, change: 6.4 },
  { label: '转化率', value: '4.33%', icon: Percent, up: true, change: 0.4 },
  { label: '退款率', value: '1.81%', icon: RotateCcw, up: false, change: 0.3 }
]

const topProducts = [
  { name: '七彩虹 RTX 4070 战斧豪华版', sales: 92, revenue: '338,560', icon: Gauge },
  { name: 'Intel Core i5-13600K 盒装', sales: 74, revenue: '92,500', icon: Cpu },
  { name: '芝奇 DDR5 6000 32GB 套装', sales: 58, revenue: '35,960', icon: MemoryStick },
  { name: '三星 980 Pro 1TB NVMe', sales: 45, revenue: '21,150', icon: HardDrive },
  { name: '华硕 TUF B760M-PLUS', sales: 31, revenue: '10,540', icon: CircuitBoard }
]

const trafficSources = [
  { label: '搜索', value: 42, icon: Search, color: '#00D4AA' },
  { label: '分类浏览', value: 28, icon: LayoutGrid, color: '#3B82F6' },
  { label: '推荐', value: 18, icon: Sparkles, color: '#F59E0B' },
  { label: 'Direct', value: 12, icon: Link, color: '#00D4AA' }
]

const regions = [
  { name: '广东', value: 22 },
  { name: '江苏', value: 16 },
  { name: '浙江', value: 14 }
]

const devices = [
  { label: '桌面端', value: 62, icon: Monitor, color: '#00D4AA' },
  { label: '移动端', value: 35, icon: Smartphone, color: '#3B82F6' },
  { label: '平板', value: 3, icon: Tablet, color: '#F59E0B' }
]
</script>

<style scoped>
@reference "../style.css";
.range-btn {
  @apply inline-flex items-center justify-center h-9 px-4 rounded-lg text-sm font-medium text-muted-foreground bg-surface-2 border border-border transition;
}
.range-btn:hover {
  @apply text-foreground bg-surface-3;
}
.range-btn.active {
  @apply text-primary-foreground bg-primary border-primary;
}
.metric-card {
  @apply bg-card border border-border rounded-lg p-5 flex flex-col gap-3 transition;
}
.metric-card:hover {
  @apply border-primary;
}
.chart-card {
  @apply bg-card border border-border rounded-lg p-5;
}
.progress-track {
  @apply h-2 rounded-full bg-surface-3 overflow-hidden;
}
.progress-fill {
  @apply h-full rounded-full bg-primary;
}
.btn-primary {
  @apply inline-flex items-center justify-center gap-2 h-11 px-6 rounded-lg bg-primary text-primary-foreground font-semibold transition hover:bg-[var(--brand-primary-hover)];
}
.fade-enter-active, .fade-leave-active {
  transition: opacity .3s, transform .3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
}
</style>
