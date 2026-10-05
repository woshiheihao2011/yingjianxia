import axios from 'axios'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 错误码分段：用户域 20xxx / 商品域 30xxx / 订单域 40xxx / 资金域 50xxx / 售后域 60xxx / 营销域 70xxx
const codeMessage = {
  20001: '用户不存在', 20002: '密码错误', 20003: '验证码错误',
  20004: '手机号已注册', 20005: '账号已锁定', 20006: 'Token 无效或过期',
  2010: '商品不存在或已删除',
  30001: '商品不存在', 30002: '商品已下架', 30003: '库存不足',
  40001: '订单不存在', 40002: '订单状态异常',
  50001: '余额不足', 50002: '提现金额超限', 50003: '支付验签失败',
  60001: '售后申请已过期', 60002: '售后已处理'
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  timeout: 15000,
  // 解决后端 Long 类型 ID（19位）超出 JS 安全整数范围导致精度丢失的问题
  transformResponse: [function (data) {
    if (typeof data === 'string') {
      // 先保护所有 JSON 字符串值（用占位符替换），避免大数字转换时破坏字符串内部的转义
      const strings = []
      let protectedData = data.replace(/"(?:\\.|[^"\\])*"/g, (match) => {
        strings.push(match)
        return `\u0000${strings.length - 1}\u0000`
      })
      // 将 16 位以上的纯数字转为字符串，避免精度丢失（此时只匹配真正的 JSON 数值）
      protectedData = protectedData.replace(/:(\s*)(\d{16,})(\s*[,}\]])/g, ':"$2"$3')
      // 恢复被保护的字符串值
      data = protectedData.replace(/\u0000(\d+)\u0000/g, (_, i) => strings[parseInt(i, 10)])
    }
    try {
      return JSON.parse(data)
    } catch (e) {
      return data
    }
  }]
})

// 请求拦截器：JWT Token + 幂等 Key 注入
request.interceptors.request.use(config => {
  const userStore = useUserStore()

  // JWT 注入
  if (userStore.accessToken) {
    config.headers['Authorization'] = `Bearer ${userStore.accessToken}`
  }

  // 幂等 Key：所有写操作 POST/PUT/DELETE 自动携带
  if (['post', 'put', 'delete'].includes(String(config.method).toLowerCase())) {
    config.headers['X-Idempotency-Key'] = userStore.generateIdempotencyKey()
  }

  return config
}, error => Promise.reject(error))

// 响应拦截器：统一响应体处理 + 401 Token 刷新
let refreshing = false
let pendingRequests = []

request.interceptors.response.use(
  response => {
    const body = response.data
    // 统一响应体：{ code, message, data, timestamp }
    if (body && body.code === 0) return body.data
    return Promise.reject({ code: body?.code, message: body?.message || '请求失败' })
  },
  async error => {
    const status = error.response?.status
    const body = error.response?.data

    // 401：Token 过期，尝试刷新
    if (status === 401) {
      const userStore = useUserStore()

      // 防止重试请求再次 401 导致死循环
      if (error.config._isRetry) {
        userStore.logout()
        router.push({ path: '/auth' })
        return Promise.reject({ code: 401, message: '登录已过期，请重新登录' })
      }

      // 已有刷新在进行中，排队等待
      if (refreshing) {
        return new Promise((resolve, reject) => {
          pendingRequests.push({ resolve, reject, config: error.config })
        })
      }

      refreshing = true
      let refreshed = false
      try {
        refreshed = await userStore.refreshAccessToken()
      } catch {
        refreshed = false
      } finally {
        refreshing = false
      }

      if (refreshed) {
        // 刷新成功：重试排队请求 + 原始请求
        const newToken = userStore.accessToken
        pendingRequests.forEach(({ resolve, config }) => {
          config.headers['Authorization'] = `Bearer ${newToken}`
          config._isRetry = true
          resolve(request(config))
        })
        pendingRequests = []

        error.config.headers['Authorization'] = `Bearer ${newToken}`
        error.config._isRetry = true
        return request(error.config)
      }

      // 刷新失败：拒绝所有排队请求，登出跳转
      pendingRequests.forEach(({ reject }) => {
        reject({ code: 401, message: '登录已过期，请重新登录' })
      })
      pendingRequests = []

      userStore.logout()
      router.push({ path: '/auth' })
      return Promise.reject({ code: 401, message: '登录已过期，请重新登录' })
    }

    const msg = codeMessage[body?.code] || body?.message || error.message || '网络异常'
    return Promise.reject({ code: body?.code || status, message: msg })
  }
)

export default request
