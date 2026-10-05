<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Search, TrendingUp, Clock, X, Sparkles, SlidersHorizontal,
  History, ChevronRight
} from 'lucide-vue-next'
import ProductCard from '@/components/ProductCard.vue'
import { searchProductsApi, getHotKeywordsApi } from '@/api/product'

const route = useRoute()
const router = useRouter()

const keyword = ref(route.query.q || '')
const suggestions = ref([])
const searchHistory = ref(JSON.parse(localStorage.getItem('yjx_search_history') || '[]'))
const results = ref([])
const loading = ref(false)
const suggestionOpen = ref(false)
const toast = ref(null)

// 分页
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

// 排序
const sortBy = ref('')

// 热搜词
const hotKeywords = ref([])

const hasQuery = computed(() => keyword.value.trim().length > 0)
const noResults = computed(() => hasQuery.value && !loading.value && results.value.length === 0)

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

function pushHistory(kw) {
  const t = new Set(searchHistory.value.filter(k => k !== kw))
  t.add(kw)
  searchHistory.value = Array.from(t).reverse().slice(0, 10)
  localStorage.setItem('yjx_search_history', JSON.stringify(searchHistory.value))
}

async function doSearch(k = keyword.value) {
  const query = k.trim()
  if (!query) return
  pushHistory(query)
  keyword.value = query
  router.replace({ path: '/search', query: { q: query } })
  suggestionOpen.value = false
  loading.value = true
  pageNum.value = 1
  try {
    const params = { keyword: query, pageNum: 1, pageSize: pageSize.value }
    if (sortBy.value) params.sortBy = sortBy.value
    const res = await searchProductsApi(params)
    results.value = ((res && res.records) || []).map(item => ({
      ...item,
      coverImage: item.imageUrls?.[0] || item.coverImage
    }))
    total.value = (res && res.total) || 0
    totalPages.value = (res && res.totalPages) || 0
  } catch (e) {
    showToast('error', e.message || '搜索失败，请稍后重试')
    // 保留旧数据不清空
  } finally {
    loading.value = false
  }
}

async function changePage(n) {
  if (n < 1 || n > totalPages.value) return
  pageNum.value = n
  loading.value = true
  try {
    const params = { keyword: keyword.value.trim(), pageNum: n, pageSize: pageSize.value }
    if (sortBy.value) params.sortBy = sortBy.value
    const res = await searchProductsApi(params)
    results.value = ((res && res.records) || []).map(item => ({
      ...item,
      coverImage: item.imageUrls?.[0] || item.coverImage
    }))
    total.value = (res && res.total) || 0
    totalPages.value = (res && res.totalPages) || 0
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e) {
    showToast('error', e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onSortChange() {
  doSearch(keyword.value)
}

function onInput() {
  const q = keyword.value.trim()
  suggestionOpen.value = q.length > 0
  suggestions.value = q.length >= 1 ? hotKeywords.value.filter(h =>
    h.kw?.toLowerCase().includes(q.toLowerCase()) || q.toLowerCase().includes(h.kw?.toLowerCase() || '')
  ).slice(0, 6) : []
}

function clearHistory() {
  searchHistory.value = []
  localStorage.removeItem('yjx_search_history')
}

async function loadHotKeywords() {
  try {
    const data = await getHotKeywordsApi()
    if (Array.isArray(data) && data.length > 0) {
      hotKeywords.value = data
    } else {
      // 后端无数据时用默认值
      hotKeywords.value = [
        { kw: 'RTX 4070 SUPER', heat: 23817 },
        { kw: 'AMD 7800X3D', heat: 19523 },
        { kw: 'DDR5 6000 32GB', heat: 17891 },
        { kw: '990 PRO 2TB', heat: 15210 },
        { kw: 'RTX 3080 Ti', heat: 12781 },
        { kw: 'i7-14700K', heat: 11987 }
      ]
    }
  } catch {
    hotKeywords.value = [
      { kw: 'RTX 4070 SUPER', heat: 23817 },
      { kw: 'AMD 7800X3D', heat: 19523 },
      { kw: 'DDR5 6000 32GB', heat: 17891 }
    ]
  }
}

const noResultSuggests = [
  { icon: 'CPU', name: 'CPU 处理器专区', path: '/browse?category=cpu' },
  { icon: 'GPU', name: '显卡专区', path: '/browse?category=gpu' },
  { icon: 'LAPTOP', name: '笔记本专区', path: '/browse?category=laptop' },
  { icon: 'SSD', name: '存储 SSD', path: '/browse?category=ssd' }
]

watch(() => route.query.q, (v) => {
  if (v && v !== keyword.value) { keyword.value = String(v); doSearch(v) }
}, { immediate: false })

onMounted(() => {
  loadHotKeywords()
  if (hasQuery.value) doSearch(keyword.value)
})
</script>

<template>
  <div class="container-app space-y-6">
    <!-- 搜索区 -->
    <section class="pt-4 md:pt-8 pb-2">
      <h1 class="text-xl md:text-2xl font-bold text-brand-ink mb-4 text-center">在硬件侠，找你心中的那片硬件</h1>
      <div class="max-w-3xl mx-auto">
        <div class="relative">
          <div class="flex h-14 rounded-xl overflow-hidden bg-brand-surface border border-brand-line focus-within:border-brand-primary transition shadow-1">
            <div class="pl-5 flex items-center text-brand-ink-3"><Search class="w-5 h-5" /></div>
            <input
              v-model="keyword" type="text"
              placeholder="搜索 CPU、显卡、笔记本，试试 'RTX 4070 SUPER'..."
              class="flex-1 px-4 text-base bg-transparent placeholder:text-brand-ink-3 focus:outline-none"
              @input="onInput"
              @keyup.enter="doSearch()"
              @focus="suggestionOpen = keyword.value.trim().length > 0 || searchHistory.length > 0"
            />
            <button v-if="keyword" @click="keyword = ''; suggestionOpen = false" class="px-3 flex items-center text-brand-ink-3 hover:text-brand-primary">
              <X class="w-4 h-4" />
            </button>
            <button @click="doSearch()"
              class="px-7 bg-brand-primary text-brand-primary-ink font-semibold hover:bg-brand-primary-hover transition flex items-center gap-2">
              <Sparkles class="w-4 h-4" />搜索
            </button>
          </div>

          <!-- 建议下拉 -->
          <div v-if="suggestionOpen" class="absolute left-0 right-0 mt-2 rounded-xl bg-brand-surface border border-brand-line shadow-3 overflow-hidden z-30">
            <div v-if="suggestions.length > 0" class="p-2 border-b border-brand-line-subtle">
              <div class="px-3 py-2 text-[11px] text-brand-ink-3 flex items-center gap-1.5"><Sparkles class="w-3.5 h-3.5 text-brand-primary" />智能联想</div>
              <button v-for="s in suggestions" :key="s.kw" @click="doSearch(s.kw)"
                class="w-full flex items-center justify-between gap-2 px-3 py-2 rounded-lg hover:bg-brand-surface-2 transition group">
                <span class="flex items-center gap-2 text-sm text-brand-ink"><Search class="w-3.5 h-3.5 text-brand-ink-3 group-hover:text-brand-primary" />{{ s.kw }}</span>
                <span class="text-xs text-brand-warning flex items-center gap-1"><TrendingUp class="w-3 h-3" />{{ (s.heat || 0).toLocaleString() }}</span>
              </button>
            </div>
            <div v-if="searchHistory.length > 0" class="p-2">
              <div class="flex items-center justify-between px-3 py-2 text-[11px] text-brand-ink-3">
                <span class="flex items-center gap-1.5"><History class="w-3.5 h-3.5" />搜索历史</span>
                <button @click="clearHistory" class="hover:text-brand-error transition">清除</button>
              </div>
              <div class="flex flex-wrap gap-1.5 px-2 pb-1">
                <button v-for="h in searchHistory" :key="h" @click="doSearch(h)"
                  class="px-3 py-1 rounded-full bg-brand-bg text-xs text-brand-ink-2 hover:bg-brand-primary-subtle hover:text-brand-primary transition border border-brand-line">
                  {{ h }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 热门词云 -->
    <section v-if="!hasQuery" class="rounded-2xl bg-brand-surface border border-brand-line p-6">
      <h2 class="text-sm font-semibold text-brand-ink flex items-center gap-2 mb-4"><TrendingUp class="w-4 h-4 text-brand-error" />实时热搜榜</h2>
      <div class="grid grid-cols-2 md:grid-cols-5 gap-3">
        <div v-for="(item, i) in hotKeywords" :key="item.kw" @click="doSearch(item.kw)"
          class="flex items-center gap-3 p-3 rounded-xl bg-brand-bg border border-brand-line hover:border-brand-primary/40 hover:bg-brand-surface-2 cursor-pointer transition group">
          <div :class="['w-6 h-6 rounded flex items-center justify-center text-xs font-black',
                        i < 3 ? 'bg-brand-error text-white' : 'bg-brand-surface-3 text-brand-ink-3']">{{ i + 1 }}</div>
          <div class="flex-1 min-w-0">
            <div class="text-sm text-brand-ink group-hover:text-brand-primary transition truncate">{{ item.kw }}</div>
            <div class="text-[11px] text-brand-ink-3 mt-0.5 flex items-center gap-1"><TrendingUp class="w-3 h-3" />{{ (item.heat || 0).toLocaleString() }}热度</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 结果页头 -->
    <section v-if="hasQuery" class="rounded-xl bg-brand-surface border border-brand-line px-5 py-4 flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-xs text-brand-ink-3">关键词</div>
        <div class="flex items-center gap-2 mt-0.5">
          <span class="text-lg font-bold text-brand-ink">"{{ keyword }}"</span>
          <span v-if="!loading" class="text-sm text-brand-ink-2">找到 <span class="text-brand-primary font-semibold">{{ total.toLocaleString() }}</span> 条结果</span>
          <span v-else class="text-sm text-brand-ink-3">搜索中...</span>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <select v-model="sortBy" @change="onSortChange" class="h-9 px-3 rounded-lg bg-brand-bg border border-brand-line text-sm text-brand-ink-2 focus:outline-none focus:border-brand-primary">
          <option value="">综合排序</option>
          <option value="sales">销量优先</option>
          <option value="price_asc">价格 ↑</option>
          <option value="price_desc">价格 ↓</option>
          <option value="created_at">最新上架</option>
        </select>
      </div>
    </section>

    <!-- 加载骨架 -->
    <div v-if="loading" class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <div v-for="i in 8" :key="i" class="rounded-xl bg-brand-surface border border-brand-line overflow-hidden animate-pulse">
        <div class="aspect-square bg-brand-surface-2"></div>
        <div class="p-3.5 space-y-2"><div class="h-4 bg-brand-surface-2 rounded w-3/4"></div><div class="h-4 bg-brand-surface-2 rounded w-1/2"></div></div>
      </div>
    </div>

    <!-- 无结果 -->
    <div v-else-if="noResults" class="rounded-2xl bg-brand-surface border border-brand-line p-12 text-center">
      <div class="w-20 h-20 mx-auto mb-5 rounded-2xl bg-brand-warning/15 flex items-center justify-center">
        <Search class="w-10 h-10 text-brand-warning" />
      </div>
      <h3 class="text-xl font-bold text-brand-ink">没有找到 "{{ keyword }}" 相关的商品</h3>
      <p class="text-sm text-brand-ink-3 mt-2 max-w-md mx-auto">换个关键词试试，或者逛逛热门分类专区，说不定有意外惊喜～</p>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-3 max-w-3xl mx-auto mt-8">
        <button v-for="s in noResultSuggests" :key="s.name" @click="router.push(s.path)"
          class="p-5 rounded-xl bg-brand-bg border border-brand-line hover:border-brand-primary/40 hover:bg-brand-surface-2 transition text-left">
          <div class="w-9 h-9 rounded-lg bg-brand-primary/15 text-brand-primary text-xs font-bold flex items-center justify-center mb-3">{{ s.icon }}</div>
          <div class="text-sm font-semibold text-brand-ink flex items-center gap-1">{{ s.name }}<ChevronRight class="w-3.5 h-3.5 text-brand-ink-3" /></div>
        </button>
      </div>
    </div>

    <!-- 结果列表 -->
    <div v-else-if="results.length > 0" class="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-5">
      <ProductCard v-for="p in results" :key="p.id" :product="p" />
    </div>

    <!-- 分页 -->
    <div v-if="hasQuery && totalPages > 1" class="flex items-center justify-center gap-1 pb-4">
      <button @click="changePage(1)" :disabled="pageNum <= 1" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition">&lt;&lt;</button>
      <button @click="changePage(pageNum - 1)" :disabled="pageNum <= 1" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition">&lt;</button>
      <button v-for="n in pageArray" :key="n" @click="changePage(n)"
        :class="['w-9 h-9 rounded-lg text-sm font-medium transition',
                 n === pageNum ? 'bg-brand-primary text-brand-primary-ink border border-brand-primary' : 'border border-brand-line text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40']">{{ n }}</button>
      <button @click="changePage(pageNum + 1)" :disabled="pageNum >= totalPages" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition">&gt;</button>
      <button @click="changePage(totalPages)" :disabled="pageNum >= totalPages" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary disabled:opacity-30 transition">&gt;&gt;</button>
    </div>

    <!-- 点击空白关闭建议 -->
    <div v-if="suggestionOpen" class="fixed inset-0 z-20" @click.self="suggestionOpen = false"></div>

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
