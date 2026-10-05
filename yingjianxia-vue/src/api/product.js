import request from './request'

// ========== 商品 CRUD ProductController /api/v1/product ==========
export function getProductsApi(params = {}) {
  return request.get('/product/search', { params })
}
export function getProductDetailApi(id) {
  return request.get(`/product/${id}`)
}
// 审核员/管理端查看商品详情（不过滤状态，不增加浏览量）
export function getProductInternalDetailApi(id) {
  return request.get(`/product/internal/${id}`)
}
export function createProductApi(payload) {
  return request.post('/product', payload)
}
export function updateProductApi(id, payload) {
  return request.put(`/product/${id}`, payload)
}
export function deleteProductApi(id) {
  return request.delete(`/product/${id}`)
}
export function publishProductApi(id) {
  return request.post(`/product/${id}/submit-review`)
}
export function offShelfProductApi(id) {
  return request.post(`/product/${id}/off-shelf`)
}
export function reListProductApi(id) {
  return request.post(`/product/${id}/re-submit`)
}

// ========== 我的发布 ==========
export function getMyListingsApi(params = {}) {
  return request.get('/product/mine', { params })
}
export function getInventoryListApi(params = {}) {
  return request.get('/product/inventory', { params })
}
export function batchOffShelfApi(productIds) {
  return request.post('/product/batch-off-shelf', { productIds })
}
export function batchDeleteApi(productIds) {
  return request.post('/product/batch-delete', { productIds })
}
export function batchRelistApi(productIds) {
  return request.post('/product/batch-relist', { productIds })
}
export function batchUpdatePriceApi(payload) {
  return request.post('/product/batch-update-price', payload)
}
export function batchUpdateStockApi(payload) {
  return request.post('/product/batch-update-stock', payload)
}

// ========== 卖家统计 ==========
export function getSellerStatsApi() {
  return request.get('/product/mine/stats')
}

// ========== 文件上传 ==========
export function uploadFileApi(formData) {
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ========== 批量导入 ==========
export function batchImportApi(formData) {
  return request.post('/product/batch-import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ========== 商品收藏 CategoryFavoriteController ==========
export function getFavoritesApi(params) {
  return request.get('/favorites', { params })
}
export function addFavoriteApi(productId) {
  return request.post(`/favorite/${productId}`)
}
export function removeFavoriteApi(productId) {
  return request.delete(`/favorite/${productId}`)
}
export function checkFavoriteApi(productId) {
  return request.get(`/favorite/${productId}/check`)
}

// ========== 浏览量 / 想要数 ==========
export function incrementViewApi(productId) {
  return request.post(`/product/${productId}/view`)
}

// ========== 商品分类 ==========
export function getCategoriesApi() {
  return request.get('/categories')
}

// ========== 购物车（order-service，暂未启动）==========
export function getCartApi() {
  return request.get('/cart')
}
export function addToCartApi(payload) {
  return request.post('/cart', payload)
}
export function updateCartApi(productId, quantity) {
  return request.put(`/cart/${productId}`, { quantity })
}
export function removeCartItemApi(productId) {
  return request.delete(`/cart/${productId}`)
}
export function clearCartApi() {
  return request.delete('/cart/clear')
}

// ========== ES 搜索 ==========
export function searchProductsApi(params) {
  return request.get('/product/search', { params })
}
export function getHotKeywordsApi() {
  return request.get('/product/hot-keywords')
}

// ========== 验机服务 InspectionController /api/v1/inspection ==========
export function getInspectionTemplatesApi() {
  return request.get('/inspection/templates')
}
export function getInspectionReportApi(productId) {
  return request.get(`/inspection/report/by-product/${productId}`)
}
export function getInspectionReportDetailApi(reportId) {
  return request.get(`/inspection/report/${reportId}`)
}
export function verifyInspectionSignatureApi(reportId, signature) {
  return request.get(`/inspection/report/${reportId}/verify`, { params: { signature } })
}
export function createInspectionTaskApi(productId) {
  return request.post('/inspection/report/create', { productId })
}
export function startInspectionReportApi(reportId) {
  return request.post(`/inspection/report/${reportId}/start`)
}
export function submitInspectionItemsApi(reportId, items) {
  return request.post('/inspection/report/submit', { reportId, items })
}

// ========== 审核 AuditController /api/v1/audit ==========
export function submitAuditApi(productId) {
  return request.post('/audit/submit', { productId })
}
export function getAuditRecordsApi(params) {
  return request.get('/audit/records', { params })
}
export function getAuditRecordDetailApi(recordId) {
  return request.get(`/audit/records/${recordId}`)
}
