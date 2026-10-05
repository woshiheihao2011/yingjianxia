import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getBuyerOrdersApi, getSellerOrdersApi, getOrderDetailApi,
  cancelOrderApi, confirmReceivedApi, remindShipApi, createOrderApi } from '@/api/index'
import * as api from '@/api/index'

export const useOrderStore = defineStore('order', () => {
  const currentOrder = ref(null)
  const orderList = ref([])
  const loading = ref(false)
  const pagination = ref({ page: 1, pageSize: 10, total: 0 })

  const statusCounts = computed(() => ({
    pendingPayment: orderList.value.filter(o => o.status === 'PENDING_PAYMENT').length,
    pendingShipment: orderList.value.filter(o => o.status === 'PENDING_SHIPMENT').length,
    pendingReceipt: orderList.value.filter(o => o.status === 'SHIPPED').length,
    pendingReview: orderList.value.filter(o => o.status === 'RECEIVED').length,
    aftersales: orderList.value.filter(o => ['REFUNDING', 'RETURNING'].includes(o.status)).length
  }))

  async function loadBuyerOrders(params = {}) {
    loading.value = true
    try {
      const data = await getBuyerOrdersApi({ ...pagination.value, ...params })
      orderList.value = data.items || data.list || []
      pagination.value.total = data.total || 0
    } finally { loading.value = false }
  }

  async function loadSellerOrders(params = {}) {
    loading.value = true
    try {
      const data = await getSellerOrdersApi({ ...pagination.value, ...params })
      orderList.value = data.items || data.list || []
      pagination.value.total = data.total || 0
    } finally { loading.value = false }
  }

  async function loadOrderDetail(id) {
    currentOrder.value = await getOrderDetailApi(id)
    return currentOrder.value
  }

  async function createOrder(payload) {
    return await createOrderApi(payload)
  }

  async function cancelOrder(id, reason) {
    await cancelOrderApi(id, reason)
    currentOrder.value = null
  }

  async function confirm(id) {
    await confirmReceivedApi(id)
    currentOrder.value = null
  }

  async function remindShip(id) {
    await remindShipApi(id)
  }

  return {
    currentOrder, orderList, loading, pagination, statusCounts,
    loadBuyerOrders, loadSellerOrders, loadOrderDetail,
    createOrder, cancelOrder, confirm, remindShip
  }
})
