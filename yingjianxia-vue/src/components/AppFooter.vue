<script setup>
import { Tag, Shield, FileText, MessageSquare, Handshake, BookOpen, Phone } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
const router = useRouter()

const sections = [
  {
    title: '买家服务',
    links: [
      { name: '浏览商品', href: '/browse' },
      { name: '搜索找货', href: '/search' },
      { name: '购物车', href: '/cart' },
      { name: '订单中心', href: '/orders' },
      { name: '优惠券积分', href: '/coupons' }
    ]
  },
  {
    title: '卖家服务',
    links: [
      { name: '免费开店', href: '/auth' },
      { name: '发布商品', href: '/sell' },
      { name: '库存管理', href: '/inventory' },
      { name: '数据中心', href: '/analytics' },
      { name: '营销活动', href: '/promotions' }
    ]
  },
  {
    title: '平台支撑',
    links: [
      { name: '信任中心', href: '/trust' },
      { name: '帮助中心', href: '/help' },
      { name: '客服工单', href: '/tickets' },
      { name: '公告通知', href: '/announcements' },
      { name: '硬件社区', href: '/community' }
    ]
  }
]

const promises = [
  { icon: Shield, title: '担保交易', desc: '先验货后放款，资金0风险' },
  { icon: FileText, title: '官方验机', desc: '第三方质检，出具报告' },
  { icon: Handshake, title: '7天售后', desc: '支持退/换货保障' },
  { icon: Phone, title: '极速客服', desc: '7x24 专业响应' }
]
</script>

<template>
  <footer class="mt-24 border-t border-brand-line bg-brand-surface/60">
    <!-- 承诺层 -->
    <div class="container-app py-10 border-b border-brand-line">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-6">
        <div v-for="p in promises" :key="p.title" class="flex items-start gap-3 p-4 rounded-lg bg-brand-bg/40 border border-brand-line-subtle">
          <div class="w-10 h-10 rounded-lg bg-brand-primary/15 border border-brand-primary/25 flex items-center justify-center shrink-0">
            <component :is="p.icon" class="w-5 h-5 text-brand-primary" />
          </div>
          <div>
            <div class="text-sm font-semibold text-brand-ink">{{ p.title }}</div>
            <div class="text-xs text-brand-ink-3 mt-1 leading-relaxed">{{ p.desc }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 链接层 -->
    <div class="container-app py-10">
      <div class="grid grid-cols-1 md:grid-cols-5 gap-8">
        <!-- 品牌列 -->
        <div class="md:col-span-2">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-10 h-10 rounded-lg bg-brand-primary/15 border border-brand-primary/30 flex items-center justify-center">
              <Tag class="w-5 h-5 text-brand-primary" />
            </div>
            <div>
              <div class="text-xl font-bold text-brand-ink">硬件侠</div>
              <div class="text-[10px] text-brand-ink-3 uppercase tracking-[0.2em]">HARDWARE · XIA</div>
            </div>
          </div>
          <p class="text-sm text-brand-ink-2 leading-relaxed max-w-sm">
            专业电脑配件二手交易平台，12+ 品类覆盖 CPU/显卡/主板/笔记本，官方验机担保交易，让每一台二手硬件都能放心交易。
          </p>
          <div class="mt-4 flex items-center gap-4 text-xs text-brand-ink-3">
            <div class="flex items-center gap-1.5"><MessageSquare class="w-4 h-4" />在线客服</div>
            <div>客服热线：400-000-0000</div>
            <div class="flex items-center gap-1.5"><BookOpen class="w-4 h-4" />服务时间 7x24</div>
          </div>
        </div>
        <!-- 链接列 -->
        <div v-for="sec in sections" :key="sec.title">
          <div class="text-sm font-semibold text-brand-ink mb-4">{{ sec.title }}</div>
          <ul class="space-y-2.5">
            <li v-for="link in sec.links" :key="link.name">
              <button @click="router.push(link.href)" class="text-sm text-brand-ink-2 hover:text-brand-primary transition">
                {{ link.name }}
              </button>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 版权层 -->
    <div class="border-t border-brand-line bg-brand-bg/60">
      <div class="container-app py-4 flex flex-col md:flex-row md:items-center md:justify-between gap-2 text-xs text-brand-ink-3">
        <div>© 2025 硬件侠 Hardware Xia · 让二手硬件有个好归宿</div>
        <div class="flex flex-wrap gap-4">
          <span>ICP 备案号：京ICP备2025000000号</span>
          <span>公安备案号：京公网安备 00000000000000号</span>
          <span>版本 v1.0.0</span>
        </div>
      </div>
    </div>
  </footer>
</template>
