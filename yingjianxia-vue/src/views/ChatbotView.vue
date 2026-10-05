<template>
  <div class="min-h-screen bg-background text-foreground">
    <!-- 页面标题 -->
    <section class="border-b border-border bg-card">
      <div class="max-w-7xl mx-auto px-4 py-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 class="text-2xl font-bold flex items-center gap-3">
            <BotMessageSquare class="w-7 h-7 text-primary" />
            客服快捷回复
          </h1>
          <p class="text-sm text-muted-foreground mt-1">为店铺配置自动回复与快捷短语，提升响应效率</p>
        </div>
        <button type="button" class="btn-primary" @click="saveToast = true">
          <Save class="w-4 h-4" />
          <span>保存设置</span>
        </button>
      </div>
    </section>

    <div class="max-w-7xl mx-auto px-4 py-8">
      <div class="grid lg:grid-cols-3 gap-6 items-start">
        <!-- 左侧配置区 -->
        <div class="lg:col-span-2 space-y-6">
          <!-- 自动回复总开关 -->
          <section class="card-surface p-5">
            <div class="flex items-center justify-between gap-4">
              <div class="flex items-start gap-4">
                <div class="inline-flex items-center justify-center w-11 h-11 rounded-lg bg-primary/10 text-primary shrink-0">
                  <Zap class="w-5 h-5" />
                </div>
                <div>
                  <h2 class="text-base font-semibold">自动回复总开关</h2>
                  <p class="text-sm text-muted-foreground mt-0.5">开启后，系统将根据规则自动回复买家咨询</p>
                </div>
              </div>
              <label class="switch" aria-label="自动回复总开关">
                <input v-model="masterToggle" type="checkbox">
                <span class="switch-slider"></span>
              </label>
            </div>
          </section>

          <!-- 欢迎语设置 -->
          <section class="card-surface p-5">
            <div class="flex items-center gap-3 mb-5">
              <Hand class="w-5 h-5 text-primary" />
              <h2 class="text-base font-semibold">欢迎语设置</h2>
            </div>
            <div class="space-y-4">
              <div>
                <label class="block text-sm font-medium mb-2">首次咨询自动回复内容</label>
                <textarea v-model="welcomeMsg" rows="3" class="input-brand" placeholder="欢迎光临我的店铺，有什么可以帮您？"></textarea>
                <p class="text-xs text-muted-foreground mt-2">首次收到买家消息时自动发送，每位买家每天只触发一次</p>
              </div>
              <div class="flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-6">
                <label class="text-sm font-medium shrink-0">再次触发时间间隔</label>
                <div class="flex items-center gap-3 flex-1">
                  <input v-model.number="welcomeInterval" type="number" min="1" max="168" class="input-brand w-24 text-center">
                  <span class="text-sm text-muted-foreground">小时</span>
                </div>
              </div>
            </div>
          </section>

          <!-- 关键词回复 -->
          <section class="card-surface p-5">
            <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-5">
              <div class="flex items-center gap-3">
                <MessageSquareText class="w-5 h-5 text-primary" />
                <h2 class="text-base font-semibold">关键词回复</h2>
              </div>
              <button type="button" class="btn-secondary btn-sm" @click="addKeywordRow">
                <Plus class="w-4 h-4" />
                <span>添加规则</span>
              </button>
            </div>

            <div class="overflow-x-auto -mx-5 px-5">
              <table class="w-full min-w-[640px]">
                <thead>
                  <tr class="border-b border-border text-left">
                    <th class="pb-3 text-xs font-semibold text-muted-foreground uppercase tracking-wider">关键词</th>
                    <th class="pb-3 text-xs font-semibold text-muted-foreground uppercase tracking-wider">匹配类型</th>
                    <th class="pb-3 text-xs font-semibold text-muted-foreground uppercase tracking-wider">回复内容</th>
                    <th class="pb-3 text-xs font-semibold text-muted-foreground uppercase tracking-wider">状态</th>
                    <th class="pb-3 text-xs font-semibold text-muted-foreground uppercase tracking-wider text-right">操作</th>
                  </tr>
                </thead>
                <tbody class="text-sm">
                  <tr v-for="(k, idx) in keywordRules" :key="idx" class="border-b border-border/50">
                    <td class="py-3 pr-4">
                      <input v-model="k.keyword" type="text" class="input-brand h-9">
                    </td>
                    <td class="py-3 pr-4">
                      <select v-model="k.matchType" class="input-brand h-9 py-0">
                        <option value="exact">精确匹配</option>
                        <option value="fuzzy">模糊匹配</option>
                      </select>
                    </td>
                    <td class="py-3 pr-4">
                      <input v-model="k.reply" type="text" class="input-brand h-9">
                    </td>
                    <td class="py-3 pr-4">
                      <label class="switch h-6 w-10">
                        <input v-model="k.enabled" type="checkbox">
                        <span class="switch-slider !h-5"></span>
                      </label>
                    </td>
                    <td class="py-3 text-right">
                      <button type="button" class="inline-flex items-center justify-center w-8 h-8 rounded-md text-muted-foreground hover:text-rose-400 hover:bg-rose-500/12 transition" @click="removeKeyword(idx)">
                        <Trash2 class="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <!-- 快捷短语库 -->
          <section class="card-surface p-5">
            <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-5">
              <div class="flex items-center gap-3">
                <BookmarkPlus class="w-5 h-5 text-primary" />
                <h2 class="text-base font-semibold">快捷短语库</h2>
              </div>
              <button type="button" class="btn-secondary btn-sm" @click="addPhrase">
                <Plus class="w-4 h-4" />
                <span>新增短语</span>
              </button>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div v-for="(p, idx) in phrases" :key="idx" class="phrase-card p-4 group relative">
                <div class="text-sm leading-relaxed pr-8">{{ p.text }}</div>
                <button
                  type="button"
                  class="absolute top-2 right-2 w-7 h-7 rounded-md text-muted-foreground opacity-0 group-hover:opacity-100 hover:text-rose-400 hover:bg-rose-500/12 transition"
                  @click="removePhrase(idx)"
                >
                  <X class="w-4 h-4" />
                </button>
                <div class="mt-2 flex items-center gap-2">
                  <span v-for="t in p.tags" :key="t" class="tag">{{ t }}</span>
                </div>
              </div>
            </div>
          </section>
        </div>

        <!-- 右侧预览区 -->
        <div class="lg:col-span-1 lg:sticky lg:top-24">
          <div class="card-surface overflow-hidden">
            <div class="p-4 border-b border-border flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center">
                <Store class="w-5 h-5 text-primary" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-sm font-semibold">买家 · 硬件发烧友</div>
                <div class="text-xs text-primary flex items-center gap-1"><span class="w-1.5 h-1.5 rounded-full bg-primary"></span>在线</div>
              </div>
            </div>
            <div class="preview-scroll p-4 space-y-3 max-h-[500px] min-h-[400px] flex flex-col">
              <!-- 模拟消息 -->
              <div class="flex justify-start">
                <div class="max-w-[80%]">
                  <div class="chat-bubble-buyer px-3 py-2 text-sm">老板，这张显卡还有吗？</div>
                  <div class="text-xs text-muted-foreground mt-1">10:02</div>
                </div>
              </div>
              <div class="flex justify-end">
                <div class="max-w-[80%] text-right">
                  <div class="chat-bubble-bot px-3 py-2 text-sm inline-block text-left">{{ welcomeMsg }}</div>
                  <div class="text-xs text-muted-foreground mt-1">10:02 · 自动</div>
                </div>
              </div>
              <div class="flex justify-start">
                <div class="max-w-[80%]">
                  <div class="chat-bubble-buyer px-3 py-2 text-sm">在吗？能便宜点吗？</div>
                  <div class="text-xs text-muted-foreground mt-1">10:03</div>
                </div>
              </div>
              <div class="flex justify-end">
                <div class="max-w-[80%] text-right">
                  <div class="chat-bubble-bot px-3 py-2 text-sm inline-block text-left">您好，商品报价已是实价，支持平台担保验机，感谢理解~</div>
                  <div class="text-xs text-muted-foreground mt-1">10:03 · 关键词触发</div>
                </div>
              </div>
              <div class="flex justify-start">
                <div class="max-w-[80%]">
                  <div class="chat-bubble-buyer px-3 py-2 text-sm">包邮吗？</div>
                  <div class="text-xs text-muted-foreground mt-1">10:05</div>
                </div>
              </div>
            </div>
            <div class="p-3 border-t border-border">
              <div class="flex gap-2">
                <input type="text" placeholder="输入消息…" class="input-brand h-10 flex-1">
                <button type="button" class="btn-primary h-10 px-4 !w-auto">
                  <Send class="w-4 h-4" />
                </button>
              </div>
              <div class="mt-3 flex flex-wrap gap-2">
                <button
                  v-for="p in phrases.slice(0, 4)"
                  :key="p.id"
                  type="button"
                  class="text-xs px-3 py-1.5 rounded-full bg-surface-2 text-muted-foreground hover:text-foreground hover:bg-surface-3 transition"
                >
                  {{ p.text.slice(0, 14) }}{{ p.text.length > 14 ? '…' : '' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 使用提示 -->
          <div class="mt-6 p-4 rounded-lg bg-primary/5 border border-primary/20">
            <div class="flex items-start gap-3">
              <Lightbulb class="w-5 h-5 text-primary shrink-0 mt-0.5" />
              <div class="text-sm leading-relaxed">
                <p class="font-medium text-primary mb-1">使用小贴士</p>
                <p class="text-muted-foreground">在旺旺/千牛聊天窗口输入「/」可快速唤起短语面板，选中后一键发送。</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <Transition name="fade">
      <div v-if="saveToast" class="fixed bottom-8 left-1/2 -translate-x-1/2 z-50 px-5 py-3 rounded-lg bg-card border border-primary shadow-xl flex items-center gap-2">
        <Check class="w-4 h-4 text-primary" />
        <span class="text-sm">设置已保存</span>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  BotMessageSquare, Save, Zap, Hand, MessageSquareText, Plus,
  BookmarkPlus, X, Store, Send, Lightbulb, Check, Trash2
} from 'lucide-vue-next'

const saveToast = ref(false)
const masterToggle = ref(true)
const welcomeMsg = ref('您好，欢迎咨询！本店商品均经过实拍验机，支持平台担保交易，请问有什么可以帮您？')
const welcomeInterval = ref(24)

const keywordRules = ref([
  { keyword: '在吗', matchType: 'fuzzy', reply: '您好~欢迎光临，商品均为现货在售，支持验机担保~', enabled: true },
  { keyword: '包邮', matchType: 'fuzzy', reply: '亲，全场满500元包邮，偏远地区补差哦~', enabled: true },
  { keyword: '价格', matchType: 'exact', reply: '报价已是实价，不议价。多件购买可咨询组合优惠~', enabled: false }
])

const phrases = ref([
  { id: 1, text: '您好，商品均为现货实拍，支持平台验机，放心购买~', tags: ['通用', '首句'] },
  { id: 2, text: '感谢您的支持，发货后会第一时间同步物流单号，请留意查收。', tags: ['售后'] },
  { id: 3, text: '这款商品目前库存仅剩 3 件，需要的话可以尽快下单哦~', tags: ['促单'] },
  { id: 4, text: '亲，本店报价已是实价不议价，如有特殊需求可以私聊协商~', tags: ['议价'] }
])

let phraseId = 5

function addKeywordRow() {
  keywordRules.value.push({ keyword: '', matchType: 'fuzzy', reply: '', enabled: true })
}
function removeKeyword(idx) {
  keywordRules.value.splice(idx, 1)
}
function addPhrase() {
  phrases.value.push({ id: phraseId++, text: '新快捷短语…', tags: ['自定义'] })
}
function removePhrase(idx) {
  phrases.value.splice(idx, 1)
}
</script>

<style scoped>
@reference "../style.css";
.btn-primary {
  @apply inline-flex items-center justify-center gap-2 h-11 px-6 rounded-lg bg-primary text-primary-foreground font-semibold transition hover:bg-[var(--brand-primary-hover)] w-full sm:w-auto;
}
.btn-secondary {
  @apply inline-flex items-center justify-center gap-2 h-11 px-5 rounded-lg bg-transparent border border-border text-foreground font-semibold transition hover:bg-surface-2;
}
.btn-sm {
  @apply h-9 px-3.5 text-sm;
}
.card-surface {
  @apply bg-card border border-border rounded-xl;
}
.input-brand {
  @apply w-full h-11 px-3.5 rounded-lg bg-background border border-border text-foreground outline-none transition placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20 text-sm;
}
textarea.input-brand {
  @apply h-auto py-3 resize-y;
}
.switch {
  @apply relative inline-flex w-11 h-6 shrink-0;
}
.switch input {
  @apply opacity-0 w-0 h-0;
}
.switch-slider {
  @apply absolute cursor-pointer inset-0 bg-surface-3 rounded-full transition;
}
.switch-slider::before {
  content: '';
  @apply absolute h-[18px] w-[18px] left-0.5 bottom-0.5 bg-ink rounded-full transition;
  background: #F0F1F5;
}
.switch input:checked + .switch-slider {
  @apply bg-primary;
}
.switch input:checked + .switch-slider::before {
  transform: translateX(20px);
  background: #0B0C10;
}
.phrase-card {
  @apply border border-border rounded-lg bg-background transition cursor-pointer;
}
.phrase-card:hover {
  @apply border-primary;
}
.tag {
  @apply inline-flex items-center h-[22px] px-2 rounded-sm text-xs font-medium bg-surface-2 text-muted-foreground;
}
.chat-bubble-buyer {
  @apply bg-surface-3 text-foreground rounded-lg rounded-br-sm;
}
.chat-bubble-bot {
  @apply bg-primary text-primary-foreground rounded-lg rounded-bl-sm;
}
.preview-scroll {
  flex: 1;
  overflow-y: auto;
}
.preview-scroll::-webkit-scrollbar { width: 6px; }
.preview-scroll::-webkit-scrollbar-thumb { background: #2A2D35; border-radius: 999px; }
.fade-enter-active, .fade-leave-active { transition: opacity .3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
