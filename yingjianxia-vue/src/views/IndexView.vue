<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Zap, Shield, Award, Rocket, ChevronRight, TrendingUp, Clock, Star, Flame } from 'lucide-vue-next'
import ProductCard from '@/components/ProductCard.vue'
import { searchProductsApi } from '@/api/product'
import bannerIndex from '@/assets/banner-index.png'

const router = useRouter()
const kw = ref('')
const products = ref([])
const hotListings = ref([])
const newArrivals = ref([])
const loadingProducts = ref(false)

function handleSearch() {
  if (kw.value) router.push({ path: '/search', query: { q: kw.value } })
  else router.push('/browse')
}

// 从后端加载真实商品数据
async function loadProducts() {
  loadingProducts.value = true
  try {
    const res = await searchProductsApi({ pageNum: 1, pageSize: 8, sortField: 'viewCount', sortDirection: 'desc' })
    const data = res.data || res
    const records = data.records || data.list || []
    if (records.length > 0) {
      products.value = records.map(item => ({
        id: item.id,
        title: item.title,
        price: Number(item.price) || 0,
        originalPrice: Number(item.originalPrice) || null,
        condition: item.conditionName || '',
        coverImage: item.imageUrls?.[0] || 'https://placehold.co/400x400/1E2026/00D4AA?text=No+Image',
        sellerName: item.shopName || '硬件侠商家',
        sellerRating: item.sellerCreditScore ? (item.sellerCreditScore / 20).toFixed(1) : '4.5',
        shopId: item.shopId,
        viewCount: item.viewCount || 0,
        specsBrief: item.specs?.map(s => s.value).filter(Boolean).slice(0, 3) || [],
        isFavorite: item.favoredByMe || false,
        inspectionReport: item.hasInspection || false
      }))
      hotListings.value = products.value.slice(0, 4)
      newArrivals.value = products.value.slice(4, 8)
    }
  } catch (e) {
    console.warn('首页商品加载失败，使用空列表', e)
  } finally {
    loadingProducts.value = false
  }
}

const categories = [
  { id: 'cpu', name: 'CPU 处理器', img: 'https://placehold.co/200x120/15161A/00D4AA?text=CPU', count: 2341 },
  { id: 'gpu', name: '显卡', img: 'https://placehold.co/200x120/15161A/00D4AA?text=GPU', count: 3892 },
  { id: 'motherboard', name: '主板', img: 'https://placehold.co/200x120/15161A/00D4AA?text=Mobo', count: 1587 },
  { id: 'ram', name: '内存条', img: 'https://placehold.co/200x120/15161A/00D4AA?text=RAM', count: 4102 },
  { id: 'ssd', name: 'SSD 存储', img: 'https://placehold.co/200x120/15161A/00D4AA?text=SSD', count: 2877 },
  { id: 'psu', name: '电源', img: 'https://placehold.co/200x120/15161A/00D4AA?text=PSU', count: 945 },
  { id: 'case', name: '机箱', img: 'https://placehold.co/200x120/15161A/00D4AA?text=Case', count: 822 },
  { id: 'laptop', name: '笔记本', img: 'https://placehold.co/200x120/15161A/00D4AA?text=Laptop', count: 1566 }
]

const banners = [
  { title: '开学季 · 整机专场', sub: '官方验机 + 半年质保 放心购', bg: 'from-[#00D4AA]/30 to-[#0B0C10]' },
  { title: '显卡白菜价', sub: 'RTX30/40 系显卡最高立省 ¥1200', bg: 'from-[#3B82F6]/30 to-[#0B0C10]' },
  { title: '商家免费入驻', sub: '前1000名免佣金 开店即享流量扶持', bg: 'from-[#F59E0B]/30 to-[#0B0C10]' }
]
const bannerIdx = ref(0)
setInterval(() => { bannerIdx.value = (bannerIdx.value + 1) % banners.length }, 5000)

onMounted(() => {
  loadProducts()
})
</script>

<template>
  <div class="container-app space-y-12">
    <section class="relative rounded-2xl overflow-hidden bg-gradient-to-br from-brand-primary/20 via-brand-surface to-brand-bg border border-brand-line py-14 px-6 md:px-12">
      <div class="absolute inset-0 opacity-20 pointer-events-none" style="background-image: radial-gradient(circle at 20% 50%, #00D4AA 0%, transparent 50%), radial-gradient(circle at 80% 20%, #3B82F6 0%, transparent 40%);"></div>
      <div class="relative flex flex-col md:flex-row items-center gap-8">
        <div class="flex-1 max-w-3xl">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium bg-brand-primary/15 text-brand-primary border border-brand-primary/30 mb-5">
          <Rocket class="w-3.5 h-3.5" />官方验机 · 担保交易 · 已服务 120万+ 硬件玩家
        </div>
        <h1 class="text-3xl md:text-5xl font-bold text-brand-ink leading-tight mb-4">
          放心买二手硬件<br />
          <span class="bg-gradient-to-r from-brand-primary to-[#3B82F6] bg-clip-text text-transparent">上硬件侠就对了</span>
        </h1>
        <p class="text-base md:text-lg text-brand-ink-2 mb-8 max-w-xl">
          12+ 电脑配件品类全覆盖，AI 假货识别 + 官方验机报告 + 7天售后保障，
          卖家极速放款、买家放心下单。
        </p>
        <div :class="['mb-6 p-4 rounded-xl border transition-all duration-500', banners[bannerIdx].bg, 'border-brand-line/50']">
          <div class="text-lg md:text-xl font-bold text-brand-ink">{{ banners[bannerIdx].title }}</div>
          <div class="text-sm text-brand-ink-2 mt-1">{{ banners[bannerIdx].sub }}</div>
          <div class="flex gap-1.5 mt-3">
            <button v-for="(b, i) in banners" :key="i" @click="bannerIdx = i"
              :class="['w-8 h-1.5 rounded-full transition', i === bannerIdx ? 'bg-brand-primary' : 'bg-brand-line']"></button>
          </div>
        </div>
        <div class="flex h-14 rounded-xl overflow-hidden bg-brand-surface border border-brand-line focus-within:border-brand-primary max-w-2xl shadow-2">
          <div class="pl-5 flex items-center text-brand-ink-3"><Search class="w-5 h-5" /></div>
          <input v-model="kw" type="text" placeholder="搜 RTX 4090、i9-13900K、DDR5 6400..." class="flex-1 px-4 text-base bg-transparent placeholder:text-brand-ink-3 focus:outline-none" @keyup.enter="handleSearch" />
          <button @click="handleSearch" class="px-8 bg-brand-primary text-brand-primary-ink text-base font-semibold hover:bg-brand-primary-hover transition flex items-center gap-2">
            <Search class="w-4 h-4" />搜索好货
          </button>
        </div>
        <div class="flex flex-wrap items-center gap-2 mt-4 text-xs text-brand-ink-3">
          <span class="flex items-center gap-1"><Flame class="w-3.5 h-3.5 text-brand-error" />热门：</span>
          <button v-for="t in ['RTX 4070 SUPER', 'AMD 7800X3D', 'DDR5 6000 32GB', '990 PRO 2TB', 'R9000P 2024']" :key="t"
            @click="kw = t; handleSearch()"
            class="px-3 py-1 rounded-full bg-brand-surface-2 hover:bg-brand-surface-3 hover:text-brand-primary transition">
            {{ t }}
          </button>
        </div>
        </div>
        <div class="hidden md:block w-[420px] shrink-0 relative">
          <img :src="bannerIndex" alt="硬件侠平台" class="w-full h-[360px] object-cover rounded-2xl border border-brand-line/50 shadow-2" />
          <div class="absolute inset-0 rounded-2xl bg-gradient-to-t from-brand-bg/60 via-transparent to-transparent pointer-events-none"></div>
        </div>
      </div>
    </section>

    <section class="grid grid-cols-2 md:grid-cols-4 gap-3 md:gap-5">
      <div class="p-5 rounded-xl bg-brand-surface border border-brand-line hover:border-brand-primary/40 transition">
        <div class="w-11 h-11 rounded-lg bg-brand-primary/15 flex items-center justify-center mb-3"><Shield class="w-5 h-5 text-brand-primary" /></div>
        <div class="font-semibold text-brand-ink">担保交易</div>
        <div class="text-xs text-brand-ink-3 mt-1">买家验货通过后放款给卖家，资金0风险</div>
      </div>
      <div class="p-5 rounded-xl bg-brand-surface border border-brand-line hover:border-brand-primary/40 transition">
        <div class="w-11 h-11 rounded-lg bg-brand-info/15 flex items-center justify-center mb-3"><Zap class="w-5 h-5 text-brand-info" /></div>
        <div class="font-semibold text-brand-ink">AI 假货识别</div>
        <div class="text-xs text-brand-ink-3 mt-1">17项AI模型自动校验描述一致率</div>
      </div>
      <div class="p-5 rounded-xl bg-brand-surface border border-brand-line hover:border-brand-primary/40 transition">
        <div class="w-11 h-11 rounded-lg bg-brand-success/15 flex items-center justify-center mb-3"><Award class="w-5 h-5 text-brand-success" /></div>
        <div class="font-semibold text-brand-ink">官方验机</div>
        <div class="text-xs text-brand-ink-3 mt-1">第三方实验室出具质检报告 + 防伪签名</div>
      </div>
      <div class="p-5 rounded-xl bg-brand-surface border border-brand-line hover:border-brand-primary/40 transition">
        <div class="w-11 h-11 rounded-lg bg-brand-warning/15 flex items-center justify-center mb-3"><Clock class="w-5 h-5 text-brand-warning" /></div>
        <div class="font-semibold text-brand-ink">7 天售后</div>
        <div class="text-xs text-brand-ink-3 mt-1">退货/换货/平台仲裁 一站式处理</div>
      </div>
    </section>

    <section>
      <div class="flex items-center justify-between mb-5">
        <div>
          <h2 class="text-xl font-bold text-brand-ink flex items-center gap-2"><TrendingUp class="w-5 h-5 text-brand-primary" />热门分类</h2>
          <p class="text-xs text-brand-ink-3 mt-1">12大品类覆盖，精准找货不迷路</p>
        </div>
        <button @click="router.push('/browse')" class="text-sm text-brand-primary hover:underline flex items-center gap-1">全部<ChevronRight class="w-4 h-4" /></button>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-3">
        <div v-for="c in categories" :key="c.id" @click="router.push({ path: '/browse', query: { category: c.id } })"
          class="rounded-xl bg-brand-surface border border-brand-line overflow-hidden cursor-pointer hover:border-brand-primary/50 transition group">
          <div class="aspect-[5/3] overflow-hidden"><img :src="c.img" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" /></div>
          <div class="p-2.5">
            <div class="text-sm font-medium text-brand-ink group-hover:text-brand-primary transition">{{ c.name }}</div>
            <div class="text-[11px] text-brand-ink-3 mt-0.5">{{ c.count }} 件在售</div>
          </div>
        </div>
      </div>
    </section>

    <section>
      <div class="flex items-center justify-between mb-5">
        <div>
          <h2 class="text-xl font-bold text-brand-ink flex items-center gap-2"><Flame class="w-5 h-5 text-brand-error" />本周热销榜</h2>
          <p class="text-xs text-brand-ink-3 mt-1">近7天下单量最高，品质口碑双保障</p>
        </div>
        <button @click="router.push('/browse?sort=sales')" class="text-sm text-brand-primary hover:underline flex items-center gap-1">更多<ChevronRight class="w-4 h-4" /></button>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <ProductCard v-for="p in hotListings" :key="p.id" :product="p" />
      </div>
    </section>

    <section>
      <div class="flex items-center justify-between mb-5">
        <div>
          <h2 class="text-xl font-bold text-brand-ink flex items-center gap-2"><Star class="w-5 h-5 text-brand-warning fill-brand-warning" />精选新品</h2>
          <p class="text-xs text-brand-ink-3 mt-1">编辑每日精选，刚刚上架的高性价比硬件</p>
        </div>
        <button @click="router.push('/browse?sort=new')" class="text-sm text-brand-primary hover:underline flex items-center gap-1">更多<ChevronRight class="w-4 h-4" /></button>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <ProductCard v-for="p in newArrivals" :key="p.id" :product="p" />
      </div>
    </section>
  </div>
</template>