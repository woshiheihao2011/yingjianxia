import request from './request'

// ========== 订单 ==========
export function createOrderApi(payload) {
  return request.post('/orders', payload)
}
export function getOrdersApi(params = {}) {
  return request.get('/orders', { params })
}
export function getOrderDetailApi(id) {
  return request.get(`/orders/${id}`)
}
export function cancelOrderApi(id, reason) {
  return request.post(`/orders/${id}/cancel`, { reason })
}
export function confirmReceivedApi(id) {
  return request.post(`/orders/${id}/confirm`)
}
export function remindShipApi(id) {
  return request.post(`/orders/${id}/remind-ship`)
}
// 买家视角 - 我的订单
export function getBuyerOrdersApi(params) { return getOrdersApi({ ...params, role: 'buyer' }) }
// 卖家视角 - 我的订单
export function getSellerOrdersApi(params) { return getOrdersApi({ ...params, role: 'seller' }) }
export function getOrderStatusLogsApi(id) {
  return request.get(`/orders/${id}/status-logs`)
}

// ========== 支付 ==========
export function createPaymentApi(orderId, channel = 'wallet') {
  return request.post('/payments', { orderId, channel })
}
export function queryPaymentApi(paymentId) {
  return request.get(`/payments/${paymentId}`)
}

// ========== 钱包 / 担保交易 ==========
export function getWalletInfoApi() {
  return request.get('/wallets/me')
}
export function getTransactionsApi(params = {}) {
  return request.get('/wallets/transactions', { params })
}
export function rechargeApi(amount, channel = 'wechat') {
  return request.post('/wallets/recharge', { amount, channel })
}
export function withdrawApi(payload) {
  return request.post('/wallets/withdraw', payload)
}
export function getWithdrawalRecordsApi(params) {
  return request.get('/wallets/withdrawals', { params })
}
export function getEscrowRecordsApi(params) {
  return request.get('/escrow/records', { params })
}

// ========== 营销 / 优惠券 / 积分 ==========
export function getCouponsApi(params = {}) {
  return request.get('/coupons', { params })
}
export function getAvailableCouponsApi(orderAmount) {
  return request.get('/coupons/available', { params: { orderAmount } })
}
export function claimCouponApi(couponId) {
  return request.post(`/coupons/${couponId}/claim`)
}
export function getPointsRecordsApi(params = {}) {
  return request.get('/points/records', { params })
}
export function getPointsSummaryApi() {
  return request.get('/points/summary')
}
export function signInApi() {
  return request.post('/points/sign-in')
}
export function redeemCouponByPointsApi(couponId) {
  return request.post('/points/redeem', { couponId })
}
export function getPromotionsApi(params = {}) {
  return request.get('/promotions', { params })
}
export function createPromotionApi(payload) {
  return request.post('/promotions', payload)
}

// ========== 物流 ==========
export function createShipmentApi(payload) {
  return request.post('/shipments', payload)
}
export function getShipmentsApi(params = {}) {
  return request.get('/shipments', { params })
}
export function getShipmentDetailApi(id) {
  return request.get(`/shipments/${id}`)
}
export function getLogisticsTracksApi(trackingNo) {
  return request.get(`/logistics/tracks`, { params: { trackingNo } })
}
export function previewWaybillApi(shipmentId) {
  return request.get(`/shipments/${shipmentId}/waybill-preview`)
}
export function calculateFreightApi(payload) {
  return request.post('/logistics/calculate-freight', payload)
}

// ========== 售后 ==========
export function applyAftersaleApi(payload) {
  return request.post('/aftersales', payload)
}
export function getAftersalesApi(params = {}) {
  return request.get('/aftersales', { params })
}
export function getAftersaleDetailApi(id) {
  return request.get(`/aftersales/${id}`)
}
export function sellerApproveAftersaleApi(id) {
  return request.post(`/aftersales/${id}/approve`)
}
export function sellerRejectAftersaleApi(id, reason) {
  return request.post(`/aftersales/${id}/reject`, { reason })
}
export function submitReturnShipmentApi(id, trackingInfo) {
  return request.post(`/aftersales/${id}/return-shipment`, trackingInfo)
}
export function sellerConfirmReturnedApi(id) {
  return request.post(`/aftersales/${id}/confirm-returned`)
}
export function requestArbitrationApi(id, reason) {
  return request.post(`/aftersales/${id}/arbitration`, { reason })
}

// ========== 评价 EvaluationController /api/v1/evaluation ==========
export function submitReviewApi(payload) {
  return request.post('/evaluation/reviews', payload)
}
export function getReviewsApi(params = {}) {
  return request.get('/evaluation/reviews', { params })
}
export function getMyReviewsApi(params) {
  return request.get('/evaluation/reviews/mine', { params })
}
export function replyReviewApi(reviewId, replyContent) {
  return request.post('/evaluation/reviews/reply', { reviewId, replyContent })
}
export function getProductReviewsApi(productId, params = {}) {
  return request.get(`/evaluation/products/${productId}/reviews`, { params })
}
export function getReviewTagsApi() {
  return request.get('/evaluation/tags')
}
export function addFollowUpReviewApi(id, content, images = []) {
  return request.post(`/evaluation/reviews/${id}/follow-up`, { content, images })
}
export function getReviewStatsApi(shopId) {
  return request.get(`/shop/${shopId}/reviews/stats`)
}

// ========== 消息 / IM ==========
export function getConversationsApi() {
  return request.get('/messages/conversations')
}
export function getMessagesApi(conversationId, params = {}) {
  return request.get(`/messages/conversations/${conversationId}`, { params })
}
export function sendMessageApi(conversationId, content, type = 'text') {
  return request.post(`/messages/conversations/${conversationId}`, { content, type })
}
export function markAsReadApi(conversationId) {
  return request.post(`/messages/conversations/${conversationId}/read`)
}
export function markAllAsReadApi() {
  return request.post('/messages/read-all')
}
export function getUnreadCountApi() {
  return request.get('/messages/unread-count')
}

// ========== 社区 ==========
export function getCommunityPostsApi(params = {}) {
  return request.get('/community/posts', { params })
}
export function getPostDetailApi(id) {
  return request.get(`/community/posts/${id}`)
}
export function createPostApi(payload) {
  return request.post('/community/posts', payload)
}
export function likePostApi(id) {
  return request.post(`/community/posts/${id}/like`)
}
export function commentPostApi(id, content) {
  return request.post(`/community/posts/${id}/comment`, { content })
}
export function getHotTopicsApi() { return request.get('/community/hot-topics') }
export function getCommunityRanksApi() { return request.get('/community/ranks') }

// ========== 客服 / 工单 / 举报 / 公告 ==========
export function getTicketsApi(params = {}) { return request.get('/tickets', { params }) }
export function createTicketApi(payload) { return request.post('/tickets', payload) }
export function getTicketDetailApi(id) { return request.get(`/tickets/${id}`) }
export function replyTicketApi(id, content) { return request.post(`/tickets/${id}/reply`, { content }) }
export function closeTicketApi(id) { return request.post(`/tickets/${id}/close`) }
export function reOpenTicketApi(id) { return request.post(`/tickets/${id}/reopen`) }
export function getFaqsApi(params = {}) { return request.get('/faqs', { params }) }
export function getQuickRepliesApi() { return request.get('/quick-replies') }
export function createReportApi(payload) { return request.post('/reports', payload) }
export function getReportsApi(params) { return request.get('/reports', { params }) }
export function getAnnouncementsApi(params = {}) { return request.get('/announcements', { params }) }
export function getAnnouncementDetailApi(id) { return request.get(`/announcements/${id}`) }

// ========== 风控 / 数据统计 ==========
export function getSellerAnalyticsApi(params = {}) { return request.get('/analytics/seller', { params }) }
export function getTrustOverviewApi() { return request.get('/trust/overview') }

// ========== WebSocket 支持 ==========
export function buildWebSocketUrl() {
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${location.host}/ws`
}
