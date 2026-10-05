<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import {
  ShoppingCart, Heart, ShieldCheck, Store, Eye, ChevronLeft, ChevronRight,
  Star, Truck, RotateCcw, BadgeCheck, MessageSquare, MapPin, Award,
  AlertCircle, Copy, ExternalLink, CheckCircle2, Clock
} from 'lucide-vue-next'
import { getProductDetailApi, getInspectionReportApi } from '@/api/product'
import { useUserStore } from '@/stores/user'
import { LEVEL_TO_CONDITION } from '@/constants/product'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const productId = computed(() => route.params.id || '1001')
const currentImage = ref(0)
const quantity = ref(1)
const favorited = ref(false)
const adding = ref(false)
const loading = ref(false)
const notFound = ref(false)

/** 默认占位图（当后端无图片时使用） */
const DEFAULT_PLACEHOLDER = 'https://placehold.co/600x600/1E2026/00D4AA?text=No+Image'

/** 空商品模板（API 返回前展示骨架） */
const emptyProduct = {
  id: null, title: '', price: 0, originalPrice: 0, marketPrice: 0,
  condition: '', conditionName: '', subTitle: '',
  coverImage: '', images: [], specs: [], highlights: [],
  description: '',
  shop: { id: null, name: '未知店铺', logo: '?', rating: 0, reviewCount: 0, soldCount: 0, region: '', level: '', responseTime: '', refundRate: '', joinedAt: '', badges: [] },
  viewCount: 0, wantCount: 0, soldCount: 0, publishedAt: '',
  serviceTags: [], inspection: null, hasInspection: false
}

const product = ref({ ...emptyProduct })

const related = ref([])

const descTab = ref('desc')

/** 将后端 ProductDetailResp 映射为页面展示模型 */
function mapRespToProduct(item) {
  const condId = LEVEL_TO_CONDITION[item.conditionLevel] || ''
  const rawImages = Array.isArray(item.imageUrls) ? item.imageUrls.filter(Boolean) : []
  const images = rawImages.length > 0 ? rawImages : [DEFAULT_PLACEHOLDER]
  const specs = (item.specs || []).map(s => ({ k: s.name, v: s.value }))
  // 服务标签：根据后端字段动态生成
  const serviceTags = []
  if (item.hasInspection) serviceTags.push({ icon: ShieldCheck, label: '官方验机', color: 'text-brand-info bg-brand-info/15' })
  serviceTags.push({ icon: RotateCcw, label: '7天无理由', color: 'text-brand-warning bg-brand-warning/15' })
  // 店铺信息（后端只冗余基础字段，其余用默认值占位）
  const shop = {
    id: item.shopId,
    name: item.shopName || '未知店铺',
    logoUrl: item.shopLogoUrl || '',
    logo: (item.shopName || '?').slice(0, 1),
    rating: item.sellerCreditScore ? (item.sellerCreditScore / 20).toFixed(1) : '4.5',
    reviewCount: 0, soldCount: 0, region: item.location || '',
    level: item.shopVerified ? '认证店铺' : '普通店铺',
    responseTime: '≤ 30分钟', refundRate: '', joinedAt: '', badges: []
  }
  if (item.shopVerified) {
    shop.badges.push({ icon: BadgeCheck, label: '实名认证', color: 'text-brand-primary bg-brand-primary/15 border-brand-primary/30' })
  }
  return {
    id: item.id,
    sellerId: item.sellerId,
    title: item.title,
    price: Number(item.price) || 0,
    originalPrice: Number(item.originalPrice) || 0,
    marketPrice: Number(item.originalPrice) || 0,
    condition: condId,
    conditionName: item.conditionName || '',
    subTitle: item.subTitle || '',
    coverImage: images[0] || '',
    images: images,
    specs,
    highlights: [],
    description: item.description || '',
    shop,
    viewCount: item.viewCount || 0,
    wantCount: item.wantCount || 0,
    soldCount: item.sales || 0,
    publishedAt: item.publishedAt ? String(item.publishedAt).replace('T', ' ').slice(0, 10) : '',
    serviceTags,
    inspection: null,
    hasInspection: !!item.hasInspection
  }
}

/** 加载验机报告 */
async function loadInspection(pid) {
  try {
    const report = await getInspectionReportApi(pid)
    if (report && report.id) {
      // 后端字段：grade(等级) / overallResult(1=合格,0=不合格) / items / signature
      // 前端展示：score(展示用) / level(等级文本)
      const passed = report.overallResult === 1 || report.overallResult === true
      product.value.inspection = {
        id: report.id,
        score: passed ? 100 : 0,
        level: report.grade || (passed ? '合格' : '不合格'),
        items: (report.items || []).map(it => ({
          label: it.itemName || it.label || '',
          result: it.result || (it.isPassed ? '通过' : '未通过'),
          detail: it.detail || it.remark || ''
        })),
        signature: report.signature || ''
      }
    }
  } catch (e) {
    // 验机报告不存在（code=3002）时不阻塞主流程
    product.value.inspection = null
  }
}

/** 跳转到卖家店铺页（后端已修复 shopId，按 shopId 路由） */
function goToShop() {
  const sid = product.value.shop.id
  if (!sid) {
    console.warn('[ProductView] 缺少 shopId，无法跳转店铺页')
    return
  }
  router.push('/seller/' + sid)
}

async function loadProduct() {
  loading.value = true
  notFound.value = false
  try {
    const item = await getProductDetailApi(productId.value)
    if (item) {
      product.value = mapRespToProduct(item)
      // 验机报告接口已公开（后端 BUG-02 已修复），未登录用户也可查看
      if (item.hasInspection) {
        await loadInspection(item.id)
      }
    } else {
      notFound.value = true
    }
  } catch (e) {
    console.error('加载商品详情失败', e)
    // 商品不存在或已下架（后端 code=2010/30001/30002）
    if (e.code === 2010 || e.code === 30001 || e.code === 30002 || e.message?.includes('不存在') || e.message?.includes('下架')) {
      notFound.value = true
    } else {
      product.value = { ...emptyProduct }
    }
  } finally {
    loading.value = false
  }
}

function prevImg() { const len = product.value.images.length; if (len) currentImage.value = (currentImage.value - 1 + len) % len }
function nextImg() { const len = product.value.images.length; if (len) currentImage.value = (currentImage.value + 1) % len }

async function addCart() {
  adding.value = true
  cartStore.addToCart({
    id: product.value.id, title: product.value.title,
    price: product.value.price, condition: product.value.condition,
    sellerName: product.value.shop.name, shopId: product.value.shop.id,
    coverImage: product.value.coverImage
  }, quantity.value)
  setTimeout(() => { adding.value = false }, 600)
}

async function buyNow() {
  await addCart()
  router.push('/cart')
}

onMounted(() => {
  window.scrollTo({ top: 0 })
  loadProduct()
})
</script>

<template>
  <div class="container-app space-y-6">
    <!-- 商品不存在/已下架 -->
    <div v-if="notFound" class="rounded-2xl bg-brand-surface border border-brand-line p-16 text-center">
      <div class="w-20 h-20 mx-auto mb-6 rounded-2xl bg-brand-warning/15 flex items-center justify-center">
        <AlertCircle class="w-10 h-10 text-brand-warning" />
      </div>
      <h2 class="text-2xl font-bold text-brand-ink mb-3">商品不存在或已下架</h2>
      <p class="text-sm text-brand-ink-3 mb-8 max-w-md mx-auto">该商品可能已被卖家删除或下架，您可以逛逛其他二手好物，说不定有意外惊喜～</p>
      <div class="flex items-center justify-center gap-3">
        <button @click="router.push('/browse')" class="px-6 h-11 rounded-lg bg-brand-primary text-brand-primary-ink font-medium hover:bg-brand-primary-hover transition">去逛商品</button>
        <button @click="router.back()" class="px-6 h-11 rounded-lg border border-brand-line text-brand-ink-2 hover:border-brand-primary/40 hover:text-brand-primary transition">返回上一页</button>
      </div>
    </div>

    <!-- 加载骨架 -->
    <div v-else-if="loading" class="flex items-center justify-center py-32">
      <div class="w-10 h-10 rounded-full border-2 border-brand-primary border-t-transparent animate-spin"></div>
      <span class="ml-3 text-sm text-brand-ink-3">加载中...</span>
    </div>

    <!-- 商品详情（仅在加载完成且未标记 notFound 时渲染） -->
    <template v-else>
    <div class="text-xs text-brand-ink-3 flex items-center gap-1.5 pt-2">
      <button @click="router.push('/')" class="hover:text-brand-primary">首页</button>
      <ChevronRight class="w-3 h-3" />
      <button @click="router.push('/browse')" class="hover:text-brand-primary">找货市场</button>
      <ChevronRight class="w-3 h-3" />
      <button @click="router.push('/browse?category=gpu')" class="hover:text-brand-primary">显卡</button>
      <ChevronRight class="w-3 h-3" />
      <span class="text-brand-ink-2 truncate max-w-[280px]">{{ product.title }}</span>
    </div>

    <!-- 主体区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-[1fr_1.1fr] gap-6 lg:gap-10">
      <!-- 左侧：图片 -->
      <div class="space-y-4">
        <div class="relative rounded-2xl overflow-hidden bg-brand-surface border border-brand-line aspect-square group">
          <img v-if="product.images && product.images.length" :src="product.images[currentImage]" class="w-full h-full object-cover" />
          <div v-else class="w-full h-full flex items-center justify-center text-brand-ink-3 text-sm">暂无图片</div>
          <button v-if="product.images && product.images.length > 1" @click="prevImg" class="absolute left-3 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-black/40 text-white opacity-0 group-hover:opacity-100 hover:bg-brand-primary transition flex items-center justify-center">
            <ChevronLeft class="w-5 h-5" />
          </button>
          <button v-if="product.images && product.images.length > 1" @click="nextImg" class="absolute right-3 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-black/40 text-white opacity-0 group-hover:opacity-100 hover:bg-brand-primary transition flex items-center justify-center">
            <ChevronRight class="w-5 h-5" />
          </button>
          <!-- 验机浮标 -->
          <div v-if="product.inspection" class="absolute top-4 left-4 px-3 py-2 rounded-xl bg-brand-info/90 backdrop-blur text-white flex items-center gap-2">
            <ShieldCheck class="w-4 h-4" />
            <div>
              <div class="text-[11px] font-bold">官方验机 · {{ product.inspection.score }} 分</div>
              <div class="text-[10px] opacity-90">AI 假货识别通过</div>
            </div>
          </div>
          <!-- 状态徽章 -->
          <div class="absolute top-4 right-4 flex gap-2">
            <span v-if="product.conditionName" class="px-2.5 py-1 rounded-lg bg-brand-success/15 text-brand-success backdrop-blur text-xs font-semibold border border-brand-success/30">{{ product.conditionName }}</span>
          </div>
        </div>
        <!-- 缩略图 -->
        <div class="grid grid-cols-5 gap-2.5">
          <button v-for="(img, i) in product.images" :key="i" @click="currentImage = i"
            :class="['rounded-lg overflow-hidden aspect-square border-2 transition', currentImage === i ? 'border-brand-primary ring-2 ring-brand-primary/25' : 'border-transparent hover:border-brand-primary/40']">
            <img :src="img" class="w-full h-full object-cover" />
          </button>
        </div>
      </div>

      <!-- 右侧：信息 -->
      <div class="space-y-4">
        <div>
          <!-- 标签 -->
          <div class="flex flex-wrap gap-1.5 mb-3">
            <span v-for="s in product.serviceTags" :key="s.label"
              :class="['inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[11px] font-semibold border border-current/25', s.color]">
              <component :is="s.icon" class="w-3 h-3" />{{ s.label }}
            </span>
          </div>
          <h1 class="text-xl md:text-2xl font-bold text-brand-ink leading-snug">{{ product.title }}</h1>
          <p class="text-sm text-brand-ink-2 mt-2 leading-relaxed">{{ product.subTitle }}</p>
        </div>

        <!-- 价格卡 -->
        <div class="rounded-xl p-5 bg-gradient-to-br from-brand-error/10 via-brand-surface to-brand-bg border border-brand-line relative overflow-hidden">
          <div class="absolute right-0 top-0 w-40 h-40 bg-brand-primary/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2"></div>
          <div class="relative flex items-end gap-3 mb-2">
            <span class="text-xs text-brand-ink-3">现价</span>
            <span class="text-4xl font-black text-brand-error">¥{{ product.price.toLocaleString() }}</span>
            <template v-if="product.originalPrice && product.originalPrice > product.price">
              <span class="text-sm text-brand-ink-3 line-through mb-1">原价 ¥{{ product.originalPrice.toLocaleString() }}</span>
              <span class="px-2 py-0.5 rounded-full bg-brand-error/15 text-brand-error text-xs font-semibold border border-brand-error/30 mb-1">省 ¥{{ (product.originalPrice - product.price).toLocaleString() }}</span>
            </template>
          </div>
          <div class="text-xs text-brand-ink-3">平台参考价 ¥{{ (product.marketPrice || product.price).toLocaleString() }} · 已累计 <span class="text-brand-warning font-semibold">{{ product.wantCount }} 人想要</span></div>
        </div>

        <!-- 服务亮点 -->
        <div v-if="product.highlights && product.highlights.length" class="grid grid-cols-2 gap-2">
          <div v-for="h in product.highlights" :key="h" class="flex items-center gap-2 p-3 rounded-lg bg-brand-surface border border-brand-line text-xs text-brand-ink-2">
            <CheckCircle2 class="w-3.5 h-3.5 text-brand-success shrink-0" />{{ h }}
          </div>
        </div>

        <!-- 规格快览 -->
        <div class="rounded-xl bg-brand-surface border border-brand-line overflow-hidden">
          <div class="grid grid-cols-2 divide-x divide-y divide-brand-line-subtle">
            <div v-for="s in product.specs.slice(0, 6)" :key="s.k" class="px-4 py-3 flex text-xs">
              <div class="w-20 shrink-0 text-brand-ink-3">{{ s.k }}</div>
              <div class="text-brand-ink font-medium">{{ s.v }}</div>
            </div>
          </div>
        </div>

        <!-- 卖家店铺摘要 -->
        <div class="rounded-xl bg-brand-surface border border-brand-line p-4">
          <div class="flex items-center gap-4">
            <button @click="goToShop" class="flex items-center gap-3 shrink-0 group">
              <div class="w-12 h-12 rounded-xl bg-brand-primary/20 border border-brand-primary/30 flex items-center justify-center text-brand-primary font-bold overflow-hidden">
                <img v-if="product.shop.logoUrl" :src="product.shop.logoUrl" class="w-full h-full object-cover" :alt="product.shop.name" />
                <span v-else>{{ product.shop.logo }}</span>
              </div>
              <div class="text-left">
                <div class="text-sm font-semibold text-brand-ink group-hover:text-brand-primary transition">{{ product.shop.name }}</div>
                <div class="text-[11px] text-brand-ink-3 mt-0.5">{{ product.shop.level }} · {{ product.shop.region }}</div>
              </div>
            </button>
            <div class="flex-1 grid grid-cols-3 gap-2 text-center border-l border-brand-line pl-4">
              <div>
                <div class="flex items-center gap-1 justify-center text-brand-warning font-bold"><Star class="w-3.5 h-3.5 fill-brand-warning" />{{ product.shop.rating }}</div>
                <div class="text-[10px] text-brand-ink-3 mt-0.5">店铺评分</div>
              </div>
              <div>
                <div class="font-bold text-brand-ink">{{ product.shop.soldCount.toLocaleString() }}</div>
                <div class="text-[10px] text-brand-ink-3 mt-0.5">累计成交</div>
              </div>
              <div>
                <div class="font-bold text-brand-success">{{ product.shop.responseTime }}</div>
                <div class="text-[10px] text-brand-ink-3 mt-0.5">响应速度</div>
              </div>
            </div>
            <div class="flex flex-col gap-1.5 shrink-0">
              <button @click="router.push('/messages')" class="px-4 h-8 rounded-lg border border-brand-primary/40 text-brand-primary text-xs hover:bg-brand-primary-subtle transition flex items-center gap-1.5">
                <MessageSquare class="w-3.5 h-3.5" />联系卖家
              </button>
              <button @click="goToShop" class="px-4 h-8 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:border-brand-primary/40 hover:text-brand-primary transition flex items-center gap-1.5">
                <Store class="w-3.5 h-3.5" />进店逛逛
              </button>
            </div>
          </div>
          <div class="flex flex-wrap gap-1.5 mt-4 pt-4 border-t border-brand-line-subtle">
            <span v-for="b in product.shop.badges" :key="b.label"
              :class="['inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-semibold border', b.color]">
              <component :is="b.icon" class="w-3 h-3" />{{ b.label }}
            </span>
          </div>
        </div>

        <!-- 数量 + 购买栏 -->
        <div class="sticky bottom-2 z-20 rounded-xl bg-brand-surface/95 backdrop-blur border border-brand-line p-4 md:static md:z-0 md:bg-transparent md:border-0 md:p-0">
          <div class="flex items-center gap-4 mb-4">
            <div class="text-sm text-brand-ink-3">数量</div>
            <div class="flex h-10 rounded-lg border border-brand-line overflow-hidden">
              <button @click="quantity = Math.max(1, quantity - 1)" class="w-10 text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition text-lg">−</button>
              <input v-model.number="quantity" min="1" type="number" class="w-14 text-center bg-transparent text-brand-ink focus:outline-none" />
              <button @click="quantity++" class="w-10 text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition text-lg">+</button>
            </div>
            <div class="text-xs text-brand-ink-3">库存 3 件 · <span class="flex items-center gap-1 inline-flex"><Eye class="w-3 h-3" />{{ product.viewCount }} 人看过</span></div>
          </div>
          <div class="grid grid-cols-[auto_1fr_1fr] md:grid-cols-3 gap-3">
            <button @click="favorited = !favorited"
              :class="['md:col-span-1 h-12 rounded-lg border text-sm font-medium transition flex items-center justify-center gap-2',
                       favorited ? 'bg-brand-error-subtle border-brand-error/40 text-brand-error' : 'border-brand-line text-brand-ink-2 hover:text-brand-error hover:border-brand-error/40']">
              <Heart :class="['w-4 h-4', favorited ? 'fill-current' : '']" />{{ favorited ? '已收藏' : '收藏' }}
            </button>
            <button @click="addCart" :disabled="adding"
              class="h-12 rounded-lg border border-brand-primary/50 text-brand-primary font-semibold hover:bg-brand-primary/10 disabled:opacity-60 transition flex items-center justify-center gap-2">
              <ShoppingCart class="w-4 h-4" />{{ adding ? '加入中...' : '加入购物车' }}
            </button>
            <button @click="buyNow"
              class="h-12 rounded-lg bg-brand-primary text-brand-primary-ink font-semibold hover:bg-brand-primary-hover transition flex items-center justify-center gap-2 shadow-1">
              <ShieldCheck class="w-4 h-4" />立即购买（担保交易）
            </button>
          </div>
          <div class="mt-3 flex flex-wrap items-center gap-3 text-[11px] text-brand-ink-3">
            <span class="flex items-center gap-1"><ShieldCheck class="w-3 h-3 text-brand-primary" />担保交易：买家确认验货通过后放款</span>
            <span class="flex items-center gap-1"><Clock class="w-3 h-3 text-brand-info" />发布于 {{ product.publishedAt }} · 有想要 {{ product.wantCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 验机报告 -->
    <div v-if="product.inspection" class="rounded-2xl bg-gradient-to-br from-brand-info/15 to-brand-surface border border-brand-info/30 p-6 md:p-8">
      <div class="flex flex-col md:flex-row md:items-start gap-6">
        <div class="md:w-64 shrink-0">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-10 h-10 rounded-lg bg-brand-info/25 flex items-center justify-center"><ShieldCheck class="w-5 h-5 text-brand-info" /></div>
            <div>
              <div class="text-xs text-brand-ink-3">官方验机报告</div>
              <div class="font-bold text-brand-ink">AI + 人工 双检</div>
            </div>
          </div>
          <div class="p-5 rounded-xl bg-brand-bg border border-brand-line text-center">
            <div class="text-[11px] text-brand-ink-3 mb-1">综合评分</div>
            <div class="text-5xl font-black text-brand-info leading-none">{{ product.inspection.score }}</div>
            <div class="mt-2 inline-block px-2.5 py-0.5 rounded-full bg-brand-info/20 text-brand-info text-xs font-semibold">{{ product.inspection.level }}</div>
          </div>
          <div class="mt-4 p-3 rounded-lg bg-brand-bg/50 border border-brand-line">
            <div class="text-[10px] text-brand-ink-3 mb-1">防伪签名（可验证）</div>
            <div class="flex items-center gap-2">
              <code class="flex-1 text-[10px] text-brand-info truncate">{{ product.inspection.signature }}</code>
              <button class="p-1.5 rounded-md hover:bg-brand-surface-2 text-brand-ink-2 hover:text-brand-primary transition"><Copy class="w-3.5 h-3.5" /></button>
            </div>
          </div>
        </div>
        <div class="flex-1">
          <h3 class="font-semibold text-brand-ink mb-4 flex items-center gap-2">28 项检测明细 <span class="text-xs text-brand-ink-3 font-normal">（展示核心 6 项）</span></h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div v-for="it in product.inspection.items" :key="it.label" class="p-4 rounded-lg bg-brand-bg border border-brand-line">
              <div class="flex items-center justify-between mb-1.5">
                <div class="text-sm font-medium text-brand-ink">{{ it.label }}</div>
                <span class="inline-flex items-center gap-1 text-xs font-semibold text-brand-success"><CheckCircle2 class="w-3.5 h-3.5" />{{ it.result }}</span>
              </div>
              <div class="text-xs text-brand-ink-3">{{ it.detail }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情 Tab -->
    <div class="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-6">
      <div class="rounded-2xl bg-brand-surface border border-brand-line overflow-hidden">
        <div class="flex items-center gap-1 px-4 border-b border-brand-line bg-brand-bg/60">
          <button v-for="t in [
            { id: 'desc', name: '商品详情' },
            { id: 'spec', name: '规格参数' },
            { id: 'review', name: '真实评价 (156)' },
            { id: 'qa', name: '问大家 (27)' }
          ]" :key="t.id" @click="descTab = t.id"
            :class="['relative px-4 py-3.5 text-sm font-medium transition',
                     descTab === t.id ? 'text-brand-primary' : 'text-brand-ink-2 hover:text-brand-primary']">
            {{ t.name }}
            <span v-if="descTab === t.id" class="absolute left-2 right-2 -bottom-px h-0.5 rounded-t bg-brand-primary"></span>
          </button>
        </div>
        <div class="p-6 md:p-8">
          <template v-if="descTab === 'desc'">
            <div class="prose prose-invert prose-sm max-w-none space-y-5">
              <div v-for="(para, i) in product.description.split('\n')" :key="i">
                <h3 v-if="para.startsWith('##')" class="text-lg font-bold text-brand-ink border-l-2 border-brand-primary pl-3">{{ para.replace(/^##+\s*/, '') }}</h3>
                <p v-else-if="para.trim()" class="text-sm text-brand-ink-2 leading-7 whitespace-pre-wrap">{{ para }}</p>
              </div>
              <img v-for="(img, i) in product.images.slice(0, 3)" :key="i" :src="img" class="w-full rounded-xl border border-brand-line my-5" />
            </div>
          </template>
          <template v-if="descTab === 'spec'">
            <div class="rounded-xl border border-brand-line overflow-hidden">
              <div class="grid grid-cols-1 md:grid-cols-2 divide-y divide-brand-line md:divide-y-0 md:divide-x md:divide-y divide-brand-line-subtle">
                <div v-for="s in product.specs" :key="s.k" class="flex">
                  <div class="w-40 shrink-0 px-4 py-3 bg-brand-bg/50 text-xs text-brand-ink-3 flex items-center">{{ s.k }}</div>
                  <div class="flex-1 px-4 py-3 text-sm text-brand-ink flex items-center">{{ s.v }}</div>
                </div>
              </div>
            </div>
          </template>
          <template v-if="descTab === 'review'">
            <div class="space-y-4">
              <div v-for="r in [
                { u: 'T***o', avatar: 'T', rating: 5, time: '2天前', content: '成色真的超预期！烤机4小时稳定72度，3DMark分数和卖家描述一致，包装也很仔细。验机报告和防伪签名都验证过，非常靠谱。', images: 4 },
                { u: 'i***1', avatar: 'i', rating: 5, time: '5天前', content: '顺丰第二天就到，烤机测试全部通过，金手指磨损很少。卖家客服也很耐心，解答了我好多小白问题。强烈推荐这店！', images: 2 },
                { u: '装***y', avatar: '装', rating: 4, time: '10天前', content: '卡是好卡，就是涡轮满载还是有点吵，不过公版就这样。性能没问题，跑Stable Diffusion出图速度飞起～', images: 0 }
              ]" :key="r.u" class="p-5 rounded-xl bg-brand-bg border border-brand-line">
                <div class="flex items-start gap-3">
                  <div class="w-9 h-9 rounded-full bg-brand-primary/20 border border-brand-primary/30 flex items-center justify-center text-brand-primary text-sm font-bold">{{ r.avatar }}</div>
                  <div class="flex-1">
                    <div class="flex items-center gap-2">
                      <span class="font-medium text-brand-ink text-sm">{{ r.u }}</span>
                      <div class="flex text-brand-warning fill-brand-warning"><Star v-for="n in r.rating" :key="n" class="w-3 h-3" /></div>
                      <span class="text-xs text-brand-ink-3 ml-auto">{{ r.time }}</span>
                    </div>
                    <div class="text-sm text-brand-ink-2 mt-2 leading-7">{{ r.content }}</div>
                    <div v-if="r.images" class="flex gap-2 mt-3">
                      <div v-for="i in r.images" :key="i" class="w-20 h-20 rounded-lg bg-brand-surface-2 overflow-hidden border border-brand-line">
                        <img :src="'https://placehold.co/160x160/15161A/00D4AA?text=review-'+i" class="w-full h-full object-cover" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <template v-if="descTab === 'qa'">
            <div class="text-xs text-brand-ink-3">问大家页面 - 买家常见问题解答区域，按设计系统渲染问答列表...</div>
          </template>
        </div>
      </div>

      <!-- 相关推荐 -->
      <aside class="space-y-3">
        <div class="sticky top-28 rounded-2xl bg-brand-surface border border-brand-line p-5">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-semibold text-brand-ink text-sm flex items-center gap-2"><AlertCircle class="w-4 h-4 text-brand-warning" />相似二手好物</h3>
            <button class="text-xs text-brand-primary hover:underline flex items-center gap-0.5">换一批 <ExternalLink class="w-3 h-3" /></button>
          </div>
          <div class="space-y-3">
            <button v-for="r in related" :key="r.id" @click="router.push('/product/' + r.id)"
              class="w-full flex gap-3 p-2 rounded-lg hover:bg-brand-surface-2 transition text-left group">
              <div class="w-16 h-16 shrink-0 rounded-lg bg-brand-bg overflow-hidden border border-brand-line">
                <img :src="r.img" class="w-full h-full object-cover" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-xs text-brand-ink line-clamp-2 leading-snug group-hover:text-brand-primary transition">{{ r.title }}</div>
                <div class="flex items-baseline justify-between mt-2">
                  <span class="text-sm font-bold text-brand-error">¥{{ r.price.toLocaleString() }}</span>
                  <span class="text-[10px] text-brand-ink-3">{{ r.sold }} 已售</span>
                </div>
              </div>
            </button>
          </div>
        </div>
      </aside>
    </div>
    </template>
  </div>
</template>
