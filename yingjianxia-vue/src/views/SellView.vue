<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { createProductApi, updateProductApi, getProductDetailApi, getCategoriesApi, uploadFileApi } from '@/api/product'
import { CONDITIONS, CONDITION_TO_LEVEL, CONDITION_NAME, LEVEL_TO_CONDITION } from '@/constants/product'
import {
  Upload, Plus, X, ChevronLeft, ChevronRight, AlertTriangle, CheckCircle2,
  MapPin, ShieldCheck, Camera, FileText, Tag as TagIcon, TrendingUp, HelpCircle,
  Eye, Sparkles, Loader2, Store, Package, Tag
} from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const step = ref(1)
const submitting = ref(false)
const submitError = ref(null)
const submitSuccess = ref(false)
const draftSavedToast = ref(false)
const editingId = ref(null)  // 编辑模式的商品 ID
const imageUploading = ref(false)  // 图片上传中
const draftRestored = ref(false)  // 是否已恢复过草稿（避免重复提示）

// ========== 草稿自动保存 ==========
const DRAFT_KEY_PREFIX = 'sell_form_draft_'
const CLOUD_DRAFT_KEY_PREFIX = 'sell_cloud_draft_'
const getDraftKey = () => `${DRAFT_KEY_PREFIX}${userStore.userInfo?.id || 'guest'}`
const getCloudDraftKey = () => `${CLOUD_DRAFT_KEY_PREFIX}${userStore.userInfo?.id || 'guest'}`

// 云端草稿 ID（自动保存到后端的草稿记录）
const cloudDraftId = ref(localStorage.getItem(getCloudDraftKey()) || null)
const cloudDraftSavedAt = ref(0)
const cloudSaving = ref(false)  // 云端保存中，避免重复请求
let cloudSaveTimer = null

// 防抖保存到 localStorage
let draftSaveTimer = null
function saveDraftToLocal() {
  clearTimeout(draftSaveTimer)
  draftSaveTimer = setTimeout(() => {
    try {
      const data = {
        step: step.value,
        form: JSON.parse(JSON.stringify(form.value)),  // 深拷贝
        updatedAt: Date.now()
      }
      localStorage.setItem(getDraftKey(), JSON.stringify(data))
    } catch (e) {
      console.warn('草稿本地保存失败', e)
    }
  }, 1500)
}

// 从 localStorage 读取草稿
function readDraftFromLocal() {
  try {
    const raw = localStorage.getItem(getDraftKey())
    if (!raw) return null
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

// 清除本地草稿
function clearLocalDraft() {
  localStorage.removeItem(getDraftKey())
}

// 清除云端草稿记录
function clearCloudDraft() {
  localStorage.removeItem(getCloudDraftKey())
  cloudDraftId.value = null
  cloudDraftSavedAt.value = 0
}

// 云端自动保存（submitForReview=false）
async function autoSaveToCloud() {
  if (editingId.value) return  // 编辑模式不自动存云端
  if (cloudSaving.value) return
  // 至少需要标题才能保存到后端
  if (!form.value.title || form.value.title.length < 2) return
  // 距上次保存不足 20 秒则跳过
  if (Date.now() - cloudDraftSavedAt.value < 20000) return

  cloudSaving.value = true
  try {
    const payload = buildPayload(false)
    if (cloudDraftId.value) {
      await updateProductApi(cloudDraftId.value, payload)
    } else {
      const id = await createProductApi(payload)
      cloudDraftId.value = String(id)
      localStorage.setItem(getCloudDraftKey(), String(id))
    }
    cloudDraftSavedAt.value = Date.now()
  } catch (e) {
    // 云端保存失败不影响用户，静默处理
    console.warn('云端草稿自动保存失败', e.message)
  } finally {
    cloudSaving.value = false
  }
}

// 启动云端定时保存（每 30 秒尝试一次）
function startCloudAutoSave() {
  clearInterval(cloudSaveTimer)
  cloudSaveTimer = setInterval(() => {
    autoSaveToCloud()
  }, 30000)
}

function stopCloudAutoSave() {
  clearInterval(cloudSaveTimer)
  cloudSaveTimer = null
}

// 页面关闭/刷新前保存
function handleBeforeUnload(e) {
  if (editingId.value) return
  saveDraftToLocal()
  clearTimeout(draftSaveTimer)
  // 立即同步写入一次（防抖可能还没触发）
  try {
    const data = {
      step: step.value,
      form: JSON.parse(JSON.stringify(form.value)),
      updatedAt: Date.now()
    }
    localStorage.setItem(getDraftKey(), JSON.stringify(data))
  } catch (e) { /* ignore */ }
  // 浏览器原生提示
  e.preventDefault()
  e.returnValue = ''
}

// 恢复草稿
function restoreDraft(draft) {
  if (draft && draft.form) {
    step.value = draft.step || 1
    form.value = { ...form.value, ...draft.form }
  }
  clearLocalDraft()
  draftRestored.value = true
}

// 步骤定义
const steps = [
  { id: 1, title: '选择类目', desc: '选择正确的商品分类' },
  { id: 2, title: '商品信息', desc: '填写标题/描述/规格' },
  { id: 3, title: '图片&成色', desc: '上传主图 + 定义成色' },
  { id: 4, title: '验机&物流', desc: '服务承诺和运费设置' },
  { id: 5, title: '预览发布', desc: '确认并提交审核' }
]

const form = ref({
  // step1 类目
  categoryId: null,
  // step2 基础信息
  title: '',
  subTitle: '',
  brand: '',
  price: null,
  originalPrice: null,
  stock: 1,
  warningThreshold: 5,
  condition: 'VERY_GOOD',
  purchaseChannel: '',
  hasBox: false,
  location: '',
  specs: [
    { k: '', v: '' },
    { k: '', v: '' }
  ],
  description: '',
  // step3 图片
  images: [],
  defects: [],
  // step4 验机+物流
  useInspection: true,
  shipFree: true,
  shipTemplate: '全国包邮（顺丰）',
  warranty: '1年店保',
  aftersalesType: '7-days',
  // 服务承诺
  tags: ['支持担保交易', '7天无理由', '假一赔三承诺']
})

// 监听表单变化，自动保存到本地草稿（form 初始化后才能 watch）
watch([form, step], () => {
  // 编辑模式不保存到本地草稿（因为云端已有）
  if (editingId.value) return
  saveDraftToLocal()
}, { deep: true })

const categories = ref([])
const categoryLoading = ref(false)

const conditions = CONDITIONS

const currentCategoryName = computed(() => {
  const c = categories.value.find(c => c.id === form.value.categoryId)
  return c?.name || ''
})
const canNext = computed(() => {
  if (step.value === 1) return !!form.value.categoryId
  if (step.value === 2) return form.value.title.length >= 2 && form.value.price > 0 && form.value.stock > 0
  if (step.value === 3) return form.value.images.length >= 3
  return true
})

function addSpec() { form.value.specs.push({ k: '', v: '' }) }
function delSpec(i) { form.value.specs.splice(i, 1) }

function addDefect() { form.value.defects.push({ area: '外观', detail: '' }) }
function delDefect(i) { form.value.defects.splice(i, 1) }

function removeImage(i) { form.value.images.splice(i, 1) }

// 图片真实上传
async function handleFileUpload(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  imageUploading.value = true
  try {
    for (const file of files) {
      if (file.size > 10 * 1024 * 1024) {
        submitError.value = `${file.name} 超过 10MB 限制`
        continue
      }
      const fd = new FormData()
      fd.append('file', file)
      const res = await uploadFileApi(fd)
      if (res && res.url) {
        form.value.images.push(res.url)
      }
    }
  } catch (e) {
    submitError.value = e.message || '图片上传失败'
  } finally {
    imageUploading.value = false
    e.target.value = ''  // 清空 input，允许重复选择同一文件
  }
}

// 成色 → 后端 conditionLevel 映射（统一从 constants 取）
function getConditionLevel(conditionId) {
  return CONDITION_TO_LEVEL[conditionId] || 3
}

// 成色 → 后端 conditionName 映射
function getConditionName(conditionId) {
  return CONDITION_NAME[conditionId] || '95成新'
}

// 构建 payload（复用给 submit 和 saveDraft）
function buildPayload(submitForReview) {
  return {
    categoryId: form.value.categoryId,
    title: form.value.title,
    subTitle: form.value.subTitle || null,
    brand: form.value.brand || null,
    price: form.value.price,
    originalPrice: form.value.originalPrice || null,
    conditionLevel: getConditionLevel(form.value.condition),
    conditionName: getConditionName(form.value.condition),
    purchaseChannel: form.value.purchaseChannel || null,
    hasBox: !!form.value.hasBox,
    location: form.value.location || null,
    stock: form.value.stock,
    warningThreshold: form.value.warningThreshold || 5,
    description: form.value.description,
    needInspection: form.value.useInspection,
    submitForReview,
    images: form.value.images,
    specs: form.value.specs
      .filter(s => s.k && s.v)
      .map((s, i) => ({ name: s.k, value: s.v, sort: i })),
    defects: form.value.defects.filter(d => d.area && d.detail),
    shipFree: form.value.shipFree,
    shipTemplate: form.value.shipTemplate || null,
    warranty: form.value.warranty || null,
    aftersalesType: form.value.aftersalesType || null,
    tags: form.value.tags
  }
}

function validatePayload(payload) {
  if (!payload.categoryId) throw new Error('请选择商品分类')
  if (!payload.title || payload.title.length < 2) throw new Error('商品标题至少 2 个字符')
  if (!payload.price || payload.price <= 0) throw new Error('请输入有效的商品价格')
  if (!payload.images || payload.images.length < 3) throw new Error('至少上传 3 张商品图片')
}

async function submit() {
  submitting.value = true
  submitError.value = null
  try {
    const payload = buildPayload(true)
    validatePayload(payload)
    // 如果有云端草稿 ID，说明已自动保存过草稿，应更新而非新建
    const productId = editingId.value || cloudDraftId.value
    if (productId) {
      await updateProductApi(productId, payload)
    } else {
      await createProductApi(payload)
    }
    clearLocalDraft()
    clearCloudDraft()
    submitSuccess.value = true
    setTimeout(() => {
      router.push('/listings')
    }, 1500)
  } catch (e) {
    submitError.value = e.message || '提交失败，请重试'
  } finally {
    submitting.value = false
  }
}

// 保存草稿
async function saveDraft() {
  submitting.value = true
  submitError.value = null
  try {
    const payload = buildPayload(false)
    // 草稿模式放宽校验：仅要求标题
    if (!payload.title || payload.title.length < 2) {
      throw new Error('请至少填写商品标题（2字以上）')
    }
    // 如果有云端草稿 ID 或编辑 ID，应更新而非新建
    const productId = editingId.value || cloudDraftId.value
    if (productId) {
      await updateProductApi(productId, payload)
    } else {
      await createProductApi(payload)
    }
    clearLocalDraft()
    clearCloudDraft()
    draftSavedToast.value = true
    setTimeout(() => { draftSavedToast.value = false }, 2000)
  } catch (e) {
    submitError.value = e.message || '保存草稿失败，请重试'
  } finally {
    submitting.value = false
  }
}

async function loadCategories() {
  categoryLoading.value = true
  try {
    const data = await getCategoriesApi()
    if (Array.isArray(data)) {
      categories.value = data.map(c => ({
        id: c.id,
        name: c.name,
        code: c.code,
        icon: c.icon
      }))
    }
  } catch (e) {
    console.error('加载分类失败', e)
    // 加载失败时使用兜底分类
    categories.value = [
      { id: 1, name: '显卡', code: 'gpu' },
      { id: 2, name: 'CPU 处理器', code: 'cpu' },
      { id: 3, name: '主板', code: 'motherboard' },
      { id: 4, name: '内存条', code: 'ram' },
      { id: 5, name: 'SSD 存储', code: 'ssd' },
      { id: 6, name: '笔记本电脑', code: 'laptop' },
      { id: 7, name: '电源', code: 'psu' },
      { id: 8, name: '散热器', code: 'cooler' },
      { id: 9, name: '机箱', code: 'case' },
      { id: 10, name: '显示器', code: 'monitor' }
    ]
  } finally {
    categoryLoading.value = false
  }
}

// 加载商品详情（编辑模式）
async function loadProductForEdit(id) {
  try {
    const data = await getProductDetailApi(id)
    if (data) {
      editingId.value = id
      form.value.categoryId = data.categoryId
      form.value.title = data.title || ''
      form.value.subTitle = data.subTitle || ''
      form.value.brand = data.brand || ''
      form.value.price = Number(data.price) || null
      form.value.originalPrice = Number(data.originalPrice) || null
      form.value.stock = data.stock ?? 1
      form.value.warningThreshold = data.warningThreshold ?? 5
      form.value.condition = LEVEL_TO_CONDITION[data.conditionLevel] || 'VERY_GOOD'
      form.value.purchaseChannel = data.purchaseChannel || ''
      form.value.hasBox = !!data.hasBox
      form.value.location = data.location || ''
      form.value.description = data.description || ''
      form.value.images = data.imageUrls || []
      form.value.useInspection = !!data.needInspection
      form.value.defects = data.defects || []
      form.value.shipFree = data.shipFree ?? true
      form.value.shipTemplate = data.shipTemplate || '全国包邮（顺丰）'
      form.value.warranty = data.warranty || '1年店保'
      form.value.aftersalesType = data.aftersalesType || '7-days'
      form.value.tags = data.tags || ['支持担保交易', '7天无理由', '假一赔三承诺']
      // 规格
      if (data.specs && data.specs.length) {
        form.value.specs = data.specs.map(s => ({ k: s.name || '', v: s.value || '' }))
      }
    }
  } catch (e) {
    console.error('加载商品详情失败', e)
  }
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/auth', query: { redirect: route.fullPath } })
    return
  }
  if (!userStore.isSeller) {
    router.push('/store-settings')
    return
  }
  loadCategories()
  // 编辑模式：URL 带 ?id=xxx
  const editId = route.query.id
  if (editId) {
    loadProductForEdit(editId)
  } else {
    // 新建模式：优先检查云端草稿
    if (cloudDraftId.value) {
      if (confirm(`检测到云端有未完成的发布草稿（ID: ${cloudDraftId.value}）\n\n是否从云端恢复？`)) {
        loadProductForEdit(cloudDraftId.value)
        clearLocalDraft()
      } else {
        clearCloudDraft()
      }
    } else {
      // 无云端草稿时检查本地草稿
      const draft = readDraftFromLocal()
      if (draft && draft.form && draft.step) {
        const time = new Date(draft.updatedAt).toLocaleString('zh-CN')
        const title = draft.form.title || '（未填写标题）'
        if (confirm(`检测到上次未完成的发布草稿\n\n标题：${title}\n步骤：第 ${draft.step} 步\n保存时间：${time}\n\n是否恢复？`)) {
          restoreDraft(draft)
        } else {
          clearLocalDraft()
        }
      }
    }
    // 启动云端定时自动保存
    startCloudAutoSave()
  }
  // 监听页面关闭/刷新
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  clearTimeout(draftSaveTimer)
  stopCloudAutoSave()
})
</script>

<template>
  <div class="container-app space-y-6">
    <div class="flex items-center justify-between flex-wrap gap-3">
      <div class="text-xs text-brand-ink-3">卖家中心 / 发布商品</div>
      <nav class="flex items-center gap-1 text-xs">
        <button @click="router.push('/store-settings')" class="px-3 py-1.5 rounded-lg text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition flex items-center gap-1.5">
          <Store class="w-3.5 h-3.5" />卖家工作台
        </button>
        <button @click="router.push('/listings')" class="px-3 py-1.5 rounded-lg text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition flex items-center gap-1.5">
          <Package class="w-3.5 h-3.5" />我的发布
        </button>
        <button @click="router.push('/analytics')" class="px-3 py-1.5 rounded-lg text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition flex items-center gap-1.5">
          <Eye class="w-3.5 h-3.5" />数据中心
        </button>
        <button @click="router.push('/promotions')" class="px-3 py-1.5 rounded-lg text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition flex items-center gap-1.5">
          <Tag class="w-3.5 h-3.5" />营销活动
        </button>
      </nav>
    </div>

    <!-- 步骤条 -->
    <div class="rounded-2xl bg-brand-surface border border-brand-line p-5 md:p-6">
      <div class="flex items-center justify-between relative">
        <div class="absolute left-12 right-12 top-6 h-0.5 bg-brand-line">
          <div class="h-full bg-brand-primary rounded-full transition-all duration-500" :style="{ width: ((step - 1) / (steps.length - 1)) * 100 + '%' }"></div>
        </div>
        <div v-for="s in steps" :key="s.id" @click="step = s.id"
          class="relative flex flex-col items-center gap-2 z-10 shrink-0 w-20 md:w-auto px-2 cursor-pointer group">
          <div :class="['w-12 h-12 rounded-full flex items-center justify-center text-sm font-bold border-2 transition',
                       s.id < step ? 'bg-brand-primary border-brand-primary text-brand-primary-ink' :
                       s.id === step ? 'bg-brand-primary/15 border-brand-primary text-brand-primary ring-4 ring-brand-primary/15' :
                       'bg-brand-surface-2 border-brand-line text-brand-ink-3 group-hover:border-brand-primary/50 group-hover:text-brand-primary']">
            <CheckCircle2 v-if="s.id < step" class="w-5 h-5" />
            <span v-else>{{ s.id }}</span>
          </div>
          <div class="text-center">
            <div :class="['text-xs font-semibold transition', s.id <= step ? 'text-brand-ink' : 'text-brand-ink-3 group-hover:text-brand-primary']">{{ s.title }}</div>
            <div class="text-[10px] text-brand-ink-3 hidden md:block mt-0.5">{{ s.desc }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 表单内容 -->
    <div class="rounded-2xl bg-brand-surface border border-brand-line p-6 md:p-8">

      <!-- Step 1 类目选择 -->
      <section v-if="step === 1">
        <h2 class="text-lg font-bold text-brand-ink mb-2 flex items-center gap-2"><TagIcon class="w-5 h-5 text-brand-primary" />第一步：选择商品类目</h2>
        <p class="text-sm text-brand-ink-3 mb-6">正确选择类目可以让商品被更多买家搜到，<span class="text-brand-warning">错误类目将被平台审核打回</span></p>

        <div v-if="categoryLoading" class="flex items-center justify-center py-12 text-brand-ink-3 text-sm">
          <Sparkles class="w-5 h-5 mr-2 animate-pulse" />分类加载中...
        </div>
        <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
          <button v-for="c in categories" :key="c.id" @click="form.categoryId = c.id"
            :class="['p-4 rounded-xl border text-left transition',
                     form.categoryId === c.id ? 'bg-brand-primary/10 border-brand-primary/40 ring-1 ring-brand-primary/20' : 'bg-brand-bg border-brand-line hover:border-brand-primary/40']">
            <div :class="['text-sm font-semibold mb-1', form.categoryId === c.id ? 'text-brand-primary' : 'text-brand-ink']">{{ c.name }}</div>
            <div class="text-[11px] text-brand-ink-3">{{ c.code }}</div>
          </button>
        </div>

        <!-- AI 智能匹配建议 -->
        <div v-if="currentCategoryName" class="mt-6 p-4 rounded-xl bg-brand-info/10 border border-brand-info/30">
          <div class="flex items-start gap-3">
            <Sparkles class="w-5 h-5 text-brand-info shrink-0 mt-0.5" />
            <div class="text-sm text-brand-ink-2 leading-relaxed">
              <span class="font-semibold text-brand-info">已选类目</span><br/>
              当前选择：<b class="text-brand-ink">{{ currentCategoryName }}</b>，该类目下商品享受平台搜索加权推荐。
            </div>
          </div>
        </div>
      </section>

      <!-- Step 2 商品信息 -->
      <section v-if="step === 2">
        <h2 class="text-lg font-bold text-brand-ink mb-2 flex items-center gap-2"><FileText class="w-5 h-5 text-brand-primary" />第二步：填写商品信息</h2>
        <p class="text-sm text-brand-ink-3 mb-6">详细完整的信息能显著提高买家下单率</p>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-5 max-w-5xl">
          <div class="md:col-span-2">
            <label class="block text-xs text-brand-ink-3 mb-1.5">商品标题 *<span class="ml-2 text-brand-ink-3 font-normal">{{ form.title.length }}/30</span></label>
            <textarea v-model="form.title" maxlength="30" rows="2" class="w-full rounded-xl bg-brand-bg border border-brand-line px-4 py-3 text-sm focus:outline-none focus:border-brand-primary resize-none" placeholder="例：品牌 + 型号 + 容量/规格 + 成色亮点..."></textarea>
          </div>
          <div class="md:col-span-2">
            <label class="block text-xs text-brand-ink-3 mb-1.5">副标题/卖点</label>
            <input v-model="form.subTitle" class="w-full h-11 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" placeholder="输入 1-2 句话突出产品卖点（显示在标题下方）" />
          </div>
          <div>
            <label class="block text-xs text-brand-ink-3 mb-1.5">售价（¥）*</label>
            <div class="relative">
              <span class="absolute left-4 top-1/2 -translate-y-1/2 text-brand-ink-3 text-sm">¥</span>
              <input v-model.number="form.price" type="number" class="w-full h-12 rounded-lg bg-brand-bg border border-brand-line pl-8 pr-4 text-lg font-bold text-brand-error focus:outline-none focus:border-brand-primary" />
            </div>
            <div class="text-[11px] text-brand-ink-3 mt-1 flex items-center gap-1"><HelpCircle class="w-3 h-3" />建议参考平台同类均价 ¥4,200 ~ ¥4,800</div>
          </div>
          <div>
            <label class="block text-xs text-brand-ink-3 mb-1.5">原价/购入价</label>
            <div class="relative">
              <span class="absolute left-4 top-1/2 -translate-y-1/2 text-brand-ink-3 text-sm">¥</span>
              <input v-model.number="form.originalPrice" type="number" class="w-full h-12 rounded-lg bg-brand-bg border border-brand-line pl-8 pr-4 text-sm focus:outline-none focus:border-brand-primary" />
            </div>
          </div>
          <div>
            <label class="block text-xs text-brand-ink-3 mb-1.5">库存数量 *</label>
            <input v-model.number="form.stock" type="number" min="1" class="w-full h-12 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" />
          </div>
          <div>
            <label class="block text-xs text-brand-ink-3 mb-1.5">库存预警阈值</label>
            <input v-model.number="form.warningThreshold" type="number" min="1" max="99" class="w-full h-12 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" placeholder="默认 5，低于此值提醒" />
          </div>
          <div>
            <label class="block text-xs text-brand-ink-3 mb-1.5">成色评级 *<span class="ml-2 font-normal text-brand-warning">直接影响AI审核通过率</span></label>
            <div class="grid grid-cols-5 gap-1.5">
              <button v-for="c in conditions" :key="c.id" @click="form.condition = c.id"
                :class="['p-2 rounded-lg border text-xs transition',
                         form.condition === c.id ? 'bg-brand-primary/10 border-brand-primary/40 ring-1 ring-brand-primary/25' : 'border-brand-line hover:border-brand-primary/30 bg-brand-bg']"
                :title="c.tip">
                <div :class="['font-bold', c.color]">{{ c.name }}</div>
              </button>
            </div>
          </div>
          <div>
            <label class="block text-xs text-brand-ink-3 mb-1.5">品牌</label>
            <input v-model="form.brand" class="w-full h-12 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" placeholder="如：微星 / 华硕 / Intel" />
          </div>
          <div>
            <label class="block text-xs text-brand-ink-3 mb-1.5">购买渠道</label>
            <select v-model="form.purchaseChannel" class="w-full h-12 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary">
              <option value="">请选择</option>
              <option value="京东自营">京东自营</option>
              <option value="天猫/淘宝">天猫/淘宝</option>
              <option value="拼多多">拼多多</option>
              <option value="线下门店">线下门店</option>
              <option value="个人闲置">个人闲置</option>
              <option value="其他">其他</option>
            </select>
          </div>
          <div>
            <label class="block text-xs text-brand-ink-3 mb-1.5">所在地</label>
            <input v-model="form.location" class="w-full h-12 rounded-lg bg-brand-bg border border-brand-line px-4 text-sm focus:outline-none focus:border-brand-primary" placeholder="如：广东省深圳市" />
          </div>
          <div>
            <label class="block text-xs text-brand-ink-3 mb-1.5">箱说全</label>
            <div class="h-12 rounded-lg bg-brand-bg border border-brand-line px-4 flex items-center">
              <label class="flex items-center gap-2 text-sm text-brand-ink cursor-pointer">
                <input type="checkbox" v-model="form.hasBox" class="w-4 h-4 accent-brand-primary" />
                包装、说明书、配件齐全
              </label>
            </div>
          </div>

          <div class="md:col-span-2 pt-4 border-t border-brand-line">
            <div class="flex items-center justify-between mb-3">
              <label class="text-xs text-brand-ink-3 font-semibold">规格参数（显示在商品详情顶部）</label>
              <button @click="addSpec" class="px-3 h-7 rounded-md bg-brand-primary/15 text-brand-primary text-xs hover:bg-brand-primary hover:text-brand-primary-ink transition flex items-center gap-1"><Plus class="w-3 h-3" />添加</button>
            </div>
            <div class="space-y-2">
              <div v-for="(s, i) in form.specs" :key="i" class="grid grid-cols-[1fr_1.5fr_auto] gap-2 items-center">
                <input v-model="s.k" placeholder="参数名（如显存）" class="h-10 rounded-lg bg-brand-bg border border-brand-line px-3 text-sm focus:outline-none focus:border-brand-primary" />
                <input v-model="s.v" placeholder="参数值（如 12GB GDDR6X 384bit）" class="h-10 rounded-lg bg-brand-bg border border-brand-line px-3 text-sm focus:outline-none focus:border-brand-primary" />
                <button @click="delSpec(i)" class="p-2 rounded-md text-brand-ink-3 hover:text-brand-error hover:bg-brand-error-subtle transition"><X class="w-4 h-4" /></button>
              </div>
            </div>
          </div>

          <div class="md:col-span-2">
            <label class="block text-xs text-brand-ink-3 mb-1.5">详细描述 *</label>
            <textarea v-model="form.description" rows="12" class="w-full rounded-xl bg-brand-bg border border-brand-line p-4 text-sm leading-7 focus:outline-none focus:border-brand-primary resize-none" placeholder="建议包含：使用场景、成色说明、跑分/烤机数据、保修、包装发票等信息"></textarea>
          </div>
        </div>
      </section>

      <!-- Step 3 图片和瑕疵 -->
      <section v-if="step === 3">
        <h2 class="text-lg font-bold text-brand-ink mb-2 flex items-center gap-2"><Camera class="w-5 h-5 text-brand-primary" />第三步：图片上传 & 瑕疵说明</h2>
        <p class="text-sm text-brand-ink-3 mb-6">图片越齐全越真实，转化率越高。<span class="text-brand-warning">至少 3 张主图（推荐 6-9 张）</span></p>

        <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-3 mb-8">
          <div v-for="(img, i) in form.images" :key="i" class="relative aspect-square rounded-xl overflow-hidden bg-brand-bg border-2 border-brand-line group">
            <img :src="img" class="w-full h-full object-cover" />
            <div v-if="i === 0" class="absolute top-2 left-2 px-2 py-0.5 rounded bg-brand-primary text-brand-primary-ink text-[10px] font-bold">主图</div>
            <button @click="removeImage(i)" class="absolute top-2 right-2 w-6 h-6 rounded-full bg-black/60 text-white opacity-0 group-hover:opacity-100 transition flex items-center justify-center">
              <X class="w-3.5 h-3.5" />
            </button>
            <div v-if="i === 0" class="absolute bottom-0 left-0 right-0 h-1 bg-brand-primary"></div>
          </div>
          <label class="aspect-square rounded-xl bg-brand-bg border-2 border-dashed border-brand-line hover:border-brand-primary/50 hover:bg-brand-primary/5 transition flex flex-col items-center justify-center text-brand-ink-3 hover:text-brand-primary cursor-pointer">
            <Loader2 v-if="imageUploading" class="w-8 h-8 mb-1.5 animate-spin" />
            <Upload v-else class="w-8 h-8 mb-1.5" />
            <div class="text-xs font-medium">{{ imageUploading ? '上传中...' : '+ 上传图片' }}</div>
            <div class="text-[10px] opacity-70 mt-1">单张 ≤ 10MB</div>
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple class="hidden" @change="handleFileUpload" />
          </label>
        </div>

        <!-- 瑕疵说明 -->
        <div class="rounded-xl bg-brand-bg border border-brand-line p-5">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="font-semibold text-brand-ink flex items-center gap-2"><AlertTriangle class="w-4 h-4 text-brand-warning" />瑕疵说明（选填）</div>
              <div class="text-xs text-brand-ink-3 mt-1">如实披露瑕疵不会影响售卖，反而能大幅减少售后纠纷</div>
            </div>
            <button @click="addDefect" class="px-3 h-8 rounded-md border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-1"><Plus class="w-3 h-3" />添加瑕疵</button>
          </div>
          <div v-if="form.defects.length === 0" class="p-8 rounded-lg bg-brand-surface-2 text-center text-sm text-brand-ink-3 border border-dashed border-brand-line">
            <CheckCircle2 class="w-8 h-8 mx-auto mb-2 text-brand-success/60" />暂无瑕疵说明 · 如商品为全新或准新，可直接进入下一步
          </div>
          <div v-else class="space-y-2.5">
            <div v-for="(d, i) in form.defects" :key="i" class="grid grid-cols-[140px_1fr_auto] gap-2 items-center">
              <select v-model="d.area" class="h-10 rounded-lg bg-brand-surface border border-brand-line px-3 text-sm focus:outline-none focus:border-brand-primary">
                <option>外观</option><option>性能</option><option>包装配件</option><option>保修记录</option>
              </select>
              <input v-model="d.detail" placeholder="描述具体瑕疵，如：左上角有 2mm 细小划痕" class="h-10 rounded-lg bg-brand-surface border border-brand-line px-3 text-sm focus:outline-none focus:border-brand-primary" />
              <button @click="delDefect(i)" class="p-2 rounded-md text-brand-ink-3 hover:text-brand-error hover:bg-brand-error-subtle transition"><X class="w-4 h-4" /></button>
            </div>
          </div>
        </div>
      </section>

      <!-- Step 4 验机 物流 服务 -->
      <section v-if="step === 4">
        <h2 class="text-lg font-bold text-brand-ink mb-2 flex items-center gap-2"><ShieldCheck class="w-5 h-5 text-brand-primary" />第四步：验机 / 物流 / 服务承诺</h2>
        <p class="text-sm text-brand-ink-3 mb-6">完善的服务能大幅提升买家信任，开启官方验机可加权推荐 +50% 曝光</p>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
          <!-- 官方验机 -->
          <div class="p-5 rounded-xl bg-brand-bg border" :class="form.useInspection ? 'border-brand-info/40 ring-2 ring-brand-info/15' : 'border-brand-line'">
            <div class="flex items-start justify-between mb-3">
              <div>
                <div class="font-semibold text-brand-ink flex items-center gap-2"><ShieldCheck class="w-4 h-4 text-brand-info" />官方验机服务</div>
                <div class="text-xs text-brand-ink-3 mt-1">平台委托第三方实验室28项专业检测，出具电子报告（带防伪签名）</div>
              </div>
              <label class="inline-flex items-center gap-2 cursor-pointer">
                <span class="relative inline-block w-11 h-6 align-middle">
                  <input type="checkbox" v-model="form.useInspection" class="peer sr-only" />
                  <span class="absolute inset-0 bg-brand-line rounded-full transition peer-checked:bg-brand-info cursor-pointer"></span>
                  <span class="absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white transition peer-checked:translate-x-5 shadow"></span>
                </span>
                <span class="text-xs font-semibold" :class="form.useInspection ? 'text-brand-info' : 'text-brand-ink-3'">{{ form.useInspection ? '已开启' : '未开启' }}</span>
              </label>
            </div>
            <div class="space-y-2 text-xs text-brand-ink-2 leading-relaxed p-3 rounded-lg bg-brand-surface border border-brand-line-subtle">
              <div class="flex justify-between"><span>验机服务费</span><span class="font-semibold text-brand-ink">¥88（买家支付）</span></div>
              <div class="flex justify-between"><span>曝光加权</span><span class="font-semibold text-brand-success">+50% 搜索加权</span></div>
              <div class="flex justify-between"><span>纠纷免责</span><span class="font-semibold text-brand-info">验机合格 → 免责</span></div>
            </div>
            <div class="mt-3 p-2.5 rounded-lg bg-brand-info/10 border border-brand-info/30 flex items-start gap-2">
              <TrendingUp class="w-4 h-4 text-brand-info shrink-0 mt-0.5" />
              <div class="text-[11px] text-brand-ink-2 leading-relaxed">平台数据：开启验机的商品，<span class="text-brand-info font-semibold">平均成交率提升 127%</span>，售后率下降 81%。</div>
            </div>
          </div>

          <!-- 物流 -->
          <div class="p-5 rounded-xl bg-brand-bg border border-brand-line">
            <div class="font-semibold text-brand-ink flex items-center gap-2 mb-3"><MapPin class="w-4 h-4 text-brand-success" />物流与运费</div>
            <div class="space-y-3">
              <label class="flex items-start gap-2 p-3 rounded-lg border cursor-pointer transition" :class="form.shipFree ? 'bg-brand-primary/10 border-brand-primary/40' : 'bg-brand-surface border-brand-line hover:border-brand-primary/30'">
                <input type="radio" v-model="form.shipFree" :value="true" class="mt-0.5 accent-brand-primary" />
                <div>
                  <div class="text-sm font-semibold text-brand-ink">全国包邮</div>
                  <div class="text-[11px] text-brand-ink-3 mt-0.5">顺丰包邮 · 满 ¥500 自动提升转化率</div>
                </div>
              </label>
              <label class="flex items-start gap-2 p-3 rounded-lg border cursor-pointer transition" :class="!form.shipFree ? 'bg-brand-primary/10 border-brand-primary/40' : 'bg-brand-surface border-brand-line hover:border-brand-primary/30'">
                <input type="radio" v-model="form.shipFree" :value="false" class="mt-0.5 accent-brand-primary" />
                <div>
                  <div class="text-sm font-semibold text-brand-ink">使用运费模板</div>
                  <div class="text-[11px] text-brand-ink-3 mt-0.5">偏远地区 +30、新疆西藏 +50</div>
                </div>
              </label>
              <select class="w-full h-11 rounded-lg bg-brand-surface border border-brand-line px-3 text-sm focus:outline-none focus:border-brand-primary">
                <option>{{ form.shipTemplate }}</option>
                <option>自定义标准运费</option>
                <option>顺丰到付</option>
              </select>
            </div>
          </div>

          <!-- 保修售后 -->
          <div class="p-5 rounded-xl bg-brand-bg border border-brand-line">
            <div class="font-semibold text-brand-ink flex items-center gap-2 mb-3"><FileText class="w-4 h-4 text-brand-warning" />售后政策</div>
            <div class="space-y-2">
              <label class="block"><span class="text-xs text-brand-ink-3 mb-1 block">售后服务类型</span>
                <select v-model="form.aftersalesType" class="w-full h-11 rounded-lg bg-brand-surface border border-brand-line px-3 text-sm focus:outline-none focus:border-brand-primary">
                  <option value="7-days">7天无理由退货（平台推荐）</option>
                  <option value="3-days">3天质量问题包退</option>
                  <option value="no-return">二手商品售出不退不换（不推荐）</option>
                </select>
              </label>
              <label class="block"><span class="text-xs text-brand-ink-3 mb-1 block">店铺质保时长</span>
                <select v-model="form.warranty" class="w-full h-11 rounded-lg bg-brand-surface border border-brand-line px-3 text-sm focus:outline-none focus:border-brand-primary">
                  <option>无</option><option>3个月</option><option>6个月</option><option>1年店保</option><option>2年店保（增值）</option>
                </select>
              </label>
            </div>
          </div>

          <!-- 服务承诺标签 -->
          <div class="p-5 rounded-xl bg-brand-bg border border-brand-line">
            <div class="font-semibold text-brand-ink mb-3">服务承诺标签（显示在商品页）</div>
            <div class="flex flex-wrap gap-2">
              <button v-for="t in ['支持担保交易', '7天无理由', '假一赔三承诺', '极速发货24h内', '支持验机服务', '顺丰空运', '包装发票齐全', '终身技术咨询']" :key="t"
                @click="form.tags.includes(t) ? form.tags = form.tags.filter(x => x !== t) : form.tags.push(t)"
                :class="['px-3 py-1.5 rounded-md text-xs font-medium transition border',
                         form.tags.includes(t) ? 'bg-brand-primary/15 border-brand-primary/30 text-brand-primary' : 'bg-brand-surface border-brand-line text-brand-ink-2 hover:border-brand-primary/30 hover:text-brand-primary']">
                <span class="mr-1">{{ form.tags.includes(t) ? '✓' : '+' }}</span>{{ t }}
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- Step 5 预览 -->
      <section v-if="step === 5">
        <h2 class="text-lg font-bold text-brand-ink mb-2 flex items-center gap-2"><Eye class="w-5 h-5 text-brand-primary" />第五步：预览并发布</h2>
        <p class="text-sm text-brand-ink-3 mb-6">请确认下方信息正确，提交后平台将在 <span class="text-brand-warning font-semibold">1-2 小时内审核</span>，通过后自动上架</p>

        <div class="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-6">
          <!-- 预览卡 -->
          <div class="rounded-2xl border border-brand-line overflow-hidden">
            <div class="bg-gradient-to-br from-brand-primary/15 via-brand-surface to-brand-bg p-6 text-xs text-brand-ink-3 mb-2 flex items-center gap-2">
              <Eye class="w-4 h-4 text-brand-primary" />以下为商品发布后的真实效果预览
            </div>
            <div class="p-6 space-y-5">
              <div class="grid grid-cols-1 md:grid-cols-[200px_1fr] gap-5">
                <div class="aspect-square rounded-xl overflow-hidden bg-brand-bg border border-brand-line">
                  <img :src="form.images[0]" class="w-full h-full object-cover" />
                </div>
                <div>
                  <div class="text-base font-bold text-brand-ink leading-snug">{{ form.title }}</div>
                  <div class="text-xs text-brand-ink-3 mt-1">{{ form.subTitle }}</div>
                  <div class="mt-3 flex items-baseline gap-2">
                    <span class="text-xs text-brand-ink-3">¥</span>
                    <span class="text-3xl font-black text-brand-error">{{ form.price }}</span>
                    <span class="text-xs text-brand-ink-3 line-through">原价 ¥{{ form.originalPrice }}</span>
                  </div>
                  <div class="flex flex-wrap gap-1.5 mt-3">
                    <span v-for="t in form.tags.slice(0, 4)" :key="t" class="px-2 py-0.5 rounded bg-brand-primary/15 text-brand-primary text-[10px] font-semibold border border-brand-primary/30">{{ t }}</span>
                  </div>
                </div>
              </div>

              <div class="rounded-lg border border-brand-line overflow-hidden">
                <div class="grid grid-cols-2 divide-x divide-y divide-brand-line-subtle text-xs">
                  <div v-for="s in form.specs.slice(0, 4)" :key="s.k" class="flex px-3 py-2">
                    <span class="w-24 shrink-0 text-brand-ink-3">{{ s.k }}</span>
                    <span class="text-brand-ink font-medium">{{ s.v }}</span>
                  </div>
                </div>
              </div>

              <div class="p-4 rounded-lg bg-brand-warning/10 border border-brand-warning/30">
                <div class="text-xs text-brand-warning font-semibold">AI 审核预测：<span class="text-brand-success">高概率通过</span>（预计 35 分钟内审核完成）</div>
                <div class="text-[11px] text-brand-ink-2 mt-1 leading-relaxed">关键词违规检测：通过 · 图片一致性评分 94 · 描述一致性评分 92 · 类目匹配度 98</div>
              </div>
            </div>
          </div>

          <!-- 提交侧栏 -->
          <div class="sticky top-28 self-start space-y-4">
            <div class="rounded-2xl bg-brand-surface border border-brand-line p-5">
              <h3 class="font-semibold text-brand-ink text-sm mb-4">提交前核对清单</h3>
              <div class="space-y-2.5 text-xs">
                <div v-for="c in [
                  { t: '类目选择', ok: true },
                  { t: '标题（2-30字）', ok: form.title.length >= 2 },
                  { t: '价格设置（>0）', ok: form.price > 0 },
                  { t: '图片 ≥ 3 张', ok: form.images.length >= 3 },
                  { t: '规格参数填写', ok: form.specs.filter(s => s.k && s.v).length >= 2 },
                  { t: '成色评级已选', ok: !!form.condition }
                ]" :key="c.t" class="flex items-center gap-2">
                  <CheckCircle2 v-if="c.ok" class="w-4 h-4 text-brand-success shrink-0" />
                  <AlertTriangle v-else class="w-4 h-4 text-brand-warning shrink-0" />
                  <span :class="c.ok ? 'text-brand-ink-2' : 'text-brand-warning font-medium'">{{ c.t }}</span>
                </div>
              </div>
              <div class="mt-5 pt-4 border-t border-brand-line space-y-2">
                <label class="flex items-start gap-2 text-[11px] text-brand-ink-2 cursor-pointer">
                  <input checked type="checkbox" class="mt-0.5 w-3.5 h-3.5 accent-brand-primary" />
                  <span>我已阅读并同意《<b class="text-brand-primary">卖家发布规则</b>》与《<b class="text-brand-primary">平台服务协议</b>》，承诺所有信息真实，虚假信息自愿接受 ¥200 违约金。</span>
                </label>
                <label class="flex items-start gap-2 text-[11px] text-brand-ink-2 cursor-pointer">
                  <input checked type="checkbox" class="mt-0.5 w-3.5 h-3.5 accent-brand-primary" />
                  <span>授权平台对商品图片和描述进行 AI 审核、并将数据用于搜索匹配。</span>
                </label>
              </div>
              <button @click="submit" :disabled="submitting"
                class="w-full h-12 mt-5 rounded-xl bg-brand-primary text-brand-primary-ink font-bold hover:bg-brand-primary-hover disabled:opacity-60 transition flex items-center justify-center gap-2 shadow-1">
                <template v-if="submitting">
                  <svg class="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-opacity="0.25" stroke-width="3"/><path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-width="3" stroke-linecap="round"/></svg>
                  提交审核中...
                </template>
                <template v-else>
                  <Sparkles class="w-4 h-4" />提交审核
                </template>
              </button>
              <div v-if="submitError" class="mt-3 p-3 rounded-lg bg-brand-error/10 border border-brand-error/30 text-xs text-brand-error flex items-start gap-2">
                <AlertTriangle class="w-4 h-4 shrink-0 mt-0.5" />{{ submitError }}
              </div>
              <button @click="step = 1" class="w-full h-9 mt-2 rounded-xl border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition">返回修改</button>
            </div>
            <div class="p-4 rounded-xl bg-brand-success/10 border border-brand-success/30 text-xs text-brand-ink-2 leading-relaxed flex items-start gap-2">
              <CheckCircle2 class="w-4 h-4 text-brand-success shrink-0 mt-0.5" />
              <div>审核通过后：<b>短信 + 站内消息</b> 双通知；可至"我的发布"查看审核进度。</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 底部步骤按钮 -->
      <div v-if="step < 5" class="flex items-center justify-between gap-3 mt-10 pt-6 border-t border-brand-line">
        <button @click="step > 1 && step--" :disabled="step === 1"
          class="px-5 h-11 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 disabled:opacity-40 transition flex items-center gap-2">
          <ChevronLeft class="w-4 h-4" />上一步
        </button>
        <div class="flex items-center gap-2">
          <button @click="saveDraft" :disabled="submitting" class="px-5 h-11 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:bg-brand-surface-2 disabled:opacity-60 transition">保存草稿</button>
          <button @click="step < steps.length && step++" :disabled="!canNext"
            class="px-6 h-11 rounded-lg bg-brand-primary text-brand-primary-ink font-semibold hover:bg-brand-primary-hover disabled:opacity-40 transition flex items-center gap-2">
            下一步<ChevronRight class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- 提交成功 Toast -->
    <Transition name="toast">
      <div v-if="submitSuccess" class="fixed top-20 right-6 z-50 px-5 py-3 rounded-lg bg-brand-success text-white text-sm shadow-lg flex items-center gap-2">
        <CheckCircle2 class="w-5 h-5" />商品提交成功，正在跳转至"我的发布"...
      </div>
    </Transition>
    <Transition name="toast">
      <div v-if="draftSavedToast" class="fixed top-20 right-6 z-50 px-5 py-3 rounded-lg bg-brand-success text-white text-sm shadow-lg flex items-center gap-2">
        <CheckCircle2 class="w-5 h-5" />草稿已保存，可继续编辑
      </div>
    </Transition>
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
