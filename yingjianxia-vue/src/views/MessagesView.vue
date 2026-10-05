<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search, ArrowLeft, ChevronRight, Send, ImagePlus, MoreVertical,
  Phone, Video, ShieldCheck, Clock, Package, Star, Bell, UserX
} from 'lucide-vue-next'

const router = useRouter()

// Tab 分类：全部 / 买家 / 卖家 / 系统
const tabs = [
  { id: 'all', name: '全部', count: 6 },
  { id: 'buyer', name: '买家消息', count: 3 },
  { id: 'seller', name: '卖家消息', count: 2 },
  { id: 'system', name: '系统通知', count: 1 }
]
const activeTab = ref('all')

// 搜索
const searchKw = ref('')

// 会话列表
const conversations = ref([
  {
    id: 'C1', name: '数码老张', category: 'buyer', online: true, lastOnline: '3分钟前在线',
    avatar: 'https://placehold.co/96x96/3B82F6/0B0C10?text=老张',
    lastText: '好的，箱说全，上海本地可以自提哦，有问题随时联系。',
    lastTime: '刚刚', unread: 5, pinned: true,
    product: { id: 'p1', title: '七彩虹 RTX 4070 战斧豪华版 成色95新', price: 3680, cover: 'https://placehold.co/120x120/15161A/00D4AA?text=RTX4070' },
    messages: [
      { me: false, text: '你好，这块 RTX 4070 还在吗？', time: '10:38', type: 'text' },
      { me: true, text: '在的，箱说全，上海本地可以自提。', time: '10:39', type: 'text' },
      { me: false, text: '能否小刀？同城我自提。', time: '10:42', type: 'text' },
      { me: true, type: 'image', image: 'https://placehold.co/320x240/1E2026/00D4AA?text=%E6%98%BE%E5%8D%A1%E5%AE%9E%E6%8B%8D', time: '10:45' },
      { me: true, text: '给你拍了实拍，成色真的新，最低 3600。', time: '10:45', type: 'text' },
      { me: false, text: '好的，箱说全，上海本地可以自提哦，有问题随时联系。', time: '10:48', type: 'text' }
    ]
  },
  {
    id: 'C2', name: '极客阿杰', category: 'seller', online: true, lastOnline: '在线',
    avatar: 'https://placehold.co/96x96/00D4AA/0B0C10?text=AJ',
    lastText: '明天下午可以当面验机。',
    lastTime: '昨天', unread: 2, pinned: false,
    product: { id: 'p2', title: 'Intel i7-12700K 散片 无磕碰 成色准新', price: 2280, cover: 'https://placehold.co/120x120/15161A/00D4AA?text=12700K' },
    messages: [
      { me: false, text: '老板 i7-12700K 还在吗？', time: '昨天 19:20', type: 'text' },
      { me: true, text: '还在的，兄弟要的话今天就能发。', time: '昨天 19:22', type: 'text' },
      { me: false, text: '明天下午可以当面验机。', time: '昨天 20:05', type: 'text' }
    ]
  },
  {
    id: 'C3', name: '装机小队', category: 'seller', online: false, lastOnline: '2小时前在线',
    avatar: 'https://placehold.co/96x96/F59E0B/0B0C10?text=小队',
    lastText: '好的，发货前我再拍视频给你。',
    lastTime: '周一', unread: 0, pinned: false,
    product: { id: 'p3', title: '芝奇 DDR5 6000 32GB(16×2) 套装', price: 1240, cover: 'https://placehold.co/120x120/15161A/00D4AA?text=DDR5' },
    messages: [
      { me: true, text: '麻烦包装严实一点哦，怕震。', time: '周一 10:00', type: 'text' },
      { me: false, text: '好的，发货前我再拍视频给你。', time: '周一 10:12', type: 'text' }
    ]
  },
  {
    id: 'C4', name: '硬件小白', category: 'buyer', online: false, lastOnline: '12/20',
    avatar: 'https://placehold.co/96x96/EF4444/0B0C10?text=小白',
    lastText: '谢谢老哥，已经收到了。',
    lastTime: '12/20', unread: 0, pinned: false,
    product: { id: 'p4', title: '金士顿 FURY Beast DDR5 6000 32GB', price: 899, cover: 'https://placehold.co/120x120/15161A/00D4AA?text=DDR5-2' },
    messages: [
      { me: false, text: '老哥请问这个内存条和 B660 兼容吗？', time: '12/20 09:00', type: 'text' },
      { me: true, text: '兼容的，放心买。', time: '12/20 09:05', type: 'text' },
      { me: false, text: '谢谢老哥，已经收到了。', time: '12/20 18:30', type: 'text' }
    ]
  },
  {
    id: 'C5', name: '订单中心', category: 'system', online: false, lastOnline: '系统通知',
    avatar: 'https://placehold.co/96x96/6B7280/0B0C10?text=系',
    lastText: '订单 ORD20260828001 买家已付款，请尽快发货。',
    lastTime: '10分钟前', unread: 1, pinned: true, isSystem: true,
    product: null,
    messages: [
      { me: false, type: 'system-card', system: { title: '买家已付款', desc: '订单 ORD20260828001，金额 ¥3,680.00，买家已付款。', linkText: '去发货' }, time: '10:00' },
      { me: false, text: '订单 ORD20260828001 买家已付款，请尽快发货。', time: '10:00', type: 'text' }
    ]
  },
  {
    id: 'C6', name: '风控侠', category: 'system', online: false, lastOnline: '安全中心',
    avatar: 'https://placehold.co/96x96/10B981/0B0C10?text=风控',
    lastText: '检测到一次异地登录，如非本人操作请及时修改密码。',
    lastTime: '昨天', unread: 0, pinned: false, isSystem: true,
    product: null,
    messages: [
      { me: false, type: 'system-card', system: { title: '安全提醒', desc: '检测到一次异地登录（上海 → 北京），如非本人操作请及时修改密码。', linkText: '查看详情' }, time: '昨天 09:30' }
    ]
  }
])

// 当前选中会话
const activeConvId = ref('C1')
const activeConv = computed(() => conversations.value.find(c => c.id === activeConvId.value) || conversations.value[0])

// 列表过滤
const filteredConvs = computed(() => {
  let list = conversations.value
  if (activeTab.value !== 'all') list = list.filter(c => c.category === activeTab.value)
  const kw = searchKw.value.trim()
  if (kw) list = list.filter(c => c.name.includes(kw) || c.lastText.includes(kw))
  return list.sort((a, b) => (b.pinned - a.pinned))
})
const totalUnread = computed(() => conversations.value.reduce((s, c) => s + (c.unread || 0), 0))

// 移动端视图：显示列表 or 聊天
const showChatMobile = ref(false)
const onPickConv = (c) => {
  activeConvId.value = c.id
  c.unread = 0
  showChatMobile.value = true
  nextTick(() => scrollToBottom())
}
const backToList = () => { showChatMobile.value = false }

// 发送消息
const inputText = ref('')
const chatScrollRef = ref(null)
const scrollToBottom = () => {
  if (chatScrollRef.value) chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
}
const sendMessage = () => {
  const text = inputText.value.trim()
  if (!text) return
  activeConv.value.messages.push({ me: true, text, time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }), type: 'text' })
  inputText.value = ''
  nextTick(() => scrollToBottom())
  // 模拟回复
  setTimeout(() => {
    activeConv.value.messages.push({
      me: false, text: '收到~我这边看看，稍等。',
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }), type: 'text'
    })
    nextTick(() => scrollToBottom())
  }, 1000)
}
const pickImage = () => {
  const url = 'https://placehold.co/280x210/1E2026/00D4AA?text=%E5%8F%91%E9%80%81%E5%9B%BE%E7%89%87'
  activeConv.value.messages.push({
    me: true, type: 'image', image: url,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  })
  nextTick(() => scrollToBottom())
}

onMounted(() => scrollToBottom())

const fmt = (n) => '¥' + Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
</script>

<template>
  <div class="container-app !p-0 !max-w-none">
    <!-- 整体 shell -->
    <div class="mx-auto max-w-7xl h-[calc(100vh-64px)] md:h-[calc(100vh-64px)] md:min-h-[760px] mt-0 md:mt-0 border-x border-brand-line flex bg-brand-surface overflow-hidden relative">

      <!-- ========== 左侧会话列表 ========== -->
      <aside
        :class="['flex flex-col border-r border-brand-line transition-all duration-300 bg-brand-surface absolute md:static inset-0 z-10 w-full md:w-[320px]',
                 showChatMobile ? '-translate-x-full md:translate-x-0' : 'translate-x-0 md:translate-x-0']">
        <!-- 标题区 -->
        <div class="p-4 border-b border-brand-line">
          <div class="flex items-center justify-between mb-3">
            <h2 class="text-lg font-bold text-brand-ink">消息中心</h2>
            <div class="flex items-center gap-1">
              <span class="relative inline-flex items-center justify-center w-9 h-9 rounded-lg text-brand-ink-2 hover:text-brand-ink hover:bg-brand-surface-2 cursor-pointer">
                <Bell class="h-4.5 w-4.5" />
                <span v-if="totalUnread" class="absolute top-1 right-1 min-w-4 h-4 px-1 rounded-full bg-brand-error text-[10px] text-white flex items-center justify-center font-bold">{{ totalUnread > 99 ? '99+' : totalUnread }}</span>
              </span>
              <span class="inline-flex items-center justify-center w-9 h-9 rounded-lg text-brand-ink-2 hover:text-brand-ink hover:bg-brand-surface-2 cursor-pointer">
                <MoreVertical class="h-4.5 w-4.5" />
              </span>
            </div>
          </div>
          <!-- 搜索框 -->
          <div class="relative mb-3">
            <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-brand-ink-3" />
            <input v-model="searchKw" type="search" placeholder="搜索联系人或消息..."
              class="h-9 w-full rounded-lg border border-brand-line bg-brand-bg pl-9 pr-3 text-sm text-brand-ink placeholder:text-brand-ink-3 outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
          </div>
          <!-- 分类 Tab -->
          <div class="flex items-center gap-1 overflow-x-auto no-scrollbar">
            <button v-for="t in tabs" :key="t.id" @click="activeTab = t.id"
              :class="['inline-flex items-center h-8 px-3 rounded-full text-xs font-medium whitespace-nowrap transition border',
                       activeTab === t.id ? 'bg-brand-primary/12 text-brand-primary border-brand-primary/25'
                                          : 'text-brand-ink-2 border-transparent hover:bg-brand-surface-2 hover:text-brand-ink']">
              {{ t.name }}
              <span v-if="t.count" class="ml-1.5 opacity-80">({{ t.count }})</span>
            </button>
          </div>
        </div>
        <!-- 会话列表 -->
        <div class="flex-1 overflow-y-auto divide-y divide-brand-line/70">
          <div v-for="c in filteredConvs" :key="c.id"
            @click="onPickConv(c)"
            :class="['relative flex items-start gap-3 px-4 py-3 cursor-pointer transition',
                     activeConvId === c.id ? 'bg-brand-primary/8' : 'hover:bg-brand-surface-2',
                     c.pinned && 'bg-brand-surface-2/60']">
            <div class="relative shrink-0">
              <img :src="c.avatar" :alt="c.name" class="w-11 h-11 rounded-full object-cover bg-brand-bg" />
              <span v-if="c.online && !c.isSystem" class="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-brand-success ring-2 ring-brand-surface"></span>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between gap-2">
                <span class="font-semibold text-brand-ink truncate">{{ c.name }}</span>
                <span class="text-[11px] text-brand-ink-3 shrink-0">{{ c.lastTime }}</span>
              </div>
              <div class="flex items-center justify-between gap-2 mt-1">
                <span class="text-sm text-brand-ink-2 truncate">{{ c.lastText }}</span>
                <span v-if="c.unread" class="min-w-5 h-5 px-1.5 rounded-full bg-brand-error text-[10px] text-white flex items-center justify-center font-bold shrink-0">
                  {{ c.unread > 99 ? '99+' : c.unread }}
                </span>
              </div>
            </div>
          </div>
          <div v-if="!filteredConvs.length" class="py-16 text-center text-sm text-brand-ink-3">
            <div class="flex justify-center mb-2"><Bell class="h-10 w-10 text-brand-ink-3" /></div>
            暂无消息
          </div>
        </div>
      </aside>

      <!-- ========== 右侧聊天窗口 ========== -->
      <section
        :class="['flex-col flex-1 bg-brand-bg absolute md:static inset-0 z-20 flex transition-all duration-300',
                 showChatMobile ? 'translate-x-0 md:translate-x-0' : 'translate-x-full md:translate-x-0']">
        <!-- 顶部栏 -->
        <div class="flex items-center justify-between gap-4 p-3 md:p-4 border-b border-brand-line bg-brand-surface shrink-0">
          <div class="flex items-center gap-3 min-w-0 flex-1">
            <button @click="backToList"
              class="md:hidden inline-flex items-center justify-center w-9 h-9 rounded-md text-brand-ink-2 hover:text-brand-ink hover:bg-brand-surface-2">
              <ArrowLeft class="w-5 h-5" />
            </button>
            <img :src="activeConv.avatar" class="w-10 h-10 rounded-full object-cover bg-brand-bg shrink-0" />
            <div class="min-w-0">
              <div class="font-semibold text-brand-ink truncate">{{ activeConv.name }}</div>
              <div class="text-xs text-brand-ink-2 flex items-center gap-1">
                <span v-if="activeConv.online" class="inline-block w-2 h-2 rounded-full bg-brand-success mr-0.5"></span>
                {{ activeConv.lastOnline }}
              </div>
            </div>
          </div>
          <!-- 关联商品卡片 -->
          <a v-if="activeConv.product" href="#"
            class="hidden md:flex items-center gap-3 rounded-lg border border-brand-line bg-brand-surface-2 p-2 pr-3 transition-colors hover:border-brand-primary/40 max-w-[340px]">
            <img :src="activeConv.product.cover" class="w-12 h-12 rounded-md object-cover bg-brand-bg" />
            <div class="min-w-0 flex-1">
              <div class="text-sm font-medium text-brand-ink truncate">{{ activeConv.product.title }}</div>
              <div class="text-sm font-bold text-brand-primary">{{ fmt(activeConv.product.price) }}</div>
            </div>
            <ChevronRight class="w-4 h-4 text-brand-ink-3 shrink-0" />
          </a>
          <!-- 右侧功能按钮 -->
          <div class="hidden md:flex items-center gap-1 shrink-0 pl-2">
            <span class="inline-flex items-center justify-center w-9 h-9 rounded-lg text-brand-ink-2 hover:text-brand-ink hover:bg-brand-surface-2 cursor-pointer"><Phone class="h-4.5 w-4.5" /></span>
            <span class="inline-flex items-center justify-center w-9 h-9 rounded-lg text-brand-ink-2 hover:text-brand-ink hover:bg-brand-surface-2 cursor-pointer"><Video class="h-4.5 w-4.5" /></span>
            <span class="inline-flex items-center justify-center w-9 h-9 rounded-lg text-brand-ink-2 hover:text-brand-ink hover:bg-brand-surface-2 cursor-pointer"><MoreVertical class="h-4.5 w-4.5" /></span>
          </div>
        </div>

        <!-- 关联商品（移动端小卡片） -->
        <a v-if="activeConv.product" href="#"
          class="md:hidden flex items-center gap-3 mx-3 my-2 p-2 rounded-lg border border-brand-line bg-brand-surface-2">
          <img :src="activeConv.product.cover" class="w-10 h-10 rounded-md object-cover bg-brand-bg" />
          <div class="min-w-0 flex-1">
            <div class="text-xs font-medium text-brand-ink truncate">{{ activeConv.product.title }}</div>
            <div class="text-xs font-bold text-brand-primary">{{ fmt(activeConv.product.price) }}</div>
          </div>
          <ChevronRight class="w-4 h-4 text-brand-ink-3 shrink-0" />
        </a>

        <!-- 聊天记录 -->
        <div ref="chatScrollRef" class="flex-1 overflow-y-auto px-3 md:px-5 py-4 space-y-6 bg-brand-bg">
          <div class="text-center">
            <span class="inline-block px-3 py-1 text-xs text-brand-ink-3 bg-brand-surface-2 rounded-full border border-brand-line">今天</span>
          </div>

          <div v-for="(m, idx) in activeConv.messages" :key="idx">
            <!-- 系统卡片消息 -->
            <div v-if="m.type === 'system-card'" class="flex justify-center">
              <div class="max-w-md w-full rounded-xl border border-brand-primary/25 bg-brand-primary/8 p-4 text-sm">
                <div class="flex items-start gap-3">
                  <ShieldCheck class="h-5 w-5 text-brand-primary shrink-0 mt-0.5" />
                  <div class="flex-1">
                    <p class="font-semibold text-brand-ink mb-1">{{ m.system.title }}</p>
                    <p class="text-brand-ink-2 mb-3">{{ m.system.desc }}</p>
                    <button class="inline-flex items-center gap-1 rounded-md bg-brand-primary px-3 py-1.5 text-xs font-medium text-brand-primary-ink hover:bg-brand-primary/90 transition">
                      {{ m.system.linkText }} <ChevronRight class="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 对方消息 -->
            <div v-else-if="!m.me" class="flex gap-3">
              <img :src="activeConv.avatar" class="w-9 h-9 rounded-full object-cover bg-brand-bg shrink-0 self-end" />
              <div class="max-w-[75%] space-y-1">
                <div v-if="m.type === 'image'" class="px-2 py-2 rounded-2xl rounded-bl-md bg-brand-surface border border-brand-line inline-block overflow-hidden">
                  <img :src="m.image" class="w-48 h-auto rounded-lg object-cover" />
                </div>
                <div v-else class="px-4 py-2.5 text-sm leading-relaxed rounded-2xl rounded-bl-md bg-brand-surface border border-brand-line text-brand-ink">
                  {{ m.text }}
                </div>
                <div class="flex items-center gap-1 text-xs text-brand-ink-3 pl-1">
                  <Clock class="h-3 w-3" /> {{ m.time }}
                </div>
              </div>
            </div>

            <!-- 我的消息 -->
            <div v-else class="flex gap-3 justify-end">
              <div class="max-w-[75%] space-y-1 text-right">
                <div v-if="m.type === 'image'" class="px-2 py-2 rounded-2xl rounded-br-md bg-brand-primary/15 border border-brand-primary/30 inline-block overflow-hidden text-left">
                  <img :src="m.image" class="w-48 h-auto rounded-lg object-cover" />
                </div>
                <div v-else class="px-4 py-2.5 text-sm leading-relaxed rounded-2xl rounded-br-md bg-brand-primary text-brand-primary-ink text-left inline-block">
                  {{ m.text }}
                </div>
                <div class="flex items-center justify-end gap-1 text-xs text-brand-ink-3 pr-1">
                  <Clock class="h-3 w-3" /> {{ m.time }}
                </div>
              </div>
              <img src="https://placehold.co/96x96/00D4AA/0B0C10?text=YE" class="w-9 h-9 rounded-full object-cover bg-brand-bg shrink-0 self-end" />
            </div>
          </div>
        </div>

        <!-- 底部输入框 -->
        <div class="p-3 md:p-4 border-t border-brand-line bg-brand-surface shrink-0">
          <form class="flex items-end gap-3" @submit.prevent="sendMessage">
            <button type="button" @click="pickImage"
              class="inline-flex items-center justify-center w-10 h-10 rounded-lg text-brand-ink-2 hover:text-brand-ink hover:bg-brand-surface-2 shrink-0 transition">
              <ImagePlus class="w-5 h-5" />
            </button>
            <div class="flex-1">
              <textarea v-model="inputText" rows="1" placeholder="输入消息…" @keydown.enter.exact.prevent="sendMessage"
                class="min-h-[40px] max-h-36 w-full rounded-xl border border-brand-line bg-brand-bg px-3.5 py-2.5 text-sm text-brand-ink placeholder:text-brand-ink-3 outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20 resize-y"></textarea>
            </div>
            <button type="submit"
              class="inline-flex items-center justify-center gap-2 h-10 px-5 rounded-lg bg-brand-primary text-brand-primary-ink font-semibold hover:bg-brand-primary/90 transition shrink-0">
              <span class="text-sm">发送</span>
              <Send class="w-4 h-4" />
            </button>
          </form>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
@reference "../style.css";
.h-4\.5 { height: 18px; }
.w-4\.5 { width: 18px; }
</style>
