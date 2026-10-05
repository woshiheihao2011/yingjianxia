<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Gift, Ticket, Star, Wallet, Clock, Calendar, Tag, ChevronRight,
  Search, Trophy, Zap, Award, ExternalLink, AlertCircle, CheckCircle2
} from 'lucide-vue-next'
import { getCouponsApi, getPointsSummaryApi, getPointsRecordsApi, signInApi, claimCouponApi } from '@/api'

const router = useRouter()
const activeTab = ref('available')
const historyTab = ref('used')
const kw = ref('')
const loading = ref(false)
const signedIn = ref(false)

const pointsSummary = ref({
  total: 0,
  available: 0,
  frozen: 0,
  monthlyGain: 0,
  level: 'LV.1 新手上路',
  levelProgress: 0,
  nextLevel: 'LV.2',
  nextLevelAt: 100
})

// 可领取优惠券（来自 /coupons）
const availableCoupons = ref([])
// 我的优惠券（来自 /coupons/mine，接口异常时空）
const myCoupons = ref([])

const usedHistory = ref([])
const expiredHistory = ref([])
const pointsHistory = ref([])

const tasks = ref([
  { id: 'T1', title: '每日签到', point: 10, done: false },
  { id: 'T2', title: '浏览 5 件商品', point: 5, done: false },
  { id: 'T3', title: '完成 1 笔订单', point: 200, done: false },
  { id: 'T4', title: '发布 1 条带图评价', point: 30, done: false },
  { id: 'T5', title: '分享商品到社交平台', point: 15, done: false }
])

const redeemCoupons = ref([
  { id: 'R1', title: '满 500-50 全品类券', cost: 500, stock: 283 },
  { id: 'R2', title: '满 2000-150 大额券', cost: 1200, stock: 61 },
  { id: 'R3', title: '满 5000-400 超级券', cost: 3200, stock: 12 }
])

const filteredAvailable = computed(() =>
  availableCoupons.value.filter(c => !kw.value || c.title.includes(kw.value) || c.desc.includes(kw.value))
)

const monthSignIn = Array.from({ length: 28 }, (_, i) => ({
  day: i + 1, done: i < 0, today: i === new Date().getDate() - 1
}))

function levelByPoints(total) {
  const levels = [
    { min: 0, name: 'LV.1 新手上路', next: 'LV.2', nextAt: 100 },
    { min: 100, name: 'LV.2 硬件爱好者', next: 'LV.3', nextAt: 500 },
    { min: 500, name: 'LV.3 资深玩家', next: 'LV.4', nextAt: 2000 },
    { min: 2000, name: 'LV.4 硬核玩家', next: 'LV.5', nextAt: 5000 },
    { min: 5000, name: 'LV.5 硬件极客', next: 'LV.6', nextAt: 15000 },
    { min: 15000, name: 'LV.6 传说级', next: 'MAX', nextAt: 15000 }
  ]
  for (let i = levels.length - 1; i >= 0; i--) {
    if (total >= levels[i].min) {
      const lv = levels[i]
      const progress = lv.nextAt > lv.min ? Math.min(100, Math.round((total - lv.min) / (lv.nextAt - lv.min) * 100)) : 100
      return { ...lv, progress }
    }
  }
  return { name: 'LV.1 新手上路', next: 'LV.2', nextAt: 100, progress: 0 }
}

async function claimCoupon(c) {
  if (c.claimed) return
  try {
    await claimCouponApi(c.id)
    c.claimed = true
    alert('领取成功')
  } catch (e) {
    alert(e.message || '领取失败')
  }
}

async function handleSignIn() {
  if (signedIn.value) return
  try {
    await signInApi()
    signedIn.value = true
    alert('签到成功 +10 积分')
    loadAll()
  } catch (e) {
    alert(e.message || '签到失败')
  }
}

// 映射优惠券
function mapCoupon(c) {
  return {
    id: c.id,
    title: c.name || c.title || '优惠券',
    desc: c.description || c.desc || '',
    amount: c.amount || c.discountAmount || 0,
    threshold: c.threshold || c.minAmount || 0,
    expiry: (c.expireAt || c.endAt || '').slice(0, 10),
    scope: c.scope || c.categoryName || '全场通用',
    hot: c.hot || false,
    claimed: false
  }
}

async function loadAll() {
  loading.value = true
  try {
    const [couponsResp, pointsResp, myCouponsResp, recordsResp] = await Promise.allSettled([
      getCouponsApi({ pageNum: 1, pageSize: 50 }),
      getPointsSummaryApi(),
      // coupons/mine 当前 500，单独处理
      (async () => { try { return await getCouponsApi({ mine: true }) } catch { return [] } })(),
      getPointsRecordsApi({ pageNum: 1, pageSize: 20 })
    ])

    // 可领取优惠券
    if (couponsResp.status === 'fulfilled') {
      const data = couponsResp.value
      const list = Array.isArray(data) ? data : (data?.records || data?.list || [])
      availableCoupons.value = list.map(mapCoupon)
    }

    // 积分汇总
    if (pointsResp.status === 'fulfilled' && pointsResp.value) {
      const p = pointsResp.value
      const lv = levelByPoints(p.totalPoints || 0)
      pointsSummary.value = {
        total: p.totalPoints || 0,
        available: p.availablePoints || 0,
        frozen: (p.totalPoints || 0) - (p.availablePoints || 0) - (p.usedPoints || 0),
        monthlyGain: 0,
        level: lv.name,
        levelProgress: lv.progress,
        nextLevel: lv.next,
        nextLevelAt: lv.nextAt
      }
    }

    // 我的优惠券（接口异常时空）
    if (myCouponsResp.status === 'fulfilled') {
      const data = myCouponsResp.value
      const list = Array.isArray(data) ? data : (data?.records || data?.list || [])
      myCoupons.value = list.map(mapCoupon).map(c => ({ ...c, claimed: true }))
    }

    // 积分记录（后端返回结构异常，降级）
    if (recordsResp.status === 'fulfilled' && recordsResp.value) {
      const data = recordsResp.value
      const list = Array.isArray(data) ? data : (data?.records || data?.list || [])
      pointsHistory.value = list.map(r => ({
        date: (r.createdAt || '').slice(0, 10),
        title: r.description || r.remark || '积分变动',
        type: (r.points || r.amount || 0) >= 0 ? 'in' : 'out',
        amount: Math.abs(r.points || r.amount || 0)
      }))
    }
  } catch (e) {
    console.error('加载优惠券/积分失败', e)
  } finally {
    loading.value = false
  }
}

const colorByAmount = (amount) => amount >= 150 ? 'from-brand-error to-[#B91C1C]' : amount >= 80 ? 'from-brand-warning to-[#B45309]' : 'from-brand-primary to-[#00B894]'

onMounted(() => {
  loadAll()
})
</script>

<template>
  <div class="container-app space-y-6">
    <div>
      <h1 class="text-2xl md:text-3xl font-bold text-brand-ink flex items-center gap-3">
        <Gift class="w-7 h-7 text-brand-primary" />优惠券 & 积分中心
      </h1>
      <p class="text-sm text-brand-ink-3 mt-1">领券省更多，积分换好物，每日签到还能领奖励</p>
    </div>

    <!-- 总览卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
      <div class="md:col-span-2 rounded-2xl p-6 md:p-8 bg-gradient-to-br from-brand-warning/20 via-brand-surface to-brand-bg border border-brand-line relative overflow-hidden">
        <div class="absolute right-0 top-0 w-60 h-60 rounded-full bg-brand-warning/10 blur-3xl -translate-y-1/3 translate-x-1/3"></div>
        <div class="relative grid grid-cols-1 md:grid-cols-[1fr_auto_1fr] gap-6 items-center">
          <div>
            <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-brand-warning/15 text-brand-warning border border-brand-warning/30 text-xs font-semibold mb-3">
              <Trophy class="w-3.5 h-3.5" />{{ pointsSummary.level }}
            </div>
            <div class="flex items-baseline gap-2">
              <span class="text-xs text-brand-ink-3">可用积分</span>
              <span class="text-5xl font-black text-brand-warning">{{ pointsSummary.available.toLocaleString() }}</span>
            </div>
            <div class="mt-3 space-y-1 text-xs text-brand-ink-3">
              <div>累计积分：<span class="text-brand-ink font-semibold">{{ pointsSummary.total.toLocaleString() }}</span> · 冻结：{{ pointsSummary.frozen.toLocaleString() }}</div>
              <div>本月新增：<span class="text-brand-success font-semibold">+{{ pointsSummary.monthlyGain }}</span></div>
            </div>
          </div>
          <div class="hidden md:block w-px h-28 bg-brand-line mx-auto"></div>
          <div>
            <div class="text-sm text-brand-ink-2 mb-2">下一级进度</div>
            <div class="flex items-baseline justify-between text-xs mb-2">
              <span class="text-brand-warning font-semibold">{{ pointsSummary.level }}</span>
              <span class="text-brand-ink-3">{{ pointsSummary.nextLevel }}</span>
            </div>
            <div class="h-2.5 rounded-full bg-brand-surface-2 overflow-hidden border border-brand-line">
              <div class="h-full rounded-full bg-gradient-to-r from-brand-warning to-brand-success transition-all"
                :style="{ width: pointsSummary.levelProgress + '%' }"></div>
            </div>
            <div class="text-xs text-brand-ink-3 mt-2">再获得 <span class="text-brand-ink font-semibold">{{ (pointsSummary.nextLevelAt - pointsSummary.total).toLocaleString() }}</span> 积分即可升级</div>
          </div>
        </div>
      </div>

      <!-- 签到 -->
      <div class="rounded-2xl bg-brand-surface border border-brand-line p-5 md:p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-semibold text-brand-ink flex items-center gap-2"><Calendar class="w-4 h-4 text-brand-primary" />每日签到</h3>
          <span class="text-xs text-brand-success font-semibold">{{ signedIn ? '今日已签到' : '点击签到' }}</span>
        </div>
        <div class="grid grid-cols-7 gap-1.5 mb-4">
          <div v-for="d in monthSignIn.slice(0, 14)" :key="d.day"
            :class="['aspect-square rounded-lg flex flex-col items-center justify-center text-[10px] transition',
                     d.today ? 'ring-2 ring-brand-primary bg-brand-primary/15 text-brand-primary' :
                     d.done ? 'bg-brand-success/15 text-brand-success border border-brand-success/30' :
                     'bg-brand-bg border border-brand-line text-brand-ink-3']">
            <span class="font-bold">{{ d.day }}</span>
            <CheckCircle2 v-if="d.done" class="w-2.5 h-2.5 mt-0.5" />
          </div>
        </div>
        <button @click="handleSignIn" :disabled="signedIn"
          :class="['w-full h-10 rounded-lg text-sm font-semibold transition flex items-center justify-center gap-2', signedIn ? 'bg-brand-surface-2 text-brand-ink-3 cursor-not-allowed' : 'bg-brand-primary text-brand-primary-ink hover:bg-brand-primary-hover']">
          <Zap class="w-4 h-4" />{{ signedIn ? '已签到' : '立即签到 +10 积分' }}
        </button>
      </div>
    </div>

    <!-- Tabs -->
    <div class="rounded-2xl bg-brand-surface border border-brand-line">
      <div class="flex items-center gap-1 px-2 md:px-4 overflow-x-auto no-scrollbar border-b border-brand-line">
        <button v-for="t in [
          { id: 'available', name: '可领优惠券', count: availableCoupons.length },
          { id: 'mine', name: '我的优惠券', count: myCoupons.length },
          { id: 'points', name: '积分任务', count: tasks.length },
          { id: 'history', name: '使用/过期记录' }
        ]" :key="t.id" @click="activeTab = t.id"
          :class="['relative shrink-0 px-4 md:px-5 py-4 text-sm font-medium transition',
                   activeTab === t.id ? 'text-brand-primary' : 'text-brand-ink-2 hover:text-brand-primary']">
          {{ t.name }}<span v-if="t.count != null" class="ml-1.5 text-[11px]" :class="activeTab === t.id ? 'text-brand-primary/70' : 'text-brand-ink-3'">({{ t.count }})</span>
          <span v-if="activeTab === t.id" class="absolute left-3 right-3 -bottom-px h-0.5 rounded-t bg-brand-primary"></span>
        </button>
      </div>

      <div class="p-5 md:p-8">
        <!-- 可领 -->
        <template v-if="activeTab === 'available'">
          <div class="flex items-center justify-between mb-5 gap-3">
            <div class="flex items-center gap-2 max-w-md w-full">
              <div class="flex-1 flex h-10 rounded-lg bg-brand-bg border border-brand-line focus-within:border-brand-primary transition">
                <div class="pl-3 flex items-center text-brand-ink-3"><Search class="w-4 h-4" /></div>
                <input v-model="kw" placeholder="搜显卡/CPU/新人券..." class="flex-1 px-3 text-sm bg-transparent placeholder:text-brand-ink-3 focus:outline-none" />
              </div>
            </div>
            <select class="h-10 px-3 rounded-lg bg-brand-bg border border-brand-line text-xs focus:outline-none focus:border-brand-primary">
              <option>全部品类</option><option>显卡</option><option>CPU</option><option>笔记本</option><option>全品类</option>
            </select>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            <div v-for="c in filteredAvailable" :key="c.id" class="group relative flex rounded-xl overflow-hidden border border-brand-line hover:border-brand-primary/40 transition bg-brand-bg">
              <div :class="['w-32 shrink-0 p-4 flex flex-col items-center justify-center text-white relative overflow-hidden bg-gradient-to-br', colorByAmount(c.amount)]">
                <div class="absolute inset-y-0 -right-2 w-4 flex flex-col justify-around pointer-events-none">
                  <div v-for="n in 8" :key="n" class="w-4 h-4 rounded-full bg-brand-bg"></div>
                </div>
                <div class="relative">
                  <div class="text-xs opacity-80">¥</div>
                  <div class="text-4xl font-black leading-none -mt-1">{{ c.amount }}</div>
                  <div class="text-[11px] opacity-90 mt-2 border-t border-white/30 pt-1">满 {{ c.threshold }} 可用</div>
                </div>
              </div>
              <div class="flex-1 p-4 flex flex-col justify-between min-w-0">
                <div>
                  <div class="flex items-start justify-between gap-2">
                    <h4 class="font-semibold text-brand-ink text-sm leading-snug">{{ c.title }}</h4>
                    <span v-if="c.hot" class="shrink-0 px-1.5 py-0.5 rounded bg-brand-error/15 text-brand-error text-[10px] font-semibold border border-brand-error/30">HOT</span>
                  </div>
                  <p class="text-[11px] text-brand-ink-3 mt-1 line-clamp-2">{{ c.desc }}</p>
                </div>
                <div class="mt-3 space-y-1.5">
                  <div class="flex items-center justify-between text-[11px] text-brand-ink-3">
                    <span class="flex items-center gap-1"><Tag class="w-3 h-3" />{{ c.scope }}</span>
                    <span class="flex items-center gap-1"><Clock class="w-3 h-3" />有效期至 {{ c.expiry }}</span>
                  </div>
                  <button @click="claimCoupon(c)"
                    :class="['w-full h-8 rounded-md text-xs font-semibold transition flex items-center justify-center gap-1',
                             c.claimed ? 'bg-brand-surface-2 border border-brand-line text-brand-ink-3 cursor-not-allowed' : 'bg-brand-primary text-brand-primary-ink hover:bg-brand-primary-hover group-hover:shadow-1']">
                    <Ticket v-if="!c.claimed" class="w-3.5 h-3.5" />{{ c.claimed ? '✓ 已领取' : '立即领取' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div v-if="filteredAvailable.length === 0" class="py-16 text-center text-sm text-brand-ink-3">暂无匹配的优惠券</div>
        </template>

        <!-- 我的优惠券 -->
        <template v-if="activeTab === 'mine'">
          <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            <div v-for="c in myCoupons" :key="c.id" class="flex rounded-xl overflow-hidden border border-brand-primary/40 bg-brand-primary/5 relative group">
              <div :class="['w-32 shrink-0 p-4 flex flex-col items-center justify-center text-white bg-gradient-to-br', colorByAmount(c.amount)]">
                <div class="text-xs opacity-80">¥</div>
                <div class="text-4xl font-black leading-none -mt-1">{{ c.amount }}</div>
                <div class="text-[11px] opacity-90 mt-2 border-t border-white/30 pt-1">满 {{ c.threshold }}</div>
              </div>
              <div class="flex-1 p-4 flex flex-col justify-between">
                <div>
                  <h4 class="font-semibold text-brand-ink text-sm">{{ c.title }}</h4>
                  <p class="text-[11px] text-brand-ink-3 mt-1">{{ c.desc }}</p>
                </div>
                <div class="mt-3 space-y-2">
                  <div class="text-[11px] text-brand-ink-3 flex items-center gap-1"><Clock class="w-3 h-3" />有效期至 {{ c.expiry }}</div>
                  <button @click="router.push('/browse')" class="w-full h-8 rounded-md bg-brand-primary text-brand-primary-ink text-xs font-semibold hover:bg-brand-primary-hover transition flex items-center justify-center gap-1">
                    立即使用<ChevronRight class="w-3 h-3" />
                  </button>
                </div>
              </div>
            </div>
            <div v-if="myCoupons.length === 0" class="md:col-span-2 xl:col-span-3 p-12 text-center">
              <div class="w-16 h-16 mx-auto mb-3 rounded-xl bg-brand-warning/15 flex items-center justify-center"><Wallet class="w-8 h-8 text-brand-warning" /></div>
              <div class="font-semibold text-brand-ink">还没有优惠券</div>
              <div class="text-sm text-brand-ink-3 mt-1">去领券区挑选心仪的券吧</div>
            </div>
          </div>
        </template>

        <!-- 积分任务 -->
        <template v-if="activeTab === 'points'">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="p-5 rounded-xl bg-brand-bg border border-brand-line">
              <h3 class="font-semibold text-brand-ink mb-4 flex items-center gap-2"><Award class="w-4 h-4 text-brand-warning" />赚积分任务</h3>
              <div class="space-y-2">
                <div v-for="t in tasks" :key="t.id" class="flex items-center justify-between p-4 rounded-lg bg-brand-surface border border-brand-line-subtle">
                  <div class="flex items-center gap-3">
                    <div :class="['w-9 h-9 rounded-lg flex items-center justify-center', t.done ? 'bg-brand-success/15 text-brand-success' : 'bg-brand-primary/15 text-brand-primary']">
                      <Star :class="['w-4 h-4', t.done ? 'fill-brand-success' : '']" />
                    </div>
                    <div>
                      <div class="text-sm text-brand-ink font-medium">{{ t.title }}</div>
                      <div v-if="t.progress" class="text-[11px] text-brand-ink-3 mt-0.5">{{ t.progress }}</div>
                    </div>
                  </div>
                  <div class="text-right">
                    <div class="text-brand-warning font-bold text-sm">+{{ t.point }}</div>
                    <button v-if="t.done" class="mt-1 text-[11px] text-brand-success">已完成</button>
                    <button v-else class="mt-1 px-2.5 h-6 rounded-md bg-brand-primary/15 text-brand-primary text-[11px] font-semibold hover:bg-brand-primary hover:text-brand-primary-ink transition">去完成</button>
                  </div>
                </div>
              </div>
            </div>

            <div class="space-y-6">
              <div class="p-5 rounded-xl bg-brand-bg border border-brand-line">
                <h3 class="font-semibold text-brand-ink mb-4 flex items-center gap-2"><Gift class="w-4 h-4 text-brand-primary" />积分兑换</h3>
                <div class="space-y-2.5">
                  <div v-for="r in redeemCoupons" :key="r.id" class="flex items-center justify-between p-4 rounded-lg bg-brand-surface border border-brand-line-subtle">
                    <div>
                      <div class="text-sm text-brand-ink font-medium">{{ r.title }}</div>
                      <div class="text-[11px] text-brand-ink-3 mt-0.5">库存：{{ r.stock }}</div>
                    </div>
                    <button :disabled="pointsSummary.available < r.cost"
                      class="px-4 h-9 rounded-lg text-xs font-semibold transition flex items-center gap-1"
                      :class="pointsSummary.available >= r.cost ? 'bg-brand-warning/15 text-brand-warning hover:bg-brand-warning hover:text-white border border-brand-warning/30' : 'bg-brand-surface-2 text-brand-ink-3 cursor-not-allowed border border-brand-line'">
                      <Star class="w-3 h-3" />{{ r.cost }} 兑换
                    </button>
                  </div>
                </div>
              </div>

              <div class="p-5 rounded-xl bg-brand-bg border border-brand-line">
                <h3 class="font-semibold text-brand-ink mb-4 flex items-center gap-2"><Clock class="w-4 h-4 text-brand-info" />近期积分变动</h3>
                <div class="space-y-2">
                  <div v-for="p in pointsHistory" :key="p.title + p.date" class="flex items-center justify-between p-3 rounded-lg bg-brand-surface border border-brand-line-subtle">
                    <div>
                      <div class="text-sm text-brand-ink">{{ p.title }}</div>
                      <div class="text-[11px] text-brand-ink-3 mt-0.5">{{ p.date }}</div>
                    </div>
                    <div :class="['text-sm font-bold', p.type === 'in' ? 'text-brand-success' : 'text-brand-error']">{{ p.type === 'in' ? '+' : '-' }}{{ p.amount }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 历史记录 -->
        <template v-if="activeTab === 'history'">
          <div class="flex gap-1 p-1 rounded-lg bg-brand-bg w-fit mb-5">
            <button @click="historyTab = 'used'"
              :class="['px-4 py-1.5 rounded-md text-xs font-medium transition', historyTab === 'used' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">已使用</button>
            <button @click="historyTab = 'expired'"
              :class="['px-4 py-1.5 rounded-md text-xs font-medium transition', historyTab === 'expired' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">已过期</button>
          </div>
          <div v-if="historyTab === 'used'" class="space-y-2.5">
            <div v-for="u in usedHistory" :key="u.id" class="flex items-center justify-between p-4 rounded-xl bg-brand-bg border border-brand-line">
              <div class="flex items-center gap-4">
                <div class="w-10 h-10 rounded-lg bg-brand-success/15 text-brand-success flex items-center justify-center">
                  <CheckCircle2 class="w-5 h-5" />
                </div>
                <div>
                  <div class="text-sm font-semibold text-brand-ink">{{ u.title }}</div>
                  <div class="text-[11px] text-brand-ink-3 mt-0.5">订单号：{{ u.orderNo }} · {{ u.usedAt }}</div>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-lg font-black text-brand-success">-¥{{ u.amount }}</span>
                <button class="px-3 h-8 rounded-md border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-1">查看订单<ExternalLink class="w-3 h-3" /></button>
              </div>
            </div>
          </div>
          <div v-if="historyTab === 'expired'" class="space-y-2.5">
            <div v-for="e in expiredHistory" :key="e.id" class="flex items-center justify-between p-4 rounded-xl bg-brand-bg border border-brand-line opacity-70">
              <div class="flex items-center gap-4">
                <div class="w-10 h-10 rounded-lg bg-brand-ink-2/15 text-brand-ink-3 flex items-center justify-center"><AlertCircle class="w-5 h-5" /></div>
                <div>
                  <div class="text-sm font-semibold text-brand-ink-2 line-through">{{ e.title }}</div>
                  <div class="text-[11px] text-brand-ink-3 mt-0.5">过期时间：{{ e.expiredAt }}</div>
                </div>
              </div>
              <span class="text-lg font-black text-brand-ink-3">¥{{ e.amount }}</span>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
