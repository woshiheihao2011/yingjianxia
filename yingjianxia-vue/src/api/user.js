import request from './request'

// ========== 认证 AuthController /api/v1/auth ==========
// 密码登录
export function loginApi(payload) {
  return request.post('/auth/login/password', payload)
}

// 验证码登录
export function loginWithCodeApi(payload) {
  return request.post('/auth/login/sms', payload)
}

// 注册
export function registerApi(payload) {
  return request.post('/auth/register/phone', payload)
}

// 发送验证码（注册/找回密码/登录）
export function sendSmsCodeApi(phone, scene = 'register') {
  return request.post('/auth/sms/send', { phone, scene })
}

// 重置密码
export function resetPasswordApi(payload) {
  return request.post('/auth/reset-password', payload)
}

// 第三方 OAuth 登录
export function oauthLoginApi(provider, code) {
  return request.post('/auth/login/oauth', { provider, code })
}

// 刷新 Token
export function refreshTokenApi(refreshToken) {
  return request.post('/auth/token/refresh', { refreshToken })
}

// 登出
export function logoutApi() {
  return request.post('/auth/logout')
}

// ========== 用户资料 UserProfileController /api/v1/user ==========
// 获取当前用户信息
export function getUserInfoApi() {
  return request.get('/user/profile')
}

// 更新用户资料
export function updateUserInfoApi(payload) {
  return request.put('/user/profile', payload)
}

// 修改密码
export function changePasswordApi(payload) {
  return request.put('/user/password', payload)
}

// 实名认证
export function submitRealNameApi(payload) {
  return request.post('/user/real-name', payload)
}

// 实名认证状态
export function getRealNameStatusApi() {
  return request.get('/user/real-name/status')
}

// 信用分查询
export function getCreditScoreApi() {
  return request.get('/user/credit-score')
}

// 收货地址 CRUD
export function getAddressesApi() {
  return request.get('/user/addresses')
}
export function createAddressApi(payload) {
  return request.post('/user/address', payload)
}
export function updateAddressApi(id, payload) {
  return request.put(`/user/address/${id}`, payload)
}
export function deleteAddressApi(id) {
  return request.delete(`/user/address/${id}`)
}
export function setDefaultAddressApi(id) {
  return request.post(`/user/address/${id}/default`)
}

// ========== 店铺 ShopController /api/v1/shop ==========
export function createShopApi(payload) {
  return request.post('/shop/open', payload)
}
// 上传店铺图片（logo/banner）
export function uploadShopImageApi(file, type = 'logo') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return request.post('/shop/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export function getShopInfoApi(id) {
  return request.get(`/shop/${id}`)
}
export function getMyShopApi() {
  return request.get('/shop/mine')
}
export function updateShopApi(shopId, payload) {
  return request.put(`/shop/${shopId}`, payload)
}
export function updateShopSettingsApi(shopId, payload) {
  return request.put(`/shop/${shopId}/settings`, payload)
}
export function getShopSettingsApi() {
  return request.get('/shop/mine/settings')
}
export function getLogisticsTemplatesApi() {
  return request.get('/shop/mine/logistics-templates')
}
export function createLogisticsTemplateApi(payload) {
  return request.post('/shop/mine/logistics-templates', payload)
}
export function updateLogisticsTemplateApi(id, payload) {
  return request.put(`/shop/mine/logistics-templates/${id}`, payload)
}
export function deleteLogisticsTemplateApi(id) {
  return request.delete(`/shop/mine/logistics-templates/${id}`)
}
export function getServicePromisesApi() {
  return request.get('/shop/mine/service-promises')
}
export function toggleServicePromiseApi(id, enabled) {
  return request.put(`/shop/mine/service-promises/${id}`, { enabled })
}
export function getShopStaffApi() {
  return request.get('/shop/mine/staff')
}
export function createShopStaffApi(payload) {
  return request.post('/shop/mine/staff', payload)
}
export function deleteShopStaffApi(id) {
  return request.delete(`/shop/mine/staff/${id}`)
}
export function getShopCertificationsApi() {
  return request.get('/shop/mine/certifications')
}
export function getShopFinanceApi() {
  return request.get('/shop/mine/finance')
}
export function updateShopFinanceApi(payload) {
  return request.put('/shop/mine/finance', payload)
}
export function applySellerVerificationApi(payload) {
  return request.post('/shop/seller-verification/apply', payload)
}
export function getShopBySellerApi(sellerId) {
  return request.get(`/shop/by-seller/${sellerId}`)
}
export function getShopStatsApi(id) {
  return request.get(`/shop/${id}/stats`)
}

/* ---------- 登录设备管理 ---------- */
export function getDevicesApi() {
  return request.get('/user/devices')
}
export function kickDeviceApi(jti) {
  return request.delete(`/user/devices/${jti}`)
}
export function kickOtherDevicesApi() {
  return request.delete('/user/devices')
}
