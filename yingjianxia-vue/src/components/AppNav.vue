<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, ShoppingCart, User, Bell, MessageSquare, Tag, Store, Menu, X, LogOut, Settings, ShieldCheck } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import * as api from '@/api/index'

const userStore = useUserStore()
const cartStore = useCartStore()
const router = useRouter()
const route = useRoute()

const keyword = ref('')
const showUserMenu = ref(false)
const mobileMenuOpen = ref(false)
const unreadCount = ref(0)

const categories = ref([
  { id: 'cpu', name: 'CPU 处理器', icon: 'Cpu' },
  { id: 'gpu', name: '显卡', icon: 'MonitorSpeaker' },
  { id: 'motherboard', name: '主板', icon: 'CircuitBoard' },
  { id: 'ram', name: '内存条', icon: 'HardDrive' },
  { id: 'ssd', name: '存储 SSD', icon: 'Database' },
  { id: 'psu', name: '电源', icon: 'Zap' },
  { id: 'case', name: '机箱', icon: 'Box' },
  { id: 'cooler', name: '散热器', icon: 'Fan' },
  { id: 'laptop', name: '笔记本', icon: 'Laptop' },
  { id: 'monitor', name: '显示器', icon: 'Monitor' }
])

const navLinks = computed(() => [
  { path: '/', name: '首页', match: ['/'] },
  { path: '/browse', name: '找货', match: ['/browse', '/search', '/product'] },
  { path: '/community', name: '社区', match: ['/community'] },
  { path: '/help', name: '帮助', match: ['/help', '/tickets', '/chatbot', '/trust'] }
])

function isActive(path) {
  if (path === '/') return route.path === '/'
  const conf = navLinks.value.find(n => n.path === path)
  return conf?.match?.some(m => route.path.startsWith(m)) || false
}

function handleSearch() {
  if (!keyword.value.trim()) {
    router.push({ path: '/browse' })
    return
  }
  router.push({ path: '/search', query: { q: keyword.value } })
}

function navigate(path) {
  router.push(path)
  mobileMenuOpen.value = false
  showUserMenu.value = false
}

function logout() {
  userStore.logout()
  router.push('/')
}

async function loadUnread() {
  if (!userStore.isLoggedIn) return
  try {
    unreadCount.value = await api.getUnreadCountApi()
  } catch {}
}

let unreadTimer = null
onMounted(() => {
  loadUnread()
  unreadTimer = setInterval(loadUnread, 30000)
})
onBeforeUnmount(() => {
  if (unreadTimer) clearInterval(unreadTimer)
})
</script>

<template>
  <header class="sticky top-0 z-50 bg-brand-bg/90 backdrop-blur-md border-b border-brand-line">
    <!-- 顶栏 -->
    <div class="container-app py-3 flex items-center gap-4">
      <!-- Logo -->
      <a @click="navigate('/')" class="flex items-center gap-2 cursor-pointer shrink-0">
        <div class="w-9 h-9 rounded-lg bg-brand-primary/15 border border-brand-primary/30 flex items-center justify-center">
          <Tag class="w-5 h-5 text-brand-primary" />
        </div>
        <div class="hidden sm:block leading-tight">
          <div class="text-lg font-bold text-brand-ink tracking-tight">硬件侠</div>
          <div class="text-[10px] text-brand-ink-3 uppercase tracking-[0.2em]">HARDWARE · XIA</div>
        </div>
      </a>

      <!-- 搜索框 -->
      <div class="flex-1 max-w-2xl">
        <div class="flex h-10 rounded-lg overflow-hidden bg-brand-surface border border-brand-line focus-within:border-brand-primary transition-colors">
          <div class="pl-3 flex items-center text-brand-ink-3">
            <Search class="w-4 h-4" />
          </div>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索 CPU、显卡、主板、二手笔记本..."
            class="flex-1 bg-transparent px-3 text-sm text-brand-ink placeholder:text-brand-ink-3 focus:outline-none"
            @keyup.enter="handleSearch"
          />
          <button
            @click="handleSearch"
            class="px-5 bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition-colors"
          >搜索</button>
        </div>
      </div>

      <!-- 移动端菜单按钮 -->
      <button @click="mobileMenuOpen = !mobileMenuOpen" class="lg:hidden text-brand-ink-2 hover:text-brand-primary p-2">
        <Menu v-if="!mobileMenuOpen" class="w-6 h-6" />
        <X v-else class="w-6 h-6" />
      </button>

      <!-- 右侧操作区 -->
      <div class="hidden lg:flex items-center gap-1">
        <!-- 卖家入口 -->
        <button v-if="userStore.isLoggedIn && userStore.isSeller" @click="navigate('/seller')"
          class="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-2 transition">
          <Store class="w-4 h-4" />卖家中心
        </button>
        <button v-else-if="userStore.isLoggedIn" @click="navigate('/store-settings')"
          class="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-brand-primary hover:bg-brand-primary-subtle transition">
          <Store class="w-4 h-4" />免费开店
        </button>
        <button v-else @click="navigate('/auth')"
          class="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-brand-primary hover:bg-brand-primary-subtle transition">
          <Store class="w-4 h-4" />免费开店
        </button>

        <!-- 消息 -->
        <button v-if="userStore.isLoggedIn" @click="navigate('/messages')"
          class="relative p-2 rounded-lg text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-2 transition">
          <MessageSquare class="w-5 h-5" />
          <span v-if="unreadCount > 0" class="absolute top-1 right-1 min-w-4 h-4 px-1 rounded-full bg-brand-error text-[10px] font-bold text-white flex items-center justify-center">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </button>

        <!-- 通知 -->
        <button v-if="userStore.isLoggedIn" @click="navigate('/announcements')"
          class="p-2 rounded-lg text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-2 transition">
          <Bell class="w-5 h-5" />
        </button>

        <!-- 购物车 -->
        <button @click="navigate('/cart')"
          class="relative p-2 rounded-lg text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-2 transition">
          <ShoppingCart class="w-5 h-5" />
          <span v-if="cartStore.totalCount > 0" class="absolute top-1 right-1 min-w-4 h-4 px-1 rounded-full bg-brand-primary text-[10px] font-bold text-brand-primary-ink flex items-center justify-center">
            {{ cartStore.totalCount > 99 ? '99+' : cartStore.totalCount }}
          </span>
        </button>

        <!-- 用户菜单 -->
        <div v-if="userStore.isLoggedIn" class="relative ml-1">
          <button @click="showUserMenu = !showUserMenu" class="flex items-center gap-2 px-3 py-1.5 rounded-lg hover:bg-brand-surface-2 transition">
            <div class="w-8 h-8 rounded-full bg-brand-primary/20 border border-brand-primary/40 flex items-center justify-center text-brand-primary text-sm font-bold">
              {{ userStore.userInfo?.nickname?.charAt(0) || userStore.userInfo?.phone?.slice(-4) || 'U' }}
            </div>
          </button>
          <div v-if="showUserMenu" class="absolute right-0 mt-2 w-48 rounded-lg bg-brand-surface border border-brand-line shadow-2 overflow-hidden z-50">
            <div class="px-4 py-3 border-b border-brand-line bg-brand-surface-2">
              <div class="text-sm font-medium text-brand-ink">{{ userStore.userInfo?.nickname || '用户' }}</div>
              <div class="text-xs text-brand-ink-3 mt-0.5">{{ userStore.userInfo?.phone || '' }}</div>
            </div>
            <button @click="navigate('/profile')" class="w-full px-4 py-2.5 text-left text-sm text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-3 transition flex items-center gap-2">
              <User class="w-4 h-4" />个人中心
            </button>
            <button v-if="userStore.isSeller" @click="navigate('/seller')"
              class="w-full px-4 py-2.5 text-left text-sm text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-3 transition flex items-center gap-2">
              <Settings class="w-4 h-4" />卖家中心
            </button>
            <button @click="navigate('/orders')" class="w-full px-4 py-2.5 text-left text-sm text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-3 transition flex items-center gap-2">
              我的订单
            </button>
            <button @click="navigate('/wallet')" class="w-full px-4 py-2.5 text-left text-sm text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-3 transition flex items-center gap-2">
              钱包
            </button>
            <button @click="navigate('/audit')" class="w-full px-4 py-2.5 text-left text-sm text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-3 transition flex items-center gap-2">
              <ShieldCheck class="w-4 h-4" />审核中心
            </button>
            <div class="border-t border-brand-line my-1"></div>
            <button @click="logout" class="w-full px-4 py-2.5 text-left text-sm text-brand-error hover:bg-brand-error-subtle transition flex items-center gap-2">
              <LogOut class="w-4 h-4" />退出登录
            </button>
          </div>
        </div>
        <button v-else @click="navigate('/auth')"
          class="flex items-center gap-2 px-4 py-2 rounded-lg bg-brand-primary text-brand-primary-ink text-sm font-medium hover:bg-brand-primary-hover transition">
          <User class="w-4 h-4" />登录
        </button>
      </div>
    </div>

    <!-- 分类导航条 -->
    <div class="border-t border-brand-line-subtle bg-brand-surface/40">
      <div class="container-app h-10 flex items-center gap-1 overflow-x-auto no-scrollbar">
        <button v-for="cat in categories" :key="cat.id"
          @click="router.push({ path: '/browse', query: { category: cat.id } })"
          class="shrink-0 px-3 h-8 rounded-md text-xs text-brand-ink-2 hover:text-brand-primary hover:bg-brand-surface-2 transition whitespace-nowrap">
          {{ cat.name }}
        </button>
      </div>
    </div>

    <!-- 移动端菜单 -->
    <div v-if="mobileMenuOpen" class="lg:hidden border-t border-brand-line bg-brand-surface-2">
      <div class="p-3 space-y-2">
        <button v-for="n in navLinks" :key="n.path" @click="navigate(n.path)"
          :class="['w-full text-left px-3 py-2.5 rounded-lg text-sm transition',
                   isActive(n.path) ? 'text-brand-primary bg-brand-primary-subtle' : 'text-brand-ink-2 hover:bg-brand-surface-3']">
          {{ n.name }}
        </button>
        <div class="border-t border-brand-line pt-2 space-y-2">
          <button @click="navigate('/cart')" class="w-full text-left px-3 py-2.5 rounded-lg text-sm text-brand-ink-2 hover:bg-brand-surface-3 transition">购物车 ({{ cartStore.totalCount }})</button>
          <button @click="navigate('/messages')" class="w-full text-left px-3 py-2.5 rounded-lg text-sm text-brand-ink-2 hover:bg-brand-surface-3 transition">消息</button>
          <button v-if="userStore.isLoggedIn" @click="navigate('/profile')" class="w-full text-left px-3 py-2.5 rounded-lg text-sm text-brand-ink-2 hover:bg-brand-surface-3 transition">个人中心</button>
          <button v-if="userStore.isLoggedIn" @click="navigate('/wallet')" class="w-full text-left px-3 py-2.5 rounded-lg text-sm text-brand-ink-2 hover:bg-brand-surface-3 transition">钱包</button>
          <button v-if="userStore.isLoggedIn" @click="navigate('/orders')" class="w-full text-left px-3 py-2.5 rounded-lg text-sm text-brand-ink-2 hover:bg-brand-surface-3 transition">我的订单</button>
          <button v-if="!userStore.isLoggedIn" @click="navigate('/auth')" class="w-full text-left px-3 py-2.5 rounded-lg text-sm bg-brand-primary text-brand-primary-ink font-medium">登录 / 注册</button>
          <button v-if="userStore.isLoggedIn" @click="logout" class="w-full text-left px-3 py-2.5 rounded-lg text-sm text-brand-error hover:bg-brand-error-subtle transition">退出登录</button>
        </div>
      </div>
    </div>
  </header>
</template>
