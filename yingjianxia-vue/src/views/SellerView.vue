<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Star, ShieldCheck, MapPin, Eye, Heart, ChevronRight,
  Store as StoreIcon, TrendingUp, Award, MessageSquare, Package, Tag
} from 'lucide-vue-next'
import ProductCard from '@/components/ProductCard.vue'
import { getShopInfoApi } from '@/api/user'
import { getProductsApi } from '@/api/product'
import { LEVEL_TO_CONDITION } from '@/constants/product'

const route = useRoute()
const router = useRouter()
const shopId = computed(() => route.params.id)
// sellerId 在店铺信息加载后从 shop.sellerId 取得，用于查询商品列表
const sellerId = ref(null)

const loading = ref(false)
const error = ref(null)
const toast = ref(null)

const shop = ref(null)

// 商品列表
const products = ref([])
const newArrivals = ref([])
const activeTab = ref('all')
const sortBy = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

const pageArray = computed(() => {
  const arr = []
  const start = Math.max(1, pageNum.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) arr.push(i)
  return arr
})

function showToast(type, msg) {
  toast.value = { type, msg }
  setTimeout(() => { toast.value = null }, 3000)
}

function mapShop(data) {
  if (!data) return null
  const shopName = data.shopName || data.name || '未知店铺'
  const logoUrl = data.logoUrl || ''
  return {
    id: data.id,
    name: shopName,
    logoUrl,
    logo: shopName.charAt(0),
    banner: data.bannerUrl || data.coverUrl || `https://placehold.co/1200x300/1E2026/00D4AA?text=${encodeURIComponent(shopName || 'Shop')}`,
    rating: data.rating || 0,
    reviewCount: data.reviewCount || 0,
    soldCount: data.soldCount || 0,
    region: data.region || data.province || '未知地区',
    openedAt: data.openedAt ? data.openedAt.substring(0, 10) : '',
    category: data.shopLevel || data.levelName || '普通店铺',
    description: data.description || '暂无店铺简介',
    certification: data.certifications || ['实名认证', '担保交易'],
    stats: [
      { label: '在售商品', value: data.onSaleCount || 0 },
      { label: '月成交', value: data.monthlySales ? `${data.monthlySales}+ 单` : '--' },
      { label: '好评率', value: data.goodRate ? `${data.goodRate}%` : '--' },
      { label: '响应速度', value: data.responseTime ? `≤ ${data.responseTime}分钟` : '--' }
    ],
    sellerBadges: [
      { icon: Award, label: data.openedAt ? `${new Date().getFullYear() - parseInt(data.openedAt.substring(0, 4))}年老店` : '新店铺', color: 'text-brand-warning bg-brand-warning/15 border-brand-warning/30' },
      { icon: TrendingUp, label: data.monthlySales ? `本月销量 ${data.monthlySales}+` : '本月销量 --', color: 'text-brand-success bg-brand-success/15 border-brand-success/30' },
      { icon: ShieldCheck, label: data.deposit ? `缴纳保证金 ¥${data.deposit}` : '担保交易', color: 'text-brand-info bg-brand-info/15 border-brand-info/30' }
    ],
    // 评分细分（后端有则用，无则用总评分）
    descScore: data.descScore != null ? data.descScore : (data.rating || 0),
    serviceScore: data.serviceScore != null ? data.serviceScore : (data.rating || 0),
    shipScore: data.shipScore != null ? data.shipScore : (data.rating || 0)
  }
}

async function loadShop() {
  loading.value = true
  error.value = null
  try {
    const data = await getShopInfoApi(shopId.value)
    shop.value = mapShop(data)
    // 调用 stats 接口获取真实评价统计，覆盖 shop 中冗余/近似字段
    try {
      const stats = await getShopStatsApi(shopId.value)
      if (stats && shop.value) {
        // 用真实评分覆盖 shops.rating（可能仍是硬编码）
        if (typeof stats.averageRating === 'number') {
          shop.value.rating = stats.averageRating
        }
        // 用真实好评率覆盖（getShop 的 goodRate 可能是 rating*20）
        if (typeof stats.positiveRate === 'number') {
          shop.value.stats[2].value = `${stats.positiveRate}%`
        }
        // 用真实评价数覆盖
        if (typeof stats.totalCount === 'number') {
          shop.value.reviewCount = stats.totalCount
        }
      }
    } catch (e) {
      console.warn('[SellerView] stats 接口加载失败，使用 getShop 返回值', e)
    }
    // 店铺信息加载成功后，提取 sellerId 用于查询商品列表
    if (data && data.sellerId) {
      sellerId.value = data.sellerId
      await loadProducts()
    }
  } catch (e) {
    error.value = e.message || '店铺信息加载失败'
    showToast('error', error.value)
  } finally {
    loading.value = false
  }
}

async function loadProducts() {
  try {
    const params = {
      sellerId: sellerId.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: 2 // 只看在售（后端 STATUS_ON_SALE=2）
    }
    if (sortBy.value) params.sortBy = sortBy.value
    const res = await getProductsApi(params)
    const records = (res && res.records) || []
    products.value = records.map(item => ({
      ...item,
      coverImage: item.imageUrls?.[0] || item.coverImage,
      condition: LEVEL_TO_CONDITION[item.conditionLevel] || 'GOOD'
    }))
    total.value = (res && res.total) || 0
    totalPages.value = (res && res.totalPages) || 0
    // 用商品列表 total 覆盖 shop.onSaleCount（后端冗余字段不准）
    if (shop.value) {
      shop.value.stats[0].value = total.value
    }
    // 新上架：取前 3 条（按创建时间排序）
    newArrivals.value = products.value.slice(0, 3)
  } catch (e) {
    showToast('error', '商品列表加载失败')
    // 保留旧数据
  }
}

function changePage(n) {
  if (n < 1 || n > totalPages.value) return
  pageNum.value = n
  loadProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function onSortChange() {
  pageNum.value = 1
  loadProducts()
}

watch(activeTab, () => {
  pageNum.value = 1
  loadProducts()
})

onMounted(() => {
  loadShop()
})
</script>

<template>
  <div class="space-y-6">
    <!-- 加载骨架 -->
    <div v-if="loading && !shop" class="animate-pulse space-y-6">
      <div class="w-full h-48 md:h-64 rounded-2xl bg-brand-surface border border-brand-line"></div>
      <div class="rounded-2xl bg-brand-surface border border-brand-line p-8">
        <div class="h-8 bg-brand-surface-2 rounded w-1/3 mb-4"></div>
        <div class="h-4 bg-brand-surface-2 rounded w-1/2"></div>
      </div>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="error && !shop" class="rounded-2xl bg-brand-surface border border-brand-line p-12 text-center">
      <div class="w-16 h-16 mx-auto mb-4 rounded-2xl bg-brand-error/15 flex items-center justify-center">
        <StoreIcon class="w-8 h-8 text-brand-error" />
      </div>
      <h3 class="text-lg font-bold text-brand-ink">店铺信息加载失败</h3>
      <p class="text-sm text-brand-ink-3 mt-2">{{ error }}</p>
      <button @click="loadShop()" class="mt-4 px-5 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition">重新加载</button>
    </div>

    <template v-else-if="shop">
      <!-- Banner -->
      <div class="w-full relative h-80 md:h-[420px] rounded-2xl overflow-hidden border border-brand-line">
        <img :src="shop.banner" class="w-full h-full object-cover" />
        <div class="absolute inset-0 bg-gradient-to-t from-brand-bg via-brand-bg/50 to-transparent"></div>
      </div>

      <div class="container-app -mt-20 md:-mt-24 space-y-6">
        <!-- 店铺信息头卡 -->
        <div class="relative rounded-2xl bg-brand-surface border border-brand-line p-6 md:p-8 shadow-2">
          <div class="flex flex-col md:flex-row md:items-end gap-5">
            <div class="flex items-end gap-5 -mt-14 md:-mt-16">
              <div class="w-28 h-28 md:w-32 md:h-32 rounded-2xl bg-gradient-to-br from-brand-primary to-[#00B894] border-4 border-brand-surface flex items-center justify-center text-4xl font-black text-brand-primary-ink shadow-2 overflow-hidden">
                <img v-if="shop.logoUrl" :src="shop.logoUrl" class="w-full h-full object-cover" :alt="shop.name" />
                <span v-else>{{ shop.logo }}</span>
              </div>
              <div class="pb-2 hidden md:block">
                <div class="flex items-center gap-2">
                  <h1 class="text-2xl font-bold text-brand-ink">{{ shop.name }}</h1>
                  <span class="px-2.5 py-0.5 rounded bg-brand-warning/15 text-brand-warning border border-brand-warning/30 text-xs font-semibold">{{ shop.category }}</span>
                </div>
                <div class="flex items-center gap-3 mt-1.5 text-xs text-brand-ink-3">
                  <span class="flex items-center gap-1"><MapPin class="w-3.5 h-3.5" />{{ shop.region }}</span>
                  <span v-if="shop.openedAt">入驻时间 {{ shop.openedAt }}</span>
                </div>
              </div>
            </div>

            <div class="md:hidden">
              <div class="flex items-center gap-2">
                <h1 class="text-xl font-bold text-brand-ink">{{ shop.name }}</h1>
                <span class="px-2.5 py-0.5 rounded bg-brand-warning/15 text-brand-warning border border-brand-warning/30 text-[11px] font-semibold">{{ shop.category }}</span>
              </div>
              <div class="flex items-center gap-3 mt-1 text-xs text-brand-ink-3">
                <span class="flex items-center gap-1"><MapPin class="w-3.5 h-3.5" />{{ shop.region }}</span>
                <span v-if="shop.openedAt">入驻 {{ shop.openedAt }}</span>
              </div>
            </div>

            <!-- 店铺动作 -->
            <div class="md:ml-auto flex flex-wrap gap-2">
              <button @click="router.push('/store-settings')" class="px-5 h-10 rounded-lg bg-brand-primary text-brand-primary-ink font-medium hover:bg-brand-primary-hover transition flex items-center gap-2">
                <StoreIcon class="w-4 h-4" />进店逛逛
              </button>
              <button class="px-5 h-10 rounded-lg border border-brand-primary/40 text-brand-primary hover:bg-brand-primary-subtle transition flex items-center gap-2">
                <Heart class="w-4 h-4" />关注店铺
              </button>
              <button @click="router.push('/messages')" class="px-5 h-10 rounded-lg border border-brand-line text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
                <MessageSquare class="w-4 h-4" />联系卖家
              </button>
            </div>
          </div>

          <!-- 徽标 -->
          <div class="flex flex-wrap gap-2 mt-5">
            <div v-for="b in shop.sellerBadges" :key="b.label"
              :class="['inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-semibold border', b.color]">
              <component :is="b.icon" class="w-3.5 h-3.5" />{{ b.label }}
            </div>
          </div>

          <!-- 核心指标 -->
          <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mt-6 pt-6 border-t border-brand-line">
            <div v-for="s in shop.stats" :key="s.label" class="p-4 rounded-xl bg-brand-bg border border-brand-line-subtle text-center">
              <div class="text-xl md:text-2xl font-bold text-brand-ink">{{ s.value }}</div>
              <div class="text-xs text-brand-ink-3 mt-1">{{ s.label }}</div>
            </div>
          </div>

          <!-- 评分 -->
          <div class="mt-6 grid grid-cols-1 md:grid-cols-[1fr_2fr] gap-6">
            <div class="p-5 rounded-xl bg-gradient-to-br from-brand-warning/15 to-brand-bg border border-brand-line">
              <div class="flex items-center gap-3">
                <div class="text-4xl font-black text-brand-warning">{{ shop.rating || '--' }}</div>
                <div>
                  <div class="flex items-center gap-0.5 text-brand-warning fill-brand-warning">
                    <Star v-for="i in 5" :key="i" class="w-4 h-4" />
                  </div>
                  <div class="text-xs text-brand-ink-3 mt-1">{{ shop.reviewCount }} 条评价</div>
                </div>
              </div>
            </div>
            <div class="grid grid-cols-3 gap-3">
              <div class="p-4 rounded-xl bg-brand-bg border border-brand-line">
                <div class="text-xs text-brand-ink-3">描述相符</div>
                <div class="mt-2"><span class="text-xl font-bold text-brand-success">{{ shop.descScore || '--' }}</span></div>
              </div>
              <div class="p-4 rounded-xl bg-brand-bg border border-brand-line">
                <div class="text-xs text-brand-ink-3">卖家服务</div>
                <div class="mt-2"><span class="text-xl font-bold text-brand-success">{{ shop.serviceScore || '--' }}</span></div>
              </div>
              <div class="p-4 rounded-xl bg-brand-bg border border-brand-line">
                <div class="text-xs text-brand-ink-3">物流发货</div>
                <div class="mt-2"><span class="text-xl font-bold text-brand-success">{{ shop.shipScore || '--' }}</span></div>
              </div>
            </div>
          </div>

          <!-- 认证标签 -->
          <div class="mt-5 flex flex-wrap gap-2">
            <span v-for="c in shop.certification" :key="c" class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-brand-surface-3 text-xs text-brand-ink-2 border border-brand-line">
              <ShieldCheck class="w-3 h-3 text-brand-primary" />{{ c }}
            </span>
          </div>
        </div>

        <!-- 新上架 -->
        <section v-if="newArrivals.length > 0" class="rounded-2xl bg-brand-surface border border-brand-line p-6">
          <div class="flex items-center justify-between mb-5">
            <h2 class="text-lg font-bold text-brand-ink flex items-center gap-2"><Tag class="w-5 h-5 text-brand-primary" />掌柜新上架</h2>
          </div>
          <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
            <ProductCard v-for="p in newArrivals" :key="p.id" :product="p" :show-seller="false" />
          </div>
        </section>

        <!-- Tab 分类 + 全部商品 -->
        <section class="rounded-2xl bg-brand-surface border border-brand-line p-6">
          <div class="flex flex-wrap items-center justify-between gap-3 mb-5">
            <div class="flex gap-1 p-1 rounded-lg bg-brand-bg overflow-x-auto">
              <button @click="activeTab = 'all'"
                :class="['shrink-0 px-3.5 py-1.5 rounded-md text-xs font-medium transition flex items-center gap-1.5',
                         activeTab === 'all' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">
                全部商品<span :class="activeTab === 'all' ? 'text-brand-primary-ink/70' : 'text-brand-ink-3'">({{ total }})</span>
              </button>
            </div>
            <div class="flex items-center gap-2 text-xs text-brand-ink-2">
              <span>排序：</span>
              <select v-model="sortBy" @change="onSortChange" class="h-8 px-2 rounded-md bg-brand-bg border border-brand-line text-xs focus:outline-none focus:border-brand-primary">
                <option value="">综合排序</option>
                <option value="price_asc">价格从低到高</option>
                <option value="price_desc">价格从高到低</option>
                <option value="sales">销量优先</option>
                <option value="created_at">最新上架</option>
              </select>
            </div>
          </div>

          <!-- 商品列表 -->
          <div v-if="products.length > 0" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <ProductCard v-for="p in products" :key="p.id" :product="p" :show-seller="false" />
          </div>

          <!-- 空状态 -->
          <div v-else class="py-12 text-center">
            <Package class="w-12 h-12 mx-auto text-brand-ink-3 mb-3" />
            <p class="text-sm text-brand-ink-3">该店铺暂无在售商品</p>
          </div>

          <!-- 分页 -->
          <div v-if="totalPages > 1" class="flex items-center justify-center gap-1 mt-8 pt-6 border-t border-brand-line">
            <button @click="changePage(1)" :disabled="pageNum <= 1" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition">&lt;&lt;</button>
            <button @click="changePage(pageNum - 1)" :disabled="pageNum <= 1" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition">&lt;</button>
            <button v-for="n in pageArray" :key="n" @click="changePage(n)"
              :class="['w-9 h-9 rounded-lg text-sm font-medium transition',
                       n === pageNum ? 'bg-brand-primary text-brand-primary-ink border border-brand-primary' : 'border border-brand-line text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40']">{{ n }}</button>
            <button @click="changePage(pageNum + 1)" :disabled="pageNum >= totalPages" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition">&gt;</button>
            <button @click="changePage(totalPages)" :disabled="pageNum >= totalPages" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition">&gt;&gt;</button>
          </div>
        </section>
      </div>
    </template>

    <!-- Toast -->
    <Transition name="toast">
      <div v-if="toast" :class="['fixed top-20 right-6 z-50 px-5 py-3 rounded-lg text-white text-sm shadow-lg flex items-center gap-2',
                                 toast.type === 'success' ? 'bg-brand-success' : 'bg-brand-error']">
        {{ toast.msg }}
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.toast-enter-active, .toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from, .toast-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
