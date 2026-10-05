<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { searchProductsApi, getCategoriesApi } from '@/api/product'
import { CONDITIONS, CONDITION_TO_LEVEL, LEVEL_TO_CONDITION, CONDITION_NAME } from '@/constants/product'
import {
  SlidersHorizontal, ChevronDown, ChevronUp, X, Sliders, Star,
  Heart, ShoppingCart, ShieldCheck, Zap
} from 'lucide-vue-next'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const filtersExpanded = ref(true)
const priceMin = ref(null)
const priceMax = ref(null)
const selectedConditions = ref([])
const selectedCategory = ref('all')
const sortBy = ref('default')
const currentPage = ref(1)
const pageSize = ref(24)
const loading = ref(false)

// 后端分类 code → 前端别名映射
const codeAliases = { memory: 'ram', storage: 'ssd' }
const aliasToCode = Object.fromEntries(Object.entries(codeAliases).map(([k, v]) => [v, k]))

const categories = ref([
  { id: 'all', name: '全部品类', count: 0 }
])

const conditions = CONDITIONS

// 前端成色字符串 → 后端 conditionLevel 数字（统一从 constants 取）
const conditionToLevel = CONDITION_TO_LEVEL
const levelToCondition = LEVEL_TO_CONDITION

// 前端排序 → 后端 sortField/sortDirection
const sortMapping = {
  default: { sortField: 'publishedAt', sortDirection: 'desc' },
  sales: { sortField: 'viewCount', sortDirection: 'desc' },
  new: { sortField: 'publishedAt', sortDirection: 'desc' },
  priceAsc: { sortField: 'price', sortDirection: 'asc' },
  priceDesc: { sortField: 'price', sortDirection: 'desc' },
  credit: { sortField: 'rating', sortDirection: 'desc' }
}

const booleans = [
  { id: 'inspection', label: '官方验机', icon: ShieldCheck, color: 'text-brand-info' },
  { id: 'newArrival', label: '近7天新上', icon: Zap, color: 'text-brand-primary' },
  { id: 'warranty', label: '带保修', icon: Star, color: 'text-brand-warning' },
  { id: 'freeShip', label: '包邮', icon: ShoppingCart, color: 'text-brand-success' }
]
const selectedBooleans = ref([])

const products = ref([])
const total = ref(0)
const categoriesLoaded = ref(false)

const activeFiltersCount = computed(() =>
  (selectedConditions.value.length) +
  (selectedBooleans.value.length) +
  ((priceMin.value != null || priceMax.value != null) ? 1 : 0)
)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const pageNumbers = computed(() => {
  const pages = []
  const tp = totalPages.value
  const cp = currentPage.value
  const start = Math.max(1, cp - 3)
  const end = Math.min(tp, start + 6)
  for (let i = start; i <= end; i++) pages.push(i)
  if (end < tp) { pages.push('...'); pages.push(tp) }
  return pages
})

function clearFilters() {
  selectedCategory.value = 'all'
  selectedConditions.value = []
  selectedBooleans.value = []
  priceMin.value = null
  priceMax.value = null
  sortBy.value = 'default'
  currentPage.value = 1
}

function toggleCategory(id) {
  selectedCategory.value = id
  currentPage.value = 1
}
function toggleCondition(id) {
  const i = selectedConditions.value.indexOf(id)
  if (i >= 0) selectedConditions.value.splice(i, 1)
  else selectedConditions.value.push(id)
  currentPage.value = 1
}
function toggleBoolean(id) {
  const i = selectedBooleans.value.indexOf(id)
  if (i >= 0) selectedBooleans.value.splice(i, 1)
  else selectedBooleans.value.push(id)
  currentPage.value = 1
}

// 根据选中的分类字符串 ID 查找后端数字 categoryId
function getCategoryIdByCode(code) {
  const cat = categories.value.find(c => c.code === code || c.id === code)
  return cat?.backendId ?? null
}

async function fetchCategories() {
  try {
    const res = await getCategoriesApi()
    const list = res.data || res || []
    const mapped = [
      { id: 'all', name: '全部品类', count: 0 },
      ...list.map(c => ({
        id: codeAliases[c.code] || c.code || String(c.id),
        code: c.code,
        backendId: c.id,
        name: c.name,
        count: 0
      }))
    ]
    categories.value = mapped
  } catch (e) {
    console.warn('分类加载失败，使用默认', e)
  } finally {
    categoriesLoaded.value = true
  }
}

async function fetchProducts() {
  loading.value = true
  try {
    const sort = sortMapping[sortBy.value] || sortMapping.default
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      sortField: sort.sortField,
      sortDirection: sort.sortDirection
    }

    // 分类
    if (selectedCategory.value !== 'all') {
      const catId = getCategoryIdByCode(aliasToCode[selectedCategory.value] || selectedCategory.value)
      if (catId) params.categoryIds = [catId]
    }

    // 成色
    if (selectedConditions.value.length > 0) {
      params.conditionLevels = selectedConditions.value
        .map(c => conditionToLevel[c])
        .filter(v => v != null)
    }

    // 价格
    if (priceMin.value != null) params.priceMin = priceMin.value
    if (priceMax.value != null) params.priceMax = priceMax.value

    // 特色服务
    if (selectedBooleans.value.includes('inspection')) params.onlyInspected = true

    const res = await searchProductsApi(params)
    const data = res.data || res
    const records = data.records || data.list || []
    total.value = data.total || 0

    products.value = records.map(item => ({
      id: item.id,
      title: item.title,
      price: Number(item.price) || 0,
      originalPrice: Number(item.originalPrice) || null,
      condition: levelToCondition[item.conditionLevel] || item.conditionName || '',
      coverImage: item.imageUrls?.[0],
      sellerName: item.shopName || '硬件侠商家',
      sellerRating: Number(item.sellerCreditScore) || 0,
      shopId: item.shopId,
      viewCount: item.viewCount || 0,
      specsBrief: item.specs?.map(s => s.value).filter(Boolean) || [],
      isFavorite: item.favoredByMe || false,
      inspectionReport: item.hasInspection || false
    }))
  } catch (e) {
    console.error('商品加载失败', e)
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 监听筛选变化自动查询（分类未加载完成时跳过，避免竞态导致分类过滤失效）
watch([selectedCategory, selectedConditions, selectedBooleans, priceMin, priceMax, sortBy, currentPage], () => {
  if (!categoriesLoaded.value) return
  fetchProducts()
})

onMounted(async () => {
  // 兼容后端 code（如 memory/storage）与前端别名（ram/ssd）
  if (route.query.category) {
    const raw = String(route.query.category)
    selectedCategory.value = codeAliases[raw] || raw
  }
  await fetchCategories()
  await fetchProducts()
})
</script>

<template>
  <div class="container-app space-y-6">
    <!-- 面包屑 + 标题 -->
    <div>
      <div class="text-xs text-brand-ink-3 mb-2">首页 / 找货市场 / <span class="text-brand-ink-2">{{ categories.find(c => c.id === selectedCategory || c.code === selectedCategory)?.name || '全部品类' }}</span></div>
      <div class="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
        <div>
          <h1 class="text-2xl md:text-3xl font-bold text-brand-ink">全部二手硬件</h1>
          <p class="text-sm text-brand-ink-3 mt-1">共 <span class="text-brand-primary font-semibold">{{ total.toLocaleString() }}</span> 件商品，实时更新</p>
        </div>
        <div class="flex items-center gap-2">
          <button @click="filtersExpanded = !filtersExpanded"
            class="px-4 h-10 rounded-lg bg-brand-surface border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
            <SlidersHorizontal class="w-4 h-4" />筛选器
            <ChevronUp v-if="filtersExpanded" class="w-3.5 h-3.5" /><ChevronDown v-else class="w-3.5 h-3.5" />
          </button>
          <button v-if="activeFiltersCount > 0" @click="clearFilters"
            class="px-4 h-10 rounded-lg border border-brand-error/30 text-sm text-brand-error hover:bg-brand-error-subtle transition flex items-center gap-1.5">
            <X class="w-3.5 h-3.5" />清除全部 ({{ activeFiltersCount }})
          </button>
        </div>
      </div>
    </div>

    <!-- 筛选器 -->
    <div v-show="filtersExpanded" class="rounded-2xl bg-brand-surface border border-brand-line p-5 space-y-5">
      <!-- 分类 -->
      <div>
        <div class="text-xs font-semibold text-brand-ink-3 mb-3 flex items-center gap-1.5"><Sliders class="w-3.5 h-3.5" />品类</div>
        <div class="flex flex-wrap gap-2">
          <button v-for="c in categories" :key="c.id" @click="toggleCategory(c.id)"
            :class="['px-3.5 py-1.5 rounded-lg text-xs transition border',
                     selectedCategory === c.id ? 'bg-brand-primary-subtle border-brand-primary/40 text-brand-primary font-medium' : 'bg-brand-bg border-brand-line text-brand-ink-2 hover:border-brand-primary/30 hover:text-brand-primary']">
            {{ c.name }}<span v-if="c.count" :class="selectedCategory === c.id ? 'text-brand-primary/70' : 'text-brand-ink-3'" class="ml-1">({{ c.count.toLocaleString() }})</span>
          </button>
        </div>
      </div>

      <!-- 成色 -->
      <div>
        <div class="text-xs font-semibold text-brand-ink-3 mb-3">成色</div>
        <div class="flex flex-wrap gap-2">
          <button v-for="c in conditions" :key="c.id" @click="toggleCondition(c.id)"
            :class="['px-4 py-1.5 rounded-lg text-xs transition border',
                     selectedConditions.includes(c.id) ? 'bg-brand-primary-subtle border-brand-primary/40 text-brand-primary font-medium' : 'bg-brand-bg border-brand-line text-brand-ink-2 hover:border-brand-primary/30 hover:text-brand-primary']">
            {{ c.name }}
          </button>
        </div>
      </div>

      <!-- 价格区间 -->
      <div>
        <div class="text-xs font-semibold text-brand-ink-3 mb-3">价格区间 (¥)</div>
        <div class="flex items-center gap-3 max-w-md">
          <input v-model="priceMin" type="number" placeholder="最低价" class="flex-1 h-9 rounded-lg bg-brand-bg border border-brand-line px-3 text-sm focus:outline-none focus:border-brand-primary" />
          <div class="text-brand-ink-3">—</div>
          <input v-model="priceMax" type="number" placeholder="最高价" class="flex-1 h-9 rounded-lg bg-brand-bg border border-brand-line px-3 text-sm focus:outline-none focus:border-brand-primary" />
          <div class="flex gap-1.5 ml-2">
            <button v-for="r in [{ n: '1k', v: [0, 1000] }, { n: '1k-3k', v: [1000, 3000] }, { n: '3k-5k', v: [3000, 5000] }, { n: '5k+', v: [5000, null] }]" :key="r.n"
              @click="priceMin = r.v[0]; priceMax = r.v[1]"
              class="px-2.5 h-9 rounded-md bg-brand-bg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition">
              {{ r.n }}
            </button>
          </div>
        </div>
      </div>

      <!-- 多选标签 -->
      <div>
        <div class="text-xs font-semibold text-brand-ink-3 mb-3">特色服务</div>
        <div class="flex flex-wrap gap-2">
          <button v-for="b in booleans" :key="b.id" @click="toggleBoolean(b.id)"
            :class="['inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs transition border',
                     selectedBooleans.includes(b.id) ? 'bg-brand-primary-subtle border-brand-primary/40 text-brand-primary font-medium' : 'bg-brand-bg border-brand-line text-brand-ink-2 hover:border-brand-primary/30 hover:text-brand-primary']">
            <component :is="b.icon" :class="['w-3.5 h-3.5', selectedBooleans.includes(b.id) ? 'text-brand-primary' : b.color]" />{{ b.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 排序栏 -->
    <div class="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-brand-surface border border-brand-line px-4 py-3">
      <div class="flex items-center gap-1 overflow-x-auto no-scrollbar">
        <button v-for="s in [
          { id: 'default', name: '综合推荐' },
          { id: 'sales', name: '销量优先' },
          { id: 'new', name: '最新上架' },
          { id: 'priceAsc', name: '价格 ↑' },
          { id: 'priceDesc', name: '价格 ↓' },
          { id: 'credit', name: '卖家信用' }
        ]" :key="s.id" @click="sortBy = s.id"
          :class="['shrink-0 px-3.5 py-1.5 rounded-md text-xs font-medium transition',
                   sortBy === s.id ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-2']">
          {{ s.name }}
        </button>
      </div>
      <div class="flex items-center gap-3 text-xs text-brand-ink-3">
        <label class="flex items-center gap-1.5 cursor-pointer"><input type="checkbox" class="w-3.5 h-3.5 accent-brand-primary" />仅显示有货</label>
        <label class="flex items-center gap-1.5 cursor-pointer"><input type="checkbox" class="w-3.5 h-3.5 accent-brand-primary" />同城卖家优先</label>
        <select class="h-7 px-2 rounded-md bg-brand-bg border border-brand-line text-xs focus:outline-none focus:border-brand-primary">
          <option>每页 24 件</option><option>每页 48 件</option><option>每页 96 件</option>
        </select>
      </div>
    </div>

    <!-- 商品网格 -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="text-brand-ink-3 text-sm">加载中...</div>
    </div>
    <div v-else-if="products.length === 0" class="flex items-center justify-center py-20">
      <div class="text-brand-ink-3 text-sm">暂无商品</div>
    </div>
    <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-5">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="flex flex-col md:flex-row items-center justify-between gap-4 pt-4">
      <div class="text-xs text-brand-ink-3">共 {{ total.toLocaleString() }} 件</div>
      <div class="flex items-center gap-1">
        <button :disabled="currentPage <= 1" @click="currentPage--"
          class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary hover:border-brand-primary/40 transition disabled:opacity-40 disabled:cursor-not-allowed">&lt;</button>
        <button v-for="n in pageNumbers" :key="n" @click="typeof n === 'number' && (currentPage = n)"
          :class="['w-9 h-9 rounded-lg text-sm transition', n === currentPage ? 'bg-brand-primary text-brand-primary-ink font-semibold' : 'border border-brand-line text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40']">
          {{ n === '...' ? '...' : n }}
        </button>
        <button :disabled="currentPage >= totalPages" @click="currentPage++"
          class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary hover:border-brand-primary/40 transition disabled:opacity-40 disabled:cursor-not-allowed">&gt;</button>
      </div>
    </div>
  </div>
</template>
