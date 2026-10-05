import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { addToCartApi, updateCartApi, removeCartItemApi, clearCartApi, getCartApi, getProductDetailApi } from '@/api/product'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loading = ref(false)

  const totalCount = computed(() => items.value.reduce((sum, it) => sum + it.quantity, 0))

  const selectedItems = computed(() => items.value.filter(it => it.selected))

  const subtotal = computed(() =>
    selectedItems.value.reduce((sum, it) => sum + it.price * it.quantity, 0)
  )

  const shippingFee = computed(() => (subtotal.value > 0 && subtotal.value < 500 ? 12 : 0))

  const discount = computed(() => 0) // 由营销服务计算

  const total = computed(() => subtotal.value + shippingFee.value - discount.value)

  // 从后端加载购物车（含商品详情）
  async function loadCart() {
    loading.value = true
    try {
      const cartList = await getCartApi()
      const list = Array.isArray(cartList) ? cartList : (cartList?.records || [])
      if (list.length === 0) {
        items.value = []
        return
      }
      // 并发获取每个商品的详情
      const detailResults = await Promise.allSettled(
        list.map(item => getProductDetailApi(item.productId))
      )
      items.value = list.map((item, i) => {
        const detail = detailResults[i].status === 'fulfilled' ? detailResults[i].value : {}
        return {
          productId: item.productId,
          title: detail.title || '商品加载中...',
          image: detail.imageUrls?.[0] || detail.coverImage || '',
          price: Number(detail.price) || 0,
          condition: detail.conditionLevel,
          sellerId: detail.sellerId,
          sellerName: detail.sellerName || '',
          shopName: detail.shopName || '',
          shopId: detail.shopId,
          stock: detail.stock != null ? detail.stock : 0,
          quantity: item.quantity,
          selected: item.isSelected === 1 || item.isSelected === true,
          specs: detail.specs?.map(s => ({ k: s.name, v: s.value })) || null
        }
      })
    } catch (e) {
      console.error('加载购物车失败', e)
    } finally {
      loading.value = false
    }
  }

  // 添加到购物车
  async function addToCart(product, quantity = 1) {
    const existing = items.value.find(it => it.productId === product.id)
    if (existing) {
      existing.quantity += quantity
    } else {
      items.value.push({
        productId: product.id,
        title: product.title,
        image: product.coverImage,
        price: product.price,
        condition: product.condition,
        sellerId: product.sellerId,
        sellerName: product.sellerName,
        shopName: product.shopName || product.shop?.name || '',
        shopId: product.shopId || product.shop?.id,
        stock: product.stock != null ? product.stock : 0,
        quantity,
        selected: true,
        specs: product.specs || null
      })
    }
    try { await addToCartApi({ productId: product.id, quantity }) } catch { /* 降级本地 */ }
  }

  // 更新数量
  async function updateQuantity(productId, qty) {
    const item = items.value.find(it => it.productId === productId)
    if (item) {
      item.quantity = qty
      try { await updateCartApi(productId, qty) } catch { /* 降级本地 */ }
    }
  }

  // 切换选中
  function toggleSelect(productId) {
    const item = items.value.find(it => it.productId === productId)
    if (item) item.selected = !item.selected
  }

  // 全选/反选
  function toggleSelectAll(selected) {
    items.value.forEach(it => it.selected = selected)
  }

  // 删除项
  async function removeItem(productId) {
    items.value = items.value.filter(it => it.productId !== productId)
    try { await removeCartItemApi(productId) } catch { /* 降级本地 */ }
  }

  // 清空
  async function clear() {
    items.value = []
    try { await clearCartApi() } catch { /* 降级本地 */ }
  }

  return {
    items,
    loading,
    totalCount,
    selectedItems,
    subtotal,
    shippingFee,
    discount,
    total,
    loadCart,
    addToCart,
    updateQuantity,
    toggleSelect,
    toggleSelectAll,
    removeItem,
    clear
  }
})
