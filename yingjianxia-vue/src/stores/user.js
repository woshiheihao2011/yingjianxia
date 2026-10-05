import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, registerApi, sendSmsCodeApi, getUserInfoApi, refreshTokenApi } from '@/api/user'

const ACCESS_TOKEN_KEY = 'yjx_access_token'
const REFRESH_TOKEN_KEY = 'yjx_refresh_token'
const USER_INFO_KEY = 'yjx_user_info'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(localStorage.getItem(ACCESS_TOKEN_KEY) || '')
  const refreshToken = ref(localStorage.getItem(REFRESH_TOKEN_KEY) || '')
  const userInfo = ref(JSON.parse(localStorage.getItem(USER_INFO_KEY) || 'null'))

  const isLoggedIn = computed(() => !!accessToken.value && !!userInfo.value)
  const isSeller = computed(() => {
    const info = userInfo.value
    if (!info) return false
    // 通过 shopId 判断是否已有店铺（GET /user/profile 返回 shopId 字段）
    if (info.shopId != null && info.shopId !== '' && info.shopId !== 0) return true
    if (info.shopInfo != null) return true
    if (Array.isArray(info.roles) && info.roles.includes('SELLER')) return true
    return false
  })
  const isVerified = computed(() => userInfo.value?.realNameVerified === true)

  // 生成幂等 Key - 用于所有写操作请求
  function generateIdempotencyKey() {
    return `idem_${Date.now()}_${Math.random().toString(36).substring(2, 15)}`
  }

  // 登录
  async function login(payload) {
    const data = await loginApi(payload)
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
    await fetchUserInfo()
  }

  // 注册
  async function register(payload) {
    await registerApi(payload)
  }

  // 发送验证码
  async function sendSmsCode(phone) {
    await sendSmsCodeApi(phone)
  }

  // 获取用户信息
  async function fetchUserInfo() {
    const data = await getUserInfoApi()
    userInfo.value = data
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(data))
  }

  // 刷新 Token
  async function refreshAccessToken() {
    try {
      const data = await refreshTokenApi(refreshToken.value)
      accessToken.value = data.accessToken
      localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
      return true
    } catch {
      logout()
      return false
    }
  }

  // 登出 - Token 加入黑名单逻辑由后端处理
  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    isLoggedIn,
    isSeller,
    isVerified,
    generateIdempotencyKey,
    login,
    register,
    sendSmsCode,
    fetchUserInfo,
    refreshAccessToken,
    logout
  }
})
