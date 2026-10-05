<script setup>
import { useRouter } from 'vue-router'
import { Heart, Eye, ShoppingCart, ShieldCheck, Star } from 'lucide-vue-next'
import { computed } from 'vue'
import { useCartStore } from '@/stores/cart'
import { CONDITION_NAME } from '@/constants/product'

const props = defineProps({
  product: { type: Object, required: true },
  showSeller: { type: Boolean, default: true }
})

const emit = defineEmits(['toggleFavorite'])

const router = useRouter()
const cartStore = useCartStore()

const conditionText = computed(() => CONDITION_NAME[props.product.condition] || props.product.condition)

const conditionClass = computed(() => ({
  'NEW': 'bg-brand-primary/15 text-brand-primary border-brand-primary/30',
  'LIKE_NEW': 'bg-brand-success/15 text-brand-success border-brand-success/30',
  'VERY_GOOD': 'bg-brand-info/15 text-brand-info border-brand-info/30',
  'GOOD': 'bg-brand-warning/15 text-brand-warning border-brand-warning/30',
  'FAIR': 'bg-brand-ink-2/15 text-brand-ink-2 border-brand-ink-2/30'
}[props.product.condition] || ''))

function goDetail() {
  router.push({ path: `/product/${props.product.id}` })
}

function addCart(e) {
  e.stopPropagation()
  cartStore.addToCart(props.product, 1)
}
</script>

<template>
  <div
    @click="goDetail"
    class="group rounded-xl bg-brand-surface border border-brand-line overflow-hidden cursor-pointer hover:border-brand-primary/50 hover:shadow-2 transition-all duration-300"
  >
    <!-- 封面 -->
    <div class="relative aspect-square overflow-hidden bg-brand-surface-2">
      <img
        :src="product.coverImage || 'https://placehold.co/400x400/15161A/00D4AA?text=' + encodeURIComponent(product.title || 'YJX')"
        :alt="product.title"
        loading="lazy"
        class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
      />
      <!-- 状态标签 -->
      <div class="absolute top-2 left-2 flex gap-1.5">
        <span v-if="product.condition" :class="['text-[10px] font-semibold px-2 py-0.5 rounded border', conditionClass]">
          {{ conditionText }}
        </span>
        <span v-if="product.inspectionReport" class="text-[10px] font-semibold px-2 py-0.5 rounded bg-brand-info/15 text-brand-info border border-brand-info/30">
          <ShieldCheck class="w-2.5 h-2.5 inline align-[-1px] mr-0.5" />验机
        </span>
      </div>
      <!-- 收藏 -->
      <button
        @click.stop="emit('toggleFavorite', product)"
        :class="['absolute top-2 right-2 w-7 h-7 rounded-full flex items-center justify-center backdrop-blur transition',
                 product.isFavorite ? 'bg-brand-error text-white' : 'bg-black/40 text-brand-ink hover:bg-brand-error/70 hover:text-white']"
      >
        <Heart :class="['w-3.5 h-3.5', product.isFavorite ? 'fill-current' : '']" />
      </button>
    </div>

    <!-- 信息 -->
    <div class="p-3.5 space-y-2.5">
      <h3 class="text-sm font-medium text-brand-ink line-clamp-2 leading-snug min-h-[40px] group-hover:text-brand-primary transition-colors">
        {{ product.title }}
      </h3>

      <!-- 规格简述 -->
      <div v-if="product.specsBrief" class="flex flex-wrap gap-1">
        <span v-for="s in product.specsBrief" :key="s"
          class="text-[10px] px-1.5 py-0.5 rounded bg-brand-surface-3 text-brand-ink-3">
          {{ s }}
        </span>
      </div>

      <!-- 价格 -->
      <div class="flex items-baseline gap-2 pt-1">
        <span class="text-lg font-bold text-brand-primary">¥{{ product.price?.toFixed?.(0) || product.price }}</span>
        <span v-if="product.originalPrice" class="text-xs text-brand-ink-3 line-through">¥{{ product.originalPrice }}</span>
      </div>

      <!-- 底部元信息 -->
      <div class="flex items-center justify-between pt-2 border-t border-brand-line-subtle text-[11px] text-brand-ink-3">
        <div v-if="showSeller" class="flex items-center gap-1 truncate max-w-[60%]">
          <div class="w-4 h-4 rounded-full bg-brand-primary/20 border border-brand-primary/30 shrink-0"></div>
          <span class="truncate">{{ product.sellerName || '硬件侠商家' }}</span>
          <Star v-if="product.sellerRating >= 4.8" class="w-3 h-3 text-brand-warning fill-brand-warning" />
        </div>
        <div class="flex items-center gap-3">
          <span class="flex items-center gap-1"><Eye class="w-3 h-3" />{{ product.viewCount || 0 }}</span>
          <button
            @click="addCart"
            class="flex items-center gap-1 px-2 py-1 rounded bg-brand-primary/10 text-brand-primary border border-brand-primary/25 hover:bg-brand-primary hover:text-brand-primary-ink transition"
          >
            <ShoppingCart class="w-3 h-3" />加入
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
