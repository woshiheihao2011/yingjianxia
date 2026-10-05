<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getPendingAuditsApi, getAuditRecordsApi, processAuditApi
} from '@/api/audit'
import { getProductInternalDetailApi } from '@/api/product'
import {
  Search, Filter, Eye, CheckCircle2, XCircle, Clock, ChevronDown, ChevronUp,
  RefreshCw, Package, AlertTriangle, FileText, ShieldCheck, ExternalLink
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

// 列表数据
const records = ref([])
const loading = ref(false)
const toast = ref(null)

// 分页
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

// 筛选
const filterType = ref(1)     // 默认商品
const filterStatus = ref('pending')  // pending / all / passed / rejected
const kw = ref('')
const filtersOpen = ref(false)

// 审核弹窗
const auditModal = ref(false)
const currentRecord = ref(null)
const currentProduct = ref(null)
const auditAction = ref(1)    // 1=通过 2=拒绝
const auditReason = ref('')
const auditComment = ref('')
const submitting = ref(false)

const targetTypeMap = {
  1: { name: '商品', icon: Package, color: 'text-brand-primary' },
  2: { name: '帖子', icon: FileText, color: 'text-brand-info' },
  3: { name: '举报', icon: AlertTriangle, color: 'text-brand-error' },
  4: { name: '提现', icon: ShieldCheck, color: 'text-brand-success' },
}

const statusMap = {
  0: { name: '待审核', class: 'bg-brand-warning/15 text-brand-warning border-brand-warning/30', icon: Clock },
  1: { name: '已通过', class: 'bg-brand-success/15 text-brand-success border-brand-success/30', icon: CheckCircle2 },
  2: { name: '已拒绝', class: 'bg-brand-error/15 text-brand-error border-brand-error/30', icon: XCircle },
}

function showToast(type, msg) {
  toast.value = { type, msg }
  setTimeout(() => { toast.value = null }, 3000)
}

const pageArray = computed(() => {
  const arr = []
  const start = Math.max(1, pageNum.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) arr.push(i)
  return arr
})

async function loadRecords(page = 1) {
  if (typeof page !== 'number' || isNaN(page)) page = 1
  loading.value = true
  pageNum.value = page
  try {
    const params = { pageNum: page, pageSize: pageSize.value, targetType: filterType.value }
    if (filterStatus.value === 'pending') params.status = 0
    else if (filterStatus.value === 'passed') params.status = 1
    else if (filterStatus.value === 'rejected') params.status = 2
    const apiFn = filterStatus.value === 'pending' ? getPendingAuditsApi : getAuditRecordsApi
    const res = await apiFn(params)
    const list = (res && res.records) || []
    records.value = list
    total.value = (res && res.total) || 0
    totalPages.value = (res && res.totalPages) || 0
  } catch (e) {
    console.error('加载审核列表失败', e)
    showToast('error', e.message || '加载失败，请稍后重试')
    // 不清空 records，保留旧数据
  } finally {
    loading.value = false
  }
}

function changePage(n) {
  if (n < 1 || n > totalPages.value) return
  loadRecords(n)
}

// 打开审核弹窗
async function openAudit(record) {
  currentRecord.value = record
  currentProduct.value = null
  auditAction.value = 1
  auditReason.value = ''
  auditComment.value = ''
  auditModal.value = true
  // 如果是商品审核，加载商品详情
  if (record.targetType === 1) {
    try {
      const product = await getProductInternalDetailApi(record.targetId)
      currentProduct.value = product
    } catch (e) {
      console.warn('加载商品详情失败', e.message)
    }
  }
}

// 提交审核
async function submitAudit() {
  if (!currentRecord.value) return
  if (auditAction.value === 2 && !auditReason.value.trim()) {
    showToast('error', '拒绝时必须填写审核理由')
    return
  }
  submitting.value = true
  try {
    await processAuditApi(currentRecord.value.id, {
      action: auditAction.value,
      reason: auditReason.value || null,
      comment: auditComment.value || null,
      evidence: []
    })
    showToast('success', auditAction.value === 1 ? '已通过审核' : '已拒绝审核')
    auditModal.value = false
    loadRecords(pageNum.value)
  } catch (e) {
    showToast('error', e.message || '审核操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 从路由 query 读取审核类型（/admin/products → type=1, /admin/orders → type=3）
  const queryType = route.query.type
  if (queryType) {
    const t = parseInt(queryType)
    if ([1, 2, 3, 4].includes(t)) filterType.value = t
  }
  loadRecords()
})

// 监听路由 query 变化（如从 /admin/products 跳转过来时）
watch(() => route.query.type, (newType) => {
  if (newType) {
    const t = parseInt(newType)
    if ([1, 2, 3, 4].includes(t) && t !== filterType.value) {
      filterType.value = t
      loadRecords(1)
    }
  }
})
</script>

<template>
  <div class="container-app space-y-6">
    <div class="flex items-center justify-between flex-wrap gap-3">
      <div class="text-xs text-brand-ink-3">平台管理 / 审核中心</div>
      <nav class="flex items-center gap-1 text-xs">
        <button @click="router.push('/')" class="px-3 py-1.5 rounded-lg text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition">首页</button>
        <button @click="router.push('/listings')" class="px-3 py-1.5 rounded-lg text-brand-ink-2 hover:bg-brand-surface-2 hover:text-brand-primary transition">我的发布</button>
      </nav>
    </div>

    <!-- 统计卡 -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <button v-for="s in [
        { id: 'pending', name: '待审核', icon: Clock, color: 'text-brand-warning' },
        { id: 'all', name: '全部记录', icon: FileText, color: 'text-brand-ink-2' },
        { id: 'passed', name: '已通过', icon: CheckCircle2, color: 'text-brand-success' },
        { id: 'rejected', name: '已拒绝', icon: XCircle, color: 'text-brand-error' },
      ]" :key="s.id" @click="filterStatus = s.id; loadRecords(1)"
        :class="['p-4 rounded-xl border transition text-left',
                 filterStatus === s.id ? 'bg-brand-primary/10 border-brand-primary/40 ring-1 ring-brand-primary/20' : 'bg-brand-surface border-brand-line hover:border-brand-primary/40']">
        <div class="flex items-center justify-between">
          <div :class="['w-8 h-8 rounded-lg flex items-center justify-center', filterStatus === s.id ? 'bg-brand-primary/20 text-brand-primary' : 'bg-brand-bg text-brand-ink-3']">
            <component :is="s.icon" class="w-4 h-4" />
          </div>
        </div>
        <div class="text-sm font-semibold text-brand-ink-2 mt-3">{{ s.name }}</div>
      </button>
    </div>

    <!-- 筛选条 -->
    <div class="rounded-xl bg-brand-surface border border-brand-line p-4 md:p-5 space-y-4">
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div class="flex flex-wrap items-center gap-2 flex-1">
          <button v-for="t in [
            { id: 1, name: '商品审核', icon: Package },
            { id: 2, name: '帖子审核', icon: FileText },
            { id: 3, name: '举报处理', icon: AlertTriangle },
            { id: 4, name: '提现审核', icon: ShieldCheck }
          ]" :key="t.id" @click="filterType = t.id; loadRecords(1)"
            :class="['inline-flex items-center gap-1.5 h-9 px-3.5 rounded-lg text-sm font-medium transition',
                     filterType === t.id ? 'bg-brand-primary text-brand-primary-ink' : 'bg-brand-bg border border-brand-line text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40']">
            <component :is="t.icon" class="w-3.5 h-3.5" />{{ t.name }}
          </button>
          <button @click="() => loadRecords(1)" class="px-4 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary hover:border-brand-primary/40 transition flex items-center gap-2">
            <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />刷新
          </button>
        </div>
      </div>
    </div>

    <!-- 审核列表 -->
    <div class="rounded-2xl bg-brand-surface border border-brand-line overflow-hidden">
      <div v-if="loading" class="flex items-center justify-center py-20">
        <RefreshCw class="w-6 h-6 text-brand-primary animate-spin" />
        <span class="ml-2 text-sm text-brand-ink-3">加载中...</span>
      </div>

      <div v-else-if="records.length === 0" class="flex flex-col items-center justify-center py-20">
        <CheckCircle2 class="w-12 h-12 text-brand-ink-3/30 mb-3" />
        <p class="text-sm text-brand-ink-3">暂无待审核记录</p>
      </div>

      <table v-else class="w-full text-sm">
        <thead class="bg-brand-bg text-brand-ink-3 text-xs">
          <tr>
            <th class="text-left px-4 py-3 font-medium">类型</th>
            <th class="text-left px-4 py-3 font-medium">目标ID</th>
            <th class="text-left px-4 py-3 font-medium">状态</th>
            <th class="text-left px-4 py-3 font-medium">审核员</th>
            <th class="text-left px-4 py-3 font-medium">提交时间</th>
            <th class="text-left px-4 py-3 font-medium">理由/意见</th>
            <th class="text-right px-4 py-3 font-medium">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-brand-line">
          <tr v-for="r in records" :key="r.id" class="hover:bg-brand-bg/50 transition">
            <td class="px-4 py-3">
              <span :class="['inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs border', (targetTypeMap[r.targetType] ? targetTypeMap[r.targetType].color : ''), 'border-brand-line']">
                {{ targetTypeMap[r.targetType]?.name || '未知' }}
              </span>
            </td>
            <td class="px-4 py-3 font-mono text-xs">
              <button v-if="r.targetType === 1" @click="openAudit(r)" class="text-brand-primary hover:underline">{{ r.targetId }}</button>
              <span v-else class="text-brand-ink-2">{{ r.targetId }}</span>
            </td>
            <td class="px-4 py-3">
              <span :class="['inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs border', statusMap[r.status]?.class || '']">
                <component :is="statusMap[r.status]?.icon" class="w-3 h-3" />
                {{ statusMap[r.status]?.name || '未知' }}
              </span>
            </td>
            <td class="px-4 py-3 text-xs text-brand-ink-2">{{ r.auditorName || '—' }}</td>
            <td class="px-4 py-3 text-xs text-brand-ink-3">{{ r.createdAt ? String(r.createdAt).replace('T', ' ').slice(0, 16) : '—' }}</td>
            <td class="px-4 py-3 text-xs text-brand-ink-3 max-w-xs truncate">
              <span v-if="r.reason || r.comment">{{ r.reason || r.comment }}</span>
              <span v-else class="text-brand-ink-3/50">—</span>
            </td>
            <td class="px-4 py-3 text-right">
              <button v-if="r.status === 0" @click="openAudit(r)"
                class="px-3 h-8 rounded-lg bg-brand-primary text-brand-primary-ink text-xs font-medium hover:bg-brand-primary-hover transition inline-flex items-center gap-1">
                <ShieldCheck class="w-3.5 h-3.5" />去审核
              </button>
              <span v-else class="text-xs text-brand-ink-3">已处理</span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="flex items-center justify-between p-4 border-t border-brand-line">
        <span class="text-xs text-brand-ink-3">共 {{ total }} 条，第 {{ pageNum }}/{{ totalPages }} 页</span>
        <div class="flex items-center gap-1">
          <button @click="changePage(1)" :disabled="pageNum <= 1" class="px-3 h-8 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary disabled:opacity-30 transition">首页</button>
          <button @click="changePage(pageNum - 1)" :disabled="pageNum <= 1" class="px-3 h-8 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary disabled:opacity-30 transition">上一页</button>
          <button v-for="n in pageArray" :key="n" @click="changePage(n)"
            :class="['px-3 h-8 rounded-lg border text-xs transition',
                     n === pageNum ? 'bg-brand-primary text-brand-primary-ink border-brand-primary' : 'border-brand-line text-brand-ink-2 hover:text-brand-primary']">{{ n }}</button>
          <button @click="changePage(pageNum + 1)" :disabled="pageNum >= totalPages" class="px-3 h-8 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary disabled:opacity-30 transition">下一页</button>
          <button @click="changePage(totalPages)" :disabled="pageNum >= totalPages" class="px-3 h-8 rounded-lg border border-brand-line text-xs text-brand-ink-2 hover:text-brand-primary disabled:opacity-30 transition">末页</button>
        </div>
      </div>
    </div>

    <!-- 审核弹窗 -->
    <div v-if="auditModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="auditModal = false">
      <div class="bg-brand-surface rounded-2xl border border-brand-line w-full max-w-2xl max-h-[90vh] overflow-y-auto m-4">
        <!-- 弹窗头部 -->
        <div class="flex items-center justify-between p-5 border-b border-brand-line">
          <h3 class="text-lg font-bold text-brand-ink flex items-center gap-2">
            <ShieldCheck class="w-5 h-5 text-brand-primary" />审核处理
          </h3>
          <button @click="auditModal = false" class="text-brand-ink-3 hover:text-brand-error text-xl leading-none">&times;</button>
        </div>

        <div class="p-5 space-y-4">
          <!-- 商品信息 -->
          <div v-if="currentProduct" class="p-4 rounded-xl bg-brand-bg border border-brand-line space-y-3">
            <div class="flex items-start gap-4">
              <img v-if="currentProduct.imageUrls?.length" :src="currentProduct.imageUrls[0]"
                   class="w-20 h-20 rounded-lg object-cover border border-brand-line" />
              <div v-else class="w-20 h-20 rounded-lg bg-brand-surface-2 border border-brand-line flex items-center justify-center">
                <Package class="w-8 h-8 text-brand-ink-3/30" />
              </div>
              <div class="flex-1 space-y-1">
                <h4 class="font-semibold text-brand-ink">{{ currentProduct.title }}</h4>
                <div class="flex flex-wrap gap-x-4 gap-y-1 text-xs text-brand-ink-3">
                  <span>价格：<b class="text-brand-primary">¥{{ currentProduct.price }}</b></span>
                  <span>库存：{{ currentProduct.stock }}</span>
                  <span>成色：{{ currentProduct.conditionName }}</span>
                  <span>分类：{{ currentProduct.categoryName }}</span>
                </div>
                <p v-if="currentProduct.description" class="text-xs text-brand-ink-2 mt-2 line-clamp-3">{{ currentProduct.description }}</p>
              </div>
            </div>
            <div v-if="currentProduct.imageUrls?.length > 1" class="flex gap-2 overflow-x-auto">
              <img v-for="(url, i) in currentProduct.imageUrls" :key="i" :src="url"
                   class="w-16 h-16 rounded-md object-cover border border-brand-line shrink-0" />
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="currentRecord && currentRecord.targetType === 1 && !currentProduct" class="flex items-center justify-center py-8">
            <RefreshCw class="w-5 h-5 text-brand-primary animate-spin" />
            <span class="ml-2 text-sm text-brand-ink-3">加载商品详情...</span>
          </div>

          <!-- 非商品类型提示 -->
          <div v-if="currentRecord && currentRecord.targetType !== 1" class="p-4 rounded-xl bg-brand-bg border border-brand-line text-sm text-brand-ink-3">
            目标类型：{{ targetTypeMap[currentRecord?.targetType]?.name }}，ID：{{ currentRecord?.targetId }}
          </div>

          <!-- 审核操作 -->
          <div class="space-y-3">
            <div class="flex gap-2">
              <button @click="auditAction = 1"
                :class="['flex-1 h-11 rounded-lg border-2 text-sm font-semibold transition flex items-center justify-center gap-2',
                         auditAction === 1 ? 'bg-brand-success/10 border-brand-success text-brand-success' : 'border-brand-line text-brand-ink-3 hover:border-brand-success/40']">
                <CheckCircle2 class="w-4 h-4" />通过
              </button>
              <button @click="auditAction = 2"
                :class="['flex-1 h-11 rounded-lg border-2 text-sm font-semibold transition flex items-center justify-center gap-2',
                         auditAction === 2 ? 'bg-brand-error/10 border-brand-error text-brand-error' : 'border-brand-line text-brand-ink-3 hover:border-brand-error/40']">
                <XCircle class="w-4 h-4" />拒绝
              </button>
            </div>

            <!-- 拒绝理由（必填） -->
            <div v-if="auditAction === 2">
              <label class="text-xs font-medium text-brand-ink-2 block mb-1">拒绝理由 <span class="text-brand-error">*</span></label>
              <select v-model="auditReason" class="w-full h-10 px-3 rounded-lg bg-brand-bg border border-brand-line text-sm focus:outline-none focus:border-brand-primary">
                <option value="">请选择拒绝理由</option>
                <option value="标题违规">标题违规</option>
                <option value="图片不清晰">图片不清晰</option>
                <option value="图片与商品不符">图片与商品不符</option>
                <option value="价格异常">价格异常</option>
                <option value="描述不完整">描述不完整</option>
                <option value="疑似假冒商品">疑似假冒商品</option>
                <option value="分类错误">分类错误</option>
                <option value="其他">其他</option>
              </select>
            </div>

            <!-- 审核意见 -->
            <div>
              <label class="text-xs font-medium text-brand-ink-2 block mb-1">审核意见（给卖家的反馈）</label>
              <textarea v-model="auditComment" rows="3" placeholder="可填写审核意见、修改建议等..."
                class="w-full px-3 py-2 rounded-lg bg-brand-bg border border-brand-line text-sm focus:outline-none focus:border-brand-primary resize-none"></textarea>
            </div>
          </div>
        </div>

        <!-- 弹窗底部 -->
        <div class="flex items-center justify-end gap-3 p-5 border-t border-brand-line">
          <button @click="auditModal = false" class="px-5 h-10 rounded-lg border border-brand-line text-sm text-brand-ink-2 hover:text-brand-primary transition">取消</button>
          <button @click="submitAudit" :disabled="submitting"
            :class="['px-5 h-10 rounded-lg text-sm font-medium text-white transition flex items-center gap-2 disabled:opacity-50',
                     auditAction === 1 ? 'bg-brand-success hover:bg-brand-success/80' : 'bg-brand-error hover:bg-brand-error/80']">
            <RefreshCw v-if="submitting" class="w-4 h-4 animate-spin" />
            {{ auditAction === 1 ? '确认通过' : '确认拒绝' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <Transition name="toast">
      <div v-if="toast" :class="['fixed top-20 right-6 z-50 px-5 py-3 rounded-lg text-white text-sm shadow-lg flex items-center gap-2',
                                 toast.type === 'success' ? 'bg-brand-success' : 'bg-brand-error']">
        <component :is="toast.type === 'success' ? CheckCircle2 : XCircle" class="w-5 h-5" />
        {{ toast.msg }}
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
  transform: translateY(-10px);
}
</style>
