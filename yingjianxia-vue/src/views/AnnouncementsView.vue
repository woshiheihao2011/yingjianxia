<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import {
  Pin, Search, Calendar, Eye, Bell, X, FileText, Download, Megaphone,
  FileX, ChevronLeft, ChevronRight, Mail, MessageSquare
} from 'lucide-vue-next'

// 分类标签
const categories = [
  { key: 'all', label: '全部', color: 'bg-zinc-500/15 text-zinc-400' },
  { key: 'platform', label: '平台公告', color: 'bg-blue-500/15 text-blue-400' },
  { key: 'activity', label: '活动通知', color: 'bg-amber-500/15 text-amber-400' },
  { key: 'policy', label: '政策更新', color: 'bg-emerald-500/15 text-emerald-400' },
  { key: 'maintenance', label: '系统维护', color: 'bg-red-500/15 text-red-400' },
]
const activeCategory = ref('all')
const searchQuery = ref('')

// 置顶公告
const pinnedAnnouncements = [
  {
    id: 1,
    title: '硬件侠 2026 年度平台服务协议更新',
    category: 'policy',
    categoryLabel: '政策更新',
    date: '2026-08-10',
    views: '32,451',
    summary: '新版服务协议将于 2026 年 9 月 1 日正式生效，涉及交易保障、售后流程与个人信息保护等重要调整。',
    body: `
      <p class="mb-4">为了进一步保障买卖双方的合法权益，优化平台交易体验，我们已对《硬件侠平台服务协议》进行了全面修订。新版协议将于 2026 年 9 月 1 日起正式生效。</p>
      <p class="mb-4 font-semibold text-foreground">本次调整主要涉及以下内容：</p>
      <ul class="list-disc pl-5 space-y-2 mb-4 text-muted-foreground">
        <li>明确买卖双方的权利义务与违约责任；</li>
        <li>完善验机报告与售后退换流程；</li>
        <li>强化对虚假描述、假冒伪劣商品的处罚措施；</li>
        <li>优化个人信息保护与数据使用条款。</li>
      </ul>
      <p class="text-muted-foreground">请各位用户及时查阅新版协议，继续使用硬件侠服务即视为同意更新后的内容。如有疑问，请联系官方客服。</p>
    `,
    attachments: [
      { name: '硬件侠平台服务协议（2026 版）.pdf' },
      { name: '卖家发布规范一览表.pdf' },
    ],
  },
  {
    id: 2,
    title: '国庆假期客服与发货安排通知',
    category: 'platform',
    categoryLabel: '平台公告',
    date: '2026-08-15',
    views: '18,927',
    summary: '国庆期间客服与物流时效将有所调整，请买卖双方提前沟通发货与售后安排。',
    body: `
      <p class="mb-4">2026 年国庆假期期间（10 月 1 日至 10 月 7 日），硬件侠将照常为您提供服务，但客服响应与物流时效将有所调整，敬请留意。</p>
      <p class="mb-4 font-semibold text-foreground">具体安排如下：</p>
      <ul class="list-disc pl-5 space-y-2 mb-4 text-muted-foreground">
        <li>客服在线时间调整为 10:00-18:00，咨询回复可能略有延迟；</li>
        <li>节日期间订单可正常下单付款，卖家发货时间以商品详情页说明为准；</li>
        <li>部分快递公司揽收时效延长 1-3 天，建议买卖双方提前沟通；</li>
        <li>售后申请可正常提交，审核处理将于 10 月 8 日起陆续进行。</li>
      </ul>
      <p class="text-muted-foreground">感谢您对硬件侠的理解与支持，祝您假期愉快。</p>
    `,
    attachments: [],
  },
]

// 公告列表
const announcementList = [
  {
    id: 3,
    title: '硬件侠担保交易服务升级公告',
    category: 'platform',
    categoryLabel: '平台公告',
    date: '2026-08-28',
    views: '8,932',
    summary: '资金托管、确认收货与自动放款规则优化，为买卖双方提供更安全的交易保障。',
    body: '<p>为进一步提升交易安全性，硬件侠担保交易服务已完成全面升级。升级后，资金托管、确认收货、自动放款等关键节点将更加透明可控。买家付款后资金由平台全程托管，新增「延迟收货」功能，自动放款时间由 7 天延长至 10 天，交易纠纷处理时效由 48 小时缩短至 24 小时。</p>',
    attachments: [],
  },
  {
    id: 4,
    title: '开学季装机狂欢，全场免邮券限时领',
    category: 'activity',
    categoryLabel: '活动通知',
    date: '2026-08-25',
    views: '15,201',
    summary: '即日起至 9 月 10 日，登录即领免邮券与满减券，分享再得 50 元无门槛券。',
    body: '<p>新学期新装备！硬件侠开学季装机狂欢活动正式上线，即日起至 2026 年 9 月 10 日，全场可领取免邮券与满减券。每日登录可领取 1 张免邮券，订单满 500 元减 30 元，满 1000 元减 80 元，分享活动页至社交平台额外获得 1 张 50 元无门槛券。</p>',
    attachments: [],
  },
  {
    id: 5,
    title: '卖家实名认证与发布规范调整',
    category: 'policy',
    categoryLabel: '政策更新',
    date: '2026-08-22',
    views: '6,430',
    summary: '9 月起新卖家需完成实名认证，高价值商品需补充凭证，违规内容将严格处理。',
    body: '<p>为营造更可信的交易环境，平台将对卖家实名认证与商品发布规范进行调整。新规将于 2026 年 9 月 1 日起执行。新注册卖家需完成实名认证后方可发布商品，商品标题与描述中不得出现站外联系方式，高价值商品（单价 2000 元以上）需上传购买凭证或 SN 码照片。</p>',
    attachments: [],
  },
  {
    id: 6,
    title: '8 月 30 日凌晨支付系统进行维护',
    category: 'maintenance',
    categoryLabel: '系统维护',
    date: '2026-08-20',
    views: '4,112',
    summary: '8 月 30 日 00:00-04:00 支付通道暂停服务，请提前安排交易与提现。',
    body: '<p>为提升支付系统稳定性，硬件侠将于 2026 年 8 月 30 日 00:00 至 04:00 进行支付通道维护。维护期间，下单支付、提现、退款等资金操作将暂时不可用；商品浏览、聊天、订单查看等其它功能不受影响。维护结束后，未完成的支付订单将自动关闭，请重新下单。</p>',
    attachments: [],
  },
  {
    id: 7,
    title: '关于打击虚假描述的专项治理公告',
    category: 'platform',
    categoryLabel: '平台公告',
    date: '2026-08-18',
    views: '9,876',
    summary: '启动「真实描述」专项治理，重点排查高价值品类，严惩虚假描述行为。',
    body: '<p>近期平台发现部分商品存在虚假成色、虚标参数、隐瞒故障等问题，严重损害了买家权益。为此，硬件侠即日起启动「真实描述」专项治理行动。通过 AI 识别与人工复核，重点排查显卡、CPU、主板等高价值品类；对虚假描述商品执行下架、扣分、限制流量等处罚。</p>',
    attachments: [],
  },
  {
    id: 8,
    title: '邀请好友得现金红包活动上线',
    category: 'activity',
    categoryLabel: '活动通知',
    date: '2026-08-15',
    views: '11,234',
    summary: '分享专属邀请链接，好友注册并完成首单，双方均可获得现金红包。',
    body: '<p>邀请好友加入硬件侠，双方均可获得现金红包奖励。活动长期有效，多邀多得。在「我的」-「邀请有礼」中复制专属邀请链接；好友通过链接完成注册并实名认证，邀请人获得 5 元现金红包；好友完成首单交易，邀请人再得 15 元现金红包。红包实时到账钱包，可直接提现或用于平台消费。</p>',
    attachments: [],
  },
]

// 过滤后的列表
const filteredList = computed(() => {
  let list = announcementList
  if (activeCategory.value !== 'all') {
    list = list.filter((a) => a.category === activeCategory.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(
      (a) => a.title.toLowerCase().includes(q) || a.summary.toLowerCase().includes(q)
    )
  }
  return list
})

// 相关公告
const relatedAnnouncements = computed(() => {
  return announcementList.slice(0, 3)
})

// 分类颜色映射
const getCategoryClass = (key) => {
  const map = {
    platform: 'bg-blue-500/15 text-blue-400 border-blue-500/20',
    activity: 'bg-amber-500/15 text-amber-400 border-amber-500/20',
    policy: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
    maintenance: 'bg-red-500/15 text-red-400 border-red-500/20',
  }
  return map[key] || 'bg-zinc-500/15 text-zinc-400 border-zinc-500/20'
}

// 弹窗
const showModal = ref(false)
const selected = ref(null)

const openAnnouncement = (item) => {
  selected.value = item
  showModal.value = true
  document.body.style.overflow = 'hidden'
}

const closeModal = () => {
  showModal.value = false
  document.body.style.overflow = ''
}

// 订阅设置
const subscribeEmail = ref(true)
const subscribeMessage = ref(true)

// 分页
const currentPage = ref(1)
const totalPages = 8

const getPageList = () => {
  const pages = [1, 2, 3]
  if (currentPage.value > 3 && currentPage.value < totalPages) {
    return [currentPage.value - 1, currentPage.value, currentPage.value + 1]
  }
  return pages
}
</script>

<template>
  <div class="min-h-screen bg-background pb-20">
    <!-- 页面标题 + 搜索 -->
    <section class="bg-background border-b border-border">
      <div class="container-app px-4 py-12 md:py-16">
        <div class="flex flex-col md:flex-row md:items-end md:justify-between gap-6">
          <div>
            <h1 class="text-3xl md:text-4xl font-bold text-foreground mb-3">公告通知</h1>
            <p class="text-muted-foreground">了解平台最新动态、活动通知与政策更新</p>
          </div>
          <div class="relative w-full md:max-w-sm">
            <Search class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <input
              v-model="searchQuery"
              type="search"
              placeholder="搜索公告标题或关键词…"
              class="w-full h-11 pl-10 pr-4 rounded-lg bg-background border border-border text-foreground placeholder:text-muted-foreground outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
            />
          </div>
        </div>
      </div>
    </section>

    <!-- 分类标签 -->
    <section class="bg-background border-b border-border sticky top-16 z-20 backdrop-blur">
      <div class="container-app px-4 py-4">
        <div class="flex items-center gap-2 overflow-x-auto no-scrollbar" role="tablist">
          <button
            v-for="cat in categories"
            :key="cat.key"
            @click="activeCategory = cat.key"
            :aria-selected="activeCategory === cat.key"
            :class="[
              'inline-flex items-center h-9 px-4 rounded-full text-sm font-medium whitespace-nowrap transition-all',
              activeCategory === cat.key
                ? 'bg-primary text-primary-foreground'
                : 'bg-surface-2 text-muted-foreground hover:text-foreground hover:bg-surface-3',
            ]"
          >
            {{ cat.label }}
          </button>
        </div>
      </div>
    </section>

    <!-- 置顶公告区 -->
    <section class="container-app px-4 py-10 md:py-12">
      <div class="flex items-center gap-2 mb-6">
        <Pin class="w-5 h-5 text-primary" />
        <h2 class="text-xl md:text-2xl font-bold text-foreground">置顶公告</h2>
      </div>
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <article
          v-for="item in pinnedAnnouncements"
          :key="item.id"
          tabindex="0"
          @click="openAnnouncement(item)"
          @keydown.enter="openAnnouncement(item)"
          class="group relative flex flex-col justify-end min-h-[220px] p-6 rounded-2xl bg-gradient-to-br from-surface-2 to-surface border border-border overflow-hidden cursor-pointer transition hover:border-primary hover:-translate-y-1 focus:outline-none focus:ring-2 focus:ring-primary/40"
        >
          <div
            class="pointer-events-none absolute -top-16 -right-16 w-48 h-48 rounded-full blur-2xl opacity-40"
            style="background: radial-gradient(circle, rgba(0, 212, 170, 0.35), transparent 70%);"
          ></div>
          <div class="relative z-10">
            <div class="flex flex-wrap items-center gap-2 mb-4">
              <span class="inline-flex items-center h-6 px-2.5 rounded-full text-xs font-semibold bg-emerald-500/15 text-emerald-400">置顶</span>
              <span
                :class="[
                  'inline-flex items-center h-6 px-2.5 rounded-full text-xs font-semibold border',
                  getCategoryClass(item.category),
                ]"
              >
                {{ item.categoryLabel }}
              </span>
            </div>
            <h3 class="text-xl md:text-2xl font-bold text-foreground mb-3 group-hover:text-primary transition-colors">
              {{ item.title }}
            </h3>
            <p class="text-muted-foreground line-clamp-2 mb-4">{{ item.summary }}</p>
            <div class="flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
              <span class="inline-flex items-center gap-1.5">
                <Calendar class="w-4 h-4" />
                {{ item.date }}
              </span>
              <span class="inline-flex items-center gap-1.5">
                <Eye class="w-4 h-4" />
                {{ item.views }} 阅读
              </span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- 公告列表 -->
    <section class="container-app px-4 py-10 md:py-12 border-t border-border">
      <h2 class="text-xl md:text-2xl font-bold text-foreground mb-6">最新公告</h2>

      <div v-if="!filteredList.length" class="py-16 text-center">
        <FileX class="w-12 h-12 text-muted-foreground mx-auto mb-4" />
        <p class="text-muted-foreground">该分类下暂无公告</p>
      </div>

      <div v-else class="space-y-4">
        <article
          v-for="item in filteredList"
          :key="item.id"
          tabindex="0"
          @click="openAnnouncement(item)"
          @keydown.enter="openAnnouncement(item)"
          class="group flex flex-col gap-3 p-5 rounded-xl bg-card border border-border cursor-pointer transition hover:border-primary hover:-translate-y-0.5 focus:outline-none focus:ring-2 focus:ring-primary/40"
        >
          <div class="flex flex-wrap items-center gap-2">
            <span
              :class="[
                'inline-flex items-center h-6 px-2.5 rounded-full text-xs font-semibold border',
                getCategoryClass(item.category),
              ]"
            >
              {{ item.categoryLabel }}
            </span>
            <span class="text-xs text-muted-foreground ml-auto inline-flex items-center gap-1">
              <Eye class="w-3.5 h-3.5" />
              {{ item.views }}
            </span>
          </div>
          <h3 class="text-lg font-bold text-foreground group-hover:text-primary transition-colors">
            {{ item.title }}
          </h3>
          <p class="text-sm text-muted-foreground line-clamp-2">{{ item.summary }}</p>
          <div class="flex items-center gap-2 text-xs text-muted-foreground">
            <Calendar class="w-3.5 h-3.5" />
            <span>{{ item.date }}</span>
          </div>
        </article>
      </div>
    </section>

    <!-- 订阅设置 -->
    <section class="container-app px-4 py-10 md:py-12 border-t border-border">
      <div class="rounded-2xl bg-card border border-border p-6 md:p-8">
        <div class="flex items-center gap-3 mb-6">
          <Bell class="w-6 h-6 text-primary" />
          <h2 class="text-xl font-bold text-foreground">订阅设置</h2>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <label class="flex items-start gap-4 cursor-pointer group">
            <input
              v-model="subscribeEmail"
              type="checkbox"
              class="mt-1 w-5 h-5 rounded border-border bg-background text-primary focus:ring-primary/30"
            />
            <div>
              <div class="font-medium text-foreground group-hover:text-primary transition-colors flex items-center gap-2">
                <Mail class="w-4 h-4" />
                邮件通知
              </div>
              <div class="text-sm text-muted-foreground mt-1">重要公告与活动第一时间发送至注册邮箱</div>
            </div>
          </label>
          <label class="flex items-start gap-4 cursor-pointer group">
            <input
              v-model="subscribeMessage"
              type="checkbox"
              class="mt-1 w-5 h-5 rounded border-border bg-background text-primary focus:ring-primary/30"
            />
            <div>
              <div class="font-medium text-foreground group-hover:text-primary transition-colors flex items-center gap-2">
                <MessageSquare class="w-4 h-4" />
                站内信通知
              </div>
              <div class="text-sm text-muted-foreground mt-1">在消息中心接收公告推送</div>
            </div>
          </label>
        </div>
      </div>
    </section>

    <!-- 分页 -->
    <section class="container-app px-4 py-8 border-t border-border">
      <nav class="flex items-center justify-center gap-2" aria-label="公告分页">
        <button
          class="inline-flex items-center justify-center min-w-[36px] h-9 px-3 rounded-lg text-sm font-medium text-muted-foreground bg-card border border-border transition hover:border-primary hover:text-foreground disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="currentPage === 1"
          @click="currentPage > 1 && currentPage--"
          aria-label="上一页"
        >
          <ChevronLeft class="w-4 h-4" />
        </button>
        <button
          v-for="p in getPageList()"
          :key="p"
          :class="[
            'inline-flex items-center justify-center min-w-[36px] h-9 px-3 rounded-lg text-sm font-medium transition border',
            currentPage === p
              ? 'bg-primary text-primary-foreground border-primary'
              : 'text-muted-foreground bg-card border-border hover:border-primary hover:text-foreground',
          ]"
          @click="currentPage = p"
        >
          {{ p }}
        </button>
        <span class="text-muted-foreground px-2">...</span>
        <button
          class="inline-flex items-center justify-center min-w-[36px] h-9 px-3 rounded-lg text-sm font-medium text-muted-foreground bg-card border border-border transition hover:border-primary hover:text-foreground"
          @click="currentPage = totalPages"
        >
          {{ totalPages }}
        </button>
        <button
          class="inline-flex items-center justify-center min-w-[36px] h-9 px-3 rounded-lg text-sm font-medium text-muted-foreground bg-card border border-border transition hover:border-primary hover:text-foreground disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="currentPage === totalPages"
          @click="currentPage < totalPages && currentPage++"
          aria-label="下一页"
        >
          <ChevronRight class="w-4 h-4" />
        </button>
      </nav>
    </section>

    <!-- 公告详情弹窗 -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="showModal && selected"
          class="fixed inset-0 z-50 flex items-start md:items-center justify-center p-4 md:p-8"
        >
          <div
            class="absolute inset-0 bg-black/70 backdrop-blur-sm"
            @click="closeModal"
          ></div>
          <div
            class="relative w-full max-w-3xl max-h-[85vh] overflow-hidden rounded-2xl bg-card border border-border shadow-2xl animate-in zoom-in-95 duration-200"
          >
            <div class="p-6 md:p-8 overflow-y-auto max-h-[85vh] custom-scrollbar">
              <div class="flex items-start justify-between gap-4 mb-4">
                <div>
                  <span
                    :class="[
                      'inline-flex items-center h-6 px-2.5 rounded-full text-xs font-semibold border mb-3',
                      getCategoryClass(selected.category),
                    ]"
                  >
                    {{ selected.categoryLabel }}
                  </span>
                  <h2 class="text-2xl font-bold text-foreground">{{ selected.title }}</h2>
                </div>
                <button
                  type="button"
                  @click="closeModal"
                  class="inline-flex items-center justify-center w-9 h-9 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition focus:outline-none focus:ring-2 focus:ring-primary/40"
                  aria-label="关闭"
                >
                  <X class="w-5 h-5" />
                </button>
              </div>

              <div class="flex flex-wrap items-center gap-4 text-sm text-muted-foreground mb-6 pb-6 border-b border-border">
                <span class="inline-flex items-center gap-1.5">
                  <Calendar class="w-4 h-4" />
                  {{ selected.date }}
                </span>
                <span class="inline-flex items-center gap-1.5">
                  <Eye class="w-4 h-4" />
                  {{ selected.views }} 阅读
                </span>
              </div>

              <div
                class="text-foreground/90 leading-relaxed space-y-4"
                v-html="selected.body"
              ></div>

              <div v-if="selected.attachments && selected.attachments.length" class="mt-8 pt-6 border-t border-border">
                <h3 class="font-semibold text-foreground mb-3 flex items-center gap-2">
                  <FileText class="w-4 h-4 text-primary" />
                  附件
                </h3>
                <ul class="space-y-2">
                  <li v-for="(att, idx) in selected.attachments" :key="idx">
                    <a
                      href="#"
                      class="inline-flex items-center gap-2 text-sm text-primary hover:underline"
                    >
                      <Download class="w-4 h-4" />
                      {{ att.name }}
                    </a>
                  </li>
                </ul>
              </div>

              <div class="mt-8 pt-6 border-t border-border">
                <h3 class="font-semibold text-foreground mb-3 flex items-center gap-2">
                  <Megaphone class="w-4 h-4 text-primary" />
                  相关公告
                </h3>
                <ul class="space-y-2">
                  <li v-for="r in relatedAnnouncements" :key="r.id">
                    <button
                      @click="openAnnouncement(r); window.scrollTo({ top: 0, behavior: 'smooth' })"
                      class="text-sm text-muted-foreground hover:text-foreground hover:text-primary transition"
                    >
                      {{ r.title }}
                    </button>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
@reference "../style.css";
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
@keyframes zoom-in-95 {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
.animate-in {
  animation: zoom-in-95 0.2s ease-out;
}
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(42, 45, 53, 1);
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 212, 170, 0.4);
}
</style>
