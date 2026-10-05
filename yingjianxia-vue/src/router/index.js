import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  // ===== 买家域 =====
  { path: '/', name: 'Index', component: () => import('@/views/IndexView.vue'), meta: { title: '硬件侠 - 电脑配件二手交易平台' } },
  { path: '/browse', name: 'Browse', component: () => import('@/views/BrowseView.vue'), meta: { title: '浏览商品 - 硬件侠' } },
  { path: '/category', redirect: '/browse' },
  { path: '/favorites', name: 'Favorites', component: () => import('@/views/FavoritesView.vue'), meta: { title: '我的收藏 - 硬件侠', requiresAuth: true } },
  { path: '/search', name: 'Search', component: () => import('@/views/SearchView.vue'), meta: { title: '搜索 - 硬件侠' } },
  { path: '/product/:id', name: 'Product', component: () => import('@/views/ProductView.vue'), meta: { title: '商品详情 - 硬件侠' } },
  { path: '/cart', name: 'Cart', component: () => import('@/views/CartView.vue'), meta: { title: '购物车 - 硬件侠', requiresAuth: true } },
  { path: '/orders', name: 'Orders', component: () => import('@/views/OrdersView.vue'), meta: { title: '订单中心 - 硬件侠', requiresAuth: true } },
  { path: '/reviews', name: 'Reviews', component: () => import('@/views/ReviewsView.vue'), meta: { title: '评价管理 - 硬件侠', requiresAuth: true } },
  { path: '/aftersales', name: 'Aftersales', component: () => import('@/views/AftersalesView.vue'), meta: { title: '售后服务 - 硬件侠', requiresAuth: true } },
  { path: '/messages', name: 'Messages', component: () => import('@/views/MessagesView.vue'), meta: { title: '消息 - 硬件侠', requiresAuth: true } },
  { path: '/community', name: 'Community', component: () => import('@/views/CommunityView.vue'), meta: { title: '硬件社区 - 硬件侠' } },

  // ===== 买家个人中心子路由（兼容旧路径） =====
  { path: '/profile', name: 'Profile', component: () => import('@/views/ProfileView.vue'), meta: { title: '个人中心 - 硬件侠', requiresAuth: true } },
  { path: '/profile/address', redirect: '/profile' },
  { path: '/profile/reviews', redirect: '/reviews' },

  // ===== 卖家域 =====
  { path: '/sell', name: 'Sell', component: () => import('@/views/SellView.vue'), meta: { title: '发布商品 - 硬件侠', requiresAuth: true } },
  { path: '/seller/:id', name: 'Seller', component: () => import('@/views/SellerView.vue'), meta: { title: '卖家店铺 - 硬件侠' } },
  // 卖家工作台（兼容 /seller 及 /seller/* 路径，统一重定向到对应功能页）
  { path: '/seller', redirect: '/analytics' },
  { path: '/seller/products', redirect: '/listings' },
  { path: '/seller/inventory', redirect: '/inventory' },
  { path: '/seller/orders', redirect: '/orders' },
  { path: '/seller/shipping', redirect: '/shipping' },
  { path: '/seller/dashboard', redirect: '/analytics' },
  { path: '/seller/wallet', redirect: '/wallet' },
  { path: '/seller/settings', redirect: '/store-settings' },
  { path: '/seller/marketing', redirect: '/promotions' },
  { path: '/listings', name: 'Listings', component: () => import('@/views/ListingsView.vue'), meta: { title: '我的发布 - 硬件侠', requiresAuth: true } },
  { path: '/inventory', name: 'Inventory', component: () => import('@/views/InventoryView.vue'), meta: { title: '库存预警 - 硬件侠', requiresAuth: true } },
  { path: '/shipping', name: 'Shipping', component: () => import('@/views/ShippingView.vue'), meta: { title: '发货管理 - 硬件侠', requiresAuth: true } },
  { path: '/logistics', name: 'Logistics', component: () => import('@/views/LogisticsView.vue'), meta: { title: '物流跟踪 - 硬件侠', requiresAuth: true } },
  { path: '/analytics', name: 'Analytics', component: () => import('@/views/AnalyticsView.vue'), meta: { title: '数据中心 - 硬件侠', requiresAuth: true } },
  { path: '/wallet', name: 'Wallet', component: () => import('@/views/WalletView.vue'), meta: { title: '钱包 - 硬件侠', requiresAuth: true } },
  { path: '/store-settings', name: 'StoreSettings', component: () => import('@/views/StoreSettingsView.vue'), meta: { title: '店铺设置 - 硬件侠', requiresAuth: true } },
  { path: '/promotions', name: 'Promotions', component: () => import('@/views/PromotionsView.vue'), meta: { title: '营销活动 - 硬件侠', requiresAuth: true } },
  { path: '/transactions', name: 'Transactions', component: () => import('@/views/TransactionsView.vue'), meta: { title: '收支明细 - 硬件侠', requiresAuth: true } },

  // ===== 平台支撑域 =====
  { path: '/auth', name: 'Auth', component: () => import('@/views/AuthView.vue'), meta: { title: '登录 / 注册 - 硬件侠', guestOnly: true } },
  { path: '/trust', name: 'Trust', component: () => import('@/views/TrustView.vue'), meta: { title: '信任中心 - 硬件侠' } },
  { path: '/help', name: 'Help', component: () => import('@/views/HelpView.vue'), meta: { title: '帮助中心 - 硬件侠' } },
  { path: '/about', redirect: '/trust' },
  { path: '/tickets', name: 'Tickets', component: () => import('@/views/TicketsView.vue'), meta: { title: '客服工单 - 硬件侠', requiresAuth: true } },
  { path: '/ticket', redirect: '/tickets' },
  { path: '/chatbot', name: 'Chatbot', component: () => import('@/views/ChatbotView.vue'), meta: { title: '客服快捷回复配置 - 硬件侠', requiresAuth: true } },
  { path: '/announcements', name: 'Announcements', component: () => import('@/views/AnnouncementsView.vue'), meta: { title: '公告通知 - 硬件侠' } },
  { path: '/notice', redirect: '/announcements' },
  { path: '/coupons', name: 'Coupons', component: () => import('@/views/CouponsView.vue'), meta: { title: '优惠券积分 - 硬件侠', requiresAuth: true } },
  { path: '/coupon', redirect: '/coupons' },
  { path: '/points', redirect: '/coupons' },
  { path: '/reports', name: 'Reports', component: () => import('@/views/ReportsView.vue'), meta: { title: '举报中心 - 硬件侠', requiresAuth: true } },

  // ===== 审核/管理域 =====
  { path: '/audit', name: 'Audit', component: () => import('@/views/AuditView.vue'), meta: { title: '审核中心 - 硬件侠', requiresAuth: true } },
  { path: '/review', redirect: '/audit' },
  { path: '/admin', redirect: '/audit' },
  { path: '/admin/products', redirect: () => ({ path: '/audit', query: { type: '1' } }) },
  { path: '/admin/inspection', redirect: () => ({ path: '/audit', query: { type: '1' } }) },
  { path: '/admin/orders', name: 'AdminOrders', component: () => import('@/views/AdminOrdersView.vue'), meta: { title: '订单管理 - 硬件侠', requiresAuth: true } },
  { path: '/admin/users', name: 'AdminUsers', component: () => import('@/views/AdminUsersView.vue'), meta: { title: '用户管理 - 硬件侠', requiresAuth: true } },
  { path: '/inspection', redirect: () => ({ path: '/audit', query: { type: '1' } }) },

  // ===== 404 =====
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFoundView.vue'), meta: { title: '页面不存在 - 硬件侠' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0, behavior: 'smooth' }
  }
})

// 全局前置守卫 - 鉴权 + 幂等 Key 生成
router.beforeEach(async (to, from) => {
  const userStore = useUserStore()

  // 标题设置
  if (to.meta?.title) {
    document.title = to.meta.title
  }

  // 已登录用户不可访问登录页
  if (to.meta?.guestOnly && userStore.isLoggedIn) {
    return { path: '/' }
  }

  // 需要登录的页面
  if (to.meta?.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/auth', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
