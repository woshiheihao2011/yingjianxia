<template>
  <div class="min-h-screen bg-background text-foreground">
    <!-- 帮助中心标题 + 搜索 -->
    <section class="border-b border-border">
      <div class="max-w-7xl mx-auto px-4 py-12 md:py-16 text-center">
        <h1 class="text-3xl md:text-4xl font-bold mb-4">帮助中心</h1>
        <p class="text-muted-foreground mb-8">常见问题、平台规则与操作指南</p>
        <form class="relative max-w-2xl mx-auto" role="search" @submit.prevent="onSearch">
          <input
            v-model="searchKeyword"
            type="search"
            placeholder="搜索问题、规则、操作指南..."
            class="w-full h-12 pl-12 pr-4 rounded-lg bg-background border border-border text-foreground placeholder:text-muted-foreground outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
          />
          <Search class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
        </form>
      </div>
    </section>

    <!-- 问题分类卡片网格 -->
    <section class="py-12 md:py-16">
      <div class="max-w-7xl mx-auto px-4">
        <h2 class="text-xl md:text-2xl font-bold mb-8">问题分类</h2>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          <a
            v-for="cat in categories"
            :key="cat.label"
            href="#faq"
            class="category-card"
          >
            <component :is="cat.icon" class="w-8 h-8 text-primary" />
            <span class="font-medium">{{ cat.label }}</span>
          </a>
        </div>
      </div>
    </section>

    <!-- 热门问题列表 -->
    <section id="faq" class="py-12 md:py-16 border-t border-border">
      <div class="max-w-3xl mx-auto px-4">
        <h2 class="text-xl md:text-2xl font-bold mb-8">热门问题</h2>
        <div class="space-y-4">
          <details
            v-for="(item, idx) in filteredFaqs"
            :key="idx"
            class="faq-details"
            :open="openIndex === idx"
            @toggle="onToggle(idx, $event)"
          >
            <summary class="list-none cursor-pointer flex items-center justify-between gap-4 p-4 font-medium">
              <span>{{ item.q }}</span>
              <ChevronDown class="w-5 h-5 text-muted-foreground transition-transform shrink-0" :class="{ 'rotate-180': openIndex === idx }" />
            </summary>
            <div class="p-[0_20px_16px] text-muted-foreground leading-7 border-t border-border pt-4">{{ item.a }}</div>
          </details>
        </div>
      </div>
    </section>

    <!-- 快速入口区 -->
    <section class="py-12 md:py-16 border-t border-border">
      <div class="max-w-7xl mx-auto px-4">
        <h2 class="text-xl md:text-2xl font-bold mb-8">快速入口</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          <RouterLink v-for="link in quickLinks" :key="link.label" :to="link.to" class="quick-link">
            <component :is="link.icon" class="w-6 h-6 text-primary" />
            <span class="font-medium">{{ link.label }}</span>
          </RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  Search, ChevronDown, BookOpen, Repeat, Wallet, Truck, Shield, Scale,
  Headset, MessageSquare, ShieldCheck, FileText, EyeOff
} from 'lucide-vue-next'

const searchKeyword = ref('')
const openIndex = ref(0)

const categories = [
  { label: '新手入门', icon: BookOpen },
  { label: '买卖流程', icon: Repeat },
  { label: '支付与提现', icon: Wallet },
  { label: '物流与售后', icon: Truck },
  { label: '账号安全', icon: Shield },
  { label: '平台规则', icon: Scale }
]

const faqs = [
  { q: '如何注册并完善账号信息？', a: '点击顶部导航「我的」进入注册/登录页面，支持手机号与常用第三方账号登录。登录后建议在「个人中心」完成实名认证、绑定收款账号并上传头像，可提升交易信任度与发布权限。' },
  { q: '买家如何下单并付款？', a: '在商品详情页点击「立即购买」或「加入购物车」后进入结算页，确认收货地址与商品信息无误后提交订单。平台采用担保交易，货款将托管在硬件侠账户，卖家发货后买家确认收货，货款才会结算给卖家。' },
  { q: '卖家发布商品需要哪些信息？', a: '进入「发布」页面后，填写商品分类、品牌型号、成色、价格、所在地、标题与详细描述，并上传清晰实物照片。建议补充 SN 码、购买凭证与测试截图，有助于买家快速决策。' },
  { q: '如何申请提现？多久到账？', a: '在「个人中心」-「我的钱包」中绑定银行卡或支付宝账号后发起提现。工作日 9:00-18:00 提交的申请通常 2 小时内到账，非工作时间及节假日顺延至下一个工作日处理。' },
  { q: '商品与描述不符怎么办？', a: '收到商品后请在 7 天内仔细验机。如发现成色、功能或配件与描述不符，可在订单详情页申请售后并上传证据。平台客服会在 24 小时内介入处理，确认属实后支持退换或部分退款。' },
  { q: '平台禁止发布哪些商品？', a: '禁止发布来路不明、赃物、假冒伪劣、无法正常使用或侵犯知识产权的硬件；禁止发布个人信息、虚拟货币、充值卡等非实物类商品；禁止在商品信息中出现站外联系方式或诱导线下交易的内容。' },
  { q: '如何保护账号安全？', a: '请开启登录保护并定期更换密码，不要在任何第三方页面输入硬件侠账号密码。平台客服不会通过私信索要验证码、支付密码或银行卡信息，如遇可疑情况请立即联系官方客服核实。' },
  { q: '物流费用由谁承担？', a: '默认由卖家承担发货运费并在商品中体现，买家可与卖家协商到付或包邮方案。建议在下单前通过私聊确认运费、包装方式及保价金额，避免后续争议。' }
]

const filteredFaqs = computed(() => {
  if (!searchKeyword.value.trim()) return faqs
  const kw = searchKeyword.value.toLowerCase()
  return faqs.filter(f => f.q.toLowerCase().includes(kw) || f.a.toLowerCase().includes(kw))
})

const quickLinks = [
  { label: '联系客服', icon: Headset, to: '/messages' },
  { label: '意见反馈', icon: MessageSquare, to: '/tickets' },
  { label: '平台信任中心', icon: ShieldCheck, to: '/trust' },
  { label: '用户协议', icon: FileText, to: '/help' },
  { label: '隐私政策', icon: EyeOff, to: '/help' }
]

function onSearch() {
  document.getElementById('faq')?.scrollIntoView({ behavior: 'smooth' })
}
function onToggle(idx, e) {
  openIndex.value = e.target.open ? idx : null
}
</script>

<style scoped>
@reference "../style.css";
.category-card {
  @apply flex flex-col items-center gap-3 p-6 rounded-lg bg-card border border-border transition;
}
.category-card:hover {
  @apply -translate-y-1 border-primary;
}
.faq-details {
  @apply border border-border rounded-lg bg-card overflow-hidden transition;
}
.faq-details[open] {
  @apply border-primary;
}
.faq-details > summary::-webkit-details-marker { display: none; }
.quick-link {
  @apply flex items-center gap-4 p-5 rounded-lg bg-card border border-border transition;
}
.quick-link:hover {
  @apply border-primary -translate-y-0.5;
}
</style>
