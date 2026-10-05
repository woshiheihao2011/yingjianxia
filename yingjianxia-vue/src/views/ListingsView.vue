<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search, Filter, Eye, Edit2, Trash2, Upload, Package, AlertTriangle,
  CheckCircle2, Clock, XCircle, ChevronDown, ChevronUp, Plus, Sparkles,
  RefreshCw, BarChart3, Tag, TrendingUp, ExternalLink, FileText, ShieldCheck
} from 'lucide-vue-next'
import {
  getMyListingsApi, getSellerStatsApi,
  offShelfProductApi, reListProductApi, publishProductApi, deleteProductApi,
  batchOffShelfApi, batchDeleteApi, batchRelistApi,
  createInspectionTaskApi, batchImportApi
} from '@/api/product'
import { PRODUCT_STATUS, STATUS_TO_LISTING_ID } from '@/constants/product'

const router = useRouter()
const kw = ref('')
const activeStatus = ref('all')
const selectedIds = ref([])
const filtersOpen = ref(true)
const loading = ref(false)
const toast = ref(null)  // { type: 'success'|'error', msg: string }

// 分页
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

// 筛选
const filterCategory = ref('')
const filterCondition = ref('')

// 高级筛选快捷选项
const quickFilters = [
  { key: 'recent7d',  label: '近7天新发布', params: {}, local: (p) => p.publishedAt && (Date.now() - new Date(p.publishedAt).getTime()) < 7 * 86400000 },
  { key: 'price1k3k', label: '价格¥1000-3000', params: { priceMin: 1000, priceMax: 3000 }, local: null },
  { key: 'inspected', label: '开启验机服务', params: { onlyInspected: true }, local: null },
  { key: 'shipFree',  label: '包邮商品', params: {}, local: (p) => p.shipFree === true },
  { key: 'hasStock',  label: '有库存', params: {}, local: (p) => p.stock > 0 },
  { key: 'salesGte10',label: '销量≥10', params: {}, local: (p) => (p.sales || 0) >= 10 },
  { key: 'ratingGte48',label: '评价≥4.8', params: {}, local: (p) => (p.rating || 0) >= 4.8 },
  { key: 'featured',  label: '已加入推荐位', params: {}, local: (p) => p.featured === true },
]
const activeQuickFilters = ref([])  // 已激活的快捷筛选 key 列表

// 排序
const sortField = ref('publishedAt')
const sortDirection = ref('desc')

const products = ref([])

// 统计数据（后端）
const sellerStats = ref(null)

function showToast(type, msg) {
  toast.value = { type, msg }
  setTimeout(() => { toast.value = null }, 3000)
}

// 本地过滤（当前页内快速过滤 + 快捷筛选）
const filtered = computed(() => {
  let list = products.value.filter(p =>
    (activeStatus.value === 'all' || p.status === activeStatus.value) &&
    (!kw.value || p.title.includes(kw.value) || (p.sku || '').includes(kw.value))
  )
  // 应用本地快捷筛选
  for (const f of quickFilters) {
    if (activeQuickFilters.value.includes(f.key) && f.local) {
      list = list.filter(f.local)
    }
  }
  return list
})

const stats = computed(() => {
  const all = total.value
  const onSale = products.value.filter(p => p.status === 'on_sale').length
  const auditing = products.value.filter(p => p.status === 'auditing').length
  const rejected = products.value.filter(p => p.status === 'rejected').length
  const offShelf = products.value.filter(p => p.status === 'off_shelf').length
  const lowStock = products.value.filter(p => p.stock !== null && p.stock <= 2).length
  return [
    { label: '全部商品', count: all, id: 'all', icon: Package },
    { label: '在售中', count: onSale, id: 'on_sale', icon: CheckCircle2 },
    { label: '待审核', count: auditing, id: 'auditing', icon: Clock },
    { label: '审核不通过', count: rejected, id: 'rejected', icon: XCircle },
    { label: '已下架', count: offShelf, id: 'off_shelf', icon: AlertTriangle },
    { label: '库存预警', count: lowStock, id: 'low_stock', icon: AlertTriangle }
  ]
})

const activeStat = computed(() => stats.value.find(s => s.id === activeStatus.value))

const pageArray = computed(() => {
  const arr = []
  const start = Math.max(1, pageNum.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) arr.push(i)
  return arr
})

function toggleSelect(id) {
  const i = selectedIds.value.indexOf(id)
  if (i >= 0) selectedIds.value.splice(i, 1)
  else selectedIds.value.push(id)
}

const allSelected = computed(() => filtered.value.length > 0 && filtered.value.every(p => selectedIds.value.includes(p.id)))
function toggleAll() {
  if (allSelected.value) selectedIds.value = selectedIds.value.filter(id => !filtered.value.find(p => p.id === id))
  else filtered.value.forEach(p => { if (!selectedIds.value.includes(p.id)) selectedIds.value.push(p.id) })
}

function statusClass(s) {
  return {
    on_sale: 'bg-brand-success/15 text-brand-success border-brand-success/30',
    auditing: 'bg-brand-warning/15 text-brand-warning border-brand-warning/30',
    rejected: 'bg-brand-error/15 text-brand-error border-brand-error/30',
    off_shelf: 'bg-brand-ink-2/15 text-brand-ink-2 border-brand-ink-2/30',
    low_stock: 'bg-brand-info/15 text-brand-info border-brand-info/30'
  }[s]
}

/** 将后端 ProductDetailResp 映射为列表行 */
function mapRespToRow(item) {
  const statusInfo = PRODUCT_STATUS[item.status] || { name: '未知', class: '' }
  const listingStatus = STATUS_TO_LISTING_ID[item.status] || 'draft'
  const isLowStock = item.stock !== null && item.stock <= 2 && listingStatus === 'on_sale'
  return {
    id: item.id,
    title: item.title,
    cover: item.imageUrls && item.imageUrls.length > 0 ? item.imageUrls[0] : '',
    price: Number(item.price) || 0,
    stock: item.stock ?? 0,
    sales: item.sales ?? 0,
    views: item.viewCount ?? 0,
    status: isLowStock ? 'low_stock' : listingStatus,
    statusName: isLowStock ? `库存预警(${item.stock})` : statusInfo.name,
    category: item.categoryName || '',
    sku: item.sku || '',
    publishedAt: item.publishedAt ? String(item.publishedAt).replace('T', ' ').slice(0, 16) : '',
    rejectReason: item.rejectReason || ''
  }
}

async function loadListings(page = 1) {
  // 防御 MouseEvent 传入（@click="loadListings" 会传事件对象）
  if (typeof page !== 'number' || isNaN(page)) page = pageNum.value || 1
  loading.value = true
  pageNum.value = page
  try {
    const params = { pageNum: page, pageSize: pageSize.value, sortField: sortField.value, sortDirection: sortDirection.value }
    if (kw.value) params.keyword = kw.value
    if (filterCategory.value) params.categoryIds = [filterCategory.value]
    if (filterCondition.value) params.conditionLevels = [filterCondition.value]
    // 合并快捷筛选中后端支持的参数
    for (const f of quickFilters) {
      if (activeQuickFilters.value.includes(f.key)) {
        Object.assign(params, f.params)
      }
    }
    const res = await getMyListingsApi(params)
    const records = (res && res.records) || []
    products.value = records.map(mapRespToRow)
    total.value = (res && res.total) || 0
    totalPages.value = (res && res.totalPages) || 0
  } catch (e) {
    console.error('加载我的发布失败', e)
    showToast('error', '加载失败，请稍后重试')
    // 保留旧数据，不清空列表
  } finally {
    loading.value = false
  }
}

// 快捷筛选切换
function toggleQuickFilter(key) {
  const i = activeQuickFilters.value.indexOf(key)
  if (i >= 0) {
    activeQuickFilters.value.splice(i, 1)
  } else {
    activeQuickFilters.value.push(key)
  }
  loadListings(1)
}

// 排序变更
const sortOptions = [
  { value: 'publishedAt', label: '发布时间倒序', direction: 'desc' },
  { value: 'sales',       label: '销量优先',     direction: 'desc' },
  { value: 'price',        label: '价格优先',     direction: 'desc' },
  { value: 'stock',        label: '库存优先',     direction: 'desc' },
  { value: 'viewCount',   label: '曝光量优先',   direction: 'desc' },
]
function onSortChange(e) {
  const opt = sortOptions.find(o => o.label === e.target.value)
  if (opt) {
    sortField.value = opt.value
    sortDirection.value = opt.direction
  }
  loadListings(1)
}

// 搜索防抖
let searchTimer = null
watch(kw, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadListings(1), 400)
})

function changePage(n) {
  if (n < 1 || n > totalPages.value) return
  loadListings(n)
}

// 批量导入
const importInput = ref(null)
const importing = ref(false)

function handleBatchImport() {
  importInput.value?.click()
}

async function onImportFile(e) {
  const file = e.target.files[0]
  if (!file) return
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await batchImportApi(formData)
    const msg = `导入完成：成功 ${res.successCount} 条，失败 ${res.failCount} 条`
    showToast(res.failCount > 0 ? 'error' : 'success', msg)
    loadListings(1)
  } catch (err) {
    showToast('error', err.message || '批量导入失败')
  } finally {
    importing.value = false
    e.target.value = ''
  }
}

// 行操作
async function handleOffShelf(p) {
  try {
    await offShelfProductApi(p.id)
    showToast('success', '已下架')
    loadListings(pageNum.value)
  } catch (e) { showToast('error', e.message || '下架失败') }
}
async function handleRelist(p) {
  try {
    await reListProductApi(p.id)
    showToast('success', '已重新提交上架')
    loadListings(pageNum.value)
  } catch (e) { showToast('error', e.message || '上架失败') }
}
async function handleResubmit(p) {
  try {
    await publishProductApi(p.id)
    showToast('success', '已重新提交审核')
    loadListings(pageNum.value)
  } catch (e) { showToast('error', e.message || '提交失败') }
}
async function handleDelete(p) {
  if (!confirm(`确认删除「${p.title}」？此操作不可撤销。`)) return
  try {
    await deleteProductApi(p.id)
    showToast('success', '已删除')
    loadListings(pageNum.value)
  } catch (e) { showToast('error', e.message || '删除失败') }
}

// 批量操作
async function handleBatchOffShelf() {
  if (!selectedIds.value.length) return
  try {
    await batchOffShelfApi(selectedIds.value)
    showToast('success', `已批量下架 ${selectedIds.value.length} 件`)
    selectedIds.value = []
    loadListings(pageNum.value)
  } catch (e) { showToast('error', e.message || '批量下架失败') }
}
async function handleBatchRelist() {
  if (!selectedIds.value.length) return
  try {
    await batchRelistApi(selectedIds.value)
    showToast('success', `已批量上架 ${selectedIds.value.length} 件`)
    selectedIds.value = []
    loadListings(pageNum.value)
  } catch (e) { showToast('error', e.message || '批量上架失败') }
}
async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  if (!confirm(`确认批量删除 ${selectedIds.value.length} 件商品？不可撤销。`)) return
  try {
    await batchDeleteApi(selectedIds.value)
    showToast('success', `已批量删除 ${selectedIds.value.length} 件`)
    selectedIds.value = []
    loadListings(pageNum.value)
  } catch (e) { showToast('error', e.message || '批量删除失败') }
}
async function handleBatchInspection() {
  if (!selectedIds.value.length) return
  let ok = 0
  for (const pid of selectedIds.value) {
    try { await createInspectionTaskApi(pid); ok++ } catch (e) { /* 单个失败跳过 */ }
  }
  showToast('success', `已为 ${ok} 件商品创建验机任务`)
  selectedIds.value = []
  loadListings(pageNum.value)
}

// 加载卖家统计
async function loadStats() {
  try {
    const data = await getSellerStatsApi()
    if (data) sellerStats.value = data
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

onMounted(() => {
  loadListings()
  loadStats()
})
</script>

<template>
  <div class="container-app space-y-6">
    <div class="text-xs text-brand-ink-3">卖家中心 / 我的发布</div>

    <!-- 统计卡 -->
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
      <button v-for="s in stats" :key="s.id" @click="activeStatus = s.id"
        :class="['p-4 rounded-xl border transition text-left',
                 activeStatus === s.id ? 'bg-brand-primary/10 border-brand-primary/40 ring-1 ring-brand-primary/20' : 'bg-brand-surface border-brand-line hover:border-brand-primary/40']">
        <div class="flex items-center justify-between">
          <div :class="['w-8 h-8 rounded-lg flex items-center justify-center', activeStatus === s.id ? 'bg-brand-primary/20 text-brand-primary' : 'bg-brand-bg text-brand-ink-3']">
            <component :is="s.icon" class="w-4 h-4" />
          </div>
          <span class="text-xs text-brand-ink-3">{{ activeStatus === s.id ? '当前筛选' : '' }}</span>
        </div>
        <div class="text-2xl font-black text-brand-ink mt-3">{{ s.count }}</div>
        <div class="text-xs text-brand-ink-3 mt-0.5">{{ s.label }}</div>
      </button>
    </div>

    <!-- 搜索+操作条 -->
    <div class="rounded-xl bg-brand-surface border border-brand-line p-4 md:p-5 space-y-4">
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div class="flex flex-wrap items-center gap-2 flex-1">
          <div class="flex h-10 rounded-lg bg-brand-bg border border-brand-line focus-within:border-brand-primary transition max-w-md w-full md:w-80">
            <div class="pl-3 flex items-center text-brand-ink-3"><Search class="w-4 h-4" /></div>
            <input v-model="kw" placeholder="搜索标题、SKU编号..." class="flex-1 px-3 text-sm bg-transparent placeholder:text-brand-ink-3 focus:outline-none" />
          </div>
          <select v-model="filterCategory" @change="loadListings(1)" class="h-10 px-3 rounded-lg bg-brand-bg border border-brand-line text-sm text-brand-ink-2 focus:outline-none focus:border-brand-primary">
            <option value="">全部分类</option><option value="1">显卡</option><option value="2">CPU</option><option value="3">主板</option><option value="4">内存</option><option value="5">SSD</option><option value="6">笔记本</option>
          </select>
          <select v-model="filterCondition" @change="loadListings(1)" class="h-10 px-3 rounded-lg bg-brand-bg border border-brand-line text-sm text-brand-ink-2 focus:outline-none focus:border-brand-primary">
            <option value="">全部成色</option><option value="1">全新</option><option value="2">准新/99新</option><option value="3">95成新</option><option value="4">9成新</option><option value="5">战损版</option>
          </select>
          <button @click="filtersOpen = !filtersOpen" class="px-4 h-10 rounded-lg bg-brand-bg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
            <Filter class="w-4 h-4" />更多筛选
            <ChevronUp v-if="filtersOpen" class="w-3 h-3" />
            <ChevronDown v-else class="w-3 h-3" />
          </button>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <button @click="router.push('/sell')" class="px-4 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition flex items-center gap-2">
            <Plus class="w-4 h-4" />发布新商品
          </button>
          <button @click="handleBatchImport" :disabled="importing" class="px-4 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2 disabled:opacity-50">
            <Upload class="w-4 h-4" :class="{ 'animate-spin': importing }" />批量导入
          </button>
          <input ref="importInput" type="file" accept=".xlsx,.xls,.csv" class="hidden" @change="onImportFile" />
          <button @click="() => loadListings(1)" class="px-4 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
            <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />刷新
          </button>
        </div>
      </div>

      <div v-if="filtersOpen" class="flex flex-wrap items-center gap-2 p-4 rounded-lg bg-brand-bg border border-brand-line-subtle">
        <span class="text-xs text-brand-ink-3 mr-1">高级筛选：</span>
        <button v-for="f in quickFilters" :key="f.key" @click="toggleQuickFilter(f.key)"
          :class="['px-3 py-1.5 rounded-md text-xs border transition',
                   activeQuickFilters.includes(f.key) ? 'bg-brand-primary/15 border-brand-primary text-brand-primary font-medium' : 'bg-brand-surface text-brand-ink-2 border-brand-line hover:border-brand-primary/30 hover:text-brand-primary']">
          {{ f.label }}
        </button>
        <div class="ml-auto flex items-center gap-2 text-xs text-brand-ink-3">
          排序：
          <select @change="onSortChange" class="h-8 px-2 rounded-md bg-brand-surface border border-brand-line focus:outline-none focus:border-brand-primary">
            <option v-for="o in sortOptions" :key="o.value" :value="o.label" :selected="o.value === sortField">{{ o.label }}</option>
          </select>
        </div>
      </div>

      <!-- 批量操作 -->
      <div v-if="selectedIds.length > 0" class="flex flex-wrap items-center gap-3 p-3 rounded-lg bg-brand-primary/10 border border-brand-primary/30">
        <span class="text-sm font-semibold text-brand-primary">已选择 {{ selectedIds.length }} 件商品</span>
        <div class="flex flex-wrap items-center gap-2">
          <button @click="handleBatchRelist" class="px-3 h-8 rounded-md bg-brand-surface border border-brand-line text-xs text-brand-ink-2 hover:border-brand-success/40 hover:text-brand-success transition flex items-center gap-1"><CheckCircle2 class="w-3.5 h-3.5" />批量上架</button>
          <button @click="handleBatchOffShelf" class="px-3 h-8 rounded-md bg-brand-surface border border-brand-line text-xs text-brand-ink-2 hover:border-brand-warning/40 hover:text-brand-warning transition flex items-center gap-1"><Clock class="w-3.5 h-3.5" />批量下架</button>
          <button @click="handleBatchInspection" class="px-3 h-8 rounded-md bg-brand-surface border border-brand-line text-xs text-brand-ink-2 hover:border-brand-primary/40 hover:text-brand-primary transition flex items-center gap-1"><Sparkles class="w-3.5 h-3.5" />开启验机</button>
          <button @click="handleBatchDelete" class="px-3 h-8 rounded-md bg-brand-surface border border-brand-line text-xs text-brand-ink-2 hover:border-brand-error/40 hover:text-brand-error transition flex items-center gap-1"><Trash2 class="w-3.5 h-3.5" />批量删除</button>
        </div>
        <button @click="selectedIds = []" class="ml-auto text-xs text-brand-ink-3 hover:text-brand-primary transition">取消选择</button>
      </div>
    </div>

    <!-- 商品表格 -->
    <div class="rounded-2xl bg-brand-surface border border-brand-line overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-sm min-w-[1100px]">
          <thead class="bg-brand-bg/60 border-b border-brand-line">
            <tr>
              <th class="w-12 px-4 py-3"><input type="checkbox" :checked="allSelected" @change="toggleAll" class="w-4 h-4 accent-brand-primary" /></th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-brand-ink-3">商品</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-brand-ink-3 w-20">类目 / SKU</th>
              <th class="text-right px-4 py-3 text-xs font-semibold text-brand-ink-3 w-24">售价</th>
              <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-20">库存</th>
              <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-24">销量</th>
              <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-24">曝光</th>
              <th class="text-center px-4 py-3 text-xs font-semibold text-brand-ink-3 w-24">状态</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-brand-ink-3 w-32">发布时间</th>
              <th class="text-right px-4 py-3 text-xs font-semibold text-brand-ink-3 w-48 shrink-0">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in filtered" :key="p.id" class="border-t border-brand-line-subtle hover:bg-brand-surface-2/60 transition">
              <td class="px-4 py-4"><input type="checkbox" :checked="selectedIds.includes(p.id)" @change="toggleSelect(p.id)" class="w-4 h-4 accent-brand-primary" /></td>
              <td class="px-4 py-4">
                <div class="flex items-start gap-3">
                  <div class="w-16 h-16 rounded-lg overflow-hidden bg-brand-bg border border-brand-line shrink-0">
                    <img :src="p.cover" class="w-full h-full object-cover" />
                  </div>
                  <div class="min-w-0">
                    <div class="text-sm text-brand-ink line-clamp-2 leading-snug hover:text-brand-primary cursor-pointer" @click="router.push('/product/' + p.id)">{{ p.title }}</div>
                    <div class="mt-1.5 flex flex-wrap gap-1">
                      <span class="text-[10px] px-1.5 py-0.5 rounded bg-brand-success/15 text-brand-success border border-brand-success/30">7天无理由</span>
                      <span class="text-[10px] px-1.5 py-0.5 rounded bg-brand-info/15 text-brand-info border border-brand-info/30">验机服务</span>
                    </div>
                    <div v-if="p.status === 'rejected' && p.rejectReason" class="mt-1.5 text-[11px] text-brand-error flex items-center gap-1">
                      <AlertTriangle class="w-3 h-3" />驳回理由：{{ p.rejectReason }}
                    </div>
                  </div>
                </div>
              </td>
              <td class="px-4 py-4">
                <div class="text-xs text-brand-ink font-medium">{{ p.category }}</div>
                <div class="text-[10px] text-brand-ink-3 mt-1 font-mono">{{ p.sku }}</div>
              </td>
              <td class="px-4 py-4 text-right">
                <div class="font-bold text-brand-error">¥{{ p.price.toLocaleString() }}</div>
              </td>
              <td class="px-4 py-4 text-center">
                <div :class="['font-semibold', p.stock === 0 ? 'text-brand-error' : p.stock <= 2 ? 'text-brand-warning' : 'text-brand-ink']">
                  {{ p.stock }}
                </div>
                <div v-if="p.stock <= 2" class="text-[10px] text-brand-warning">库存紧张</div>
              </td>
              <td class="px-4 py-4 text-center text-brand-ink font-medium">{{ p.sales }}</td>
              <td class="px-4 py-4 text-center text-brand-ink-2">{{ p.views.toLocaleString() }}</td>
              <td class="px-4 py-4 text-center">
                <span :class="['inline-block px-2 py-0.5 rounded text-[11px] font-semibold border', statusClass(p.status)]">{{ p.statusName }}</span>
              </td>
              <td class="px-4 py-4 text-xs text-brand-ink-3 whitespace-nowrap">{{ p.publishedAt }}</td>
              <td class="px-4 py-4">
                <div class="flex items-center justify-end gap-1 flex-wrap">
                  <button @click="router.push('/product/' + p.id)" class="p-1.5 rounded-md hover:bg-brand-bg text-brand-ink-3 hover:text-brand-primary transition" title="预览"><Eye class="w-3.5 h-3.5" /></button>
                  <button @click="router.push('/sell?id=' + p.id)" class="p-1.5 rounded-md hover:bg-brand-bg text-brand-ink-3 hover:text-brand-primary transition" title="编辑"><Edit2 class="w-3.5 h-3.5" /></button>
                  <button v-if="p.status === 'on_sale'" @click="handleOffShelf(p)" class="p-1.5 rounded-md hover:bg-brand-warning/10 text-brand-ink-3 hover:text-brand-warning transition" title="下架"><XCircle class="w-3.5 h-3.5" /></button>
                  <button v-else-if="p.status === 'off_shelf' || p.status === 'low_stock'" @click="handleRelist(p)" class="p-1.5 rounded-md hover:bg-brand-success/10 text-brand-ink-3 hover:text-brand-success transition" title="上架"><CheckCircle2 class="w-3.5 h-3.5" /></button>
                  <button v-if="p.status === 'rejected'" @click="handleResubmit(p)" class="p-1.5 rounded-md hover:bg-brand-info/10 text-brand-ink-3 hover:text-brand-info transition" title="重新提交"><RefreshCw class="w-3.5 h-3.5" /></button>
                  <button @click="handleDelete(p)" class="p-1.5 rounded-md hover:bg-brand-error/10 text-brand-ink-3 hover:text-brand-error transition" title="删除"><Trash2 class="w-3.5 h-3.5" /></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="flex flex-col md:flex-row items-center justify-between gap-3 px-5 py-4 border-t border-brand-line">
        <div class="text-xs text-brand-ink-3">显示 {{ filtered.length }} / 共 {{ total }} 条 · 每页 {{ pageSize }} 条</div>
        <div class="flex items-center gap-1">
          <button @click="changePage(pageNum - 1)" :disabled="pageNum <= 1" class="w-8 h-8 rounded-md border border-brand-line text-brand-ink-3 hover:text-brand-primary hover:border-brand-primary/40 disabled:opacity-40 transition text-xs">&lt;</button>
          <button v-for="n in pageArray" :key="n" @click="changePage(n)"
            :class="['w-8 h-8 rounded-md text-xs font-semibold transition', n === pageNum ? 'bg-brand-primary text-brand-primary-ink' : 'border border-brand-line text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40']">{{ n }}</button>
          <button @click="changePage(pageNum + 1)" :disabled="pageNum >= totalPages" class="w-8 h-8 rounded-md border border-brand-line text-brand-ink-3 hover:text-brand-primary hover:border-brand-primary/40 disabled:opacity-40 transition text-xs">&gt;</button>
        </div>
      </div>
    </div>

    <!-- 底部数据 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="p-5 rounded-xl bg-brand-surface border border-brand-line">
        <div class="text-xs font-semibold text-brand-ink-3 mb-3 flex items-center gap-1.5"><BarChart3 class="w-3.5 h-3.5 text-brand-primary" />本周上架表现</div>
        <div class="space-y-2.5 text-xs">
          <div class="flex justify-between"><span class="text-brand-ink-3">发布量</span><span class="font-semibold text-brand-ink">{{ sellerStats?.totalPublished ?? '--' }} 件</span></div>
          <div class="flex justify-between"><span class="text-brand-ink-3">审核通过率</span><span class="font-semibold text-brand-success">{{ sellerStats?.approvalRate != null ? sellerStats.approvalRate + '%' : '--' }}</span></div>
          <div class="flex justify-between"><span class="text-brand-ink-3">平均审核时长</span><span class="font-semibold text-brand-info">{{ sellerStats?.avgReviewTime ?? '--' }}</span></div>
          <div class="flex justify-between"><span class="text-brand-ink-3">首周成交率</span><span class="font-semibold text-brand-warning">{{ sellerStats?.firstWeekSaleRate != null ? sellerStats.firstWeekSaleRate + '%' : '--' }}</span></div>
        </div>
      </div>
      <div class="p-5 rounded-xl bg-brand-surface border border-brand-line">
        <div class="text-xs font-semibold text-brand-ink-3 mb-3 flex items-center gap-1.5"><TrendingUp class="w-3.5 h-3.5 text-brand-success" />TOP5 爆款榜</div>
        <div class="space-y-2">
          <div v-if="!sellerStats?.topProducts?.length" class="text-xs text-brand-ink-3 text-center py-4">暂无数据</div>
          <div v-for="(rank, i) in (sellerStats?.topProducts || []).slice(0, 5)" :key="i" class="flex items-center gap-2 text-xs">
            <div :class="['w-5 h-5 rounded flex items-center justify-center text-[10px] font-black', i < 3 ? 'bg-brand-error text-white' : 'bg-brand-surface-2 text-brand-ink-3']">{{ i+1 }}</div>
            <span class="text-brand-ink-2 truncate">{{ rank.title }} ¥{{ rank.price }} · {{ rank.sales }}单</span>
          </div>
        </div>
      </div>
      <div class="p-5 rounded-xl bg-brand-surface border border-brand-line">
        <div class="text-xs font-semibold text-brand-ink-3 mb-3 flex items-center gap-1.5"><FileText class="w-3.5 h-3.5 text-brand-warning" />发布规范提醒</div>
        <ul class="space-y-2 text-[11px] text-brand-ink-2 leading-relaxed">
          <li class="flex gap-2"><span class="text-brand-warning">1.</span>标题 2-30 字，清晰描述品牌+型号+规格</li>
          <li class="flex gap-2"><span class="text-brand-warning">2.</span>图片≥3张，建议包含正面/背面/标签/瑕疵</li>
          <li class="flex gap-2"><span class="text-brand-warning">3.</span>如实标注成色，瑕疵务必在描述中写明</li>
          <li class="flex gap-2"><span class="text-brand-warning">4.</span>禁止使用"全新"描述仅拆封的二手商品</li>
        </ul>
        <button class="mt-4 w-full h-9 rounded-lg border border-brand-primary/40 text-xs text-brand-primary hover:bg-brand-primary-subtle transition flex items-center justify-center gap-1">
          查看完整卖家规范<ExternalLink class="w-3 h-3" />
        </button>
      </div>
    </div>

    <!-- Toast 提示 -->
    <div v-if="toast" class="fixed top-6 right-6 z-50 max-w-sm">
      <div :class="['px-4 py-3 rounded-lg shadow-lg text-sm font-medium animate-fade-in', toast.type === 'success' ? 'bg-brand-success text-white' : 'bg-brand-error text-white']">
        {{ toast.msg }}
      </div>
    </div>
  </div>
</template>
