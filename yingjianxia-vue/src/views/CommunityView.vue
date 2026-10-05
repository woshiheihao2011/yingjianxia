<template>
  <div class="min-h-screen bg-background text-foreground">
    <!-- Hero -->
    <section class="relative overflow-hidden" id="community-hero">
      <div class="absolute inset-0 bg-gradient-to-r from-background via-background/85 to-background/70"></div>
      <div class="relative max-w-7xl mx-auto px-4 py-14 lg:py-24">
        <div class="max-w-2xl space-y-6">
          <h1 class="text-4xl lg:text-5xl font-bold tracking-tight">硬件社区</h1>
          <p class="text-lg text-muted-foreground">装机指南、避坑攻略、硬件评测、二手验机技巧，和万千发烧友一起交流。</p>
          <div class="flex flex-col sm:flex-row gap-4">
            <form class="relative flex-1 max-w-md" role="search" @submit.prevent="scrollToPosts">
              <input
                v-model="search"
                type="search"
                placeholder="搜索帖子、关键词…"
                class="w-full h-12 pl-11 pr-4 rounded-lg bg-background border border-border text-foreground placeholder:text-muted-foreground outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
              />
              <Search class="absolute left-3.5 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
            </form>
            <RouterLink to="#post-form" class="btn-primary">
              <PenLine class="w-4 h-4" />
              发布攻略
            </RouterLink>
          </div>
          <div class="flex flex-wrap gap-6 text-sm text-muted-foreground">
            <span class="flex items-center gap-2"><FileText class="w-4 h-4 text-primary" />12.8k 篇攻略</span>
            <span class="flex items-center gap-2"><Users class="w-4 h-4 text-primary" />4.2k 位达人</span>
            <span class="flex items-center gap-2"><MessageCircle class="w-4 h-4 text-primary" />56k 条讨论</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 内容分类标签 -->
    <section class="sticky top-16 z-30 bg-background/95 backdrop-blur border-b border-border" id="community-tabs">
      <div class="max-w-7xl mx-auto px-4 py-3">
        <div class="flex items-center gap-2 overflow-x-auto no-scrollbar">
          <button
            v-for="t in tabs"
            :key="t.value"
            class="tab"
            :class="{ 'active': activeTab === t.value }"
            @click="activeTab = t.value"
          >{{ t.label }}</button>
        </div>
      </div>
    </section>

    <!-- 主内容区 -->
    <section class="py-8" id="posts-area">
      <div class="max-w-7xl mx-auto px-4">
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
          <!-- 左侧内容 -->
          <div class="lg:col-span-8 space-y-10">
            <!-- 精选攻略 -->
            <div>
              <h2 class="text-xl font-bold mb-4 flex items-center gap-2">
                <Sparkles class="w-5 h-5 text-primary" />
                精选攻略
              </h2>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div v-for="(f, idx) in featured" :key="idx" class="card group" :class="{ 'md:col-span-2': idx === 2 }">
                  <div :class="idx === 2 ? 'grid md:grid-cols-2' : ''">
                    <div :class="idx === 2 ? 'aspect-video md:aspect-auto' : 'aspect-video'" class="overflow-hidden bg-surface-2 flex items-center justify-center">
                      <component :is="f.icon" class="w-20 h-20 text-primary/40 group-hover:scale-105 transition" />
                    </div>
                    <div :class="idx === 2 ? 'p-5 space-y-3 flex flex-col justify-center' : 'p-4 space-y-3'">
                      <span class="tag inline-flex w-fit">{{ f.tag }}</span>
                      <h3 :class="idx === 2 ? 'text-lg font-semibold line-clamp-2' : 'font-semibold line-clamp-2'">{{ f.title }}</h3>
                      <p v-if="idx === 2" class="text-sm text-muted-foreground line-clamp-2">{{ f.desc }}</p>
                      <div class="flex items-center justify-between" :class="{ 'pt-1': idx === 2 }">
                        <div class="flex items-center gap-2">
                          <div class="w-8 h-8 rounded-full bg-surface-3 flex items-center justify-center text-primary text-xs font-semibold">{{ f.author[0] }}</div>
                          <span class="text-sm text-muted-foreground">{{ f.author }}</span>
                        </div>
                        <div class="flex items-center gap-3 text-xs text-muted-foreground">
                          <span class="flex items-center gap-1"><Heart class="w-3.5 h-3.5" />{{ f.likes }}</span>
                          <span class="flex items-center gap-1"><MessageSquare class="w-3.5 h-3.5" />{{ f.comments }}</span>
                          <span v-if="idx === 2" class="flex items-center gap-1"><Eye class="w-3.5 h-3.5" />{{ f.views }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 帖子列表 -->
            <div>
              <div class="flex items-center justify-between mb-4">
                <h2 class="text-xl font-bold flex items-center gap-2">
                  <ListOrdered class="w-5 h-5 text-primary" />
                  最新帖子
                </h2>
                <div class="flex items-center gap-2 text-sm text-muted-foreground">
                  <button class="hover:text-foreground transition" :class="{ 'text-foreground': sort === 'new' }" @click="sort = 'new'">最新</button>
                  <span>/</span>
                  <button class="hover:text-foreground transition" :class="{ 'text-foreground': sort === 'hot' }" @click="sort = 'hot'">最热</button>
                </div>
              </div>
              <div class="space-y-4">
                <article v-for="(p, idx) in posts" :key="idx" class="card p-4 sm:p-5 hover:border-primary transition">
                  <div class="flex items-start gap-4">
                    <div class="w-10 h-10 rounded-full bg-surface-3 flex items-center justify-center shrink-0 text-primary font-semibold">{{ p.author[0] }}</div>
                    <div class="flex-1 min-w-0">
                      <div class="flex flex-wrap items-center gap-2 mb-2">
                        <span class="text-sm font-medium">{{ p.author }}</span>
                        <span class="text-xs text-muted-foreground">{{ p.time }}</span>
                        <span class="tag">{{ p.tag }}</span>
                      </div>
                      <h3 class="font-semibold mb-1 hover:text-primary transition cursor-pointer">{{ p.title }}</h3>
                      <p class="text-sm text-muted-foreground line-clamp-2 mb-3">{{ p.desc }}</p>
                      <div class="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
                        <span class="flex items-center gap-1"><ThumbsUp class="w-3.5 h-3.5" />{{ p.likes }}</span>
                        <span class="flex items-center gap-1"><MessageSquare class="w-3.5 h-3.5" />{{ p.comments }}</span>
                        <span class="flex items-center gap-1"><Eye class="w-3.5 h-3.5" />{{ p.views }}</span>
                      </div>
                    </div>
                  </div>
                </article>
              </div>
            </div>

            <!-- 分页 -->
            <nav class="flex items-center justify-center gap-2 pt-2">
              <button class="page-btn" aria-label="上一页"><ChevronLeft class="w-4 h-4" /></button>
              <button v-for="p in 5" :key="p" class="page-btn" :class="{ 'active': p === 1 }">{{ p }}</button>
              <button class="page-btn" aria-label="下一页"><ChevronRight class="w-4 h-4" /></button>
            </nav>
          </div>

          <!-- 右侧边栏 -->
          <aside class="lg:col-span-4 space-y-6">
            <!-- 热门话题 -->
            <div class="widget">
              <h3 class="font-semibold mb-4 flex items-center gap-2">
                <Flame class="w-4 h-4 text-primary" />
                热门话题
              </h3>
              <ul class="space-y-3">
                <li v-for="(h, idx) in hotTopics" :key="idx" class="flex items-center gap-3">
                  <span class="rank" :class="idx < 3 ? 'rank-hot' : 'rank-normal'">{{ idx + 1 }}</span>
                  <div class="flex-1 min-w-0">
                    <a href="#" class="text-sm font-medium hover:text-primary truncate block">{{ h.title }}</a>
                    <span class="text-xs text-muted-foreground">{{ h.discuss }} 讨论</span>
                  </div>
                </li>
              </ul>
            </div>

            <!-- 社区达人榜 -->
            <div class="widget">
              <h3 class="font-semibold mb-4 flex items-center gap-2">
                <Trophy class="w-4 h-4 text-primary" />
                社区达人榜
              </h3>
              <ul class="space-y-3">
                <li v-for="(u, idx) in topUsers" :key="idx" class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-full bg-surface-3 flex items-center justify-center shrink-0 text-primary font-semibold">{{ u.name[0] }}</div>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center justify-between">
                      <span class="text-sm font-medium">{{ u.name }}</span>
                      <span class="text-xs text-primary bg-primary/12 px-2 py-0.5 rounded">Lv.{{ u.level }}</span>
                    </div>
                    <span class="text-xs text-muted-foreground">发布 {{ u.posts }} 篇攻略 · 获赞 {{ u.likes }}</span>
                  </div>
                </li>
              </ul>
            </div>

            <!-- 最新活动 -->
            <div class="widget">
              <h3 class="font-semibold mb-4 flex items-center gap-2">
                <CalendarDays class="w-4 h-4 text-primary" />
                最新活动
              </h3>
              <div class="space-y-3">
                <div v-for="(a, idx) in activities" :key="idx" class="block p-3 rounded-lg bg-muted hover:bg-surface-3 transition cursor-pointer">
                  <div class="flex items-center justify-between mb-1">
                    <span class="text-xs font-medium" :class="a.status === '进行中' ? 'text-primary' : a.status === '报名中' ? 'text-primary' : 'text-muted-foreground'">{{ a.status }}</span>
                    <span class="text-xs text-muted-foreground">{{ a.date }}</span>
                  </div>
                  <h4 class="text-sm font-medium">{{ a.title }}</h4>
                  <p class="text-xs text-muted-foreground mt-1">{{ a.desc }}</p>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </section>

    <!-- 发帖入口 -->
    <section id="post-form" class="py-16 border-t border-border">
      <div class="max-w-3xl mx-auto px-4">
        <div class="card p-6 lg:p-8 space-y-6">
          <div class="flex items-center gap-3">
            <PenTool class="w-6 h-6 text-primary" />
            <h2 class="text-2xl font-bold">发布攻略 / 提问</h2>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-2">
              <label class="text-sm font-medium">标题</label>
              <input v-model="form.title" type="text" placeholder="一句话描述你的问题或经验" class="form-input">
            </div>
            <div class="space-y-2">
              <label class="text-sm font-medium">分类</label>
              <select v-model="form.category" class="form-input">
                <option>装机指南</option>
                <option>避坑攻略</option>
                <option>硬件评测</option>
                <option>二手验机</option>
                <option>问答互助</option>
                <option>晒单</option>
              </select>
            </div>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium">正文内容</label>
            <textarea v-model="form.content" rows="8" placeholder="详细描述你的经验或问题，建议附上配置清单、截图或测试数据…" class="form-input"></textarea>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-medium">添加标签</label>
            <div class="flex flex-wrap gap-2">
              <span v-for="t in form.tags" :key="t" class="tag cursor-pointer hover:bg-rose-500/20 hover:text-rose-400" @click="removeTag(t)">
                #{{ t }} <X class="w-3 h-3 ml-1" />
              </span>
              <div class="flex items-center gap-2">
                <input v-model="newTag" type="text" placeholder="回车添加" class="form-input h-8 w-32 text-sm" @keyup.enter="addTag">
              </div>
            </div>
          </div>
          <div class="flex items-center gap-3 pt-2">
            <button type="button" class="btn-primary" @click="submitPost">
              <Send class="w-4 h-4" />
              发布
            </button>
            <button type="button" class="btn-secondary">
              <Image class="w-4 h-4" />
              上传图片
            </button>
            <button type="button" class="btn-secondary">保存草稿</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  Search, PenLine, FileText, Users, MessageCircle, Sparkles,
  Heart, Eye, ListOrdered, ThumbsUp, ChevronLeft, ChevronRight,
  Flame, Trophy, CalendarDays, PenTool, X, Send, Image,
  Gauge, Cpu, Monitor, MessageSquare
} from 'lucide-vue-next'

const search = ref('')
const activeTab = ref('all')
const sort = ref('new')
const form = ref({ title: '', category: '装机指南', content: '', tags: ['RTX40', '验机技巧'] })
const newTag = ref('')

const tabs = [
  { label: '全部', value: 'all' },
  { label: '装机指南', value: 'guide' },
  { label: '避坑攻略', value: 'pitfall' },
  { label: '硬件评测', value: 'review' },
  { label: '二手验机', value: 'inspection' },
  { label: '问答互助', value: 'qa' },
  { label: '晒单', value: 'showcase' }
]

const featured = [
  { tag: '二手验机', title: '二手 RTX 40 系显卡验机清单：从外观到上机的 12 步', author: '极客阿杰', likes: '1.2k', comments: 86, icon: Gauge },
  { tag: '装机指南', title: '2026 年中端游戏主机装机方案：万元内性价比之选', author: '数码老张', likes: '986', comments: 64, icon: Monitor },
  { tag: '硬件评测', title: 'Intel i5-13600K 一年长测：游戏与生产力的真实表现', desc: '用了整整一年，从温度、功耗、游戏帧数到二手残值，给你一份 unbiased 的参考。', author: '装机小队', likes: '2.4k', comments: 152, views: '18k', icon: Cpu }
]

const posts = [
  { tag: '装机指南', title: '第一次装机？这份电源选型和走线指南请收好', desc: '从功率计算到模组线选择，再到机箱风道，手把手教你避开新手常见坑。', author: '极客阿杰', time: '2 小时前', likes: 328, comments: 42, views: '5.2k' },
  { tag: '避坑攻略', title: '闲鱼买 CPU 遇到的 5 种常见套路与反制方法', desc: '从"工程样品"到"换盖 U"，汇总了近期高发的骗局特征，看完少踩一半坑。', author: '数码老张', time: '5 小时前', likes: 856, comments: 127, views: '12k' },
  { tag: '问答互助', title: 'B760 主板带 13600K 需要关闭 CEP 吗？实测对比', desc: '默认 BIOS 温度偏高，关闭 CEP 后烤机降了 12 度，但游戏帧数基本没变化。', author: '小白装机', time: '昨天', likes: 215, comments: 38, views: '4.1k' },
  { tag: '晒单', title: '全二手配件组了一台 itx 小钢炮，总价不到 4k', desc: '从机箱到散热全部捡垃圾，附上配置单和购买渠道，供大家参考。', author: '装机小队', time: '2 天前', likes: 672, comments: 95, views: '9.8k' },
  { tag: '二手验机', title: '硬盘健康度 95% 还能买吗？SSD 寿命判断指南', desc: '看懂 SMART 关键项、写入量与 TBW，教你判断二手 SSD 是否值得入手。', author: '验机达人', time: '3 天前', likes: 453, comments: 56, views: '7.3k' }
]

const hotTopics = [
  { title: 'RTX 40 系二手显卡验机清单', discuss: '2.3k' },
  { title: '万元内性价比装机方案', discuss: '1.8k' },
  { title: '二手 CPU 常见骗局汇总', discuss: '1.5k' },
  { title: 'DDR4 还能战几年', discuss: '982' },
  { title: 'itx 小钢炮散热方案', discuss: '876' }
]

const topUsers = [
  { name: '数码老张', level: 9, posts: 342, likes: '12k' },
  { name: '极客阿杰', level: 8, posts: 286, likes: '9.8k' },
  { name: '装机小队', level: 7, posts: 198, likes: '7.2k' },
  { name: '验机达人', level: 7, posts: 165, likes: '6.5k' }
]

const activities = [
  { status: '进行中', title: '晒单赢机械键盘', desc: '发布装机/晒单帖，点赞前 10 名获奖励。', date: '08-20 ~ 09-20' },
  { status: '报名中', title: '二手验机知识竞赛', desc: '答题瓜分平台积分与验机优惠券。', date: '09-01 ~ 09-15' },
  { status: '已结束', title: '暑期装机打卡挑战', desc: '累计发布 3 篇攻略即可解锁勋章。', date: '07-15 ~ 07-30' }
]

function scrollToPosts() { document.getElementById('posts-area')?.scrollIntoView({ behavior: 'smooth' }) }
function addTag() {
  const t = newTag.value.trim()
  if (t && !form.value.tags.includes(t)) form.value.tags.push(t)
  newTag.value = ''
}
function removeTag(t) { form.value.tags = form.value.tags.filter(x => x !== t) }
function submitPost() {
  alert('帖子已提交审核！')
  form.value = { title: '', category: '装机指南', content: '', tags: [] }
}
</script>

<style scoped>
@reference "../style.css";
.btn-primary {
  @apply inline-flex items-center justify-center gap-2 h-11 px-6 rounded-lg bg-primary text-primary-foreground font-semibold transition hover:bg-[var(--brand-primary-hover)] active:translate-y-px;
}
.btn-secondary {
  @apply inline-flex items-center justify-center gap-2 h-11 px-5 rounded-lg bg-transparent border border-border text-foreground font-medium transition hover:bg-surface-2;
}
.card {
  @apply rounded-2xl bg-card border border-border overflow-hidden transition cursor-pointer;
}
.card:hover {
  @apply -translate-y-1 border-primary;
}
.tag {
  @apply inline-flex items-center h-6 px-2.5 rounded-full bg-surface-2 text-muted-foreground text-xs font-medium transition;
}
.tag:hover {
  @apply text-primary bg-primary/12;
}
.tab {
  @apply inline-flex items-center h-9 px-4 rounded-full text-muted-foreground text-sm font-medium transition whitespace-nowrap;
}
.tab:hover {
  @apply text-foreground bg-surface-2;
}
.tab.active {
  @apply text-primary-foreground bg-primary;
}
.page-btn {
  @apply inline-flex items-center justify-center min-w-9 h-9 px-2.5 rounded-lg bg-card border border-border text-muted-foreground text-sm font-medium transition;
}
.page-btn:hover {
  @apply bg-surface-2 text-foreground;
}
.page-btn.active {
  @apply bg-primary text-primary-foreground border-primary;
}
.rank {
  @apply inline-flex items-center justify-center w-6 h-6 rounded text-xs font-bold;
}
.rank-hot {
  @apply bg-primary/12 text-primary;
}
.rank-normal {
  @apply bg-surface-2 text-muted-foreground;
}
.widget {
  @apply rounded-2xl bg-card border border-border p-5;
}
.form-input {
  @apply w-full h-11 px-3.5 rounded-lg bg-background border border-border text-foreground outline-none transition text-sm placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20;
}
textarea.form-input {
  @apply h-auto py-3 resize-y;
}
select.form-input {
  @apply appearance-none bg-no-repeat;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%236B7280' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-position: right 12px center;
  padding-right: 36px;
}
</style>
