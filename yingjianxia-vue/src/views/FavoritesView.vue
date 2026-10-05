<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Heart, ShoppingBag, Trash2, ArrowLeft } from 'lucide-vue-next'
import { getFavoritesApi, removeFavoriteApi } from '@/api/product'

const router = useRouter()
const loading = ref(false)
const products = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

async function loadFavorites() {
  loading.value = true
  try {
    const data = await getFavoritesApi({ page: currentPage.value, size: pageSize.value })
    const list = data.records || data.list || []
    products.value = list.map(p => ({
      ...p,
      coverImage: p.coverImage || p.image || (Array.isArray(p.imageUrls) ? p.imageUrls[0] : null) || 'https://placehold.co/400x400/1E2026/00D4AA?text=No+Image'
    }))
    total.value = data.total || products.value.length
  } catch (e) {
    console.error('加载收藏失败', e)
    products.value = []
  } finally {
    loading.value = false
  }
}

async function handleRemove(product) {
  try {
    if (!confirm(`确定取消收藏「${product.title || '该商品'}」？`)) return
    await removeFavoriteApi(product.id)
    alert('已取消收藏')
    loadFavorites()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

function goProduct(id) {
  router.push('/product/' + id)
}

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === currentPage.value) return
  currentPage.value = p
}

onMounted(() => {
  loadFavorites()
})
</script>

<template>
  <div class="container-app space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl md:text-3xl font-bold text-brand-ink flex items-center gap-3">
          <Heart class="w-7 h-7 text-brand-error" />我的收藏
        </h1>
        <p class="text-sm text-brand-ink-3 mt-1">共 {{ total }} 件收藏商品</p>
      </div>
      <button @click="router.push('/browse')" class="px-4 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition flex items-center gap-2">
        <ShoppingBag class="w-4 h-4" />去逛逛
      </button>
    </div>

    <div v-if="loading" class="rounded-2xl bg-brand-surface border border-brand-line p-16 text-center">
      <div class="w-12 h-12 mx-auto rounded-full border-2 border-brand-primary border-t-transparent animate-spin"></div>
      <p class="text-sm text-brand-ink-3 mt-4">加载中...</p>
    </div>

    <div v-else-if="products.length === 0" class="rounded-2xl bg-brand-surface border border-brand-line p-16 text-center">
      <div class="w-20 h-20 mx-auto mb-4 rounded-2xl bg-brand-error/15 flex items-center justify-center">
        <Heart class="w-10 h-10 text-brand-error" />
      </div>
      <h3 class="font-bold text-brand-ink text-xl">还没有收藏商品</h3>
      <p class="text-sm text-brand-ink-3 mt-2">去市场逛逛，收藏心仪的硬件吧～</p>
      <button @click="router.push('/browse')" class="mt-6 px-6 h-10 rounded-lg bg-brand-primary text-brand-primary-ink font-medium hover:bg-brand-primary-hover transition flex items-center gap-2 mx-auto">
        <ShoppingBag class="w-4 h-4" />去逛逛
      </button>
    </div>

    <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <div v-for="p in products" :key="p.id" class="group rounded-xl bg-brand-surface border border-brand-line overflow-hidden hover:border-brand-primary/40 hover:shadow-lg transition-all duration-200">
        <div class="relative aspect-square bg-brand-bg overflow-hidden cursor-pointer" @click="goProduct(p.id)">
          <img v-if="p.coverImage || p.image" :src="p.coverImage || p.image" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
          <div v-else class="w-full h-full flex items-center justify-center text-brand-ink-3 text-sm">无图</div>
          <button @click.stop="handleRemove(p)" class="absolute top-2 right-2 w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center hover:bg-brand-error transition-colors opacity-0 group-hover:opacity-100">
            <Trash2 class="w-4 h-4" />
          </button>
        </div>
        <div class="p-3">
          <div class="text-sm text-brand-ink line-clamp-2 leading-snug cursor-pointer hover:text-brand-primary transition-colors" @click="goProduct(p.id)">
            {{ p.title || p.name || '商品' }}
          </div>
          <div class="flex items-baseline justify-between mt-2">
            <span class="text-lg font-bold text-brand-error">¥{{ Number(p.price || 0).toFixed(2) }}</span>
            <span v-if="p.condition" class="text-[11px] text-brand-ink-3">{{ p.condition }}</span>
          </div>
          <button @click="goProduct(p.id)" class="w-full mt-2 h-8 rounded-md bg-brand-primary/10 text-brand-primary text-xs font-medium hover:bg-brand-primary hover:text-brand-primary-ink transition-colors flex items-center justify-center gap-1">
            查看详情
          </button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-1 pt-4">
      <button @click="goPage(currentPage - 1)" :disabled="currentPage <= 1" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary hover:border-brand-primary/40 transition text-xs disabled:opacity-40">&lt;</button>
      <button v-for="n in Math.min(totalPages, 5)" :key="n" @click="goPage(n)" :class="['w-9 h-9 rounded-lg text-xs transition', n === currentPage ? 'bg-brand-primary text-brand-primary-ink font-semibold' : 'border border-brand-line text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40']">{{ n }}</button>
      <button @click="goPage(currentPage + 1)" :disabled="currentPage >= totalPages" class="w-9 h-9 rounded-lg border border-brand-line text-brand-ink-3 hover:text-brand-primary hover:border-brand-primary/40 transition text-xs disabled:opacity-40">&gt;</button>
    </div>
  </div>
</template>
