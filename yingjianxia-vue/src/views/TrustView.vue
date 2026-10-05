<template>
  <div class="min-h-screen bg-background text-foreground">
    <!-- Hero -->
    <section class="relative overflow-hidden">
      <div class="max-w-7xl mx-auto px-4 py-16 lg:py-24">
        <div class="text-center max-w-3xl mx-auto space-y-6">
          <h1 class="text-4xl md:text-5xl lg:text-6xl font-bold tracking-tight">放心交易，平台全程护航</h1>
          <p class="text-base md:text-lg text-muted-foreground">从验机到收货，每一道环节都有硬件侠守护。真实检测、资金托管、实名认证、纠纷仲裁，让二手硬件交易更安心。</p>
          <div class="flex flex-wrap items-center justify-center gap-4 pt-2">
            <RouterLink to="/browse" class="btn-primary">去逛硬件</RouterLink>
            <RouterLink to="/sell" class="btn-secondary">发布闲置</RouterLink>
          </div>
        </div>

        <div class="mt-14 grid grid-cols-1 sm:grid-cols-3 gap-4 md:gap-6">
          <div class="rounded-xl border border-border bg-card p-6 text-center">
            <div class="text-3xl md:text-4xl font-bold text-primary font-mono">128,600+</div>
            <div class="mt-2 text-sm text-muted-foreground">累计验机数</div>
          </div>
          <div class="rounded-xl border border-border bg-card p-6 text-center">
            <div class="text-3xl md:text-4xl font-bold text-primary font-mono">¥9.8 亿</div>
            <div class="mt-2 text-sm text-muted-foreground">担保交易金额</div>
          </div>
          <div class="rounded-xl border border-border bg-card p-6 text-center">
            <div class="text-3xl md:text-4xl font-bold text-primary font-mono">98.7%</div>
            <div class="mt-2 text-sm text-muted-foreground">纠纷解决率</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 四大保障 -->
    <section class="py-16 lg:py-20 border-t border-border">
      <div class="max-w-7xl mx-auto px-4">
        <div class="text-center mb-12">
          <h2 class="text-2xl md:text-3xl font-bold">四大交易保障</h2>
          <p class="mt-3 text-muted-foreground">多重机制叠加，买卖双方权益都能被看见和保护</p>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <div v-for="item in guarantees" :key="item.title" class="guarantee-card">
            <div class="icon-wrap">
              <component :is="item.icon" class="w-6 h-6" />
            </div>
            <div>
              <h3 class="font-semibold text-lg">{{ item.title }}</h3>
              <p class="mt-2 text-sm text-muted-foreground leading-relaxed">{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 验机流程 -->
    <section class="py-16 lg:py-20 border-t border-border">
      <div class="max-w-7xl mx-auto px-4">
        <div class="grid lg:grid-cols-2 gap-12 lg:gap-16 items-start">
          <div>
            <h2 class="text-2xl md:text-3xl font-bold">专业验机流程</h2>
            <p class="mt-3 text-muted-foreground">每一笔走平台验机的订单，都会经过以下标准化检测步骤，确保你收到的硬件与描述一致。</p>
            <div class="mt-8 hidden lg:block rounded-xl border border-border bg-card p-6">
              <div class="flex items-start gap-4">
                <Shield class="w-6 h-6 text-primary mt-0.5 shrink-0" />
                <div>
                  <h4 class="font-semibold">平台担保承诺</h4>
                  <p class="mt-2 text-sm text-muted-foreground leading-relaxed">若检测报告与实物严重不符，或商品存在未披露功能性故障，买家可在签收后 7 天内申请退换或全额退款。</p>
                </div>
              </div>
            </div>
          </div>

          <div class="relative">
            <div class="timeline-line"></div>
            <div v-for="(step, idx) in timelineSteps" :key="idx" class="timeline-step">
              <div class="timeline-dot">
                <Check v-if="idx === timelineSteps.length - 1" class="w-3.5 h-3.5" />
                <span v-else class="text-xs font-bold">{{ idx + 1 }}</span>
              </div>
              <h3 class="font-semibold">{{ step.title }}</h3>
              <p class="mt-2 text-sm text-muted-foreground">{{ step.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 安全提示 -->
    <section class="py-16 lg:py-20 border-t border-border">
      <div class="max-w-7xl mx-auto px-4">
        <div class="grid md:grid-cols-2 gap-6">
          <div class="rounded-2xl border border-border bg-card p-6 md:p-8">
            <div class="flex items-center gap-3 mb-5">
              <div class="flex items-center justify-center w-10 h-10 rounded-md bg-primary/12 text-primary">
                <AlertTriangle class="w-5 h-5" />
              </div>
              <h2 class="text-xl font-bold">防骗指南</h2>
            </div>
            <ul class="space-y-3 text-sm text-muted-foreground">
              <li v-for="tip in safetyTips" :key="tip" class="flex items-start gap-3">
                <CheckCircle class="w-4 h-4 text-primary mt-0.5 shrink-0" />
                <span>{{ tip }}</span>
              </li>
            </ul>
          </div>

          <div class="rounded-2xl border border-border bg-card p-6 md:p-8">
            <div class="flex items-center gap-3 mb-5">
              <div class="flex items-center justify-center w-10 h-10 rounded-md bg-primary/12 text-primary">
                <Flag class="w-5 h-5" />
              </div>
              <h2 class="text-xl font-bold">举报入口</h2>
            </div>
            <p class="text-sm text-muted-foreground leading-relaxed mb-6">发现虚假描述、欺诈行为或违规商品？你可以通过以下渠道快速举报，平台将在 2 小时内响应。</p>
            <div class="space-y-3">
              <RouterLink to="/messages" class="btn-primary w-full md:w-auto">联系在线客服</RouterLink>
              <a href="mailto:support@yingjianxia.com?subject=举报违规商品" class="btn-secondary w-full md:w-auto">邮件举报：support@yingjianxia.com</a>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 常见问题 -->
    <section class="py-16 lg:py-20 border-t border-border">
      <div class="max-w-3xl mx-auto px-4">
        <div class="text-center mb-10">
          <h2 class="text-2xl md:text-3xl font-bold">常见问题</h2>
          <p class="mt-3 text-muted-foreground">关于交易保障你可能还想知道</p>
        </div>
        <div class="space-y-4">
          <details v-for="(faq, idx) in faqs" :key="idx" class="faq-details">
            <summary class="flex items-center justify-between gap-4 cursor-pointer list-none p-[18px_20px] font-medium hover:bg-surface-2/50 rounded-t-lg">
              <span>{{ faq.q }}</span>
              <ChevronDown class="w-4.5 h-4.5 text-muted-foreground transition-transform" :class="{ 'rotate-180': openFaq === idx }" />
            </summary>
            <div class="p-[0_20px_18px] text-muted-foreground text-sm leading-7">{{ faq.a }}</div>
          </details>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  ClipboardCheck, ShieldCheck, UserCheck, Scale,
  Shield, Check, AlertTriangle, Flag, CheckCircle, ChevronDown
} from 'lucide-vue-next'

const openFaq = ref(null)

const guarantees = [
  { icon: ClipboardCheck, title: '平台验机', desc: '硬件统一寄送至平台检测中心，外观、功能、SN 码逐项核对，杜绝货不对板。' },
  { icon: ShieldCheck, title: '担保交易', desc: '买家付款后资金由平台托管，确认收货或验机通过后再放款，交易零风险。' },
  { icon: UserCheck, title: '实名认证', desc: '卖家需通过实名与人脸认证，信用档案透明可查，骗子无处藏身。' },
  { icon: Scale, title: '纠纷仲裁', desc: '交易出现争议可申请平台介入，专业客服依据凭证公平裁决，最快 24 小时结案。' }
]

const timelineSteps = [
  { title: '买家下单并选择平台验机', desc: '下单时勾选"平台验机"服务，货款进入托管账户。' },
  { title: '卖家寄送至检测中心', desc: '卖家按平台地址发货，物流信息实时同步，买家可随时查看。' },
  { title: '工程师专业检测', desc: '48 小时内完成外观、接口、性能、温度等多维度测试。' },
  { title: '生成电子检测报告', desc: '报告含实拍图、跑分数据、SN 核对结果，买家确认后进入下一步。' },
  { title: '确认发货并签收', desc: '买家确认报告无误后，平台打包发货，签收后进入 7 天保障期。' }
]

const safetyTips = [
  '拒绝任何脱离平台的私下转账或扫码付款请求。',
  '收货时务必拍摄完整开箱视频，保留物流外包装。',
  '优先选择带有"平台验机"标识的商品，检测报告即凭证。',
  '对价格明显低于市场价的商品保持警惕，谨防 bait-and-switch。'
]

const faqs = [
  { q: '平台验机需要额外付费吗？', a: '平台验机服务费按商品成交价的 3% 收取，由买家在下单时支付。若交易未达成或验机未通过导致退款，该费用将全额退还。' },
  { q: '验机不通过怎么办？', a: '若检测发现商品与卖家描述严重不符或存在功能性故障，平台会自动终止交易并全额退款给买家，同时通知卖家取回商品。' },
  { q: '担保交易的款项多久到账？', a: '买家确认收货或系统自动确认收货后，款项将在 24 小时内结算至卖家账户。若交易触发纠纷仲裁，到账时间以仲裁结果为准。' },
  { q: '如何申请纠纷仲裁？', a: '进入"订单中心"找到对应订单，点击"申请平台介入"，上传聊天记录、开箱视频、检测报告等凭证，客服会在 24 小时内联系双方处理。' },
  { q: '实名认证信息会被泄露吗？', a: '平台采用加密存储与脱敏展示，实名信息仅用于身份核验与风控，不会向交易对方披露，亦不会用于任何第三方营销。' }
]
</script>

<style scoped>
@reference "../style.css";
.btn-primary {
  @apply inline-flex items-center justify-center gap-2 h-11 px-6 rounded-lg bg-primary text-primary-foreground font-semibold transition hover:bg-[var(--brand-primary-hover)] active:translate-y-px;
}
.btn-secondary {
  @apply inline-flex items-center justify-center gap-2 h-11 px-6 rounded-lg bg-transparent border border-border text-foreground font-semibold transition hover:bg-surface-2 active:translate-y-px;
}
.guarantee-card {
  @apply flex flex-col gap-4 p-7 rounded-2xl bg-card border border-border transition;
}
.guarantee-card:hover {
  @apply -translate-y-1 border-primary;
}
.icon-wrap {
  @apply flex items-center justify-center w-12 h-12 rounded-lg bg-primary/12 text-primary;
}
.timeline-line {
  @apply absolute left-6 top-12 bottom-0 w-0.5 bg-border;
}
.timeline-step {
  @apply relative pl-[72px] pb-8 last:pb-0;
}
.timeline-dot {
  @apply absolute left-3 top-1 w-6 h-6 rounded-full bg-primary text-primary-foreground flex items-center justify-center;
}
.faq-details {
  @apply rounded-lg border border-border bg-card overflow-hidden;
}
.faq-details[open] {
  @apply border-primary;
}
@media (max-width: 768px) {
  .timeline-line { left: 16px; }
  .timeline-step { padding-left: 52px; }
  .timeline-dot { left: 4px; }
}
</style>
