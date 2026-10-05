<script setup>
import { useRouter } from 'vue-router'
import { Home, ShoppingBag, Compass } from 'lucide-vue-next'

const router = useRouter()
</script>

<template>
  <div class="min-h-screen bg-background flex items-center justify-center px-4 py-20">
    <div class="text-center max-w-md">
      <div class="mb-8">
        <div class="relative inline-block">
          <span
            class="text-[140px] md:text-[180px] font-black leading-none bg-gradient-to-br from-primary via-primary/70 to-blue-500 bg-clip-text text-transparent"
            style="-webkit-text-stroke: 1px rgba(0, 212, 170, 0.1);"
          >
            404
          </span>
          <div
            class="pointer-events-none absolute -inset-8 -z-10 blur-3xl opacity-40 rounded-full"
            style="background: radial-gradient(circle, rgba(0, 212, 170, 0.3), transparent 70%);"
          ></div>
        </div>
      </div>

      <h1 class="text-2xl md:text-3xl font-bold text-foreground mb-3">页面走丢了</h1>
      <p class="text-muted-foreground mb-8 leading-relaxed">
        抱歉，您访问的页面不存在或已被移动。<br />
        不妨回到首页继续您的硬件寻宝之旅。
      </p>

      <div class="flex flex-col sm:flex-row items-center justify-center gap-3">
        <button
          @click="router.push('/')"
          class="inline-flex items-center justify-center gap-2 h-11 px-6 rounded-lg bg-primary text-primary-foreground font-semibold transition hover:bg-primary/90 active:translate-y-px w-full sm:w-auto"
        >
          <Home class="w-4.5 h-4.5" />
          返回首页
        </button>
        <button
          @click="router.push('/browse')"
          class="inline-flex items-center justify-center gap-2 h-11 px-6 rounded-lg bg-transparent text-foreground border border-border font-semibold transition hover:bg-surface-2 w-full sm:w-auto"
        >
          <ShoppingBag class="w-4.5 h-4.5" />
          逛逛市场
        </button>
      </div>

      <div class="mt-16 pt-8 border-t border-border">
        <p class="text-xs text-muted-foreground mb-4">热门入口</p>
        <div class="flex flex-wrap items-center justify-center gap-2">
          <button
            @click="router.push('/community')"
            class="inline-flex items-center gap-1.5 h-8 px-3 rounded-full bg-surface-2 text-sm text-muted-foreground hover:text-foreground hover:bg-surface-3 transition"
          >
            <Compass class="w-3.5 h-3.5" />
            硬件社区
          </button>
          <button
            @click="router.push('/trust')"
            class="inline-flex items-center gap-1.5 h-8 px-3 rounded-full bg-surface-2 text-sm text-muted-foreground hover:text-foreground hover:bg-surface-3 transition"
          >
            信任中心
          </button>
          <button
            @click="router.push('/help')"
            class="inline-flex items-center gap-1.5 h-8 px-3 rounded-full bg-surface-2 text-sm text-muted-foreground hover:text-foreground hover:bg-surface-3 transition"
          >
            帮助中心
          </button>
          <button
            @click="router.push('/announcements')"
            class="inline-flex items-center gap-1.5 h-8 px-3 rounded-full bg-surface-2 text-sm text-muted-foreground hover:text-foreground hover:bg-surface-3 transition"
          >
            平台公告
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
