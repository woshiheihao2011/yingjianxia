<template>
  <div class="min-h-screen bg-background text-foreground">
    <!-- 页面标题 + 提交按钮 -->
    <section class="border-b border-border">
      <div class="max-w-7xl mx-auto px-4 py-10 md:py-14">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 class="text-2xl md:text-3xl font-bold">客服工单</h1>
            <p class="text-muted-foreground mt-2">提交问题、查看进度、与客服沟通</p>
          </div>
          <a href="#new-ticket" class="btn-primary shrink-0">
            <PlusCircle class="w-5 h-5" />
            <span>提交新工单</span>
          </a>
        </div>
      </div>
    </section>

    <div class="max-w-7xl mx-auto px-4 py-8 md:py-10">
      <!-- 工单状态标签 -->
      <div class="flex items-center gap-2 overflow-x-auto no-scrollbar pb-2 mb-6">
        <button
          v-for="t in statusTabs"
          :key="t.value"
          type="button"
          class="ticket-tab"
          :class="{ 'active': activeStatus === t.value }"
          @click="activeStatus = t.value"
        >{{ t.label }}</button>
      </div>

      <!-- 工单列表 -->
      <section class="mb-10" aria-label="工单列表">
        <div class="space-y-4">
          <details
            v-for="(t, idx) in filteredTickets"
            :key="t.id"
            class="ticket-row"
            :open="idx === 0"
          >
            <summary class="ticket-summary list-none cursor-pointer">
              <div class="flex items-center gap-2">
                <Ticket class="w-4 h-4 text-muted-foreground" />
                <span class="font-mono text-sm">{{ t.id }}</span>
              </div>
              <div>
                <div class="font-medium">{{ t.title }}</div>
                <div class="text-xs text-muted-foreground mt-1">问题类型：{{ t.type }} · 关联订单：{{ t.order }}</div>
              </div>
              <div class="hidden lg:block text-sm text-muted-foreground">
                <div class="flex items-center gap-2"><Calendar class="w-3.5 h-3.5" /> {{ t.createdAt }}</div>
              </div>
              <div class="hidden md:block text-sm text-muted-foreground">
                <div class="flex items-center gap-2"><MessageCircle class="w-3.5 h-3.5" /> {{ t.lastReply }}</div>
              </div>
              <div class="flex md:justify-end">
                <span class="status-badge" :class="statusClass(t.status)">
                  <span class="w-1.5 h-1.5 rounded-full bg-current"></span>{{ t.statusLabel }}
                </span>
              </div>
            </summary>
            <div class="ticket-detail">
              <div class="py-4 grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div class="lg:col-span-2 space-y-4">
                  <div v-if="t.status !== 'resolved' && t.status !== 'closed'">
                    <h3 class="text-sm font-semibold mb-2">问题描述</h3>
                    <p class="text-sm text-muted-foreground leading-relaxed">{{ t.description }}</p>
                    <div v-if="t.attachments.length" class="flex flex-wrap gap-2 mt-3">
                      <a v-for="att in t.attachments" :key="att" href="#" class="inline-flex items-center gap-2 px-3 py-1.5 rounded-md bg-muted text-xs hover:bg-surface-3 transition">
                        <Paperclip class="w-3.5 h-3.5" /> {{ att }}
                      </a>
                    </div>

                    <!-- 聊天记录 -->
                    <div class="pt-4 mt-4 border-t border-border">
                      <h3 class="text-sm font-semibold mb-4">沟通记录</h3>
                      <div class="space-y-4">
                        <div v-for="(msg, mi) in t.messages" :key="mi" class="flex gap-3" :class="{ 'justify-end': msg.from === 'me' }">
                          <template v-if="msg.from !== 'me'">
                            <div class="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center shrink-0">
                              <Headset class="w-4 h-4 text-primary" />
                            </div>
                            <div class="max-w-[80%]">
                              <div class="chat-bubble-agent px-4 py-2.5 text-sm leading-relaxed">{{ msg.text }}</div>
                              <div class="text-xs text-muted-foreground mt-1">{{ msg.sender }} · {{ msg.time }}</div>
                            </div>
                          </template>
                          <template v-else>
                            <div class="max-w-[80%] text-right">
                              <div class="chat-bubble-user px-4 py-2.5 text-sm leading-relaxed inline-block text-left">{{ msg.text }}</div>
                              <div class="text-xs text-muted-foreground mt-1">我 · {{ msg.time }}</div>
                            </div>
                            <div class="w-8 h-8 rounded-full bg-surface-3 flex items-center justify-center shrink-0 text-primary text-xs font-semibold">我</div>
                          </template>
                        </div>
                      </div>
                    </div>

                    <!-- 补充回复 -->
                    <form class="pt-4 mt-4 border-t border-border" @submit.prevent="replyTicket(t.id)">
                      <label class="block text-sm font-semibold mb-2">补充回复</label>
                      <textarea v-model="replyText[t.id]" rows="2" class="reply-input" placeholder="补充描述或上传新附件…"></textarea>
                      <div class="flex items-center justify-between gap-3 mt-3">
                        <button type="button" class="inline-flex items-center gap-2 h-9 px-3 rounded-md border border-border text-sm text-muted-foreground hover:text-foreground hover:bg-muted transition">
                          <Paperclip class="w-4 h-4" /> 添加附件
                        </button>
                        <button type="submit" class="inline-flex items-center gap-2 h-9 px-4 rounded-md bg-primary text-primary-foreground text-sm font-semibold hover:bg-[var(--brand-primary-hover)] transition">
                          <span>发送</span>
                          <Send class="w-4 h-4" />
                        </button>
                      </div>
                    </form>
                  </div>
                  <div v-else>
                    <h3 class="text-sm font-semibold mb-2">{{ t.status === 'resolved' ? '处理结果' : '关闭原因' }}</h3>
                    <p class="text-sm text-muted-foreground leading-relaxed">{{ t.result }}</p>
                  </div>
                </div>
                <div class="lg:col-span-1">
                  <div class="p-4 rounded-md bg-card border border-border">
                    <h4 class="text-sm font-semibold mb-3">工单信息</h4>
                    <dl class="space-y-3 text-sm">
                      <div class="flex justify-between">
                        <dt class="text-muted-foreground">关联订单</dt>
                        <dd class="font-mono">{{ t.order }}</dd>
                      </div>
                      <div class="flex justify-between">
                        <dt class="text-muted-foreground">问题类型</dt>
                        <dd>{{ t.type }}</dd>
                      </div>
                      <div class="flex justify-between">
                        <dt class="text-muted-foreground">提交时间</dt>
                        <dd>{{ t.createdAt }}</dd>
                      </div>
                      <div class="flex justify-between">
                        <dt class="text-muted-foreground">最近回复</dt>
                        <dd>{{ t.lastReply }}</dd>
                      </div>
                    </dl>
                  </div>
                </div>
              </div>
            </div>
          </details>
        </div>
      </section>

      <!-- 提交新工单表单 -->
      <section id="new-ticket" class="mb-10">
        <div class="rounded-xl border border-border bg-card p-6 md:p-8">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 rounded-md bg-primary/20 flex items-center justify-center">
              <PlusCircle class="w-5 h-5 text-primary" />
            </div>
            <div>
              <h2 class="text-xl font-bold">提交新工单</h2>
              <p class="text-sm text-muted-foreground">请尽量详细描述问题，有助于客服快速处理</p>
            </div>
          </div>
          <form class="space-y-5" @submit.prevent="submitTicket">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div>
                <label class="block text-sm font-medium mb-2">问题类型</label>
                <select v-model="newForm.type" class="form-input form-select" required>
                  <option value="" disabled selected>请选择问题类型</option>
                  <option value="account">账号</option>
                  <option value="transaction">交易</option>
                  <option value="logistics">物流</option>
                  <option value="aftersales">售后</option>
                  <option value="complaint">投诉</option>
                  <option value="other">其他</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-medium mb-2">关联订单 <span class="text-muted-foreground font-normal">（选填）</span></label>
                <input v-model="newForm.order" type="text" placeholder="例如：ORD-20261128-8821" class="form-input">
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium mb-2">问题标题</label>
              <input v-model="newForm.title" type="text" placeholder="用一句话概括遇到的问题" class="form-input" required>
            </div>
            <div>
              <label class="block text-sm font-medium mb-2">详细描述</label>
              <textarea v-model="newForm.description" rows="5" placeholder="请描述问题发生的时间、操作步骤、期望结果与实际结果…" class="form-textarea" required></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium mb-2">上传图片 <span class="text-muted-foreground font-normal">（选填）</span></label>
              <label class="upload-zone cursor-pointer">
                <UploadCloud class="w-8 h-8 text-primary" />
                <span class="text-sm">点击或拖拽上传图片</span>
                <span class="text-xs text-muted-foreground">支持 JPG、PNG，最多 5 张，单张不超过 10MB</span>
                <input type="file" accept="image/*" multiple class="hidden">
              </label>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div>
                <label class="block text-sm font-medium mb-2">联系方式</label>
                <input v-model="newForm.contact" type="text" placeholder="手机号或邮箱，方便客服联系" class="form-input" required>
              </div>
              <div class="flex items-end">
                <p class="text-xs text-muted-foreground">客服会在工作时间内尽快处理，紧急问题请拨打客服热线。</p>
              </div>
            </div>
            <div class="flex items-center gap-3 pt-2">
              <button type="submit" class="btn-primary">
                <span>提交工单</span>
                <ArrowRight class="w-4 h-4" />
              </button>
              <button type="reset" class="btn-secondary" @click="newForm = { type:'', order:'', title:'', description:'', contact:'' }">重置</button>
            </div>
          </form>
        </div>
      </section>

      <!-- 常见问题快捷入口 -->
      <section>
        <h2 class="text-xl font-bold mb-5">常见问题</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <RouterLink v-for="f in faqLinks" :key="f.label" to="/help" class="faq-card">
            <component :is="f.icon" class="w-5 h-5 text-primary" />
            <span class="font-medium">{{ f.label }}</span>
          </RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import {
  Ticket, Calendar, MessageCircle, Paperclip, Headset, Send,
  PlusCircle, UploadCloud, ArrowRight, Repeat, Truck, Wallet, Shield
} from 'lucide-vue-next'

const activeStatus = ref('all')
const replyText = reactive({})

const statusTabs = [
  { label: '全部', value: 'all' },
  { label: '处理中', value: 'processing' },
  { label: '待回复', value: 'pending' },
  { label: '已解决', value: 'resolved' },
  { label: '已关闭', value: 'closed' }
]

const tickets = ref([
  {
    id: '#TK-20261201-01', status: 'processing', statusLabel: '处理中',
    title: '交易纠纷：显卡成色与描述不符', type: '交易', order: 'ORD-20261128-8821',
    createdAt: '2026-12-01 09:23', lastReply: '2小时前',
    description: '收到的 RTX 4070 显卡背板有明显划痕，与卖家描述的「九成新、无明显划痕」不符。我已上传收货开箱视频与多角度照片，请平台介入核实。',
    attachments: ['开箱视频.mp4', '背板划痕_01.jpg'],
    messages: [
      { from: 'agent', sender: '客服小侠', time: '2026-12-01 10:05', text: '您好，已收到您的工单。请提供收货时的完整开箱视频，并确认显卡 SN 码是否与商品详情页一致。' },
      { from: 'me', time: '2026-12-01 11:20', text: '已补充上传，麻烦查看。' },
      { from: 'agent', sender: '客服小侠', time: '2小时前', text: '收到，已转交售后专员处理，预计 24 小时内给出处理方案，请保持手机畅通。' }
    ]
  },
  {
    id: '#TK-20261125-03', status: 'pending', statusLabel: '待回复',
    title: '物流异常：快递超过 5 天未更新', type: '物流', order: 'ORD-20261120-3341',
    createdAt: '2026-11-25 16:40', lastReply: '1天前',
    description: '物流信息显示已揽收但 5 天没有更新，联系快递员未回复。请协助核实包裹状态。',
    attachments: [],
    messages: [
      { from: 'me', time: '2026-11-25 16:40', text: '快递已经 5 天没动了，麻烦帮我查一下。' }
    ]
  },
  {
    id: '#TK-20261110-07', status: 'resolved', statusLabel: '已解决',
    title: '账号问题：无法修改绑定手机号', type: '账号', order: '无',
    createdAt: '2026-11-10 11:05', lastReply: '2026-11-12',
    result: '已协助完成手机号换绑，旧手机号已解绑，新手机号可正常接收验证码。如您仍有问题，可重新提交工单。'
  },
  {
    id: '#TK-20261028-12', status: 'closed', statusLabel: '已关闭',
    title: '售后咨询：退货运费承担方式', type: '售后', order: 'ORD-20261025-1192',
    createdAt: '2026-10-28 14:22', lastReply: '2026-10-30',
    result: '因您超过 7 天未补充凭证，系统已自动关闭。如有需要请重新提交工单。'
  }
])

const filteredTickets = computed(() =>
  activeStatus.value === 'all' ? tickets.value : tickets.value.filter(t => t.status === activeStatus.value)
)

const newForm = ref({ type: '', order: '', title: '', description: '', contact: '' })

const faqLinks = [
  { label: '买卖流程', icon: Repeat },
  { label: '物流与售后', icon: Truck },
  { label: '支付与提现', icon: Wallet },
  { label: '账号安全', icon: Shield }
]

function statusClass(s) {
  return {
    processing: 'status-processing',
    pending: 'status-pending',
    resolved: 'status-resolved',
    closed: 'status-closed'
  }[s]
}
function replyTicket(id) {
  if (!replyText[id]?.trim()) return
  alert('回复已发送')
  replyText[id] = ''
}
function submitTicket() {
  alert('工单已提交，客服将尽快处理')
  newForm.value = { type: '', order: '', title: '', description: '', contact: '' }
}
</script>

<style scoped>
@reference "../style.css";
.btn-primary {
  @apply inline-flex items-center justify-center gap-2 h-11 px-6 rounded-md bg-primary text-primary-foreground font-semibold transition hover:bg-[var(--brand-primary-hover)];
}
.btn-secondary {
  @apply inline-flex items-center justify-center h-11 px-5 rounded-md border border-border text-foreground font-medium transition hover:bg-muted;
}
.ticket-tab {
  @apply inline-flex items-center h-9 px-4 rounded-full text-sm font-medium text-muted-foreground bg-transparent border border-transparent transition whitespace-nowrap;
}
.ticket-tab:hover {
  @apply text-foreground bg-surface-2;
}
.ticket-tab.active {
  @apply text-primary bg-primary/12 border-primary/25;
}
.ticket-row {
  @apply border border-border rounded-lg bg-card overflow-hidden transition;
}
.ticket-row[open] {
  @apply border-primary;
}
.ticket-summary {
  @apply grid grid-cols-1 gap-3 p-4 items-center;
}
@media (min-width: 768px) {
  .ticket-summary { grid-template-columns: 120px 1fr 140px 110px; }
}
@media (min-width: 1024px) {
  .ticket-summary { grid-template-columns: 120px 1fr 140px 120px 110px; }
}
.ticket-detail {
  @apply px-4 pb-4 border-t border-border;
}
.status-badge {
  @apply inline-flex items-center gap-1.5 h-6 px-2.5 rounded-full text-xs font-semibold;
}
.status-processing { @apply text-blue-400 bg-blue-500/12; }
.status-pending { @apply text-amber-400 bg-amber-500/12; }
.status-resolved { @apply text-emerald-400 bg-emerald-500/12; }
.status-closed { @apply text-muted-foreground bg-surface-3; }
.chat-bubble-user {
  @apply bg-primary text-primary-foreground rounded-lg rounded-br-sm;
}
.chat-bubble-agent {
  @apply bg-surface-3 text-foreground rounded-lg rounded-bl-sm;
}
.form-input {
  @apply w-full h-11 px-3.5 rounded-lg bg-background border border-border text-foreground outline-none transition placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20;
}
.form-textarea {
  @apply w-full min-h-[120px] p-3 rounded-lg bg-background border border-border text-foreground outline-none resize-y transition placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20;
}
.form-select {
  appearance: none;
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 36px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%236B7280' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
}
.upload-zone {
  @apply flex flex-col items-center justify-center gap-2 p-6 rounded-lg border border-dashed border-border bg-background text-muted-foreground transition;
}
.upload-zone:hover {
  @apply border-primary bg-primary/12;
}
.faq-card {
  @apply flex items-center gap-3 p-4 rounded-lg bg-card border border-border transition;
}
.faq-card:hover {
  @apply border-primary -translate-y-0.5;
}
.reply-input {
  @apply w-full min-h-[80px] p-2.5 rounded-lg bg-background border border-border text-foreground outline-none resize-y transition placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20;
}
</style>
