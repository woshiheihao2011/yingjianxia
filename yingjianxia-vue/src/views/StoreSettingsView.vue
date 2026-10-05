<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  getMyShopApi, updateShopApi, updateShopSettingsApi, applySellerVerificationApi,
  getShopSettingsApi, getLogisticsTemplatesApi, createLogisticsTemplateApi,
  updateLogisticsTemplateApi, deleteLogisticsTemplateApi,
  getServicePromisesApi, toggleServicePromiseApi, getShopStaffApi,
  createShopStaffApi, deleteShopStaffApi,
  getShopCertificationsApi, getShopFinanceApi, updateShopFinanceApi,
  createShopApi, uploadShopImageApi
} from '@/api/user'
import {
  Store, Palette, Truck, Shield, Users,
  Upload, Save, MapPin, FileText, Tag, ChevronRight, Eye, AlertTriangle,
  BadgeCheck, Plus, Loader2, Package
} from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('basic')
const saving = ref(false)
const savedToast = ref(false)
const saveError = ref(null)
const loading = ref(true)
const shopId = ref(null)

// 店铺基础信息
const basic = ref({
  name: '', category: '', logo: '', banner: '',
  intro: '', mainCategory: '', phone: '', wechat: '',
  announcement: '', businessHours: '', defaultExpress: '',
  acceptBargain: false, supportFaceTrade: false, shipTimePromise: 48
})

// 各模块数据
const logisticsTemplates = ref([])
const logisticsModal = ref({ visible: false, editing: null, name: '', type: 'free', baseFee: 0, stepFee: 0, baseUnit: 1, stepUnit: 1, freeShippingThreshold: null, region: '', isDefault: false })
const servicePromises = ref([])
const staffs = ref([])
const staffModal = ref({ visible: false, name: '', phone: '', role: 'operator', permissions: [] })
const certifications = ref([])
const finance = ref({
  bankAccountName: '', bankAccountNo: '', bankName: '',
  withdrawThreshold: 0, withdrawFeeRate: 0, settlementCycle: 'weekly'
})

// 服务承诺描述映射
const promiseDescs = {
  '7day_return': '签收后7天支持退换',
  'fake_one_pay_three': '假货申请即获三倍赔偿',
  'fast_refund': '退款极速到账',
  'shipping_insurance': '支持运费险',
  'quality_guarantee': '品质保证服务'
}
const promiseColors = {
  '7day_return': 'text-brand-warning bg-brand-warning/15',
  'fake_one_pay_three': 'text-brand-error bg-brand-error/15',
  'fast_refund': 'text-brand-info bg-brand-info/15',
  'shipping_insurance': 'text-brand-primary bg-brand-primary/15',
  'quality_guarantee': 'text-brand-success bg-brand-success/15'
}
const promiseIcons = {
  '7day_return': FileText,
  'fake_one_pay_three': BadgeCheck,
  'fast_refund': Shield,
  'shipping_insurance': Truck,
  'quality_guarantee': BadgeCheck
}

// 认证状态映射
const certStatusMap = {
  'not_applied': { text: '未申请', color: 'bg-brand-ink-2/15 text-brand-ink-3 border-brand-ink-2/30' },
  'reviewing': { text: '审核中', color: 'bg-brand-warning/15 text-brand-warning border-brand-warning/30' },
  'approved': { text: '已通过', color: 'bg-brand-success/15 text-brand-success border-brand-success/30' },
  'rejected': { text: '已拒绝', color: 'bg-brand-error/15 text-brand-error border-brand-error/30' }
}

async function loadShop() {
  loading.value = true
  try {
    // 先尝试获取店铺信息（可能不存在）
    let shopData = null
    try {
      shopData = await getMyShopApi()
    } catch (e) {
      // 店铺不存在时不报错，shopId 保持 null，进入创建模式
      console.log('未检测到店铺，进入创建模式')
    }

    if (shopData) {
      shopId.value = shopData.id
      basic.value = {
        name: shopData.shopName || '', category: shopData.category || '',
        logo: shopData.logoUrl || '', banner: shopData.coverUrl || '',
        intro: shopData.description || '', mainCategory: shopData.mainCategory || '',
        phone: shopData.contactPhone || '', wechat: shopData.contactWechat || '',
        announcement: shopData.announcement || '', businessHours: shopData.businessHours || '',
        defaultExpress: shopData.defaultExpress || '',
        acceptBargain: shopData.acceptBargain ?? false,
        supportFaceTrade: shopData.supportFaceTrade ?? false,
        shipTimePromise: shopData.shipTimePromise ?? 48
      }

      // 已有店铺时加载其他模块数据
      const [settingsResp, logisticsResp, promisesResp, staffResp, certResp, financeResp] = await Promise.allSettled([
        getShopSettingsApi(),
        getLogisticsTemplatesApi(),
        getServicePromisesApi(),
        getShopStaffApi(),
        getShopCertificationsApi(),
        getShopFinanceApi()
      ])

      if (settingsResp.status === 'fulfilled' && settingsResp.value) {
        const s = settingsResp.value
        basic.value.phone = s.contactPhone || basic.value.phone
        basic.value.wechat = s.contactWechat || basic.value.wechat
        basic.value.businessHours = s.businessHours || basic.value.businessHours
        basic.value.defaultExpress = s.defaultExpress || basic.value.defaultExpress
        basic.value.acceptBargain = s.acceptBargain ?? basic.value.acceptBargain
        basic.value.supportFaceTrade = s.supportFaceTrade ?? basic.value.supportFaceTrade
        basic.value.shipTimePromise = s.shipTimePromise ?? basic.value.shipTimePromise
        basic.value.announcement = s.announcement || basic.value.announcement
      }
      if (logisticsResp.status === 'fulfilled') {
        logisticsTemplates.value = Array.isArray(logisticsResp.value) ? logisticsResp.value : []
      }
      if (promisesResp.status === 'fulfilled') {
        servicePromises.value = Array.isArray(promisesResp.value) ? promisesResp.value : []
      }
      if (staffResp.status === 'fulfilled') {
        staffs.value = Array.isArray(staffResp.value) ? staffResp.value : []
      }
      if (certResp.status === 'fulfilled') {
        certifications.value = Array.isArray(certResp.value) ? certResp.value : []
      }
      if (financeResp.status === 'fulfilled' && financeResp.value) {
        const f = financeResp.value
        finance.value = {
          bankAccountName: f.bankAccountName || '',
          bankAccountNo: f.bankAccountNo || '',
          bankName: f.bankName || '',
          withdrawThreshold: f.withdrawThreshold ?? 0,
          withdrawFeeRate: f.withdrawFeeRate ?? 0,
          settlementCycle: f.settlementCycle || 'weekly'
        }
      }
    }
  } catch (e) {
    console.error('加载店铺信息失败', e)
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    let currentShopId = shopId.value

    // 首次保存：先创建店铺
    if (!currentShopId) {
      if (!basic.value.name) {
        alert('请填写店铺名称')
        saving.value = false
        return
      }
      const created = await createShopApi({
        shopName: basic.value.name,
        description: basic.value.intro,
        contactPhone: basic.value.phone
      })
      currentShopId = created?.id || created?.shopId
      if (!currentShopId) {
        alert('店铺创建失败，请重试')
        saving.value = false
        return
      }
      shopId.value = currentShopId
    }

    // 更新店铺基础信息
    await updateShopApi(currentShopId, {
      shopName: basic.value.name,
      description: basic.value.intro,
      contactPhone: basic.value.phone,
      contactWechat: basic.value.wechat,
      announcement: basic.value.announcement,
      logoUrl: basic.value.logo,
      coverUrl: basic.value.banner
    })
    // 更新店铺设置
    await updateShopSettingsApi(currentShopId, {
      contactPhone: basic.value.phone,
      contactWechat: basic.value.wechat,
      businessHours: basic.value.businessHours,
      defaultExpress: basic.value.defaultExpress,
      acceptBargain: basic.value.acceptBargain,
      supportFaceTrade: basic.value.supportFaceTrade,
      shipTimePromise: basic.value.shipTimePromise,
      announcement: basic.value.announcement,
      logoUrl: basic.value.logo,
      coverUrl: basic.value.banner
    })
    savedToast.value = true
    setTimeout(() => { savedToast.value = false }, 2000)
    // 创建店铺后刷新用户信息，更新卖家角色
    await userStore.fetchUserInfo?.()
    loadShop()
  } catch (e) {
    saveError.value = e.message || '保存失败'
    setTimeout(() => { saveError.value = null }, 3000)
  } finally {
    saving.value = false
  }
}

const promiseSaving = ref({})
async function togglePromise(p) {
  promiseSaving.value[p.id] = true
  const oldVal = p.enabled
  p.enabled = !oldVal
  try {
    await toggleServicePromiseApi(p.id, !oldVal)
  } catch (e) {
    p.enabled = oldVal
    p._error = e.message || '更新失败'
    setTimeout(() => { p._error = null }, 3000)
  } finally {
    promiseSaving.value[p.id] = false
  }
}

// 运费模板 CRUD
function openLogisticsModal(t = null) {
  if (t) {
    logisticsModal.value = { visible: true, editing: t.id, name: t.name, type: t.type || 'standard', baseFee: t.baseFee ?? 0, stepFee: t.stepFee ?? 0, baseUnit: t.baseUnit ?? 1, stepUnit: t.stepUnit ?? 1, freeShippingThreshold: t.freeShippingThreshold ?? null, region: t.region || '', isDefault: t.isDefault ?? false }
  } else {
    logisticsModal.value = { visible: true, editing: null, name: '', type: 'free', baseFee: 0, stepFee: 0, baseUnit: 1, stepUnit: 1, freeShippingThreshold: null, region: '全国', isDefault: false }
  }
}

async function saveLogisticsTemplate() {
  const m = logisticsModal.value
  if (!m.name?.trim()) { alert('请填写模板名称'); return }
  const payload = { name: m.name, type: m.type, baseFee: Number(m.baseFee) || 0, stepFee: Number(m.stepFee) || 0, baseUnit: Number(m.baseUnit) || 1, stepUnit: Number(m.stepUnit) || 1, freeShippingThreshold: m.freeShippingThreshold ? Number(m.freeShippingThreshold) : null, region: m.region || '全国', isDefault: m.isDefault }
  try {
    if (m.editing) {
      await updateLogisticsTemplateApi(m.editing, payload)
    } else {
      await createLogisticsTemplateApi(payload)
    }
    logisticsModal.value.visible = false
    logisticsTemplates.value = await getLogisticsTemplatesApi()
  } catch (e) {
    alert('保存失败：' + (e.message || '未知错误'))
  }
}

async function removeLogisticsTemplate(t) {
  if (!confirm(`确认删除运费模板「${t.name}」？`)) return
  try {
    await deleteLogisticsTemplateApi(t.id)
    logisticsTemplates.value = logisticsTemplates.value.filter(x => x.id !== t.id)
  } catch (e) {
    alert('删除失败：' + (e.message || '未知错误'))
  }
}

// 员工管理 CRUD
function openStaffModal() {
  staffModal.value = { visible: true, name: '', phone: '', role: 'operator', permissions: [] }
}

async function saveStaff() {
  const m = staffModal.value
  if (!m.name?.trim()) { alert('请填写员工姓名'); return }
  if (!m.phone?.trim()) { alert('请填写手机号'); return }
  try {
    await createShopStaffApi({ name: m.name, phone: m.phone, role: m.role, permissions: m.permissions })
    staffModal.value.visible = false
    staffs.value = await getShopStaffApi()
    savedToast.value = true
    setTimeout(() => { savedToast.value = false }, 2000)
  } catch (e) {
    alert('添加失败：' + (e.message || '未知错误'))
  }
}

async function removeStaff(s) {
  if (!confirm(`确认移除员工「${s.name}」？`)) return
  try {
    await deleteShopStaffApi(s.id)
    staffs.value = staffs.value.filter(x => x.id !== s.id)
  } catch (e) {
    alert('移除失败：' + (e.message || '未知错误'))
  }
}

// 店铺图片处理：选择本地文件后上传到服务器
const uploading = ref(false)

async function onLogoFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) { alert('图片大小不能超过 2MB'); return }
  uploading.value = true
  try {
    const res = await uploadShopImageApi(file, 'logo')
    basic.value.logo = res.url
  } catch (err) {
    alert('上传失败：' + (err.message || '未知错误'))
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

async function onBannerFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { alert('图片大小不能超过 5MB'); return }
  uploading.value = true
  try {
    const res = await uploadShopImageApi(file, 'banner')
    basic.value.banner = res.url
  } catch (err) {
    alert('上传失败：' + (err.message || '未知错误'))
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

async function saveFinance() {
  saving.value = true
  try {
    await updateShopFinanceApi({
      bankAccountName: finance.value.bankAccountName,
      bankAccountNo: finance.value.bankAccountNo,
      bankName: finance.value.bankName,
      withdrawThreshold: Number(finance.value.withdrawThreshold) || 0,
      withdrawFeeRate: finance.value.withdrawFeeRate,
      settlementCycle: finance.value.settlementCycle
    })
    savedToast.value = true
    setTimeout(() => { savedToast.value = false }, 2000)
    loadShop()
  } catch (e) {
    saveError.value = e.message || '保存失败'
    setTimeout(() => { saveError.value = null }, 3000)
  } finally {
    saving.value = false
  }
}

// 认证申请弹窗
const certModal = ref({ visible: false, type: '', name: '', form: { businessLicense: '', legalPerson: '', contactPhone: '', depositAmount: '', categoryCode: '' } })

function openCertModal(c) {
  certModal.value = { visible: true, type: c.type, name: c.name, form: { businessLicense: '', legalPerson: '', contactPhone: '', depositAmount: '', categoryCode: '' } }
}

async function submitCert() {
  const m = certModal.value
  try {
    if (m.type === 'seller_verified') {
      await applySellerVerificationApi(m.form)
    } else {
      savedToast.value = true
      setTimeout(() => { savedToast.value = false }, 2000)
    }
    certModal.value.visible = false
    certifications.value = await getShopCertificationsApi()
  } catch (e) {
    saveError.value = e.message || '认证申请失败'
    setTimeout(() => { saveError.value = null }, 3000)
  }
}

const tabs = [
  { id: 'basic', icon: Store, name: '基础信息' },
  { id: 'appearance', icon: Palette, name: '店铺外观' },
  { id: 'logistics', icon: Truck, name: '物流运费' },
  { id: 'service', icon: Shield, name: '服务承诺' },
  { id: 'staff', icon: Users, name: '员工管理' },
  { id: 'cert', icon: BadgeCheck, name: '认证与资质' },
  { id: 'finance', icon: FileText, name: '财务设置' }
]

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/auth', query: { redirect: '/store-settings' } })
  } else {
    loadShop()
  }
})
</script>

<template>
  <div class="container-app space-y-6">
    <div class="text-xs text-brand-ink-3 flex items-center gap-1.5">
      <button @click="router.push('/')" class="hover:text-brand-primary">首页</button>
      <ChevronRight class="w-3 h-3" />
      <span class="text-brand-ink-2">卖家中心</span>
      <ChevronRight class="w-3 h-3" />
      <span class="text-brand-ink">店铺设置</span>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-[240px_1fr] gap-6">
      <aside class="lg:sticky lg:top-28 self-start rounded-xl bg-brand-surface border border-brand-line overflow-hidden h-fit">
        <div class="p-5 border-b border-brand-line bg-gradient-to-br from-brand-primary/15 to-transparent">
          <div class="flex items-center gap-3">
            <div class="w-11 h-11 rounded-xl bg-brand-primary/20 border border-brand-primary/30 flex items-center justify-center">
              <Store class="w-5.5 h-5.5 text-brand-primary" />
            </div>
            <div>
              <div class="font-semibold text-brand-ink text-sm">卖家工作台</div>
              <div class="text-[11px] text-brand-ink-3 mt-0.5">店铺设置 v1.0</div>
            </div>
          </div>
        </div>
        <nav class="p-2 space-y-0.5">
          <!-- 核心操作：发布商品 -->
          <button @click="router.push('/sell')" class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition bg-brand-primary/10 text-brand-primary font-semibold hover:bg-brand-primary/20 mb-1">
            <Plus class="w-4 h-4" />发布商品
          </button>
          <button @click="router.push('/listings')" class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary mb-2">
            <Package class="w-4 h-4" />我的发布
          </button>
          <div class="border-t border-brand-line my-1"></div>
          <button v-for="t in tabs" :key="t.id" @click="activeTab = t.id"
            :class="['w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition',
                     activeTab === t.id ? 'bg-brand-primary-subtle text-brand-primary font-medium' : 'text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary']">
            <component :is="t.icon" class="w-4 h-4" />{{ t.name }}
          </button>
          <div class="my-2 border-t border-brand-line"></div>
          <button @click="router.push('/analytics')" class="w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-sm text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition">
            <span class="flex items-center gap-3"><Eye class="w-4 h-4" />数据中心</span><ChevronRight class="w-3.5 h-3.5" />
          </button>
          <button @click="router.push('/promotions')" class="w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-sm text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition">
            <span class="flex items-center gap-3"><Tag class="w-4 h-4" />营销活动</span><ChevronRight class="w-3.5 h-3.5" />
          </button>
        </nav>
      </aside>

      <section class="rounded-xl bg-brand-surface border border-brand-line p-6 md:p-8">
        <div v-if="loading" class="flex items-center justify-center py-20">
          <Loader2 class="w-6 h-6 text-brand-primary animate-spin" />
          <span class="ml-2 text-sm text-brand-ink-3">加载中...</span>
        </div>

        <template v-else>
          <div class="flex items-center justify-between mb-8 pb-6 border-b border-brand-line">
            <div>
              <h2 class="text-xl font-bold text-brand-ink flex items-center gap-2">
                <component :is="tabs.find(t => t.id === activeTab)?.icon" class="w-5 h-5 text-brand-primary" />
                {{ tabs.find(t => t.id === activeTab)?.name }}
              </h2>
              <p class="text-xs text-brand-ink-3 mt-1">配置店铺对外展示、服务承诺及各项运营参数</p>
            </div>
            <div class="flex gap-2" v-if="activeTab !== 'finance'">
              <button class="px-4 h-9 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-1.5">
                <Eye class="w-3.5 h-3.5" />预览
              </button>
              <button @click="save" :disabled="saving"
                class="px-5 h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-xs font-medium hover:bg-brand-primary-hover disabled:opacity-60 transition flex items-center gap-1.5">
                <Save class="w-3.5 h-3.5" />{{ saving ? '保存中...' : '保存设置' }}
              </button>
            </div>
            <div class="flex gap-2" v-else>
              <button @click="saveFinance" :disabled="saving"
                class="px-5 h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-xs font-medium hover:bg-brand-primary-hover disabled:opacity-60 transition flex items-center gap-1.5">
                <Save class="w-3.5 h-3.5" />{{ saving ? '保存中...' : '保存设置' }}
              </button>
            </div>
          </div>

          <!-- 基础信息 -->
          <template v-if="activeTab === 'basic'">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-5 max-w-4xl">
              <div class="md:col-span-2 p-5 rounded-xl bg-brand-bg border border-brand-primary/30">
                <div class="text-xs text-brand-primary font-semibold mb-2 flex items-center gap-1.5"><AlertTriangle class="w-3.5 h-3.5" />重要提示</div>
                <div class="text-xs text-brand-ink-2 leading-relaxed">店铺名称修改后需要 1-3 个工作日平台审核，审核通过后生效。主营类目每个自然年仅允许修改 1 次，请谨慎选择。</div>
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">店铺名称 *</label>
                <input v-model="basic.name" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">经营类目 *</label>
                <select v-model="basic.category" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary">
                  <option value="diy-hardware">DIY 电脑配件</option>
                  <option value="laptop">二手笔记本</option>
                  <option value="monitor">显示器外设</option>
                  <option value="whole-set">整机组装</option>
                </select>
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">客服电话 *</label>
                <input v-model="basic.phone" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">微信号</label>
                <input v-model="basic.wechat" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">主营商品标签</label>
                <input v-model="basic.mainCategory" placeholder="如：显卡 · CPU · 主板" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">营业时间</label>
                <input v-model="basic.businessHours" placeholder="如：09:00-21:00" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div class="md:col-span-2">
                <label class="block text-xs text-brand-ink-3 mb-1.5">店铺简介 *</label>
                <textarea v-model="basic.intro" rows="4" class="w-full rounded-lg bg-brand-bg border border-brand-line p-4 text-sm focus:outline-none focus:border-brand-primary resize-none"></textarea>
              </div>
              <div class="md:col-span-2">
                <label class="block text-xs text-brand-ink-3 mb-1.5">店铺公告</label>
                <textarea v-model="basic.announcement" rows="2" class="w-full rounded-lg bg-brand-bg border border-brand-line p-4 text-sm focus:outline-none focus:border-brand-primary resize-none"></textarea>
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">发货承诺（小时）</label>
                <input v-model="basic.shipTimePromise" type="number" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">默认快递</label>
                <input v-model="basic.defaultExpress" placeholder="如：顺丰速运" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div class="flex items-center gap-6 md:col-span-2">
                <label class="flex items-center gap-2 text-sm text-brand-ink-2 cursor-pointer">
                  <input v-model="basic.acceptBargain" type="checkbox" class="w-4 h-4 accent-brand-primary" />支持议价
                </label>
                <label class="flex items-center gap-2 text-sm text-brand-ink-2 cursor-pointer">
                  <input v-model="basic.supportFaceTrade" type="checkbox" class="w-4 h-4 accent-brand-primary" />支持面交
                </label>
              </div>
            </div>
          </template>

          <!-- 外观 -->
          <template v-if="activeTab === 'appearance'">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl">
              <div class="p-5 rounded-xl bg-brand-bg border border-brand-line">
                <label class="block text-xs text-brand-ink-3 mb-3">店铺 Logo <span class="text-brand-error">*</span> <span class="ml-2">建议 200x200 正方形</span></label>
                <div class="flex items-center gap-4">
                  <div class="w-24 h-24 rounded-xl bg-brand-surface-2 border border-brand-line overflow-hidden flex items-center justify-center group relative">
                    <img v-if="basic.logo" :src="basic.logo" class="w-full h-full object-cover" @error="basic.logo = ''" />
                    <Store v-else class="w-8 h-8 text-brand-ink-3" />
                    <label class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition flex flex-col items-center justify-center text-white text-xs cursor-pointer">
                      <Upload class="w-4 h-4 mb-0.5" />更换
                      <input type="file" accept="image/*" class="hidden" @change="onLogoFileChange" />
                    </label>
                  </div>
                  <div class="flex-1 space-y-2 text-xs text-brand-ink-3">
                    <div>· 格式：JPG/PNG/WebP</div>
                    <div>· 大小 ≤ 2MB</div>
                    <div v-if="uploading" class="text-brand-primary flex items-center gap-1"><Loader2 class="w-3 h-3 animate-spin" />上传中...</div>
                    <input v-model="basic.logo" placeholder="Logo URL（上传后自动填充）" class="w-full h-9 rounded border border-brand-line px-3 text-xs bg-brand-surface" />
                  </div>
                </div>
              </div>
              <div class="p-5 rounded-xl bg-brand-bg border border-brand-line">
                <label class="block text-xs text-brand-ink-3 mb-3">店铺 Banner * <span class="ml-2">建议 1200x300</span></label>
                <div class="rounded-lg overflow-hidden border border-brand-line aspect-[4/1] group relative">
                  <img v-if="basic.banner" :src="basic.banner" class="w-full h-full object-cover" @error="basic.banner = ''" />
                  <div v-else class="w-full h-full bg-brand-surface-2 flex items-center justify-center"><Store class="w-8 h-8 text-brand-ink-3" /></div>
                  <label class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition flex items-center justify-center text-white text-sm gap-1.5 cursor-pointer">
                    <Upload class="w-4 h-4" />重新上传
                    <input type="file" accept="image/*" class="hidden" @change="onBannerFileChange" />
                  </label>
                </div>
                <input v-model="basic.banner" placeholder="Banner URL（上传后自动填充）" class="mt-3 w-full h-9 rounded border border-brand-line px-3 text-xs bg-brand-surface" />
                <div v-if="uploading" class="mt-2 text-xs text-brand-primary flex items-center gap-1"><Loader2 class="w-3 h-3 animate-spin" />上传中...</div>
                <div class="mt-2 text-xs text-brand-ink-3">Banner 影响店铺首页首屏，建议上传高清、产品主题清晰的主图</div>
              </div>
            </div>
          </template>

          <!-- 物流运费 -->
          <template v-if="activeTab === 'logistics'">
            <div class="flex items-center justify-between mb-4">
              <div class="text-sm font-medium text-brand-ink">运费模板管理</div>
              <button @click="openLogisticsModal()" class="px-4 h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-xs font-medium hover:bg-brand-primary-hover transition flex items-center gap-1.5"><Plus class="w-3.5 h-3.5" />新增模板</button>
            </div>
            <div class="space-y-3">
              <div v-for="t in logisticsTemplates" :key="t.id" class="p-5 rounded-xl bg-brand-bg border border-brand-line hover:border-brand-primary/40 transition">
                <div class="flex items-start justify-between gap-4">
                  <div class="flex-1">
                    <div class="flex items-center gap-2 mb-2">
                      <span class="font-semibold text-brand-ink">{{ t.name }}</span>
                      <span v-if="t.isDefault" class="px-2 py-0.5 rounded bg-brand-primary/15 text-brand-primary text-[11px] font-semibold border border-brand-primary/30">默认</span>
                      <span class="px-2 py-0.5 rounded bg-brand-surface-2 text-[11px] text-brand-ink-3">{{ t.type === 'free' ? '包邮' : t.type === 'standard' ? '标准运费' : '自定义' }}</span>
                    </div>
                    <div class="flex items-center gap-3 text-xs text-brand-ink-3">
                      <span class="flex items-center gap-1"><MapPin class="w-3.5 h-3.5" />{{ t.region || '全国' }}</span>
                      <span v-if="t.type !== 'free'">基础运费 ¥{{ t.baseFee }} + 续件 ¥{{ t.stepFee }}</span>
                      <span v-if="t.freeShippingThreshold">满 ¥{{ t.freeShippingThreshold }} 包邮</span>
                    </div>
                  </div>
                  <div class="flex items-center gap-2">
                    <button @click="openLogisticsModal(t)" class="px-3 h-8 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:bg-brand-surface-2 transition">编辑</button>
                    <button @click="removeLogisticsTemplate(t)" class="px-3 h-8 rounded-lg border border-brand-error/30 text-xs text-brand-error hover:bg-brand-error/10 transition">删除</button>
                  </div>
                </div>
              </div>
              <div v-if="logisticsTemplates.length === 0" class="p-12 text-center rounded-xl bg-brand-bg border border-brand-line">
                <Truck class="w-10 h-10 mx-auto text-brand-ink-3 mb-2" />
                <p class="text-sm text-brand-ink-3">暂无运费模板，点击「新增模板」创建</p>
              </div>
            </div>

            <!-- 运费模板弹窗 -->
            <div v-if="logisticsModal.visible" class="fixed inset-0 z-50 bg-black/40 flex items-center justify-center" @click.self="logisticsModal.visible = false">
              <div class="w-[480px] max-w-[90vw] bg-brand-surface rounded-2xl shadow-xl border border-brand-line p-6">
                <h3 class="text-lg font-semibold text-brand-ink mb-4">{{ logisticsModal.editing ? '编辑运费模板' : '新增运费模板' }}</h3>
                <div class="space-y-4">
                  <div>
                    <label class="block text-xs text-brand-ink-3 mb-1.5">模板名称 *</label>
                    <input v-model="logisticsModal.name" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：全国包邮" />
                  </div>
                  <div class="grid grid-cols-2 gap-3">
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">计费方式</label>
                      <select v-model="logisticsModal.type" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg">
                        <option value="free">包邮</option>
                        <option value="standard">标准运费</option>
                        <option value="custom">自定义</option>
                      </select>
                    </div>
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">覆盖地区</label>
                      <input v-model="logisticsModal.region" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：全国" />
                    </div>
                  </div>
                  <div v-if="logisticsModal.type !== 'free'" class="grid grid-cols-2 gap-3">
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">基础运费（元）</label>
                      <input v-model="logisticsModal.baseFee" type="number" min="0" step="0.01" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" />
                    </div>
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">续件运费（元）</label>
                      <input v-model="logisticsModal.stepFee" type="number" min="0" step="0.01" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" />
                    </div>
                  </div>
                  <div>
                    <label class="block text-xs text-brand-ink-3 mb-1.5">满额包邮（元，0 表示不启用）</label>
                    <input v-model="logisticsModal.freeShippingThreshold" type="number" min="0" step="0.01" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如 99" />
                  </div>
                  <label class="flex items-center gap-2 text-sm text-brand-ink-2 cursor-pointer">
                    <input type="checkbox" v-model="logisticsModal.isDefault" class="w-4 h-4 accent-brand-primary" />设为默认模板
                  </label>
                </div>
                <div class="flex justify-end gap-3 mt-6">
                  <button @click="logisticsModal.visible = false" class="px-5 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:bg-brand-surface-2 transition">取消</button>
                  <button @click="saveLogisticsTemplate" class="px-5 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition">保存</button>
                </div>
              </div>
            </div>
          </template>

          <!-- 服务承诺 -->
          <template v-if="activeTab === 'service'">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 max-w-4xl">
              <div v-for="s in servicePromises" :key="s.id" class="p-5 rounded-xl bg-brand-bg border border-brand-line">
                <div class="flex items-start justify-between gap-4">
                  <div class="flex items-start gap-3 flex-1">
                    <div :class="['w-10 h-10 rounded-lg flex items-center justify-center shrink-0', promiseColors[s.code] || 'bg-brand-surface-2 text-brand-ink-3']">
                      <component :is="promiseIcons[s.code] || Shield" class="w-5 h-5" />
                    </div>
                    <div>
                      <div class="font-semibold text-brand-ink">{{ s.name }}</div>
                      <div class="text-xs text-brand-ink-3 mt-1">{{ promiseDescs[s.code] || '' }}</div>
                      <div v-if="s._error" class="text-xs text-brand-error mt-1">{{ s._error }}</div>
                    </div>
                  </div>
                  <button type="button" :disabled="promiseSaving[s.id]" @click="togglePromise(s)"
                    class="inline-flex items-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-wait">
                    <span class="relative inline-block w-10 h-6 align-middle">
                      <span :class="['absolute inset-0 rounded-full transition', s.enabled ? 'bg-brand-primary' : 'bg-brand-line']"></span>
                      <span :class="['absolute top-0.5 w-5 h-5 rounded-full bg-white transition', s.enabled ? 'translate-x-4 left-0.5' : 'left-0.5']"></span>
                    </span>
                    <span :class="['text-xs', s.enabled ? 'text-brand-success' : 'text-brand-ink-3']">{{ promiseSaving[s.id] ? '保存中' : (s.enabled ? '已开启' : '未开启') }}</span>
                  </button>
                </div>
              </div>
            </div>
          </template>

          <!-- 员工 -->
          <template v-if="activeTab === 'staff'">
            <div class="flex items-center justify-between mb-4">
              <div class="text-sm font-medium text-brand-ink">员工与子账号</div>
              <button @click="openStaffModal()" class="px-4 h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-xs font-medium hover:bg-brand-primary-hover transition flex items-center gap-1.5"><Plus class="w-3.5 h-3.5" />邀请员工</button>
            </div>
            <div class="rounded-lg border border-brand-line overflow-hidden">
              <table class="w-full text-sm">
                <thead class="bg-brand-bg">
                  <tr>
                    <th class="text-left px-5 py-3 text-xs font-semibold text-brand-ink-2">员工</th>
                    <th class="text-left px-5 py-3 text-xs font-semibold text-brand-ink-2">角色</th>
                    <th class="text-left px-5 py-3 text-xs font-semibold text-brand-ink-2">联系方式</th>
                    <th class="text-left px-5 py-3 text-xs font-semibold text-brand-ink-2">权限范围</th>
                    <th class="text-left px-5 py-3 text-xs font-semibold text-brand-ink-2">加入时间</th>
                    <th class="text-right px-5 py-3 text-xs font-semibold text-brand-ink-2">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="s in staffs" :key="s.id" class="border-t border-brand-line-subtle hover:bg-brand-surface-2 transition">
                    <td class="px-5 py-4">
                      <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-full bg-brand-primary/20 border border-brand-primary/30 flex items-center justify-center text-brand-primary text-xs font-bold">{{ (s.name || '?').charAt(0) }}</div>
                        <div class="text-brand-ink font-medium">{{ s.name }}</div>
                      </div>
                    </td>
                    <td class="px-5 py-4 text-brand-ink-2">{{ s.role || '—' }}</td>
                    <td class="px-5 py-4 text-brand-ink-3">{{ s.phone || '—' }}</td>
                    <td class="px-5 py-4 text-brand-ink-3">{{ Array.isArray(s.permissions) ? s.permissions.join('、') : (s.perms || '—') }}</td>
                    <td class="px-5 py-4 text-brand-ink-3">{{ (s.createdAt || s.joinAt || '').slice(0, 10) }}</td>
                    <td class="px-5 py-4 text-right">
                      <button @click="removeStaff(s)" class="text-xs text-brand-error hover:underline">移除</button>
                    </td>
                  </tr>
                  <tr v-if="staffs.length === 0">
                    <td colspan="6" class="px-5 py-12 text-center text-sm text-brand-ink-3">暂无员工，点击「邀请员工」添加</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- 邀请员工弹窗 -->
            <div v-if="staffModal.visible" class="fixed inset-0 z-50 bg-black/40 flex items-center justify-center" @click.self="staffModal.visible = false">
              <div class="w-[480px] max-w-[90vw] bg-brand-surface rounded-2xl shadow-xl border border-brand-line p-6">
                <h3 class="text-lg font-semibold text-brand-ink mb-4">邀请员工</h3>
                <div class="space-y-4">
                  <div>
                    <label class="block text-xs text-brand-ink-3 mb-1.5">员工姓名 *</label>
                    <input v-model="staffModal.name" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：张三" />
                  </div>
                  <div>
                    <label class="block text-xs text-brand-ink-3 mb-1.5">手机号 *</label>
                    <input v-model="staffModal.phone" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：13900005678" />
                  </div>
                  <div>
                    <label class="block text-xs text-brand-ink-3 mb-1.5">角色</label>
                    <select v-model="staffModal.role" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg">
                      <option value="operator">运营</option>
                      <option value="customer_service">客服</option>
                    </select>
                  </div>
                  <div>
                    <label class="block text-xs text-brand-ink-3 mb-1.5">权限范围</label>
                    <div class="flex flex-wrap gap-2">
                      <label v-for="p in ['product','order','finance','customer_service']" :key="p" class="flex items-center gap-1.5 text-sm text-brand-ink-2 cursor-pointer">
                        <input type="checkbox" :value="p" v-model="staffModal.permissions" class="w-4 h-4 accent-brand-primary" />
                        {{ {product:'商品管理', order:'订单管理', finance:'财务管理', customer_service:'客服'}[p] }}
                      </label>
                    </div>
                  </div>
                </div>
                <div class="flex justify-end gap-3 mt-6">
                  <button @click="staffModal.visible = false" class="px-5 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:bg-brand-surface-2 transition">取消</button>
                  <button @click="saveStaff" class="px-5 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition">添加</button>
                </div>
              </div>
            </div>
          </template>

          <!-- 认证 -->
          <template v-if="activeTab === 'cert'">
            <div class="space-y-5 max-w-3xl">
              <div v-for="c in certifications" :key="c.type" class="p-5 rounded-xl bg-brand-bg border border-brand-line">
                <div class="flex items-start justify-between gap-4">
                  <div class="flex-1">
                    <div class="flex items-center gap-2 mb-2">
                      <span class="font-semibold text-brand-ink">{{ c.name }}</span>
                      <span :class="['px-2 py-0.5 rounded text-[11px] font-semibold border', certStatusMap[c.status]?.color || 'bg-brand-ink-2/15 text-brand-ink-3 border-brand-ink-2/30']">{{ certStatusMap[c.status]?.text || c.status }}</span>
                    </div>
                    <div class="text-xs text-brand-ink-3">
                      <span v-if="c.certifiedAt">认证时间：{{ c.certifiedAt.slice(0, 10) }}</span>
                      <span v-else-if="c.status === 'not_applied'">尚未申请，点击右侧按钮申请</span>
                      <span v-else-if="c.status === 'reviewing'">审核中，预计 1-2 个工作日完成</span>
                    </div>
                  </div>
                  <button v-if="c.status === 'not_applied'" @click="openCertModal(c)" class="px-4 h-9 rounded-lg bg-brand-primary text-brand-primary-ink text-xs font-medium hover:bg-brand-primary-hover transition">申请认证</button>
                  <button v-else class="text-xs text-brand-primary hover:underline">查看详情</button>
                </div>
              </div>
            </div>

            <!-- 认证申请弹窗 -->
            <div v-if="certModal.visible" class="fixed inset-0 z-50 bg-black/40 flex items-center justify-center" @click.self="certModal.visible = false">
              <div class="w-[480px] max-w-[90vw] bg-brand-surface rounded-2xl shadow-xl border border-brand-line p-6">
                <h3 class="text-lg font-semibold text-brand-ink mb-4">申请「{{ certModal.name }}」</h3>
                <div class="space-y-4">
                  <template v-if="certModal.type === 'seller_verified'">
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">营业执照号</label>
                      <input v-model="certModal.form.businessLicense" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：91110000MA00ABCD12" />
                    </div>
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">法定代表人</label>
                      <input v-model="certModal.form.legalPerson" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：张三" />
                    </div>
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">联系电话</label>
                      <input v-model="certModal.form.contactPhone" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：13900001234" />
                    </div>
                  </template>
                  <template v-else-if="certModal.type === 'business_license'">
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">营业执照号</label>
                      <input v-model="certModal.form.businessLicense" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：91110000MA00ABCD12" />
                    </div>
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">法定代表人</label>
                      <input v-model="certModal.form.legalPerson" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：张三" />
                    </div>
                  </template>
                  <template v-else-if="certModal.type === 'deposit'">
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">保证金金额（元）</label>
                      <input v-model="certModal.form.depositAmount" type="number" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：10000" />
                    </div>
                  </template>
                  <template v-else-if="certModal.type === 'category_access'">
                    <div>
                      <label class="block text-xs text-brand-ink-3 mb-1.5">申请类目编码</label>
                      <input v-model="certModal.form.categoryCode" class="w-full h-10 rounded-lg border border-brand-line px-3 text-sm bg-brand-bg" placeholder="如：computer" />
                    </div>
                  </template>
                  <p class="text-xs text-brand-ink-3">提交后将在 1-2 个工作日内完成审核</p>
                </div>
                <div class="flex justify-end gap-3 mt-6">
                  <button @click="certModal.visible = false" class="px-5 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:bg-brand-surface-2 transition">取消</button>
                  <button @click="submitCert" class="px-5 h-10 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition">提交申请</button>
                </div>
              </div>
            </div>
          </template>

          <!-- 财务 -->
          <template v-if="activeTab === 'finance'">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-5 max-w-4xl">
              <div class="md:col-span-2 p-5 rounded-xl bg-brand-success/10 border border-brand-success/30">
                <div class="flex items-start gap-3">
                  <Shield class="w-5 h-5 text-brand-success shrink-0 mt-0.5" />
                  <div class="text-xs text-brand-ink-2 leading-relaxed">
                    <div class="font-semibold text-brand-success mb-1">资金安全保障</div>
                    卖家资金由持牌支付机构托管，单笔提现到账平均 6 分钟。支持 T+1 自动提现。
                  </div>
                </div>
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">开户人姓名 *</label>
                <input v-model="finance.bankAccountName" placeholder="请输入开户人姓名" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">银行卡号 *</label>
                <input v-model="finance.bankAccountNo" placeholder="请输入银行卡号" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">开户银行 *</label>
                <input v-model="finance.bankName" placeholder="如：招商银行" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">提现费率</label>
                <input :value="finance.withdrawFeeRate ? (finance.withdrawFeeRate * 100).toFixed(2) + '%' : '0.60%'" disabled class="w-full h-11 rounded-lg bg-brand-surface-2 border border-brand-line px-4 text-sm cursor-not-allowed text-brand-ink" />
                <div class="text-xs text-brand-ink-3 mt-1.5">第三方支付机构收取，平台不额外加收</div>
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">自动提现阈值（元）</label>
                <input v-model="finance.withdrawThreshold" type="number" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
                <div class="text-xs text-brand-ink-3 mt-1.5">当可提现余额大于该值时，系统将在每日 10:00 自动发起提现</div>
              </div>
              <div>
                <label class="block text-xs text-brand-ink-3 mb-1.5">结算周期</label>
                <select v-model="finance.settlementCycle" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary">
                  <option value="daily">T+1（确认收货后次日结算）</option>
                  <option value="weekly">T+7（确认收货后7天结算）</option>
                  <option value="monthly">T+30（确认收货后30天结算）</option>
                </select>
                <div class="text-xs text-brand-ink-3 mt-1.5">缩短周期需满足：店铺评分 ≥ 4.9 、信用分 ≥ 800</div>
              </div>
            </div>
          </template>
        </template>
      </section>

      <Transition name="toast">
        <div v-if="savedToast" class="fixed top-20 right-6 z-50 px-4 py-2.5 rounded-lg bg-brand-success text-white text-sm shadow-lg flex items-center gap-2">
          <BadgeCheck class="w-4 h-4" />保存成功
        </div>
      </Transition>
      <Transition name="toast">
        <div v-if="saveError" class="fixed top-20 right-6 z-50 px-4 py-2.5 rounded-lg bg-brand-error text-white text-sm shadow-lg flex items-center gap-2">
          <AlertTriangle class="w-4 h-4" />{{ saveError }}
        </div>
      </Transition>    </div>
  </div>
</template>

<style scoped>
.toast-enter-active, .toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from, .toast-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
