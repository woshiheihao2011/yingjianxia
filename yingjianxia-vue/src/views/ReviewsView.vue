<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Star, User, Store, ImagePlus, X, ThumbsUp, MessageCircle,
  Send, ChevronDown, ChevronUp, ShieldCheck
} from 'lucide-vue-next'

const router = useRouter()

// 角色切换
const role = ref('buyer') // buyer | seller

// 买家 Tab
const buyerTab = ref('pending') // pending | done

// 卖家 Tab
const sellerTab = ref('received') // received | reply

const fmt = (n) => '¥' + Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2 })

// =========== 买家：待评价 ===========
const buyerPending = [
  {
    orderNo: 'ORD202608250001',
    product: '三星 980 Pro 1TB NVMe PCIe 4.0', seller: '硬盘收藏家',
    price: 499, date: '2026-08-25',
    cover: 'https://placehold.co/192x192/15161A/00D4AA?text=980+Pro'
  },
  {
    orderNo: 'ORD202608230002',
    product: '猫头鹰 NH-D15 双塔散热器 侧透静音款', seller: '静音党小王',
    price: 480, date: '2026-08-23',
    cover: 'https://placehold.co/192x192/15161A/00D4AA?text=NH-D15'
  }
]

// =========== 买家：已评价 ===========
const buyerDone = [
  {
    id: 'R-10001', orderNo: 'ORD202608200001',
    product: '七彩虹 RTX 4070 战斧豪华版 成色95新', seller: '极客老王的硬件仓库',
    price: 3680, date: '2026-08-22',
    cover: 'https://placehold.co/192x192/15161A/00D4AA?text=RTX4070',
    productCover: 'https://placehold.co/96x96/15161A/00D4AA?text=RTX4070',
    me: { name: '叶', avatar: 'https://placehold.co/96x96/00D4AA/0B0C10?text=YE' },
    star: 5, content: '成色非常好，烤机测试稳定，温度控制很棒。卖家服务态度好，发货快，沟通顺畅，下次还会再来！',
    tags: ['物流快', '成色好', '沟通顺畅'],
    images: [
      'https://placehold.co/160x160/1E2026/00D4AA?text=实拍1',
      'https://placehold.co/160x160/1E2026/00D4AA?text=实拍2',
      'https://placehold.co/160x160/1E2026/00D4AA?text=实拍3'
    ],
    likes: 12, replyOpen: false, reply: '',
    sellerReply: { time: '2026-08-23 10:30', content: '感谢兄弟的支持！后续有任何问题随时联系~' }
  },
  {
    id: 'R-10002', orderNo: 'ORD202608150008',
    product: '华硕 ROG STRIX B660-A GAMING WIFI 主板', seller: '阿楠装机铺',
    price: 1699, date: '2026-08-18',
    cover: 'https://placehold.co/192x192/15161A/00D4AA?text=B660-A',
    productCover: 'https://placehold.co/96x96/15161A/00D4AA?text=B660-A',
    me: { name: '叶', avatar: 'https://placehold.co/96x96/00D4AA/0B0C10?text=YE' },
    star: 4, content: '整体还行，就是BIOS没更新，自己刷了一下。',
    tags: ['物有所值'],
    images: [], likes: 4, replyOpen: false, reply: ''
  }
]

// =========== 卖家：收到的评价 ===========
const sellerReceived = [
  {
    id: 'R-20001', buyer: '显卡发烧友', buyerAvatar: 'https://placehold.co/96x96/3B82F6/0B0C10?text=GF',
    product: '七彩虹 RTX 4070 战斧豪华版', productCover: 'https://placehold.co/96x96/15161A/00D4AA?text=RTX4070',
    price: 3680, date: '2026-08-28 11:30',
    star: 5, content: '成色真的新，烤机稳，包装也很到位。',
    tags: ['成色超预期', '包装完美'], images: [], likes: 6,
    replyOpen: false, reply: '', replied: true, replyContent: '多谢支持！'
  },
  {
    id: 'R-20002', buyer: '李装机', buyerAvatar: 'https://placehold.co/96x96/F59E0B/0B0C10?text=LJ',
    product: '金士顿 FURY Beast DDR5 6000 32GB(16×2)', productCover: 'https://placehold.co/96x96/15161A/00D4AA?text=DDR5',
    price: 1798, date: '2026-08-27 15:05',
    star: 3, content: '内存条有轻微划痕，不过跑起来没问题，中评吧。',
    tags: [], images: ['https://placehold.co/160x160/1E2026/EF4444?text=划痕'], likes: 2,
    replyOpen: false, reply: '', replied: false
  }
]

// =========== 评价弹窗 ===========
const reviewModal = ref(false)
const reviewTarget = ref({})
const stars = ref(0)
const hoverStar = ref(0)
const activeTags = ref(new Set())
const tagList = ['物流快', '成色好', '成色超预期', '物有所值', '沟通顺畅', '包装完美', '发货快', '专业靠谱']
const reviewContent = ref('')
const reviewImgs = ref([])

const openReview = (item) => {
  reviewTarget.value = { ...item }
  stars.value = 0
  hoverStar.value = 0
  activeTags.value = new Set()
  reviewContent.value = ''
  reviewImgs.value = []
  reviewModal.value = true
}
const toggleTag = (t) => {
  const set = new Set(activeTags.value)
  set.has(t) ? set.delete(t) : set.add(t)
  activeTags.value = set
}
const pickReviewImg = (e) => {
  for (const f of e.target.files) {
    if (reviewImgs.value.length >= 6) break
    reviewImgs.value.push({ name: f.name, url: URL.createObjectURL(f) })
  }
}
const removeReviewImg = (i) => reviewImgs.value.splice(i, 1)
const submitReview = () => {
  if (stars.value === 0) return alert('请先评分')
  alert(`评价提交成功：${stars.value} 星，标签 ${activeTags.value.size} 个`)
  reviewModal.value = false
}

// =========== 其他交互 ===========
const onLike = (item) => { item.likes += 1 }
const submitReply = (item) => {
  if (!item.reply.trim()) return
  if (role.value === 'buyer') item.sellerReply = { time: new Date().toLocaleString(), content: item.reply }
  else { item.replyContent = item.reply; item.replied = true }
  item.replyOpen = false
  item.reply = ''
}
</script>

<template>
  <div class="container-app space-y-6 pb-20">
    <!-- 标题 + 角色切换 -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
      <h1 class="text-2xl font-bold text-brand-ink">评价管理</h1>
      <div class="inline-flex rounded-xl bg-brand-surface-2 p-1 border border-brand-line self-start">
        <button @click="role = 'buyer'"
          :class="['inline-flex items-center gap-2 h-9 px-4 rounded-lg text-sm font-medium transition',
                   role === 'buyer' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">
          <User class="h-4 w-4" /> 我是买家
        </button>
        <button @click="role = 'seller'"
          :class="['inline-flex items-center gap-2 h-9 px-4 rounded-lg text-sm font-medium transition',
                   role === 'seller' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">
          <Store class="h-4 w-4" /> 我是卖家
        </button>
      </div>
    </div>

    <!-- ============== 买家视角 ============== -->
    <div v-if="role === 'buyer'">
      <!-- Tab -->
      <div class="mb-5 flex items-center gap-2 overflow-x-auto no-scrollbar border-b border-brand-line pb-3">
        <button @click="buyerTab = 'pending'"
          :class="['h-9 px-4 rounded-full text-sm font-medium transition border',
                   buyerTab === 'pending' ? 'bg-brand-primary/12 text-brand-primary border-brand-primary/30' : 'text-brand-ink-2 border-transparent hover:bg-brand-surface hover:text-brand-ink']">
          待评价 ({{ buyerPending.length }})
        </button>
        <button @click="buyerTab = 'done'"
          :class="['h-9 px-4 rounded-full text-sm font-medium transition border',
                   buyerTab === 'done' ? 'bg-brand-primary/12 text-brand-primary border-brand-primary/30' : 'text-brand-ink-2 border-transparent hover:bg-brand-surface hover:text-brand-ink']">
          已评价 ({{ buyerDone.length }})
        </button>
      </div>

      <!-- 待评价 -->
      <div v-if="buyerTab === 'pending'" class="space-y-4">
        <article v-for="o in buyerPending" :key="o.orderNo"
          class="rounded-2xl border border-brand-line bg-brand-surface p-4 md:p-5 transition hover:border-brand-primary/40">
          <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div class="flex items-start gap-4">
              <img :src="o.cover" :alt="o.product" class="h-20 w-20 md:h-24 md:w-24 rounded-lg object-cover bg-brand-bg" />
              <div>
                <h3 class="font-medium text-brand-ink line-clamp-2 max-w-md">{{ o.product }}</h3>
                <p class="mt-1 text-sm text-brand-ink-2">卖家：{{ o.seller }}</p>
                <p class="mt-1 text-sm text-brand-ink-3">成交于 {{ o.date }} · {{ o.orderNo }}</p>
              </div>
            </div>
            <div class="flex flex-col items-start gap-3 md:items-end">
              <p class="font-mono text-lg font-bold text-brand-primary">{{ fmt(o.price) }}</p>
              <button @click="openReview(o)"
                class="inline-flex items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 py-2 text-sm font-medium text-brand-primary-ink transition hover:bg-brand-primary/90">
                <Star class="h-3.5 w-3.5" /> 去评价
              </button>
            </div>
          </div>
        </article>
        <div v-if="!buyerPending.length" class="rounded-2xl border border-brand-line bg-brand-surface py-16 text-center">
          <Star class="h-16 w-16 text-brand-ink-3 mx-auto" />
          <h3 class="mt-4 text-lg font-medium text-brand-ink">暂无待评价</h3>
          <p class="mt-1 text-sm text-brand-ink-2">所有订单都评价啦~</p>
        </div>
      </div>

      <!-- 已评价 -->
      <div v-else class="space-y-4">
        <article v-for="r in buyerDone" :key="r.id"
          class="rounded-2xl border border-brand-line bg-brand-surface p-4 md:p-5 transition hover:border-brand-primary/30">
          <div class="flex items-start justify-between gap-4 flex-col md:flex-row">
            <div class="flex items-start gap-4">
              <img :src="r.me.avatar" :alt="r.me.name" class="h-12 w-12 rounded-full object-cover bg-brand-bg" />
              <div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="font-medium text-brand-ink">{{ r.me.name }}</span>
                  <span class="inline-flex items-center gap-1 text-xs text-brand-primary bg-brand-primary/12 px-2 py-0.5 rounded-full">
                    <ShieldCheck class="h-3 w-3" /> 真实买家
                  </span>
                  <span class="text-xs text-brand-ink-3">{{ r.date }}</span>
                </div>
                <div class="mt-1.5 flex items-center gap-0.5">
                  <Star v-for="i in 5" :key="i" class="h-4 w-4" :class="i <= r.star ? 'text-brand-primary fill-brand-primary' : 'text-brand-ink-3'" />
                  <span class="ml-2 text-sm font-medium text-brand-primary">{{ r.star }}.0</span>
                </div>
              </div>
            </div>
            <a href="#" class="flex items-center gap-3 rounded-lg border border-brand-line bg-brand-surface-2 p-2 transition-colors hover:border-brand-primary/30">
              <img :src="r.productCover" class="h-12 w-12 rounded-md object-cover bg-brand-bg" />
              <div class="hidden md:block">
                <p class="text-xs text-brand-ink-2">购买商品</p>
                <p class="max-w-[180px] truncate text-sm text-brand-ink">{{ r.product }}</p>
                <p class="text-xs font-mono text-brand-primary mt-0.5">{{ fmt(r.price) }}</p>
              </div>
            </a>
          </div>
          <div class="mt-4 space-y-3">
            <div v-if="r.tags.length" class="flex flex-wrap gap-2">
              <span v-for="t in r.tags" :key="t" class="rounded-full bg-brand-primary/12 px-2.5 py-1 text-xs font-medium text-brand-primary">{{ t }}</span>
            </div>
            <p class="text-brand-ink leading-relaxed">{{ r.content }}</p>
            <div v-if="r.images.length" class="flex flex-wrap gap-2">
              <img v-for="(img, i) in r.images" :key="i" :src="img" class="h-24 w-24 rounded-lg object-cover bg-brand-bg cursor-zoom-in border border-brand-line" />
            </div>
            <div class="flex items-center gap-3 pt-1">
              <button @click="onLike(r)" class="inline-flex items-center gap-1 text-sm text-brand-ink-2 hover:text-brand-primary transition">
                <ThumbsUp class="h-3.5 w-3.5" /> 有用 ({{ r.likes }})
              </button>
              <button @click="r.replyOpen = !r.replyOpen" class="inline-flex items-center gap-1 text-sm text-brand-ink-2 hover:text-brand-primary transition">
                <MessageCircle class="h-3.5 w-3.5" /> 追评
              </button>
            </div>
            <!-- 追评输入 -->
            <Transition name="slide">
              <div v-if="r.replyOpen" class="rounded-lg border border-brand-line bg-brand-surface-2 p-3 flex flex-col md:flex-row items-stretch md:items-end gap-2">
                <input v-model="r.reply" type="text" placeholder="写下你的追加评论..."
                  class="flex-1 h-10 rounded-lg border border-brand-line bg-brand-bg px-3 text-sm text-brand-ink outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
                <button @click="submitReply(r)"
                  class="h-10 rounded-lg bg-brand-primary px-4 text-sm font-medium text-brand-primary-ink transition hover:bg-brand-primary/90 inline-flex items-center justify-center gap-1.5">
                  <Send class="h-3.5 w-3.5" /> 发布追评
                </button>
              </div>
            </Transition>
            <!-- 卖家回复 -->
            <div v-if="r.sellerReply" class="rounded-lg border border-brand-primary/20 bg-brand-primary/5 p-3">
              <div class="flex items-center gap-2 text-xs mb-1.5">
                <Store class="h-3.5 w-3.5 text-brand-primary" />
                <span class="font-medium text-brand-primary">{{ r.seller }}</span>
                <span class="text-brand-ink-3">· {{ r.sellerReply.time }}</span>
              </div>
              <p class="text-sm text-brand-ink-2">{{ r.sellerReply.content }}</p>
            </div>
          </div>
        </article>
      </div>
    </div>

    <!-- ============== 卖家视角 ============== -->
    <div v-else>
      <div class="mb-5 flex items-center gap-2 overflow-x-auto no-scrollbar border-b border-brand-line pb-3">
        <button @click="sellerTab = 'received'"
          :class="['h-9 px-4 rounded-full text-sm font-medium transition border',
                   sellerTab === 'received' ? 'bg-brand-primary/12 text-brand-primary border-brand-primary/30' : 'text-brand-ink-2 border-transparent hover:bg-brand-surface hover:text-brand-ink']">
          收到的评价
        </button>
        <button @click="sellerTab = 'reply'"
          :class="['h-9 px-4 rounded-full text-sm font-medium transition border',
                   sellerTab === 'reply' ? 'bg-brand-primary/12 text-brand-primary border-brand-primary/30' : 'text-brand-ink-2 border-transparent hover:bg-brand-surface hover:text-brand-ink']">
          待回复 ({{ sellerReceived.filter(r => !r.replied).length }})
        </button>
      </div>

      <div class="space-y-4">
        <article v-for="r in (sellerTab === 'reply' ? sellerReceived.filter(x => !x.replied) : sellerReceived)" :key="r.id"
          class="rounded-2xl border border-brand-line bg-brand-surface p-4 md:p-5 transition hover:border-brand-primary/30">
          <div class="flex items-start justify-between gap-4 flex-col md:flex-row">
            <div class="flex items-start gap-4">
              <img :src="r.buyerAvatar" :alt="r.buyer" class="h-12 w-12 rounded-full object-cover bg-brand-bg" />
              <div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="font-medium text-brand-ink">{{ r.buyer }}</span>
                  <span class="text-xs text-brand-ink-3">{{ r.date }}</span>
                </div>
                <div class="mt-1.5 flex items-center gap-0.5">
                  <Star v-for="i in 5" :key="i" class="h-4 w-4" :class="i <= r.star ? 'text-brand-primary fill-brand-primary' : 'text-brand-ink-3'" />
                  <span class="ml-2 text-sm font-medium" :class="r.star >= 4 ? 'text-brand-success' : r.star === 3 ? 'text-brand-warning' : 'text-brand-error'">{{ r.star }} 星</span>
                </div>
              </div>
            </div>
            <div class="flex items-center gap-3 rounded-lg border border-brand-line bg-brand-surface-2 p-2">
              <img :src="r.productCover" class="h-12 w-12 rounded-md object-cover bg-brand-bg" />
              <div>
                <p class="max-w-[200px] truncate text-sm text-brand-ink">{{ r.product }}</p>
                <p class="text-xs font-mono text-brand-primary mt-0.5">{{ fmt(r.price) }}</p>
              </div>
            </div>
          </div>
          <div class="mt-4 space-y-3">
            <div v-if="r.tags.length" class="flex flex-wrap gap-2">
              <span v-for="t in r.tags" :key="t" class="rounded-full bg-brand-primary/12 px-2.5 py-1 text-xs font-medium text-brand-primary">{{ t }}</span>
            </div>
            <p class="text-brand-ink leading-relaxed">{{ r.content }}</p>
            <div v-if="r.images.length" class="flex flex-wrap gap-2">
              <img v-for="(img, i) in r.images" :key="i" :src="img" class="h-24 w-24 rounded-lg object-cover bg-brand-bg border border-brand-line" />
            </div>
            <div class="flex items-center gap-3 pt-1">
              <span class="text-sm text-brand-ink-2">有用 {{ r.likes }}</span>
              <button v-if="!r.replied" @click="r.replyOpen = !r.replyOpen"
                class="inline-flex items-center gap-1 text-sm text-brand-primary hover:underline transition">
                <MessageCircle class="h-3.5 w-3.5" /> {{ r.replyOpen ? '收起' : '回复' }}
              </button>
              <span v-else class="inline-flex items-center gap-1 text-sm text-brand-success">
                <ChevronUp v-if="r.replyOpen" class="h-3.5 w-3.5" @click="r.replyOpen = false" />
                <ChevronDown v-else class="h-3.5 w-3.5" @click="r.replyOpen = true" />
                已回复
              </span>
            </div>
            <Transition name="slide">
              <div v-if="r.replyOpen" class="space-y-2">
                <div v-if="r.replied && r.replyContent" class="rounded-lg border border-brand-primary/20 bg-brand-primary/5 p-3">
                  <div class="flex items-center gap-2 text-xs mb-1.5">
                    <Store class="h-3.5 w-3.5 text-brand-primary" />
                    <span class="font-medium text-brand-primary">我的回复</span>
                  </div>
                  <p class="text-sm text-brand-ink-2">{{ r.replyContent }}</p>
                </div>
                <div class="rounded-lg border border-brand-line bg-brand-surface-2 p-3 flex flex-col md:flex-row items-stretch md:items-end gap-2">
                  <input v-model="r.reply" type="text" placeholder="请输入回复内容..."
                    class="flex-1 h-10 rounded-lg border border-brand-line bg-brand-bg px-3 text-sm text-brand-ink outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
                  <button @click="submitReply(r)"
                    class="h-10 rounded-lg bg-brand-primary px-4 text-sm font-medium text-brand-primary-ink transition hover:bg-brand-primary/90 inline-flex items-center justify-center gap-1.5">
                    <Send class="h-3.5 w-3.5" /> 发布回复
                  </button>
                </div>
              </div>
            </Transition>
          </div>
        </article>
      </div>
    </div>

    <!-- ============ 评价弹窗 ============ -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="reviewModal"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
          @click.self="reviewModal = false">
          <div class="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl bg-brand-surface border border-brand-line shadow-2xl">
            <div class="flex items-center justify-between p-5 border-b border-brand-line sticky top-0 bg-brand-surface z-10">
              <div>
                <h2 class="text-lg font-semibold text-brand-ink">评价订单</h2>
                <p class="text-xs text-brand-ink-3 mt-0.5">{{ reviewTarget.product }} · 卖家 {{ reviewTarget.seller }}</p>
              </div>
              <button @click="reviewModal = false" class="inline-flex h-9 w-9 items-center justify-center rounded-lg text-brand-ink-2 transition hover:bg-brand-surface-2 hover:text-brand-ink">
                <X class="h-5 w-5" />
              </button>
            </div>
            <div class="p-5 md:p-6 space-y-6">
              <!-- 评分 -->
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink">综合评分</label>
                <div class="flex items-center gap-1" @mouseleave="hoverStar = 0">
                  <button v-for="i in 5" :key="i" type="button"
                    @mouseenter="hoverStar = i" @click="stars = i"
                    class="p-1 transition-transform hover:scale-110">
                    <Star class="h-8 w-8 transition-colors"
                      :class="(hoverStar || stars) >= i ? 'text-brand-primary fill-brand-primary' : 'text-brand-ink-3'" />
                  </button>
                  <span class="ml-3 font-mono text-2xl font-bold text-brand-primary">{{ stars }}.0</span>
                </div>
              </div>
              <!-- 快捷标签 -->
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink">快捷标签（可多选）</label>
                <div class="flex flex-wrap gap-2">
                  <button v-for="t in tagList" :key="t" type="button" @click="toggleTag(t)"
                    :class="['inline-flex items-center h-8 px-3.5 rounded-full text-sm font-medium border transition',
                             activeTags.has(t)
                               ? 'bg-brand-primary/12 text-brand-primary border-brand-primary/35'
                               : 'bg-brand-surface-2 text-brand-ink-2 border-brand-line hover:border-brand-primary hover:text-brand-primary']">
                    {{ t }}
                  </button>
                </div>
              </div>
              <!-- 文字内容 -->
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink">评价内容</label>
                <textarea v-model="reviewContent" rows="4" placeholder="分享你的真实体验，帮助其他买家决策..."
                  class="w-full rounded-lg border border-brand-line bg-brand-surface-2 p-3 text-sm text-brand-ink placeholder:text-brand-ink-3 outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20 resize-y"></textarea>
                <div class="text-right text-xs text-brand-ink-3 mt-1">{{ reviewContent.length }}/500</div>
              </div>
              <!-- 图片 -->
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink">上传图片（最多 6 张）</label>
                <div class="flex flex-wrap gap-3">
                  <div v-for="(img, i) in reviewImgs" :key="i" class="relative w-24 h-24 rounded-lg border border-brand-line bg-brand-surface-2 overflow-hidden">
                    <img :src="img.url" class="w-full h-full object-cover" />
                    <button @click="removeReviewImg(i)" class="absolute top-1 right-1 inline-flex h-6 w-6 items-center justify-center rounded-md bg-black/60 text-white hover:bg-black/80">
                      <X class="h-3.5 w-3.5" />
                    </button>
                  </div>
                  <label v-if="reviewImgs.length < 6"
                    class="w-24 h-24 rounded-lg border border-dashed border-brand-line bg-brand-surface-2 text-brand-ink-3 cursor-pointer hover:border-brand-primary hover:text-brand-primary transition flex flex-col items-center justify-center gap-1">
                    <ImagePlus class="h-5 w-5" />
                    <span class="text-xs">添加图片</span>
                    <input type="file" accept="image/*" multiple class="hidden" @change="pickReviewImg" />
                  </label>
                </div>
              </div>
            </div>
            <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end p-5 border-t border-brand-line">
              <button @click="reviewModal = false"
                class="h-10 rounded-lg border border-brand-line bg-brand-surface px-4 text-sm font-medium text-brand-ink transition hover:border-brand-primary hover:text-brand-primary">取消</button>
              <button @click="submitReview"
                class="h-10 rounded-lg bg-brand-primary px-5 text-sm font-semibold text-brand-primary-ink transition hover:bg-brand-primary/90 inline-flex items-center justify-center gap-2">
                <Star class="h-4 w-4" /> 发布评价
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
@reference "../style.css";
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-enter-active, .slide-leave-active { transition: all 0.25s ease; overflow: hidden; }
.slide-enter-from, .slide-leave-to { opacity: 0; max-height: 0; padding: 0; margin: 0; }
.line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>
