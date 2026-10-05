<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { createOrderApi } from '@/api'
import { getAddressesApi } from '@/api/user'
import {
  ShoppingCart, Trash2, Minus, Plus, Package, Tag, ShieldCheck,
  Truck, AlertTriangle, Heart, ChevronRight, CreditCard, Gift
} from 'lucide-vue-next'

const router = useRouter()
const cartStore = useCartStore()
const loading = ref(false)
const toast = ref(null)

// 页面加载时从后端拉取购物车数据
onMounted(() => {
  if (cartStore.items.length === 0 && typeof cartStore.loadCart === 'function') {
    cartStore.loadCart()
  }
})

const allSelected = computed(() =>
  cartStore.items.length > 0 && cartStore.items.every(it => it.selected)
)

function showToast(type, msg) {
  toast.value = { type, msg }
  setTimeout(() => { toast.value = null }, 2500)
}

function toggleAll() { cartStore.toggleSelectAll(!allSelected.value) }

function decreaseQty(item) {
  if (item.quantity > 1) cartStore.updateQuantity(item.productId, item.quantity - 1)
}
function increaseQty(item) {
  const max = item.stock || 0
  if (max > 0 && item.quantity >= max) {
    showToast('error', `库存仅剩 ${max} 件`)
    return
  }
  cartStore.updateQuantity(item.productId, item.quantity + 1)
}

/** 输入框输入时实时联动金额（直接改 item.quantity，subtotal 计算属性自动更新） */
function onQtyInput(item, e) {
  const raw = e.target.value
  if (raw === '') { item.quantity = 0; return }
  const n = parseInt(raw, 10)
  if (isNaN(n) || n < 0) return
  item.quantity = n
}

/** 失焦时校验并同步到 store（校验最小 1、不超过库存、调用后端 API） */
function onQtyBlur(item) {
  const max = item.stock || 0
  let n = Math.max(1, Math.floor(item.quantity) || 1)
  if (max > 0 && n > max) {
    n = max
    showToast('error', `库存仅剩 ${max} 件，已自动调整`)
  }
  if (n !== item.quantity) item.quantity = n
  cartStore.updateQuantity(item.productId, n)
}

function removeItem(id) { cartStore.removeItem(id) }

/** 将 specs 数组 [{k,v}] 转为可读文本，如 "尺寸: 100*100 · 重量: 3kg" */
function specsText(specs) {
  if (!specs || !Array.isArray(specs) || specs.length === 0) return ''
  return specs.map(s => `${s.k || s.name || ''}: ${s.v || s.value || ''}`).join(' · ')
}

/** 结算：调用后端创建订单，跳转到订单页 */
async function checkout() {
  if (loading.value) return // 防止重复点击
  const selected = cartStore.selectedItems
  if (selected.length === 0) {
    showToast('error', '请选择要结算的商品')
    return
  }

  loading.value = true
  try {
    // 1. 获取默认收货地址
    let addressId = null
    let addressSnapshot = null
    try {
      const addrList = await getAddressesApi()
      const list = Array.isArray(addrList) ? addrList : (addrList?.records || [])
      const def = list.find(a => a.isDefault) || list[0]
      if (!def) {
        showToast('error', '请先添加收货地址')
        router.push('/profile/addresses')
        return
      }
      addressId = def.id
      // 构建收货地址快照（防止后续地址被修改）
      addressSnapshot = JSON.stringify({
        receiverName: def.receiverName || def.name || '',
        phone: def.phone || def.mobile || '',
        province: def.province || '',
        city: def.city || '',
        district: def.district || '',
        detail: def.detail || def.address || '',
        fullAddress: def.fullAddress || `${def.province || ''}${def.city || ''}${def.district || ''}${def.detail || def.address || ''}`
      })
    } catch (e) {
      showToast('error', '获取收货地址失败，请先添加地址')
      return
    }

    // 2. 按 sellerId 分组拆单（后端按多卖家拆单）
    const groups = {}
    selected.forEach(it => {
      const sid = it.sellerId || 0
      if (!groups[sid]) groups[sid] = []
      groups[sid].push({ productId: it.productId, quantity: it.quantity, sellerId: sid })
    })

    // 3. 逐组创建订单（幂等键基于商品ID+数量生成，防止重复下单）
    // 注意：idempotency_key 字段为 varchar(64)，需控制长度
    const orderIds = []
    for (const sid in groups) {
      const itemsKey = groups[sid]
        .map(it => `${it.productId}_${it.quantity}`)
        .sort()
        .join('-')
      // 对 itemsKey 做简单哈希，避免幂等键超长（超过64字符会触发数据库304错误）
      let hash = 0
      for (let i = 0; i < itemsKey.length; i++) {
        hash = ((hash << 5) - hash) + itemsKey.charCodeAt(i)
        hash |= 0
      }
      const idempotencyKey = `order_${sid}_${Math.abs(hash)}`
      const payload = {
        items: groups[sid],
        addressId,
        addressSnapshot,
        idempotencyKey
      }
      const res = await createOrderApi(payload)
      if (res && res.id) orderIds.push(res.id)
    }

    // 4. 清空已结算的商品
    await cartStore.clear()

    showToast('success', `订单创建成功，共 ${orderIds.length} 笔订单`)
    setTimeout(() => router.push('/orders?status=pending_payment'), 1000)
  } catch (e) {
    console.error('checkout error:', e)
    // 幂等键重复（4005）说明订单已创建，不算失败，跳转订单页
    if (e.code === 4005 || e.code === 409) {
      showToast('success', '订单已创建，请勿重复下单')
      try { await cartStore.clear() } catch (err) { console.error('clear cart failed:', err) }
      setTimeout(() => router.push('/orders?status=pending_payment'), 1000)
    } else {
      showToast('error', e.message || '创建订单失败')
    }
  } finally {
    loading.value = false
  }
}

// 推荐商品
const recommend = [
  { id: 8101, title: '利民 PA120 双塔散热 支持LGA1700', price: 269, img: 'https://placehold.co/240x240/15161A/00D4AA?text=PA120' },
  { id: 8102, title: '海韵 FOCUS GX-1000 金牌全模组', price: 999, img: 'https://placehold.co/240x240/15161A/00D4AA?text=GX1000' },
  { id: 8103, title: '芝奇 DDR5 6400 32GB RGB 套条', price: 1588, img: 'https://placehold.co/240x240/15161A/00D4AA?text=DDR5' },
  { id: 8104, title: '追风者 P600S 机箱 侧透静音', price: 699, img: 'https://placehold.co/240x240/15161A/00D4AA?text=P600S' }
]
</script>

<template>
  <div class="container-app space-y-6">
    <!-- Toast 提示 -->
    <Transition name="toast">
      <div v-if="toast" class="fixed top-4 left-1/2 -translate-x-1/2 z-50 px-4 py-2.5 rounded-xl shadow-lg text-sm font-medium text-white flex items-center gap-2"
        :class="toast.type === 'error' ? 'bg-brand-error' : 'bg-brand-success'">
        <AlertTriangle v-if="toast.type === 'error'" class="w-4 h-4" />
        <span>{{ toast.msg }}</span>
      </div>
    </Transition>
    <div>
      <h1 class="text-2xl md:text-3xl font-bold text-brand-ink flex items-center gap-3">
        <span class="relative inline-block">
          <ShoppingCart class="w-7 h-7 text-brand-primary" />
          <span v-if="cartStore.totalCount" class="absolute -top-1 -right-2 min-w-5 h-5 px-1 rounded-full bg-brand-error text-[10px] font-bold text-white flex items-center justify-center">{{ cartStore.totalCount }}</span>
        </span>
        我的购物车
      </h1>
      <p class="text-sm text-brand-ink-3 mt-1">购物车商品保留 30 天，库存状态以结算时为准</p>
    </div>

    <div v-if="cartStore.items.length === 0" class="rounded-2xl bg-brand-surface border border-brand-line p-16 text-center">
      <div class="w-20 h-20 mx-auto mb-5 rounded-2xl bg-brand-primary/15 flex items-center justify-center">
        <ShoppingCart class="w-10 h-10 text-brand-primary" />
      </div>
      <h3 class="text-xl font-bold text-brand-ink">购物车空空如也</h3>
      <p class="text-sm text-brand-ink-3 mt-2">赶紧去挑选心仪的硬件吧～</p>
      <div class="mt-8 flex items-center justify-center gap-3">
        <button @click="router.push('/browse')" class="px-6 h-11 rounded-lg bg-brand-primary text-brand-primary-ink font-medium hover:bg-brand-primary-hover transition flex items-center gap-2">
          <Package class="w-4 h-4" />去逛逛市场
        </button>
        <button @click="router.push('/')" class="px-6 h-11 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition">返回首页</button>
      </div>
      <div class="mt-10 grid grid-cols-2 md:grid-cols-4 gap-4 max-w-4xl mx-auto">
        <button v-for="r in recommend" :key="r.id" @click="router.push('/product/' + r.id)"
          class="p-3 rounded-xl bg-brand-bg border border-brand-line hover:border-brand-primary/40 transition text-left group">
          <div class="aspect-square rounded-lg overflow-hidden bg-brand-surface-2 mb-3"><img :src="r.img" class="w-full h-full object-cover group-hover:scale-105 transition" /></div>
          <div class="text-xs text-brand-ink line-clamp-2 leading-snug group-hover:text-brand-primary transition">{{ r.title }}</div>
          <div class="text-sm font-bold text-brand-error mt-2">¥{{ r.price }}</div>
        </button>
      </div>
    </div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-6">
      <!-- 商品列表 -->
      <section class="rounded-2xl bg-brand-surface border border-brand-line overflow-hidden">
        <!-- 表头 -->
        <div class="hidden md:grid grid-cols-[auto_1fr_120px_180px_120px_60px] items-center px-5 py-3 bg-brand-bg/60 border-b border-brand-line text-xs font-semibold text-brand-ink-3">
          <input type="checkbox" :checked="allSelected" @change="toggleAll" class="w-4 h-4 accent-brand-primary" />
          <div>商品信息</div>
          <div class="text-center">单价</div>
          <div class="text-center">数量</div>
          <div class="text-center">小计</div>
          <div class="text-right">操作</div>
        </div>

        <div class="divide-y divide-brand-line-subtle">
          <div v-for="item in cartStore.items" :key="item.productId" class="p-4 md:p-5 flex flex-col md:grid md:grid-cols-[auto_1fr_120px_180px_120px_60px] md:items-center gap-4 relative">
            <input type="checkbox" v-model="item.selected" class="hidden md:block w-4 h-4 accent-brand-primary" />
            <!-- 商品 -->
            <div class="flex items-start gap-4 md:gap-5">
              <input type="checkbox" v-model="item.selected" class="md:hidden w-4 h-4 mt-4 accent-brand-primary" />
              <div @click="router.push('/product/' + item.productId)" class="w-20 h-20 md:w-24 md:h-24 rounded-xl overflow-hidden bg-brand-bg border border-brand-line shrink-0 cursor-pointer group">
                <img :src="item.image" class="w-full h-full object-cover group-hover:scale-105 transition" />
              </div>
              <div class="flex-1 min-w-0">
                <button @click="router.push('/product/' + item.productId)" class="text-left text-sm text-brand-ink line-clamp-2 leading-snug hover:text-brand-primary transition">{{ item.title }}</button>
                <div class="flex flex-wrap items-center gap-1.5 mt-2">
                  <span class="text-[10px] px-1.5 py-0.5 rounded bg-brand-primary/15 text-brand-primary border border-brand-primary/30">{{ item.condition || '95新' }}</span>
                  <span v-if="specsText(item.specs)" class="text-[10px] px-1.5 py-0.5 rounded bg-brand-surface-3 text-brand-ink-3">{{ specsText(item.specs) }}</span>
                </div>
                <button @click="router.push('/seller/' + item.shopId)" class="text-[11px] text-brand-ink-3 hover:text-brand-primary mt-2 inline-flex items-center gap-1">
                  <Tag class="w-3 h-3" />{{ item.shopName || '未知店铺' }}<ChevronRight class="w-3 h-3" />
                </button>
              </div>
            </div>
            <div class="text-center text-sm font-bold text-brand-error md:pl-2">¥{{ item.price.toLocaleString() }}</div>
            <div class="flex flex-col items-center justify-center gap-1 min-w-0">
              <div class="flex h-9 rounded-lg border border-brand-line overflow-hidden w-fit">
                <button @click="decreaseQty(item)" :disabled="item.quantity <= 1" class="w-9 text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary disabled:opacity-30 transition text-lg">−</button>
                <input :value="item.quantity" @input="onQtyInput(item, $event)" @blur="onQtyBlur(item)" class="w-14 text-center bg-transparent text-sm text-brand-ink focus:outline-none min-w-0" />
                <button @click="increaseQty(item)" class="w-9 text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition text-lg">+</button>
              </div>
              <div v-if="item.stock > 0 && item.quantity >= item.stock" class="text-[10px] text-brand-error">库存仅剩 {{ item.stock }} 件</div>
            </div>
            <div class="text-center text-base font-black text-brand-error md:pl-2">¥{{ (item.price * item.quantity).toLocaleString() }}</div>
            <div class="flex md:flex-col items-center md:items-end justify-end gap-2">
              <button class="p-2 rounded-lg hover:bg-brand-surface-2 text-brand-ink-3 hover:text-brand-primary transition" title="移入收藏">
                <Heart class="w-4 h-4" />
              </button>
              <button @click="removeItem(item.productId)" class="p-2 rounded-lg hover:bg-brand-error-subtle text-brand-ink-3 hover:text-brand-error transition" title="删除">
                <Trash2 class="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        <!-- 底部操作条 -->
        <div class="sticky bottom-0 bg-brand-surface/95 backdrop-blur border-t border-brand-line px-4 md:px-5 py-4 flex flex-wrap items-center justify-between gap-3">
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" :checked="allSelected" @change="toggleAll" class="w-4 h-4 accent-brand-primary" />
            <span class="text-sm text-brand-ink-2">全选</span>
          </label>
          <div class="flex items-center gap-2 text-xs text-brand-ink-3">
            <button @click="router.push('/browse')" class="px-4 h-9 rounded-md border border-brand-line hover:border-brand-primary/40 hover:text-brand-primary transition">继续逛逛</button>
            <button v-if="cartStore.items.some(i => i.selected)" @click="cartStore.items.filter(i => i.selected).forEach(i => removeItem(i.productId))"
              class="px-4 h-9 rounded-md border border-brand-error/30 text-brand-error hover:bg-brand-error-subtle transition">删除选中</button>
          </div>
        </div>
      </section>

      <!-- 结算侧栏 -->
      <aside class="space-y-4 lg:sticky lg:top-28 lg:self-start">
        <!-- 订单汇总 -->
        <div class="rounded-2xl bg-brand-surface border border-brand-line p-5">
          <h3 class="text-sm font-semibold text-brand-ink mb-4 flex items-center gap-2"><CreditCard class="w-4 h-4 text-brand-primary" />订单汇总</h3>
          <div class="space-y-3 text-sm">
            <div class="flex justify-between text-brand-ink-2">
              <span>已选商品 ({{ cartStore.selectedItems.length }})</span>
              <span class="text-brand-ink">¥{{ cartStore.subtotal.toFixed(0) }}</span>
            </div>
            <div class="flex justify-between text-brand-ink-2">
              <span class="flex items-center gap-1"><Truck class="w-3.5 h-3.5" />运费</span>
              <span v-if="cartStore.shippingFee === 0" class="text-brand-success">包邮</span>
              <span v-else class="text-brand-ink">¥{{ cartStore.shippingFee.toFixed(0) }}</span>
            </div>
            <div class="flex justify-between text-brand-ink-2">
              <span class="flex items-center gap-1"><Gift class="w-3.5 h-3.5" />优惠抵扣</span>
              <span class="text-brand-success">-¥{{ cartStore.discount.toFixed(0) }}</span>
            </div>
            <div class="flex justify-between text-brand-ink-2">
              <span class="flex items-center gap-1">可用积分</span>
              <span class="text-brand-warning flex items-center gap-1">12,800 分可用<input type="checkbox" class="w-3.5 h-3.5 ml-1 accent-brand-primary" /></span>
            </div>
          </div>

          <!-- 优惠券 -->
          <div class="mt-4 p-3 rounded-lg bg-brand-bg border border-brand-line">
            <div class="flex items-center justify-between">
              <div class="text-xs text-brand-ink-2 flex items-center gap-1.5"><Tag class="w-3.5 h-3.5 text-brand-warning" />优惠券</div>
              <button class="text-xs text-brand-primary hover:underline flex items-center gap-0.5">选择优惠券<ChevronRight class="w-3 h-3" /></button>
            </div>
            <div class="text-xs text-brand-success mt-1.5">已选 1 张 · 已抵 ¥50</div>
          </div>

          <!-- 合计 -->
          <div class="mt-5 pt-4 border-t border-brand-line">
            <div class="flex items-end justify-between">
              <div class="text-sm text-brand-ink-2">应付总额</div>
              <div class="flex items-baseline gap-1">
                <span class="text-xs text-brand-ink-3">¥</span>
                <span class="text-3xl font-black text-brand-error">{{ cartStore.total.toFixed(0) }}</span>
              </div>
            </div>
            <div class="flex items-start gap-2 mt-2 p-3 rounded-lg bg-brand-warning/10 border border-brand-warning/20">
              <AlertTriangle class="w-4 h-4 text-brand-warning shrink-0 mt-0.5" />
              <div class="text-[11px] text-brand-ink-2 leading-relaxed">平台实行<span class="text-brand-warning font-semibold">担保交易</span>：买家付款后资金暂存平台，买家确认收货无异议后才会结算给卖家。</div>
            </div>
          </div>

          <button @click="checkout" :disabled="cartStore.selectedItems.length === 0 || loading"
            class="w-full h-12 mt-4 rounded-xl bg-brand-primary text-brand-primary-ink font-bold hover:bg-brand-primary-hover disabled:opacity-40 transition flex items-center justify-center gap-2 shadow-1">
            <ShieldCheck class="w-4 h-4" />
            {{ loading ? '创建订单中...' : `去结算 (${cartStore.selectedItems.length})` }}
          </button>
          <button v-if="cartStore.selectedItems.length > 0" class="w-full h-10 mt-2 rounded-xl border border-brand-primary/40 text-brand-primary text-sm hover:bg-brand-primary-subtle transition flex items-center justify-center gap-2">
            <Heart class="w-3.5 h-3.5" />批量移入收藏夹
          </button>
        </div>

        <!-- 安全承诺 -->
        <div class="rounded-2xl bg-brand-surface border border-brand-line p-5 space-y-3">
          <h4 class="text-xs font-semibold text-brand-ink-3 uppercase tracking-wider">交易保障</h4>
          <div class="flex items-center gap-3 text-xs">
            <div class="w-8 h-8 rounded-lg bg-brand-primary/15 flex items-center justify-center shrink-0"><ShieldCheck class="w-4 h-4 text-brand-primary" /></div>
            <div class="text-brand-ink-2 leading-relaxed"><span class="font-semibold text-brand-ink">担保交易</span><br/>买家验货通过 → 放款给卖家</div>
          </div>
          <div class="flex items-center gap-3 text-xs">
            <div class="w-8 h-8 rounded-lg bg-brand-info/15 flex items-center justify-center shrink-0"><Package class="w-4 h-4 text-brand-info" /></div>
            <div class="text-brand-ink-2 leading-relaxed"><span class="font-semibold text-brand-ink">顺丰包邮</span><br/>满 ¥500 自动包邮，次日达</div>
          </div>
          <div class="flex items-center gap-3 text-xs">
            <div class="w-8 h-8 rounded-lg bg-brand-warning/15 flex items-center justify-center shrink-0"><AlertTriangle class="w-4 h-4 text-brand-warning" /></div>
            <div class="text-brand-ink-2 leading-relaxed"><span class="font-semibold text-brand-ink">7 天售后</span><br/>支持退/换货 + 平台仲裁</div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}
</style>
