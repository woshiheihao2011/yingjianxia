<script setup>
import { ref, computed } from 'vue'
import {
  FilePlus, Clock, Plus, X, Upload, ChevronDown, ChevronRight,
  User, MessageSquare, CheckCircle, AlertTriangle, Package,
  FileText, ShieldCheck
} from 'lucide-vue-next'

// Tab 切换：申请售后 / 售后进度
const activeSection = ref('apply') // apply | progress

// ====== 申请售后 ======
const candidateOrders = [
  { id: 'ORD20260820004', title: '三星 980 Pro 1TB NVMe PCIe 4.0', date: '2026-08-20', price: 499,
    cover: 'https://placehold.co/120x120/15161A/00D4AA?text=980+Pro' },
  { id: 'ORD20260827002', title: 'Intel Core i5-13600K 盒装 成色准新', date: '2026-08-27', price: 1250,
    cover: 'https://placehold.co/120x120/15161A/00D4AA?text=13600K' },
  { id: 'ORD20260826003', title: '芝奇 DDR5 6000 32GB(16×2) 套装 焰锋戟', date: '2026-08-26', price: 1240,
    cover: 'https://placehold.co/120x120/15161A/00D4AA?text=DDR5' }
]
const selectedOrderId = ref('ORD20260820004')

const aftersalesTypes = [
  { id: 'refund', name: '仅退款', desc: '商品无需退回' },
  { id: 'return', name: '退货退款', desc: '退回商品后退款' },
  { id: 'exchange', name: '换货', desc: '与卖家协商更换' }
]
const selectedType = ref('refund')

const reasons = ['请选择原因', '商品与描述不符', '商品存在质量问题', '配件缺失 / 漏发', '疑似假冒 / 非正品', '运输途中损坏', '买错了 / 不需要了', '其他']
const selectedReason = ref('')
const refundAmount = ref('')
const maxRefund = computed(() => {
  const o = candidateOrders.find(x => x.id === selectedOrderId.value)
  return o ? o.price : 0
})
const description = ref('')

// 凭证上传
const uploadedFiles = ref([])
const onPickFiles = (e) => {
  const f = e.target.files
  if (!f) return
  for (const file of f) {
    if (uploadedFiles.value.length >= 6) break
    const url = URL.createObjectURL(file)
    uploadedFiles.value.push({ name: file.name, url })
  }
}
const removeFile = (i) => uploadedFiles.value.splice(i, 1)

const submitAftersales = () => {
  if (selectedReason.value === '' || selectedReason.value === '请选择原因') return alert('请选择申请原因')
  if (!Number(refundAmount.value) || Number(refundAmount.value) > maxRefund.value) return alert('请输入有效退款金额（最多 ' + maxRefund.value + '）')
  alert(`售后申请已提交：订单 ${selectedOrderId.value}，类型 ${selectedType.value}，退款 ${refundAmount.value} 元`)
}

// ====== 售后进度 ======
const progressList = [
  {
    id: 'AFT20260828001', orderNo: 'ORD20260820004', type: '仅退款',
    product: '三星 980 Pro 1TB NVMe PCIe 4.0', amount: 499, status: 'processing',
    cover: 'https://placehold.co/120x120/15161A/00D4AA?text=980+Pro',
    reason: '疑似假冒 / 非正品',
    createdAt: '2026-08-28 11:20',
    steps: [
      { title: '提交申请', time: '2026-08-28 11:20', done: true },
      { title: '卖家审核中', time: '预计 2026-08-29 11:20 前响应', done: false, current: true },
      { title: '平台处理', time: '', done: false },
      { title: '退款完成', time: '', done: false }
    ]
  },
  {
    id: 'AFT20260825012', orderNo: 'ORD20260818005', type: '退货退款',
    product: '猫头鹰 NH-D15 双塔散热器 侧透静音款', amount: 480, status: 'completed',
    cover: 'https://placehold.co/120x120/15161A/00D4AA?text=NH-D15',
    reason: '成色与描述不符',
    createdAt: '2026-08-25 14:10',
    steps: [
      { title: '提交申请', time: '2026-08-25 14:10', done: true },
      { title: '卖家审核', time: '2026-08-25 15:30 通过', done: true },
      { title: '平台处理 · 退货验收', time: '2026-08-26 09:15 已签收', done: true },
      { title: '退款完成', time: '2026-08-26 14:10 到账 +¥480.00', done: true }
    ]
  },
  {
    id: 'AFT20260820008', orderNo: 'ORD20260815002', type: '仅退款',
    product: '追风者 P600S 中塔机箱', amount: 60, status: 'rejected',
    cover: 'https://placehold.co/120x120/15161A/00D4AA?text=P600S',
    reason: '运输途中损坏',
    createdAt: '2026-08-20 09:00',
    rejectReason: '经核实，开箱视频未显示损坏痕迹，售后不成立。建议协商解决。',
    steps: [
      { title: '提交申请', time: '2026-08-20 09:00', done: true },
      { title: '卖家审核', time: '2026-08-20 11:00 拒绝', done: true, error: true },
      { title: '平台仲裁', time: '2026-08-21 18:00 维持原判', done: true, error: true }
    ]
  }
]
const statusBadge = (s) => ({
  processing: 'bg-brand-warning/12 text-brand-warning',
  completed: 'bg-brand-success/12 text-brand-success',
  rejected: 'bg-brand-error/12 text-brand-error'
}[s] || 'bg-brand-ink-2/12 text-brand-ink-2')
const statusText = (s) => ({ processing: '处理中', completed: '已完成', rejected: '已拒绝' }[s] || s)
</script>

<template>
  <div class="container-app space-y-6 pb-20">
    <!-- 标题 + Tab -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-brand-ink">售后 / 退款</h1>
        <p class="mt-1 text-sm text-brand-ink-2">提交售后申请、查看处理进度或与卖家协商</p>
      </div>
      <div class="inline-flex rounded-xl bg-brand-surface-2 p-1 border border-brand-line self-start" role="group">
        <button @click="activeSection = 'apply'"
          :class="['inline-flex items-center gap-2 h-9 px-4 rounded-lg text-sm font-medium transition',
                   activeSection === 'apply' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">
          <FilePlus class="h-4 w-4" /> 申请售后
        </button>
        <button @click="activeSection = 'progress'"
          :class="['inline-flex items-center gap-2 h-9 px-4 rounded-lg text-sm font-medium transition',
                   activeSection === 'progress' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">
          <Clock class="h-4 w-4" /> 售后进度
        </button>
      </div>
    </div>

    <!-- ============ 申请售后 ============ -->
    <section v-if="activeSection === 'apply'" class="grid gap-6 lg:grid-cols-[1fr_360px]">
      <!-- 左侧表单 -->
      <div class="rounded-2xl border border-brand-line bg-brand-surface p-4 md:p-6">
        <h2 class="mb-5 text-lg font-semibold text-brand-ink">填写售后申请</h2>
        <div class="space-y-5">
          <!-- 选择订单商品 -->
          <div>
            <label class="mb-2 block text-sm font-medium text-brand-ink">选择订单商品</label>
            <div class="space-y-3">
              <label v-for="o in candidateOrders" :key="o.id"
                :class="['flex items-center gap-3 p-3 rounded-xl border cursor-pointer transition',
                         selectedOrderId === o.id ? 'border-brand-primary bg-brand-primary/8' : 'border-brand-line bg-brand-surface-2 hover:border-brand-primary/50']">
                <input type="radio" :value="o.id" v-model="selectedOrderId" class="h-4 w-4 accent-brand-primary" />
                <img :src="o.cover" :alt="o.title" class="h-14 w-14 rounded-lg object-cover bg-brand-bg" />
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-medium text-brand-ink">{{ o.title }}</p>
                  <p class="text-xs text-brand-ink-2">订单号 {{ o.id }} · {{ o.date }}</p>
                  <p class="mt-0.5 font-mono text-sm text-brand-primary">¥{{ o.price.toLocaleString() }}</p>
                </div>
              </label>
            </div>
          </div>

          <!-- 售后类型 -->
          <div>
            <label class="mb-2 block text-sm font-medium text-brand-ink">售后类型</label>
            <div class="flex flex-wrap gap-3">
              <label v-for="t in aftersalesTypes" :key="t.id"
                :class="['inline-flex items-center gap-2 px-3.5 py-2.5 rounded-lg border cursor-pointer transition text-sm font-medium',
                         selectedType === t.id
                           ? 'border-brand-primary bg-brand-primary/12 text-brand-primary'
                           : 'border-brand-line bg-brand-surface-2 text-brand-ink-2 hover:border-brand-primary']">
                <input type="radio" :value="t.id" v-model="selectedType" class="h-4 w-4 accent-brand-primary" />
                <div class="leading-tight">
                  <p>{{ t.name }}</p>
                  <p class="text-xs font-normal opacity-70 mt-0.5">{{ t.desc }}</p>
                </div>
              </label>
            </div>
          </div>

          <!-- 原因 -->
          <div>
            <label class="mb-2 block text-sm font-medium text-brand-ink">申请原因</label>
            <select v-model="selectedReason"
              class="h-10 w-full rounded-lg border border-brand-line bg-brand-surface-2 px-3 text-sm text-brand-ink outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20">
              <option v-for="r in reasons" :key="r" :value="r">{{ r }}</option>
            </select>
          </div>

          <!-- 退款金额 -->
          <div>
            <label class="mb-2 block text-sm font-medium text-brand-ink">退款金额</label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-brand-ink-2">¥</span>
              <input v-model="refundAmount" type="number" :max="maxRefund" step="0.01"
                :placeholder="`最高可退 ¥${maxRefund.toLocaleString()}`"
                class="h-11 w-full rounded-lg border border-brand-line bg-brand-surface-2 pl-8 pr-4 text-brand-ink outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
            </div>
          </div>

          <!-- 详细描述 -->
          <div>
            <label class="mb-2 block text-sm font-medium text-brand-ink">详细描述</label>
            <textarea v-model="description" rows="4" placeholder="请详细描述问题，帮助卖家快速处理..."
              class="w-full rounded-lg border border-brand-line bg-brand-surface-2 p-3 text-sm text-brand-ink placeholder:text-brand-ink-3 outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20 resize-y"></textarea>
          </div>

          <!-- 凭证上传 -->
          <div>
            <label class="mb-2 block text-sm font-medium text-brand-ink">凭证上传（最多 6 张）</label>
            <div class="flex flex-wrap gap-3">
              <div v-for="(f, i) in uploadedFiles" :key="i" class="relative w-24 h-24 rounded-lg border border-brand-line bg-brand-surface-2 overflow-hidden">
                <img :src="f.url" class="w-full h-full object-cover" />
                <button @click="removeFile(i)" class="absolute top-1 right-1 inline-flex h-6 w-6 items-center justify-center rounded-md bg-black/60 text-white hover:bg-black/80">
                  <X class="h-3.5 w-3.5" />
                </button>
              </div>
              <label v-if="uploadedFiles.length < 6"
                class="w-24 h-24 rounded-lg border border-dashed border-brand-line bg-brand-surface-2 text-brand-ink-3 cursor-pointer hover:border-brand-primary hover:text-brand-primary transition flex flex-col items-center justify-center gap-1">
                <Upload class="h-5 w-5" />
                <span class="text-xs">添加图片</span>
                <input type="file" accept="image/*" multiple class="hidden" @change="onPickFiles" />
              </label>
            </div>
          </div>

          <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end pt-2 border-t border-brand-line">
            <button @click="activeSection = 'progress'"
              class="h-10 rounded-lg border border-brand-line bg-brand-surface px-4 text-sm font-medium text-brand-ink transition hover:border-brand-primary hover:text-brand-primary">查看进度</button>
            <button @click="submitAftersales"
              class="h-10 rounded-lg bg-brand-primary px-5 text-sm font-semibold text-brand-primary-ink transition hover:bg-brand-primary/90 inline-flex items-center justify-center gap-2">
              <FilePlus class="h-4 w-4" /> 提交申请
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧信息 -->
      <aside class="space-y-5">
        <div class="rounded-2xl border border-brand-line bg-brand-surface p-5">
          <h3 class="mb-3 font-semibold text-brand-ink">售后规则</h3>
          <ul class="space-y-2.5 text-sm text-brand-ink-2">
            <li class="flex gap-2"><CheckCircle class="h-4 w-4 text-brand-success mt-0.5 shrink-0" />签收后 <strong class="text-brand-ink">7 天</strong> 内可申请售后</li>
            <li class="flex gap-2"><CheckCircle class="h-4 w-4 text-brand-success mt-0.5 shrink-0" />卖家需在 <strong class="text-brand-ink">24 小时</strong> 内响应</li>
            <li class="flex gap-2"><CheckCircle class="h-4 w-4 text-brand-success mt-0.5 shrink-0" />逾期未处理，系统自动同意申请</li>
            <li class="flex gap-2"><CheckCircle class="h-4 w-4 text-brand-success mt-0.5 shrink-0" />协商未果，可申请平台介入仲裁</li>
          </ul>
        </div>
        <div class="rounded-2xl border border-brand-primary/30 bg-brand-primary/8 p-5">
          <div class="flex items-center gap-3">
            <ShieldCheck class="h-10 w-10 text-brand-primary shrink-0" />
            <div>
              <h3 class="font-semibold text-brand-ink">平台担保</h3>
              <p class="text-sm text-brand-ink-2 mt-1">资金由硬件侠平台托管，保障买卖双方权益。遇到纠纷，请提交完整证据。</p>
            </div>
          </div>
        </div>
      </aside>
    </section>

    <!-- ============ 售后进度 ============ -->
    <section v-else class="space-y-4">
      <article v-for="p in progressList" :key="p.id"
        class="rounded-2xl border border-brand-line bg-brand-surface overflow-hidden transition hover:border-brand-primary/40">
        <!-- 卡片头 -->
        <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between p-4 md:p-5 border-b border-brand-line bg-brand-surface/50">
          <div class="flex items-start gap-4">
            <img :src="p.cover" class="h-16 w-16 rounded-lg object-cover bg-brand-bg shrink-0" />
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-2 mb-1">
                <span class="font-mono text-xs text-brand-ink-2">售后 {{ p.id }}</span>
                <span class="text-xs text-brand-ink-3">·</span>
                <span class="text-xs text-brand-ink-3">{{ p.createdAt }} 提交</span>
                <span :class="['inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium', statusBadge(p.status)]">{{ statusText(p.status) }}</span>
              </div>
              <h3 class="font-medium text-brand-ink line-clamp-1">{{ p.product }}</h3>
              <p class="text-xs text-brand-ink-3 mt-0.5">订单 {{ p.orderNo }} · {{ p.type }} · 原因：{{ p.reason }}</p>
            </div>
          </div>
          <div class="md:text-right md:min-w-[120px]">
            <p class="text-xs text-brand-ink-2">申请金额</p>
            <p class="font-mono text-xl font-bold text-brand-primary">¥{{ p.amount.toLocaleString() }}</p>
          </div>
        </div>
        <!-- 时间轴 -->
        <div class="p-4 md:p-5">
          <div v-if="p.rejectReason" class="mb-4 rounded-lg border border-brand-error/30 bg-brand-error/8 p-3 text-sm text-brand-error flex gap-2 items-start">
            <AlertTriangle class="h-4 w-4 shrink-0 mt-0.5" />
            <div><strong>拒绝原因：</strong>{{ p.rejectReason }}</div>
          </div>
          <div class="relative pl-2">
            <div v-for="(s, i) in p.steps" :key="i" class="relative flex gap-4 pb-6 last:pb-0">
              <div v-if="i !== p.steps.length - 1"
                :class="['absolute left-[15px] top-[30px] w-0.5 bottom-0',
                         s.done ? (s.error ? 'bg-brand-error' : 'bg-brand-success') : 'bg-brand-line']"></div>
              <div :class="['relative z-10 flex h-[30px] w-[30px] items-center justify-center rounded-full shrink-0',
                s.done && !s.error ? 'bg-brand-success text-brand-primary-ink ring-4 ring-brand-success/20' :
                s.error ? 'bg-brand-error text-white ring-4 ring-brand-error/20' :
                s.current ? 'bg-brand-primary text-brand-primary-ink ring-4 ring-brand-primary/20 animate-pulse' :
                           'bg-brand-surface-2 text-brand-ink-3 ring-2 ring-brand-line']">
                <CheckCircle v-if="s.done && !s.error" class="h-3.5 w-3.5" />
                <AlertTriangle v-else-if="s.error" class="h-3.5 w-3.5" />
                <Clock v-else class="h-3.5 w-3.5" />
              </div>
              <div class="flex-1 min-w-0 pt-0.5">
                <h4 :class="['font-medium text-sm', !s.done && !s.current ? 'text-brand-ink-3' : 'text-brand-ink']">{{ s.title }}</h4>
                <p v-if="s.time" :class="['mt-0.5 text-xs', s.error ? 'text-brand-error' : 'text-brand-ink-2']">{{ s.time }}</p>
              </div>
            </div>
          </div>
        </div>
        <!-- 操作按钮 -->
        <div v-if="p.status === 'processing'" class="px-4 md:px-5 pb-5 flex flex-wrap gap-2 justify-end">
          <button class="inline-flex items-center gap-1.5 h-9 px-3 rounded-lg border border-brand-line bg-brand-surface text-sm font-medium text-brand-ink transition hover:border-brand-primary hover:text-brand-primary">
            <MessageSquare class="h-3.5 w-3.5" /> 联系卖家
          </button>
          <button class="inline-flex items-center gap-1.5 h-9 px-3 rounded-lg border border-brand-error/30 bg-brand-error/8 text-sm font-medium text-brand-error transition hover:bg-brand-error/15">
            <AlertTriangle class="h-3.5 w-3.5" /> 申请平台介入
          </button>
        </div>
      </article>
      <div v-if="!progressList.length" class="rounded-2xl border border-brand-line bg-brand-surface py-16 text-center">
        <FileText class="h-16 w-16 text-brand-ink-3 mx-auto" />
        <h3 class="mt-4 text-lg font-medium text-brand-ink">暂无售后记录</h3>
        <p class="mt-1 text-sm text-brand-ink-2">还没有提交过售后申请</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
@reference "../style.css";
.line-clamp-1 { display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
</style>
