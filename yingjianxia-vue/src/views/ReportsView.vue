<script setup>
import { ref, computed, reactive } from 'vue'
import {
  ShieldAlert, Info, UploadCloud, X, FileCheck, Gavel, MessageCircle,
  AlertTriangle, CheckCircle2, Clock, Eye, PlusCircle
} from 'lucide-vue-next'

// 举报类型标签
const reportTabs = [
  { key: 'all', label: '全部' },
  { key: 'product', label: '商品违规' },
  { key: 'fake', label: '虚假交易' },
  { key: 'fraud', label: '诈骗' },
  { key: 'stolen', label: '盗图' },
  { key: 'abuse', label: '恶意辱骂' },
  { key: 'other', label: '其他' },
]
const activeTab = ref('all')

// 举报原因选项（根据类型联动）
const reasonOptions = {
  product: ['发布虚假信息', '商品描述与实际不符', '发布违禁品', '价格欺诈', '其他'],
  fake: ['刷单虚假交易', '虚假好评', '虚假成交记录', '其他'],
  fraud: ['诱导线下交易', '收款不发货', '身份冒用', '钓鱼诈骗', '其他'],
  stolen: ['盗用他人商品图片', '盗用品牌宣传图', '盗用他人实拍图', '其他'],
  abuse: ['恶意辱骂骚扰', '人身攻击威胁', '发布歧视性言论', '其他'],
  other: ['其他违规行为'],
}

// 表单数据
const form = reactive({
  type: '',
  targetType: '',
  targetId: '',
  reason: '',
  description: '',
  anonymous: false,
})

// 图片上传
const uploadedImages = ref([
  { id: 1, url: 'https://images.unsplash.com/photo-1587202372775-e229f1e2816a?w=400&h=400&fit=crop' },
])

const handleFileChange = (e) => {
  const files = Array.from(e.target.files || [])
  files.forEach((file) => {
    if (uploadedImages.value.length >= 6) return
    const reader = new FileReader()
    reader.onload = (ev) => {
      uploadedImages.value.push({
        id: Date.now() + Math.random(),
        url: ev.target.result,
      })
    }
    reader.readAsDataURL(file)
  })
}

const removeImage = (id) => {
  uploadedImages.value = uploadedImages.value.filter((img) => img.id !== id)
}

// 当前可选的原因列表
const currentReasons = computed(() => {
  if (form.type && reasonOptions[form.type]) {
    return reasonOptions[form.type]
  }
  return ['发布虚假信息', '盗用他人图片', '价格欺诈', '诱导线下交易', '恶意辱骂/骚扰', '发布违禁品', '身份冒用', '其他']
})

// 重置表单
const resetForm = () => {
  form.type = ''
  form.targetType = ''
  form.targetId = ''
  form.reason = ''
  form.description = ''
  form.anonymous = false
  uploadedImages.value = []
}

// 提交举报
const submitReport = () => {
  if (!form.type || !form.targetType || !form.targetId || !form.reason || !form.description) {
    alert('请完整填写必填项')
    return
  }
  alert('举报已提交，平台将在 1-3 个工作日内审核处理')
  resetForm()
}

// 举报记录
const reportRecords = [
  {
    id: 'RPT202608201432',
    date: '2026-08-20 14:32',
    type: '诈骗',
    typeKey: 'fraud',
    target: '用户「数码老张」',
    reason: '诱导线下交易并收款不发货',
    status: 'processing',
    statusText: '受理中',
    result: '平台已冻结该账号，正在进一步核实资金流向。',
  },
  {
    id: 'RPT202608150918',
    date: '2026-08-15 09:18',
    type: '盗图',
    typeKey: 'stolen',
    target: '商品「RTX 4070 显卡」',
    reason: '商品图片盗用其他卖家实拍图',
    status: 'resolved',
    statusText: '已处理',
    result: '商品已下架，卖家被扣 6 分信用分。',
  },
  {
    id: 'RPT202608102105',
    date: '2026-08-10 21:05',
    type: '商品违规',
    typeKey: 'product',
    target: '商品「DDR5 6000 内存套装」',
    reason: '商品描述与实际规格不符',
    status: 'pending',
    statusText: '待处理',
    result: '排队审核中，预计 24 小时内反馈。',
  },
]

// 过滤后的记录
const filteredRecords = computed(() => {
  if (activeTab.value === 'all') return reportRecords
  return reportRecords.filter((r) => r.typeKey === activeTab.value)
})

const statusClass = (s) => {
  const map = {
    pending: 'bg-amber-500/15 text-amber-400',
    processing: 'bg-blue-500/15 text-blue-400',
    resolved: 'bg-emerald-500/15 text-emerald-400',
  }
  return map[s] || 'bg-zinc-500/15 text-zinc-400'
}

const statusIcon = (s) => {
  const map = { pending: Clock, processing: AlertTriangle, resolved: CheckCircle2 }
  return map[s] || Clock
}
</script>

<template>
  <div class="min-h-screen bg-background pb-20">
    <!-- 页面标题 + 举报须知 -->
    <section class="bg-background border-b border-border">
      <div class="container-app px-4 py-12 md:py-16">
        <div class="max-w-3xl">
          <div class="flex items-center gap-3 mb-4">
            <ShieldAlert class="w-8 h-8 text-primary" />
            <h1 class="text-3xl md:text-4xl font-bold text-foreground">举报中心</h1>
          </div>
          <p class="text-muted-foreground mb-6">
            维护公平交易环境，共同打击违规与欺诈行为。平台承诺对举报信息严格保密，并在 1-3 个工作日内完成审核。
          </p>
          <div class="rounded-xl bg-primary/10 border border-primary/30 p-4 md:p-5">
            <div class="flex items-start gap-3">
              <Info class="w-5 h-5 text-primary flex-shrink-0 mt-0.5" />
              <div class="text-sm leading-relaxed">
                <p class="font-medium text-foreground mb-2">举报须知</p>
                <ul class="list-disc list-inside space-y-1 text-muted-foreground">
                  <li>请如实填写举报信息，恶意举报将承担相应责任。</li>
                  <li>上传的证据图片需清晰可见，建议包含商品截图、聊天记录、订单号等。</li>
                  <li>涉及诈骗、盗图等严重违规行为，平台将优先处理并同步警方。</li>
                  <li>处理结果将通过站内信通知，可在「我的举报记录」中查看进度。</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 举报类型标签 -->
    <section class="bg-background border-b border-border sticky top-16 z-20 backdrop-blur">
      <div class="container-app px-4 py-4">
        <div class="flex items-center gap-2 overflow-x-auto no-scrollbar" role="tablist">
          <button
            v-for="tab in reportTabs"
            :key="tab.key"
            role="tab"
            :aria-selected="activeTab === tab.key"
            @click="activeTab = tab.key"
            :class="[
              'inline-flex items-center h-9 px-4 rounded-full text-sm font-medium whitespace-nowrap transition-all border',
              activeTab === tab.key
                ? 'text-primary bg-primary/10 border-primary'
                : 'text-muted-foreground bg-surface border-border hover:text-foreground hover:bg-surface-2',
            ]"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>
    </section>

    <!-- 主体内容 -->
    <section class="container-app px-4 py-12 md:py-16">
      <div class="grid grid-cols-1 lg:grid-cols-5 gap-8">
        <!-- 左侧：提交表单 -->
        <div class="lg:col-span-3">
          <h2 class="text-xl md:text-2xl font-bold text-foreground mb-6">提交举报</h2>
          <form
            class="rounded-2xl bg-card border border-border p-5 md:p-8 space-y-6"
            @submit.prevent="submitReport"
          >
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-medium text-foreground mb-2">
                  举报类型<span class="text-red-400"> *</span>
                </label>
                <select
                  v-model="form.type"
                  class="w-full h-11 px-3.5 rounded-lg bg-background border border-border text-foreground outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
                >
                  <option value="">请选择举报类型</option>
                  <option value="product">商品违规</option>
                  <option value="fake">虚假交易</option>
                  <option value="fraud">诈骗</option>
                  <option value="stolen">盗图</option>
                  <option value="abuse">恶意辱骂</option>
                  <option value="other">其他</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-medium text-foreground mb-2">
                  被举报对象<span class="text-red-400"> *</span>
                </label>
                <select
                  v-model="form.targetType"
                  class="w-full h-11 px-3.5 rounded-lg bg-background border border-border text-foreground outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
                >
                  <option value="">请选择对象类型</option>
                  <option value="product">商品</option>
                  <option value="user">用户</option>
                  <option value="order">订单</option>
                </select>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-foreground mb-2">
                对象编号 / 链接<span class="text-red-400"> *</span>
              </label>
              <input
                v-model="form.targetId"
                type="text"
                placeholder="例如商品编号、用户昵称、订单号或商品链接"
                class="w-full h-11 px-3.5 rounded-lg bg-background border border-border text-foreground outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 placeholder:text-muted-foreground"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-foreground mb-2">
                举报原因<span class="text-red-400"> *</span>
              </label>
              <select
                v-model="form.reason"
                class="w-full h-11 px-3.5 rounded-lg bg-background border border-border text-foreground outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
              >
                <option value="">请选择具体原因</option>
                <option v-for="r in currentReasons" :key="r" :value="r">{{ r }}</option>
              </select>
            </div>

            <div>
              <label class="block text-sm font-medium text-foreground mb-2">
                详细描述<span class="text-red-400"> *</span>
              </label>
              <textarea
                v-model="form.description"
                placeholder="请详细说明事件经过，包括时间、涉及金额、沟通记录等，便于平台快速核实。"
                rows="5"
                class="w-full px-3.5 py-3 rounded-lg bg-background border border-border text-foreground outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 placeholder:text-muted-foreground resize-none"
              ></textarea>
              <p class="mt-2 text-xs text-muted-foreground">建议 50-500 字，客观描述事实，避免情绪化表达。</p>
            </div>

            <!-- 证据上传区 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-2">上传证据图片</label>
              <label
                class="flex flex-col items-center justify-center gap-3 p-8 rounded-lg bg-background border-2 border-dashed border-border text-muted-foreground text-center cursor-pointer transition hover:border-primary hover:bg-primary/5"
              >
                <UploadCloud class="w-10 h-10 text-primary" />
                <div>
                  <p class="font-medium text-foreground">点击或拖拽上传证据</p>
                  <p class="text-sm mt-1">支持 JPG、PNG、GIF，单张不超过 10MB，最多 6 张</p>
                </div>
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  class="hidden"
                  @change="handleFileChange"
                />
              </label>
              <div v-if="uploadedImages.length" class="grid grid-cols-3 sm:grid-cols-4 gap-3 mt-4">
                <div
                  v-for="img in uploadedImages"
                  :key="img.id"
                  class="relative aspect-square rounded-lg overflow-hidden border border-border group"
                >
                  <img :src="img.url" alt="证据" class="w-full h-full object-cover" />
                  <button
                    type="button"
                    @click="removeImage(img.id)"
                    class="absolute top-1.5 right-1.5 w-6 h-6 rounded bg-red-500 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition"
                    aria-label="删除图片"
                  >
                    <X class="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
              <div class="mt-4 p-4 rounded-lg bg-muted border border-border">
                <p class="text-sm font-medium text-foreground mb-2 flex items-center gap-2">
                  <FileCheck class="w-4 h-4 text-primary" />
                  证据要求
                </p>
                <ul class="text-sm text-muted-foreground space-y-1 list-disc list-inside">
                  <li>商品页面截图需包含价格、描述、卖家信息。</li>
                  <li>聊天记录需完整展示时间、双方账号与关键对话。</li>
                  <li>支付凭证需包含交易单号、金额与收款方。</li>
                  <li>图片不得遮挡、涂抹关键信息，确保审核人员可辨。</li>
                </ul>
              </div>
            </div>

            <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pt-2">
              <label class="inline-flex items-center gap-2.5 cursor-pointer text-sm text-muted-foreground">
                <input
                  v-model="form.anonymous"
                  type="checkbox"
                  class="w-4.5 h-4.5 rounded border-border bg-background text-primary focus:ring-primary/30"
                />
                <span>匿名举报（平台仍可在必要时联系您核实）</span>
              </label>
              <div class="flex items-center gap-3">
                <button
                  type="button"
                  @click="resetForm"
                  class="h-9 px-4 rounded-lg bg-transparent text-foreground border border-border text-sm font-medium transition hover:bg-surface-2"
                >
                  重置
                </button>
                <button
                  type="submit"
                  class="h-9 px-5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold transition hover:bg-primary/90 active:translate-y-px"
                >
                  提交举报
                </button>
              </div>
            </div>
          </form>
        </div>

        <!-- 右侧：我的举报记录 -->
        <div class="lg:col-span-2">
          <h2 class="text-xl md:text-2xl font-bold text-foreground mb-6">我的举报记录</h2>

          <div v-if="!filteredRecords.length" class="rounded-2xl bg-card border border-border p-10 text-center">
            <Eye class="w-12 h-12 text-muted-foreground mx-auto mb-4" />
            <p class="text-muted-foreground">该分类下暂无举报记录</p>
          </div>

          <div v-else class="space-y-4">
            <div
              v-for="record in filteredRecords"
              :key="record.id"
              class="rounded-xl bg-card border border-border p-5 transition hover:border-primary/40"
            >
              <div class="flex items-start justify-between gap-3 mb-3">
                <div>
                  <p class="text-xs text-muted-foreground">{{ record.date }}</p>
                  <p class="font-medium text-foreground mt-1">{{ record.type }}</p>
                </div>
                <span
                  :class="[
                    'inline-flex items-center gap-1.5 h-7 px-3 rounded-full text-xs font-medium',
                    statusClass(record.status),
                  ]"
                >
                  <component :is="statusIcon(record.status)" class="w-3.5 h-3.5" />
                  {{ record.statusText }}
                </span>
              </div>
              <p class="text-sm text-muted-foreground mb-1">
                被举报对象：<span class="text-foreground">{{ record.target }}</span>
              </p>
              <p class="text-sm text-muted-foreground mb-3">原因：{{ record.reason }}</p>
              <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pt-3 border-t border-border">
                <span class="text-xs text-muted-foreground">处理结果：{{ record.result }}</span>
                <button
                  type="button"
                  class="h-8 px-3.5 rounded-md bg-transparent text-foreground border border-border text-xs font-medium transition hover:bg-surface-2 self-start sm:self-auto"
                >
                  补充材料
                </button>
              </div>
            </div>
          </div>

          <!-- 处理结果示例 -->
          <div class="mt-8">
            <h3 class="text-lg font-bold text-foreground mb-4 flex items-center gap-2">
              <PlusCircle class="w-5 h-5 text-primary" />
              处理结果示例
            </h3>
            <div class="rounded-xl bg-surface-2 border border-border p-5 space-y-4">
              <div class="flex items-start gap-3 p-4 rounded-lg bg-red-500/10 border border-red-500/30">
                <Gavel class="w-5.5 h-5.5 text-red-400 flex-shrink-0 mt-0.5" />
                <div>
                  <p class="font-medium text-foreground">处罚措施</p>
                  <p class="text-sm mt-1 text-muted-foreground">
                    该商品已下架，卖家账号被限制发布 7 天，信用分扣减 6 分。情节严重者将永久封禁并移交相关部门。
                  </p>
                </div>
              </div>
              <div class="flex items-start gap-3 p-4 rounded-lg bg-blue-500/10 border border-blue-500/30">
                <MessageCircle class="w-5.5 h-5.5 text-blue-400 flex-shrink-0 mt-0.5" />
                <div>
                  <p class="font-medium text-foreground">平台提示</p>
                  <p class="text-sm mt-1 text-muted-foreground">
                    如您的权益受到损失，可在订单详情页申请售后或联系客服发起担保交易赔付。请保留好沟通与支付凭证。
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
