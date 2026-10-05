<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  getUserInfoApi, getCreditScoreApi, getRealNameStatusApi, getAddressesApi,
  updateUserInfoApi, createAddressApi, updateAddressApi, deleteAddressApi,
  setDefaultAddressApi, changePasswordApi, submitRealNameApi,
  getDevicesApi, kickDeviceApi, kickOtherDevicesApi, sendSmsCodeApi
} from '@/api/user'
import { getOrdersApi, getWalletInfoApi, getCouponsApi } from '@/api'
import { getFavoritesApi } from '@/api/product'
import {
  User, Phone, ShieldCheck, CreditCard, MapPin, Store, ShoppingBag, Heart,
  Gift, Settings, Edit2, Plus, Trash2, Star, ChevronRight, Package,
  Clock, Eye, LogOut, Award, BadgeCheck, Monitor, AlertCircle
} from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('profile')
const saving = ref(false)
const loading = ref(false)
const isEditing = ref(false)
const editBackup = ref({})

// ---------- 收货地址 ----------
const showAddressForm = ref(false)
const editingAddressId = ref(null)
const addressForm = ref({
  receiverName: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})

function openAddressForm(addr) {
  if (addr) {
    editingAddressId.value = addr.id
    addressForm.value = {
      receiverName: addr.name,
      phone: addr.phone,
      province: addr.region.split(' ')[0] || '',
      city: addr.region.split(' ')[1] || '',
      district: addr.region.split(' ')[2] || '',
      detail: addr.detail,
      isDefault: addr.isDefault
    }
  } else {
    editingAddressId.value = null
    addressForm.value = { receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
  }
  showAddressForm.value = true
}

function resetAddressForm() {
  showAddressForm.value = false
  editingAddressId.value = null
  addressForm.value = { receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
}

async function saveAddress() {
  if (!addressForm.value.receiverName || !addressForm.value.phone || !addressForm.value.detail) {
    alert('请填写收货人、手机号和详细地址')
    return
  }
  try {
    const payload = { ...addressForm.value }
    if (editingAddressId.value) {
      await updateAddressApi(editingAddressId.value, payload)
      alert('地址更新成功')
    } else {
      await createAddressApi(payload)
      alert('地址添加成功')
    }
    resetAddressForm()
    loadAddresses()
  } catch (e) {
    alert(e.message || '保存失败')
  }
}

async function handleDeleteAddress(addr) {
  if (!confirm(`确定删除「${addr.name}」的地址？`)) return
  try {
    await deleteAddressApi(addr.id)
    alert('已删除')
    loadAddresses()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

async function handleSetDefault(addr) {
  try {
    await setDefaultAddressApi(addr.id)
    alert('已设为默认地址')
    loadAddresses()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

async function loadAddresses() {
  try {
    const data = await getAddressesApi()
    if (Array.isArray(data)) {
      addresses.value = data.map(a => ({
        id: a.id,
        name: a.receiverName || '',
        phone: a.phone || '',
        region: [a.province, a.city, a.district].filter(Boolean).join(' '),
        detail: a.detail || '',
        isDefault: a.isDefault === true
      }))
    }
  } catch (e) {
    console.error('加载地址失败', e)
  }
}

// ---------- 账号安全 ----------
const showPwdModal = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdSaving = ref(false)

async function savePassword() {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    alert('请填写原密码和新密码')
    return
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    alert('两次输入的新密码不一致')
    return
  }
  pwdSaving.value = true
  try {
    await changePasswordApi({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    alert('密码修改成功，请重新登录')
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    showPwdModal.value = false
    userStore.logout()
    router.push('/login')
  } catch (e) {
    alert(e.message || '修改失败')
  } finally {
    pwdSaving.value = false
  }
}

// 实名认证
const showRealNameModal = ref(false)
const realNameForm = ref({ realName: '', idCard: '' })
const realNameStatus = ref(0)

// 登录设备管理
const showDeviceModal = ref(false)
const deviceList = ref([])
const currentDevice = ref({
  deviceName: '',
  os: '',
  browser: '',
  loginAt: '',
  ip: '—',
  isCurrent: true
})

function detectDevice() {
  const ua = navigator.userAgent
  let browser = '未知浏览器'
  let os = '未知系统'
  if (/Edg\//.test(ua)) browser = 'Microsoft Edge'
  else if (/Chrome\//.test(ua)) browser = 'Google Chrome'
  else if (/Firefox\//.test(ua)) browser = 'Firefox'
  else if (/Safari\//.test(ua)) browser = 'Safari'
  if (/Windows/.test(ua)) os = 'Windows'
  else if (/Mac OS X/.test(ua)) os = 'macOS'
  else if (/Android/.test(ua)) os = 'Android'
  else if (/iPhone|iPad/.test(ua)) os = 'iOS'
  currentDevice.value.browser = browser
  currentDevice.value.os = os
  currentDevice.value.deviceName = `${os} · ${browser}`
  currentDevice.value.loginAt = (userStore.userInfo?.loginAt || new Date().toISOString()).slice(0, 19).replace('T', ' ')
}

async function loadDevices() {
  try {
    const data = await getDevicesApi()
    if (Array.isArray(data)) {
      deviceList.value = data.map(d => ({
        jti: d.jti,
        deviceName: d.deviceName || '未知设备',
        os: d.os || '—',
        browser: d.browser || '—',
        ip: d.ip || '—',
        loginAt: (d.loginAt || '').slice(0, 19).replace('T', ' '),
        lastActiveAt: (d.lastActiveAt || '').slice(0, 19).replace('T', ' '),
        isCurrent: d.current === true
      }))
      const cur = deviceList.value.find(d => d.isCurrent)
      if (cur) {
        currentDevice.value = { ...cur }
      }
    }
  } catch (e) {
    console.error('加载设备列表失败', e)
  }
}

function openDeviceModal() {
  showDeviceModal.value = true
  loadDevices()
}

async function handleKickDevice(device) {
  if (!confirm(`确定踢出「${device.deviceName}」？该设备将被强制下线。`)) return
  try {
    await kickDeviceApi(device.jti)
    alert('已踢出该设备')
    loadDevices()
  } catch (e) {
    alert(e.message || '踢出失败')
  }
}

async function handleKickOtherDevices() {
  if (!confirm('确定退出所有其他设备？其他设备将被强制下线。')) return
  try {
    await kickOtherDevicesApi()
    alert('已退出所有其他设备')
    loadDevices()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

// 换绑手机号
const showPhoneModal = ref(false)
const phoneForm = ref({ newPhone: '', code: '' })
const smsSending = ref(false)
const smsCountdown = ref(0)
let smsTimer = null

async function sendPhoneCode() {
  if (!/^1\d{10}$/.test(phoneForm.value.newPhone)) {
    alert('请输入正确的手机号')
    return
  }
  smsSending.value = true
  try {
    await sendSmsCodeApi(phoneForm.value.newPhone, 'change_phone')
    alert('验证码已发送')
    smsCountdown.value = 60
    smsTimer = setInterval(() => {
      smsCountdown.value--
      if (smsCountdown.value <= 0) clearInterval(smsTimer)
    }, 1000)
  } catch (e) {
    alert(e.message || '验证码发送失败')
  } finally {
    smsSending.value = false
  }
}

async function submitPhoneChange() {
  if (!phoneForm.value.newPhone || !phoneForm.value.code) {
    alert('请填写新手机号和验证码')
    return
  }
  alert('手机号换绑功能后端暂未支持，敬请期待')
}

async function submitRealName() {
  if (!realNameForm.value.realName || !realNameForm.value.idCard) {
    alert('请填写真实姓名和身份证号')
    return
  }
  try {
    await submitRealNameApi(realNameForm.value)
    alert('实名认证提交成功')
    showRealNameModal.value = false
    realNameStatus.value = 1
    loadAll()
  } catch (e) {
    alert(e.message || '提交失败')
  }
}

// 订单状态映射：后端 status → 前端状态
const ORDER_STATUS = {
  0: '待付款', 1: '待发货', 2: '待收货', 3: '待评价',
  4: '已取消', 5: '已完成', 6: '售后中'
}

const profile = ref({
  nickname: '',
  avatar: '',
  phone: '',
  email: '',
  gender: 'secret',
  birthday: '',
  region: '',
  realNameStatus: 0,
  createdAt: ''
})

const credit = ref({
  score: 0, level: '—', rank: '—',
  items: [
    { label: '身份认证', status: false },
    { label: '手机号验证', status: false },
    { label: '实名认证', status: false },
    { label: '芝麻信用', status: false },
    { label: '保证金缴纳', status: false }
  ],
  recent: []
})

const addresses = ref([])

// 订单各状态数量
const orderCounts = ref({ 0: 0, 1: 0, 2: 0, 3: 0 })
// 钱包余额
const walletBalance = ref(0)
// 优惠券数量
const couponCount = ref(0)
// 收藏数量
const favoriteCount = ref(0)

// 根据信用分计算等级
function creditLevel(score) {
  if (score >= 750) return { level: '优秀', rank: 'Top 8%' }
  if (score >= 700) return { level: '良好', rank: 'Top 20%' }
  if (score >= 650) return { level: '中等', rank: 'Top 40%' }
  if (score >= 600) return { level: '一般', rank: 'Top 60%' }
  return { level: '待提升', rank: '—' }
}

// 头像首字母
const avatarChar = computed(() => {
  const n = profile.value.nickname
  return n ? n.charAt(0).toUpperCase() : 'U'
})

const quickLinks = computed(() => [
  { icon: ShoppingBag, name: '我的订单', count: orderCounts.value[0] + orderCounts.value[1] + orderCounts.value[2] + orderCounts.value[3], path: '/orders', color: 'text-brand-primary bg-brand-primary/15' },
  { icon: Heart, name: '我的收藏', count: favoriteCount.value, path: '/favorites', color: 'text-brand-error bg-brand-error/15' },
  { icon: CreditCard, name: '钱包余额', count: '¥' + Number(walletBalance.value).toFixed(2), path: '/wallet', color: 'text-brand-info bg-brand-info/15' },
  { icon: Gift, name: '优惠券', count: couponCount.value, path: '/coupons', color: 'text-brand-warning bg-brand-warning/15' }
])

const orderStats = computed(() => [
  { icon: Clock, name: '待付款', count: orderCounts.value[0], status: 0 },
  { icon: Package, name: '待发货', count: orderCounts.value[1], status: 1 },
  { icon: Eye, name: '待收货', count: orderCounts.value[2], status: 2 },
  { icon: Star, name: '待评价', count: orderCounts.value[3], status: 3 }
])

function logout() {
  userStore.logout()
  router.push('/')
}

// ---------- 个人资料编辑 ----------
function startEdit() {
  editBackup.value = { ...profile.value }
  isEditing.value = true
}

function cancelEdit() {
  profile.value = { ...editBackup.value }
  isEditing.value = false
}

async function saveProfile() {
  saving.value = true
  try {
    await updateUserInfoApi({
      nickname: profile.value.nickname,
      email: profile.value.email,
      gender: profile.value.gender,
      birthday: profile.value.birthday,
      region: profile.value.region
    })
    alert('保存成功')
    isEditing.value = false
    // 同步 userStore
    if (userStore.userInfo) {
      userStore.userInfo.nickname = profile.value.nickname
    }
  } catch (e) {
    alert(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 点击订单状态跳转到订单页
function goOrdersByStatus(status) {
  router.push({ path: '/orders', query: { status } })
}

async function loadAll() {
  loading.value = true
  try {
    // 并发加载
    const [profileResp, creditResp, realNameResp, addrResp, ordersResp, walletResp, couponsResp, favResp] =
      await Promise.allSettled([
        getUserInfoApi(),
        getCreditScoreApi(),
        getRealNameStatusApi(),
        getAddressesApi(),
        getOrdersApi({ pageNum: 1, pageSize: 200 }),
        getWalletInfoApi(),
        getCouponsApi({ pageNum: 1, pageSize: 200 }),
        getFavoritesApi({ page: 1, size: 1 })
      ])

    // 用户资料
    if (profileResp.status === 'fulfilled' && profileResp.value) {
      const u = profileResp.value
      profile.value = {
        nickname: u.nickname || '硬件侠用户',
        avatar: u.avatarUrl || '',
        phone: u.phone || '',
        email: u.email || '',
        gender: u.gender || 'secret',
        birthday: u.birthday || '',
        region: u.region || '',
        realNameStatus: u.realNameStatus ?? 0,
        createdAt: u.createdAt || ''
      }
    }

    // 信用分
    if (creditResp.status === 'fulfilled') {
      const score = creditResp.value || 0
      const lv = creditLevel(score)
      credit.value = { ...credit.value, score, ...lv }
    }

    // 实名状态
    if (realNameResp.status === 'fulfilled') {
      const rs = realNameResp.value ?? 0
      profile.value.realNameStatus = rs
      realNameStatus.value = rs
      credit.value.items = credit.value.items.map(it => {
        if (it.label === '实名认证') it.status = rs === 1
        if (it.label === '手机号验证') it.status = true
        return it
      })
    }

    // 收货地址
    if (addrResp.status === 'fulfilled' && Array.isArray(addrResp.value)) {
      addresses.value = addrResp.value.map(a => ({
        id: a.id,
        name: a.receiverName || '',
        phone: a.phone || '',
        region: [a.province, a.city, a.district].filter(Boolean).join(' '),
        detail: a.detail || '',
        isDefault: a.isDefault === true
      }))
    }

    // 订单统计（按状态聚合）
    if (ordersResp.status === 'fulfilled' && ordersResp.value) {
      const records = ordersResp.value.records || ordersResp.value.list || []
      const counts = { 0: 0, 1: 0, 2: 0, 3: 0 }
      records.forEach(o => {
        if (counts[o.status] !== undefined) counts[o.status]++
      })
      orderCounts.value = counts
    }

    // 钱包余额
    if (walletResp.status === 'fulfilled' && walletResp.value) {
      walletBalance.value = walletResp.value.availableBalance || walletResp.value.totalBalance || 0
    }

    // 优惠券数量
    if (couponsResp.status === 'fulfilled') {
      const data = couponsResp.value
      if (Array.isArray(data)) couponCount.value = data.length
      else if (data && Array.isArray(data.records)) couponCount.value = data.records.length
      else couponCount.value = 0
    }

    // 收藏数量
    if (favResp.status === 'fulfilled' && favResp.value) {
      favoriteCount.value = favResp.value.total || 0
    }
  } catch (e) {
    console.error('加载个人中心数据失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (userStore.userInfo?.nickname) profile.value.nickname = userStore.userInfo.nickname
  detectDevice()
  loadAll()
})
</script>

<template>
  <div class="container-app space-y-6">
    <!-- 顶部用户信息卡 -->
    <div class="rounded-2xl bg-gradient-to-br from-brand-primary/20 via-brand-surface to-brand-bg border border-brand-line p-6 md:p-8 relative overflow-hidden">
      <div class="absolute top-0 right-0 w-80 h-80 rounded-full bg-brand-primary/10 -translate-y-1/3 translate-x-1/3 blur-3xl"></div>
      <div class="relative flex flex-col md:flex-row md:items-center gap-6">
        <div class="flex items-center gap-5">
          <div class="w-20 h-20 md:w-24 md:h-24 rounded-2xl bg-gradient-to-br from-brand-primary to-[#00B894] flex items-center justify-center text-4xl font-bold text-brand-primary-ink shadow-2">
            {{ avatarChar }}
          </div>
          <div>
            <div class="flex items-center gap-2">
              <h2 class="text-2xl font-bold text-brand-ink">{{ profile.nickname || '硬件侠用户' }}</h2>
              <span v-if="profile.realNameStatus === 1" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-brand-primary/15 text-brand-primary border border-brand-primary/30">
                <BadgeCheck class="w-3 h-3" />已实名
              </span>
              <span v-else class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-brand-warning/15 text-brand-warning border border-brand-warning/30">
                <Clock class="w-3 h-3" />未实名
              </span>
            </div>
            <div class="flex items-center gap-4 mt-2 text-sm text-brand-ink-2">
              <span class="flex items-center gap-1"><Phone class="w-3.5 h-3.5" />{{ profile.phone }}</span>
              <span class="flex items-center gap-1"><MapPin class="w-3.5 h-3.5" />{{ profile.region }}</span>
            </div>
            <div class="flex items-center gap-3 mt-3">
              <div class="flex items-center gap-1.5 px-3 py-1 rounded-full bg-brand-warning/15 border border-brand-warning/30">
                <Award class="w-3.5 h-3.5 text-brand-warning" />
                <span class="text-xs font-semibold text-brand-warning">信用 {{ credit.score }}</span>
                <span class="text-xs text-brand-ink-3">· {{ credit.level }}</span>
              </div>
              <div class="text-xs text-brand-ink-3">加入时间：{{ profile.createdAt ? profile.createdAt.slice(0, 10) : '—' }}</div>
            </div>
          </div>
        </div>
        <div class="md:ml-auto flex flex-wrap gap-2">
          <button v-if="!userStore.isSeller" @click="router.push('/store-settings')"
            class="px-5 h-10 rounded-lg bg-brand-primary text-brand-primary-ink font-medium hover:bg-brand-primary-hover transition flex items-center gap-2">
            <Store class="w-4 h-4" />免费开店
          </button>
          <button v-else @click="router.push('/store-settings')"
            class="px-5 h-10 rounded-lg border border-brand-primary/40 text-brand-primary hover:bg-brand-primary-subtle transition flex items-center gap-2">
            <Store class="w-4 h-4" />进入卖家中心
          </button>
          <button @click="logout" class="px-5 h-10 rounded-lg border border-brand-line text-brand-ink-2 hover:text-brand-error hover:border-brand-error/30 transition flex items-center gap-2">
            <LogOut class="w-4 h-4" />退出登录
          </button>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="relative grid grid-cols-2 md:grid-cols-4 gap-3 mt-8">
        <button v-for="q in quickLinks" :key="q.name" @click="router.push(q.path)"
          class="p-4 rounded-xl bg-brand-surface/60 backdrop-blur border border-brand-line hover:border-brand-primary/40 transition text-left">
          <div class="flex items-center justify-between">
            <div :class="['w-9 h-9 rounded-lg flex items-center justify-center', q.color]">
              <component :is="q.icon" class="w-4.5 h-4.5" />
            </div>
            <ChevronRight class="w-4 h-4 text-brand-ink-3" />
          </div>
          <div class="mt-3 text-xs text-brand-ink-3">{{ q.name }}</div>
          <div class="mt-0.5 text-lg font-bold text-brand-ink">{{ q.count }}</div>
        </button>
      </div>
    </div>

    <!-- 订单摘要 -->
    <div class="rounded-xl bg-brand-surface border border-brand-line p-5">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-semibold text-brand-ink flex items-center gap-2"><ShoppingBag class="w-4 h-4 text-brand-primary" />我的订单</h3>
        <button @click="router.push('/orders')" class="text-xs text-brand-primary hover:underline flex items-center gap-0.5">查看全部<ChevronRight class="w-3 h-3" /></button>
      </div>
      <div class="grid grid-cols-4 gap-3">
        <div v-for="s in orderStats" :key="s.name" @click="goOrdersByStatus(s.status)" class="p-4 rounded-lg bg-brand-bg border border-brand-line-subtle text-center cursor-pointer hover:border-brand-primary/40 transition group">
          <div class="relative w-10 h-10 mx-auto rounded-lg bg-brand-surface-2 flex items-center justify-center text-brand-ink-2 group-hover:text-brand-primary transition">
            <component :is="s.icon" class="w-5 h-5" />
            <span v-if="s.count" class="absolute -top-1 -right-1 min-w-5 h-5 px-1 rounded-full bg-brand-error text-[10px] font-bold text-white flex items-center justify-center border border-brand-bg">{{ s.count }}</span>
          </div>
          <div class="text-xs text-brand-ink-3 mt-2">{{ s.name }}</div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-[220px_1fr] gap-6">
      <!-- 侧边 Tab -->
      <aside class="lg:sticky lg:top-28 self-start rounded-xl bg-brand-surface border border-brand-line overflow-hidden h-fit">
        <div class="p-2 space-y-0.5">
          <button v-for="t in [
            { id: 'profile', icon: User, name: '个人资料' },
            { id: 'credit', icon: ShieldCheck, name: '信用中心' },
            { id: 'address', icon: MapPin, name: '收货地址' },
            { id: 'security', icon: Settings, name: '账号安全' }
          ]" :key="t.id" @click="activeTab = t.id"
            :class="['w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition',
                     activeTab === t.id ? 'bg-brand-primary-subtle text-brand-primary font-medium' : 'text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary']">
            <component :is="t.icon" class="w-4 h-4" />{{ t.name }}
          </button>
        </div>
      </aside>

      <!-- 内容区 -->
      <section class="rounded-xl bg-brand-surface border border-brand-line p-6 md:p-8">
        <!-- Profile Tab -->
        <template v-if="activeTab === 'profile'">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-semibold text-brand-ink">个人资料</h3>
            <button v-if="!isEditing" @click="startEdit" class="px-4 h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition flex items-center gap-2">
              <Edit2 class="w-4 h-4" />编辑
            </button>
            <span v-else class="text-xs text-brand-primary">编辑中</span>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-5 max-w-2xl">
            <div>
              <label class="block text-xs text-brand-ink-3 mb-1.5">昵称</label>
              <input v-model="profile.nickname" :disabled="!isEditing" class="w-full h-11 rounded-lg border border-brand-line px-4 text-sm text-brand-ink focus:outline-none focus:border-brand-primary transition" :class="isEditing ? 'bg-brand-bg' : 'bg-brand-surface-2 text-brand-ink-2 cursor-not-allowed'" />
            </div>
            <div>
              <label class="block text-xs text-brand-ink-3 mb-1.5">手机号（无法修改）</label>
              <input v-model="profile.phone" disabled class="w-full h-11 rounded-lg bg-brand-surface-2 border border-brand-line px-4 text-sm text-brand-ink-3 cursor-not-allowed" />
            </div>
            <div>
              <label class="block text-xs text-brand-ink-3 mb-1.5">邮箱</label>
              <input v-model="profile.email" :disabled="!isEditing" class="w-full h-11 rounded-lg border border-brand-line px-4 text-sm text-brand-ink focus:outline-none focus:border-brand-primary transition" :class="isEditing ? 'bg-brand-bg' : 'bg-brand-surface-2 text-brand-ink-2 cursor-not-allowed'" />
            </div>
            <div>
              <label class="block text-xs text-brand-ink-3 mb-1.5">性别</label>
              <select v-model="profile.gender" :disabled="!isEditing" class="w-full h-11 rounded-lg border border-brand-line px-4 text-sm text-brand-ink focus:outline-none focus:border-brand-primary transition" :class="isEditing ? 'bg-brand-bg' : 'bg-brand-surface-2 text-brand-ink-2 cursor-not-allowed'">
                <option value="male">男</option><option value="female">女</option><option value="secret">保密</option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-brand-ink-3 mb-1.5">生日</label>
              <input v-model="profile.birthday" type="date" :disabled="!isEditing" class="w-full h-11 rounded-lg border border-brand-line px-4 text-sm text-brand-ink focus:outline-none focus:border-brand-primary transition" :class="isEditing ? 'bg-brand-bg' : 'bg-brand-surface-2 text-brand-ink-2 cursor-not-allowed'" />
            </div>
            <div>
              <label class="block text-xs text-brand-ink-3 mb-1.5">常住地区</label>
              <input v-model="profile.region" :disabled="!isEditing" class="w-full h-11 rounded-lg border border-brand-line px-4 text-sm text-brand-ink focus:outline-none focus:border-brand-primary transition" :class="isEditing ? 'bg-brand-bg' : 'bg-brand-surface-2 text-brand-ink-2 cursor-not-allowed'" />
            </div>
          </div>
          <div v-if="isEditing" class="mt-8 pt-6 border-t border-brand-line flex gap-3">
            <button @click="saveProfile" :disabled="saving" class="px-6 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover disabled:opacity-60 transition">{{ saving ? '保存中...' : '保存修改' }}</button>
            <button @click="cancelEdit" class="px-6 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition">取消</button>
          </div>
        </template>

        <!-- Credit Tab -->
        <template v-if="activeTab === 'credit'">
          <h3 class="text-lg font-semibold text-brand-ink mb-6">信用中心</h3>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-5 mb-8">
            <div class="md:col-span-1 p-5 rounded-xl bg-gradient-to-br from-brand-warning/20 to-brand-bg border border-brand-line">
              <div class="text-xs text-brand-ink-3 mb-2">我的信用分</div>
              <div class="text-5xl font-black text-brand-warning mt-1">{{ credit.score }}</div>
              <div class="flex items-center gap-2 mt-3">
                <span class="px-2 py-0.5 rounded bg-brand-warning/20 text-brand-warning text-xs font-semibold">{{ credit.level }}</span>
                <span class="text-xs text-brand-ink-3">全站 {{ credit.rank }}</span>
              </div>
            </div>
            <div class="md:col-span-2 p-5 rounded-xl bg-brand-bg border border-brand-line">
              <div class="text-sm font-medium text-brand-ink mb-4">信用构成</div>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div v-for="it in credit.items" :key="it.label" class="flex items-center justify-between p-3 rounded-lg bg-brand-surface border border-brand-line-subtle">
                  <span class="text-sm text-brand-ink-2">{{ it.label }}</span>
                  <span v-if="it.status" class="inline-flex items-center gap-1 text-xs text-brand-success"><ShieldCheck class="w-3.5 h-3.5" />已完成</span>
                  <span v-else class="inline-flex items-center gap-1 text-xs text-brand-warning"><Clock class="w-3.5 h-3.5" />待补充</span>
                </div>
              </div>
            </div>
          </div>
          <div>
            <div class="text-sm font-medium text-brand-ink mb-3">信用变更记录</div>
            <div class="rounded-lg bg-brand-bg border border-brand-line overflow-hidden">
              <div v-for="(r, i) in credit.recent" :key="i" :class="['flex items-center gap-4 px-4 py-3', i > 0 ? 'border-t border-brand-line-subtle' : '']">
                <div class="text-xs text-brand-ink-3 w-24 shrink-0">{{ r.date }}</div>
                <div class="flex-1 text-sm text-brand-ink">{{ r.reason }}</div>
                <div :class="['text-sm font-semibold', r.delta > 0 ? 'text-brand-success' : 'text-brand-error']">
                  {{ r.delta > 0 ? '+' : '' }}{{ r.delta }} 分
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- Address Tab -->
        <template v-if="activeTab === 'address'">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-semibold text-brand-ink">收货地址</h3>
            <button @click="openAddressForm(null)"
              class="px-4 h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition flex items-center gap-2">
              <Plus class="w-4 h-4" />新增地址
            </button>
          </div>
          <div v-if="showAddressForm" class="mb-6 p-5 rounded-xl bg-brand-bg border border-brand-primary/30 grid grid-cols-1 md:grid-cols-2 gap-4">
            <input v-model="addressForm.receiverName" placeholder="收货人姓名" class="h-11 rounded-lg bg-brand-surface border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
            <input v-model="addressForm.phone" placeholder="手机号" class="h-11 rounded-lg bg-brand-surface border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
            <input v-model="addressForm.province" placeholder="省份" class="h-11 rounded-lg bg-brand-surface border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
            <input v-model="addressForm.city" placeholder="城市" class="h-11 rounded-lg bg-brand-surface border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
            <input v-model="addressForm.district" placeholder="区/县" class="h-11 rounded-lg bg-brand-surface border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
            <input v-model="addressForm.detail" placeholder="详细地址（街道、门牌号等）" class="md:col-span-2 h-11 rounded-lg bg-brand-surface border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
            <label class="flex items-center gap-2 text-sm text-brand-ink-2"><input v-model="addressForm.isDefault" type="checkbox" class="w-3.5 h-3.5 accent-brand-primary" />设为默认地址</label>
            <div class="flex justify-end gap-2">
              <button @click="resetAddressForm" class="px-4 h-9 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary transition">取消</button>
              <button @click="saveAddress" class="px-4 h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition">{{ editingAddressId ? '保存修改' : '保存' }}</button>
            </div>
          </div>
          <div class="space-y-3">
            <div v-for="a in addresses" :key="a.id" class="p-5 rounded-xl bg-brand-bg border border-brand-line hover:border-brand-primary/40 transition">
              <div class="flex items-start justify-between gap-4">
                <div class="flex-1">
                  <div class="flex items-center gap-3">
                    <span class="font-semibold text-brand-ink">{{ a.name }}</span>
                    <span class="text-sm text-brand-ink-2">{{ a.phone }}</span>
                    <span v-if="a.isDefault" class="px-2 py-0.5 rounded bg-brand-primary/15 text-brand-primary text-[11px] font-semibold border border-brand-primary/30">默认</span>
                    <button v-if="!a.isDefault" @click="handleSetDefault(a)" class="text-[11px] text-brand-ink-3 hover:text-brand-primary">设为默认</button>
                  </div>
                  <div class="text-sm text-brand-ink-2 mt-2">{{ a.region }}，{{ a.detail }}</div>
                </div>
                <div class="flex items-center gap-1">
                  <button @click="openAddressForm(a)" class="p-2 rounded-lg hover:bg-brand-surface-2 text-brand-ink-3 hover:text-brand-primary transition"><Edit2 class="w-4 h-4" /></button>
                  <button @click="handleDeleteAddress(a)" class="p-2 rounded-lg hover:bg-brand-error-subtle text-brand-ink-3 hover:text-brand-error transition"><Trash2 class="w-4 h-4" /></button>
                </div>
              </div>
            </div>
            <div v-if="addresses.length === 0" class="p-12 text-center rounded-xl bg-brand-bg border border-brand-line">
              <MapPin class="w-10 h-10 mx-auto text-brand-ink-3 mb-2" />
              <p class="text-sm text-brand-ink-3">暂无收货地址</p>
            </div>
          </div>
        </template>

        <!-- Security Tab -->
        <template v-if="activeTab === 'security'">
          <h3 class="text-lg font-semibold text-brand-ink mb-6">账号安全</h3>
          <div class="space-y-3 max-w-2xl">
            <div class="flex items-center justify-between p-4 rounded-xl bg-brand-bg border border-brand-line hover:border-brand-primary/30 transition">
              <div>
                <div class="text-sm font-medium text-brand-ink">登录密码</div>
                <div class="text-xs text-brand-ink-3 mt-1">建议定期更新密码，保障账户安全</div>
              </div>
              <button @click="showPwdModal = true" class="px-4 h-9 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition">修改密码</button>
            </div>
            <div class="flex items-center justify-between p-4 rounded-xl bg-brand-bg border border-brand-line hover:border-brand-primary/30 transition">
              <div>
                <div class="text-sm font-medium text-brand-ink">手机号</div>
                <div class="text-xs text-brand-ink-3 mt-1">已绑定 {{ profile.phone || '—' }}（重要通知通道）</div>
              </div>
              <button @click="showPhoneModal = true" class="px-4 h-9 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition">换绑</button>
            </div>
            <div class="flex items-center justify-between p-4 rounded-xl bg-brand-bg border border-brand-line hover:border-brand-primary/30 transition">
              <div>
                <div class="text-sm font-medium text-brand-ink">邮箱验证</div>
                <div class="text-xs text-brand-ink-3 mt-1">{{ profile.email ? '已绑定 ' + profile.email : '未绑定邮箱' }}</div>
              </div>
              <button @click="startEdit(); activeTab = 'profile'" class="px-4 h-9 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition">更换</button>
            </div>
            <div class="flex items-center justify-between p-4 rounded-xl bg-brand-bg border border-brand-line hover:border-brand-primary/30 transition">
              <div>
                <div class="text-sm font-medium text-brand-ink">实名认证</div>
                <div class="text-xs text-brand-ink-3 mt-1">{{ realNameStatus === 1 ? '已完成实名，平台交易更加安全' : '未完成实名认证，部分功能受限' }}</div>
              </div>
              <button v-if="realNameStatus !== 1" @click="showRealNameModal = true" class="px-4 h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-xs font-medium hover:bg-brand-primary-hover transition">去认证</button>
              <span v-else class="px-3 h-9 rounded-lg bg-brand-success/15 text-brand-success text-xs font-medium flex items-center">已认证</span>
            </div>
            <div class="flex items-center justify-between p-4 rounded-xl bg-brand-bg border border-brand-line hover:border-brand-primary/30 transition">
              <div>
                <div class="text-sm font-medium text-brand-ink">登录设备</div>
                <div class="text-xs text-brand-ink-3 mt-1">当前设备：{{ currentDevice.deviceName || '检测中...' }}</div>
              </div>
              <button @click="openDeviceModal" class="px-4 h-9 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition">管理</button>
            </div>
          </div>
        </template>

        <!-- 修改密码弹窗 -->
        <div v-if="showPwdModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div class="w-full max-w-md rounded-2xl bg-brand-surface border border-brand-line p-6">
            <h3 class="text-lg font-semibold text-brand-ink mb-4">修改密码</h3>
            <div class="space-y-4">
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">原密码</label>
                <input v-model="pwdForm.oldPassword" type="password" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">新密码</label>
                <input v-model="pwdForm.newPassword" type="password" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">确认新密码</label>
                <input v-model="pwdForm.confirmPassword" type="password" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
            </div>
            <div class="flex justify-end gap-3 mt-6">
              <button @click="showPwdModal = false" class="px-5 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary transition">取消</button>
              <button @click="savePassword" :disabled="pwdSaving" class="px-5 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover disabled:opacity-60 transition">{{ pwdSaving ? '提交中...' : '确认修改' }}</button>
            </div>
          </div>
        </div>

        <!-- 实名认证弹窗 -->
        <div v-if="showRealNameModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div class="w-full max-w-md rounded-2xl bg-brand-surface border border-brand-line p-6">
            <h3 class="text-lg font-semibold text-brand-ink mb-4">实名认证</h3>
            <div class="space-y-4">
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">真实姓名</label>
                <input v-model="realNameForm.realName" placeholder="请输入身份证上的姓名" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">身份证号</label>
                <input v-model="realNameForm.idCard" placeholder="请输入18位身份证号" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
            </div>
            <div class="flex justify-end gap-3 mt-6">
              <button @click="showRealNameModal = false" class="px-5 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary transition">取消</button>
              <button @click="submitRealName" class="px-5 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition">提交认证</button>
            </div>
          </div>
        </div>

        <!-- 登录设备管理弹窗 -->
        <div v-if="showDeviceModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div class="w-full max-w-lg rounded-2xl bg-brand-surface border border-brand-line p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-semibold text-brand-ink">登录设备管理</h3>
              <button @click="handleKickOtherDevices" v-if="deviceList.length > 1" class="text-xs text-brand-error hover:underline">退出所有其他设备</button>
            </div>
            <div class="space-y-3 max-h-96 overflow-y-auto">
              <div v-for="d in deviceList" :key="d.jti" class="p-4 rounded-xl bg-brand-bg border" :class="d.isCurrent ? 'border-brand-primary/40' : 'border-brand-line'">
                <div class="flex items-center justify-between mb-2">
                  <div class="flex items-center gap-2">
                    <Monitor class="w-5 h-5" :class="d.isCurrent ? 'text-brand-primary' : 'text-brand-ink-3'" />
                    <span class="font-medium text-brand-ink">{{ d.deviceName }}</span>
                  </div>
                  <span v-if="d.isCurrent" class="px-2 py-0.5 rounded bg-brand-success/15 text-brand-success text-[11px] font-semibold">当前设备</span>
                </div>
                <div class="grid grid-cols-2 gap-2 text-xs text-brand-ink-3 mb-3">
                  <div>操作系统：<span class="text-brand-ink-2">{{ d.os }}</span></div>
                  <div>浏览器：<span class="text-brand-ink-2">{{ d.browser }}</span></div>
                  <div>IP 地址：<span class="text-brand-ink-2">{{ d.ip }}</span></div>
                  <div>登录时间：<span class="text-brand-ink-2">{{ d.loginAt }}</span></div>
                </div>
                <div v-if="!d.isCurrent" class="flex justify-end">
                  <button @click="handleKickDevice(d)" class="px-3 h-8 rounded-lg bg-brand-error/10 text-brand-error text-xs font-medium hover:bg-brand-error/20 transition">踢出设备</button>
                </div>
              </div>
              <div v-if="deviceList.length === 0" class="p-8 text-center text-sm text-brand-ink-3">暂无登录设备</div>
            </div>
            <div class="flex justify-end gap-3 mt-6">
              <button @click="showDeviceModal = false" class="px-5 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary transition">关闭</button>
              <button @click="logout()" class="px-5 h-10 rounded-lg bg-brand-error text-white text-sm font-medium hover:bg-brand-error/90 transition">退出当前设备</button>
            </div>
          </div>
        </div>

        <!-- 换绑手机号弹窗 -->
        <div v-if="showPhoneModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div class="w-full max-w-md rounded-2xl bg-brand-surface border border-brand-line p-6">
            <h3 class="text-lg font-semibold text-brand-ink mb-4">换绑手机号</h3>
            <div class="mb-4 p-3 rounded-lg bg-brand-bg text-xs text-brand-ink-3">
              当前绑定手机号：<span class="text-brand-ink-2 font-medium">{{ profile.phone || '—' }}</span>
            </div>
            <div class="space-y-4">
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">新手机号</label>
                <input v-model="phoneForm.newPhone" placeholder="请输入新手机号" maxlength="11" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">短信验证码</label>
                <div class="flex gap-2">
                  <input v-model="phoneForm.code" placeholder="请输入验证码" class="flex-1 h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
                  <button @click="sendPhoneCode" :disabled="smsSending || smsCountdown > 0" class="px-4 h-11 rounded-lg bg-brand-primary text-brand-primary-ink text-xs font-medium hover:bg-brand-primary-hover disabled:opacity-50 transition whitespace-nowrap">
                    {{ smsCountdown > 0 ? `${smsCountdown}s` : (smsSending ? '发送中...' : '获取验证码') }}
                  </button>
                </div>
              </div>
            </div>
            <div class="flex justify-end gap-3 mt-6">
              <button @click="showPhoneModal = false" class="px-5 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary transition">取消</button>
              <button @click="submitPhoneChange" class="px-5 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition">确认换绑</button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
