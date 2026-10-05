<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Eye, EyeOff, Plus, Minus, ArrowRightLeft, List, Landmark,
  Wallet, Smartphone, ShieldCheck, Lock, Gauge, ArrowDownLeft,
  ArrowUpRight, Snowflake, X, Check, ChevronRight, HelpCircle,
  Cpu, MessageCircle, AtSign, PlayCircle, User
} from 'lucide-vue-next'
import { getWalletInfoApi, getTransactionsApi, rechargeApi, withdrawApi } from '@/api'

const router = useRouter()

// 余额显示控制
const balanceVisible = ref(true)
const loading = ref(false)

// 总余额、冻结、可用
const balance = ref({
  total: 0,
  frozen: 0,
  available: 0,
  updatedAt: ''
})

const formatMoney = (num) => '¥' + Number(num || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const formatMoneyNum = (num) => Number(num || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

// 弹窗控制
const activeModal = ref(null)
const openModal = (m) => { activeModal.value = m }
const closeModal = () => { activeModal.value = null }

// ---------- 充值 ----------
const rechargeAmounts = [50, 100, 200, 500, 1000, 'custom']
const selectedRechargeAmt = ref(100)
const customRecharge = ref('')
const rechargeMethods = [
  { id: 'wechat', name: '微信支付', icon: Smartphone, color: 'text-brand-success' },
  { id: 'alipay', name: '支付宝', icon: Wallet, color: 'text-brand-info' },
  { id: 'bank', name: '银行卡', icon: Landmark, color: 'text-brand-primary' }
]
const selectedRechargeMethod = ref('wechat')

const isCustomRecharge = computed(() => selectedRechargeAmt.value === 'custom')
const actualRechargeAmount = computed(() =>
  isCustomRecharge.value ? Number(customRecharge.value || 0) : selectedRechargeAmt.value
)
const handleRecharge = async () => {
  if (actualRechargeAmount.value <= 0) return alert('请输入有效充值金额')
  try {
    await rechargeApi(actualRechargeAmount.value, selectedRechargeMethod.value)
    alert(`充值成功：${formatMoney(actualRechargeAmount.value)}`)
    closeModal()
    loadWallet()
  } catch (e) {
    alert(e.message || '充值失败')
  }
}

// ---------- 提现 ----------
const withdrawAmount = ref('')
const withdrawAll = () => { withdrawAmount.value = String(balance.value.available) }
const actualWithdraw = computed(() => Number(withdrawAmount.value || 0))
const fee = 0
const handleWithdraw = async () => {
  if (actualWithdraw.value <= 0) return alert('请输入提现金额')
  if (actualWithdraw.value > balance.value.available) return alert('超出可用余额')
  try {
    await withdrawApi({ amount: actualWithdraw.value, channel: 'bank', account: 'default' })
    alert(`提现申请已提交：${formatMoney(actualWithdraw.value)}`)
    closeModal()
    loadWallet()
  } catch (e) {
    alert(e.message || '提现失败')
  }
}

// ---------- 转账 ----------
const transferAccount = ref('')
const transferAmount = ref('')
const transferRemark = ref('')
const handleTransfer = () => {
  alert('转账功能暂未开放')
}

// ---------- 支付方式 ----------
const payMethods = [
  { id: 1, type: 'bank', name: '中国工商银行', sub: '储蓄卡 · 尾号 6688', icon: Landmark, color: 'bg-brand-primary/12 text-brand-primary' },
  { id: 2, type: 'alipay', name: '支付宝', sub: '已绑定', icon: Wallet, color: 'bg-brand-info/12 text-brand-info' },
  { id: 3, type: 'wechat', name: '微信支付', sub: '已绑定', icon: Smartphone, color: 'bg-brand-success/12 text-brand-success' }
]
const unbindMethod = (m) => {
  if (confirm(`确定解绑「${m.name}」？`)) {
    alert(`已解绑 ${m.name}`)
  }
}

// ---------- 最近交易 ----------
const txIconCls = (t) => {
  const base = 'h-10 w-10 items-center justify-center rounded-full'
  if (t.direction === 'in') return `${base} bg-brand-success/12 text-brand-success inline-flex`
  if (t.direction === 'frozen') return `${base} bg-brand-primary/12 text-brand-primary inline-flex`
  return `${base} bg-brand-error/12 text-brand-error inline-flex`
}
const txAmountCls = (t) => {
  if (t.direction === 'in') return 'font-mono text-base font-semibold text-brand-success'
  if (t.direction === 'frozen') return 'font-mono text-base font-semibold text-brand-ink-2'
  return 'font-mono text-base font-semibold text-brand-error'
}
const recentTx = ref([])
const signedTx = (t) => (t.direction === 'in' ? '+' : '-') + formatMoneyNum(t.amount)

// 交易类型映射
const TX_TYPE_MAP = {
  RECHARGE: { title: '账户充值', direction: 'in', icon: ArrowDownLeft },
  WITHDRAW: { title: '提现', direction: 'out', icon: ArrowUpRight },
  PAY: { title: '订单支付', direction: 'out', icon: ArrowUpRight },
  REFUND: { title: '退款', direction: 'in', icon: ArrowDownLeft },
  INCOME: { title: '订单收入', direction: 'in', icon: ArrowDownLeft },
  FROZEN: { title: '订单冻结', direction: 'frozen', icon: Snowflake },
  UNFREEZE: { title: '冻结解除', direction: 'frozen', icon: Snowflake }
}

function mapTx(t) {
  const cfg = TX_TYPE_MAP[t.type] || { title: t.type || '交易', direction: 'out', icon: ArrowUpRight }
  return {
    id: t.id || t.txNo,
    title: cfg.title + (t.remark ? ' - ' + t.remark : ''),
    sub: (t.createdAt || '').slice(0, 19).replace('T', ' '),
    amount: t.amount || 0,
    direction: cfg.direction,
    icon: cfg.icon
  }
}

async function loadWallet() {
  loading.value = true
  try {
    const [walletResp, txResp] = await Promise.allSettled([
      getWalletInfoApi(),
      getTransactionsApi({ pageNum: 1, pageSize: 10 })
    ])

    if (walletResp.status === 'fulfilled' && walletResp.value) {
      const w = walletResp.value
      balance.value = {
        total: w.totalBalance || 0,
        frozen: w.frozenBalance || 0,
        available: w.availableBalance || 0,
        updatedAt: (w.updatedAt || '').slice(0, 16).replace('T', ' ')
      }
    }

    if (txResp.status === 'fulfilled' && txResp.value) {
      const records = txResp.value.records || txResp.value.list || []
      recentTx.value = records.map(mapTx)
    }
  } catch (e) {
    console.error('加载钱包失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadWallet()
})
</script>

<template>
  <div class="container-app space-y-6 pb-20">
    <!-- 标题 -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
      <h1 class="text-2xl font-bold text-brand-ink">我的钱包</h1>
      <button @click="router.push('/transactions')" class="inline-flex items-center gap-1.5 text-sm font-medium text-brand-ink-2 transition-colors hover:text-brand-primary">
        <List class="h-4 w-4" /> 收支明细
      </button>
    </div>

    <!-- 余额卡片区 -->
    <section class="grid gap-4 md:grid-cols-3" aria-label="账户余额">
      <!-- 总余额 -->
      <div class="rounded-2xl p-6 flex flex-col justify-between border border-brand-line bg-gradient-to-br from-brand-surface to-brand-surface-2">
        <div class="flex items-center justify-between">
          <span class="text-sm text-brand-ink-2">总余额</span>
          <button @click="balanceVisible = !balanceVisible"
            class="inline-flex items-center justify-center rounded-md p-1.5 text-brand-ink-3 transition-colors hover:bg-brand-surface-3 hover:text-brand-ink"
            :aria-label="balanceVisible ? '隐藏金额' : '显示金额'">
            <Eye v-if="balanceVisible" class="h-4 w-4" />
            <EyeOff v-else class="h-4 w-4" />
          </button>
        </div>
        <div class="mt-4">
          <span class="text-lg font-medium text-brand-primary">¥</span>
          <span :class="['font-mono text-4xl md:text-5xl font-bold tracking-tight text-brand-ink', !balanceVisible && 'blur-md select-none']">
            {{ balanceVisible ? formatMoneyNum(balance.total) : '****' }}
          </span>
        </div>
        <p class="mt-2 text-xs text-brand-ink-3">上次更新：{{ balance.updatedAt }}</p>
      </div>
      <!-- 冻结金额 -->
      <div class="rounded-2xl p-6 flex flex-col justify-center border border-brand-line bg-brand-surface">
        <span class="text-sm text-brand-ink-2">冻结金额</span>
        <div class="mt-3">
          <span class="text-base font-medium text-brand-ink-2">¥</span>
          <span :class="['font-mono text-3xl font-bold text-brand-ink', !balanceVisible && 'blur-md select-none']">
            {{ balanceVisible ? formatMoneyNum(balance.frozen) : '****' }}
          </span>
        </div>
        <p class="mt-2 text-xs text-brand-ink-3">交易中订单担保金额</p>
      </div>
      <!-- 可用余额 -->
      <div class="rounded-2xl p-6 flex flex-col justify-center border border-brand-line bg-brand-surface">
        <span class="text-sm text-brand-ink-2">可用余额</span>
        <div class="mt-3">
          <span class="text-base font-medium text-brand-primary">¥</span>
          <span :class="['font-mono text-3xl font-bold text-brand-primary', !balanceVisible && 'blur-md select-none']">
            {{ balanceVisible ? formatMoneyNum(balance.available) : '****' }}
          </span>
        </div>
        <p class="mt-2 text-xs text-brand-ink-3">可提现 / 可直接支付</p>
      </div>
    </section>

    <!-- 快捷操作 -->
    <section class="grid grid-cols-3 gap-3 md:gap-4" aria-label="快捷操作">
      <button @click="openModal('recharge')"
        class="flex flex-col items-center justify-center gap-2 rounded-2xl border border-brand-line bg-brand-surface py-5 text-brand-ink transition-colors hover:border-brand-primary hover:bg-brand-primary/10 hover:text-brand-primary">
        <Plus class="h-6 w-6" />
        <span class="text-sm font-medium">充值</span>
      </button>
      <button @click="openModal('withdraw')"
        class="flex flex-col items-center justify-center gap-2 rounded-2xl border border-brand-line bg-brand-surface py-5 text-brand-ink transition-colors hover:border-brand-primary hover:bg-brand-primary/10 hover:text-brand-primary">
        <Minus class="h-6 w-6" />
        <span class="text-sm font-medium">提现</span>
      </button>
      <button @click="openModal('transfer')"
        class="flex flex-col items-center justify-center gap-2 rounded-2xl border border-brand-line bg-brand-surface py-5 text-brand-ink transition-colors hover:border-brand-primary hover:bg-brand-primary/10 hover:text-brand-primary">
        <ArrowRightLeft class="h-6 w-6" />
        <span class="text-sm font-medium">转账</span>
      </button>
    </section>

    <!-- 左侧支付方式+安全 / 右侧最近交易 -->
    <div class="grid gap-6 lg:grid-cols-3">
      <!-- 左侧 -->
      <div class="space-y-6 lg:col-span-1">
        <!-- 支付方式管理 -->
        <section class="rounded-2xl border border-brand-line bg-brand-surface p-5">
          <div class="mb-4 flex items-center justify-between">
            <h2 class="font-semibold text-brand-ink">支付方式</h2>
            <button @click="openModal('add-method')" class="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-brand-primary transition-colors hover:bg-brand-primary/12">
              <Plus class="h-3.5 w-3.5" /> 添加
            </button>
          </div>
          <div class="space-y-3">
            <div v-for="m in payMethods" :key="m.id"
              class="flex items-center justify-between rounded-xl border border-brand-line bg-brand-surface-2 p-3">
              <div class="flex items-center gap-3">
                <div :class="['inline-flex h-9 w-9 items-center justify-center rounded-lg', m.color]">
                  <component :is="m.icon" class="h-5 w-5" />
                </div>
                <div>
                  <p class="text-sm font-medium text-brand-ink">{{ m.name }}</p>
                  <p class="text-xs text-brand-ink-3">{{ m.sub }}</p>
                </div>
              </div>
              <button @click="unbindMethod(m)"
                class="rounded-md px-2 py-1 text-xs text-brand-ink-3 transition-colors hover:bg-brand-error/12 hover:text-brand-error">
                解绑
              </button>
            </div>
          </div>
        </section>

        <!-- 安全提示 -->
        <section class="rounded-2xl border border-brand-line bg-brand-surface p-5">
          <h2 class="mb-4 font-semibold text-brand-ink">账户安全</h2>
          <div class="space-y-3">
            <div class="flex items-center gap-3 rounded-xl border border-brand-line bg-brand-surface-2 p-3">
              <ShieldCheck class="h-5 w-5 text-brand-success" />
              <div class="flex-1">
                <p class="text-sm font-medium text-brand-ink">实名认证</p>
                <p class="text-xs text-brand-ink-3">已完成 · 叶 **</p>
              </div>
              <span class="rounded-full bg-brand-success/12 px-2 py-0.5 text-xs font-medium text-brand-success">已认证</span>
            </div>
            <div class="flex items-center gap-3 rounded-xl border border-brand-line bg-brand-surface-2 p-3">
              <Lock class="h-5 w-5 text-brand-primary" />
              <div class="flex-1">
                <p class="text-sm font-medium text-brand-ink">支付密码</p>
                <p class="text-xs text-brand-ink-3">已设置 6 位数字密码</p>
              </div>
              <button class="text-xs font-medium text-brand-primary hover:underline">修改</button>
            </div>
            <div class="flex items-center gap-3 rounded-xl border border-brand-line bg-brand-surface-2 p-3">
              <Gauge class="h-5 w-5 text-brand-warning" />
              <div class="flex-1">
                <p class="text-sm font-medium text-brand-ink">交易限额</p>
                <p class="text-xs text-brand-ink-3">单笔 ¥50,000 · 单日 ¥200,000</p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 右侧：最近交易 -->
      <section class="rounded-2xl border border-brand-line bg-brand-surface p-5 lg:col-span-2">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="font-semibold text-brand-ink">最近交易</h2>
          <button @click="router.push('/transactions')" class="inline-flex items-center gap-1 text-sm font-medium text-brand-primary transition-colors hover:underline">
            查看全部 <ChevronRight class="h-4 w-4" />
          </button>
        </div>
        <div class="divide-y divide-brand-line">
          <div v-for="t in recentTx" :key="t.id" class="flex items-center justify-between py-4">
            <div class="flex items-center gap-4">
              <div :class="txIconCls(t)">
                <component :is="t.icon" class="h-5 w-5" />
              </div>
              <div>
                <p class="text-sm font-medium text-brand-ink">{{ t.title }}</p>
                <p class="text-xs text-brand-ink-3">{{ t.sub }}</p>
              </div>
            </div>
            <span :class="txAmountCls(t)">{{ signedTx(t) }}</span>
          </div>
        </div>
      </section>
    </div>

    <!-- ===================== Modal 遮罩层 ===================== -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="activeModal"
          class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
          @click.self="closeModal">
          <div class="w-full max-w-md max-h-[calc(100vh-32px)] overflow-y-auto rounded-2xl bg-brand-surface border border-brand-line shadow-2xl">
            <!-- 充值 -->
            <div v-if="activeModal === 'recharge'" class="p-5 md:p-6 space-y-5">
              <div class="flex items-center justify-between mb-1">
                <h2 class="text-lg font-semibold text-brand-ink">账户充值</h2>
                <button @click="closeModal" class="inline-flex h-8 w-8 items-center justify-center rounded-md text-brand-ink-3 transition-colors hover:bg-brand-surface-2 hover:text-brand-ink">
                  <X class="h-5 w-5" />
                </button>
              </div>
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink-2">充值金额</label>
                <div class="mb-3 grid grid-cols-3 sm:grid-cols-5 gap-2">
                  <button v-for="a in rechargeAmounts" :key="a" type="button" @click="selectedRechargeAmt = a"
                    :class="['inline-flex items-center justify-center h-11 rounded-lg border text-sm font-medium transition',
                             selectedRechargeAmt === a ? 'border-brand-primary bg-brand-primary/12 text-brand-primary'
                                                         : 'border-brand-line bg-brand-surface-2 text-brand-ink hover:border-brand-primary hover:text-brand-primary']">
                    {{ a === 'custom' ? '自定义' : '¥' + a }}
                  </button>
                </div>
                <div v-if="isCustomRecharge" class="relative">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-brand-ink-2">¥</span>
                  <input v-model="customRecharge" type="number" min="1" step="0.01" placeholder="请输入充值金额"
                    class="h-11 w-full rounded-lg border border-brand-line bg-brand-surface-2 pl-8 pr-4 text-brand-ink outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
                </div>
              </div>
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink-2">支付方式</label>
                <div class="space-y-2">
                  <div v-for="m in rechargeMethods" :key="m.id" @click="selectedRechargeMethod = m.id"
                    :class="['flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition',
                             selectedRechargeMethod === m.id ? 'border-brand-primary bg-brand-primary/12' : 'border-brand-line bg-brand-surface-2 hover:border-brand-primary/60']">
                    <span :class="['w-4.5 h-4.5 rounded-full border-2 flex items-center justify-center', selectedRechargeMethod === m.id ? 'border-brand-primary' : 'border-brand-ink-3']">
                      <span v-if="selectedRechargeMethod === m.id" class="w-2 h-2 rounded-full bg-brand-primary"></span>
                    </span>
                    <component :is="m.icon" :class="['h-5 w-5', m.color]" />
                    <span class="flex-1 text-sm font-medium text-brand-ink">{{ m.name }}</span>
                  </div>
                </div>
              </div>
              <button @click="handleRecharge"
                class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 text-sm font-semibold text-brand-primary-ink transition-colors hover:bg-brand-primary/90">
                <Check class="h-4 w-4" /> 确认充值
              </button>
            </div>

            <!-- 提现 -->
            <div v-else-if="activeModal === 'withdraw'" class="p-5 md:p-6 space-y-5">
              <div class="flex items-center justify-between mb-1">
                <h2 class="text-lg font-semibold text-brand-ink">余额提现</h2>
                <button @click="closeModal" class="inline-flex h-8 w-8 items-center justify-center rounded-md text-brand-ink-3 transition-colors hover:bg-brand-surface-2 hover:text-brand-ink">
                  <X class="h-5 w-5" />
                </button>
              </div>
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink-2">到账银行卡</label>
                <div class="flex items-center gap-3 p-3 rounded-lg border border-brand-line bg-brand-surface-2">
                  <Landmark class="h-5 w-5 text-brand-primary" />
                  <div class="flex-1">
                    <p class="text-sm font-medium text-brand-ink">中国工商银行（6688）</p>
                    <p class="text-xs text-brand-ink-3">预计 2 小时内到账</p>
                  </div>
                  <Check class="h-4 w-4 text-brand-primary" />
                </div>
              </div>
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink-2">提现金额</label>
                <div class="relative">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-lg font-medium text-brand-ink-2">¥</span>
                  <input v-model="withdrawAmount" type="number" min="1" step="0.01" placeholder="请输入提现金额"
                    class="h-12 w-full rounded-lg border border-brand-line bg-brand-surface-2 pl-9 pr-4 text-xl font-semibold text-brand-ink outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
                </div>
                <div class="mt-2 flex items-center justify-between text-xs text-brand-ink-3">
                  <span>可用余额 {{ formatMoney(balance.available) }}</span>
                  <button @click="withdrawAll" class="text-brand-primary hover:underline">全部提现</button>
                </div>
              </div>
              <div class="rounded-lg border border-brand-line bg-brand-surface-2 p-3 text-sm space-y-1">
                <div class="flex items-center justify-between">
                  <span class="text-brand-ink-2">预计到账时间</span>
                  <span class="font-medium text-brand-ink">2 小时内</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-brand-ink-2">手续费</span>
                  <span class="font-medium text-brand-ink">{{ formatMoney(fee) }}</span>
                </div>
              </div>
              <button @click="handleWithdraw"
                class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 text-sm font-semibold text-brand-primary-ink transition-colors hover:bg-brand-primary/90">
                <Check class="h-4 w-4" /> 确认提现
              </button>
            </div>

            <!-- 转账 -->
            <div v-else-if="activeModal === 'transfer'" class="p-5 md:p-6 space-y-5">
              <div class="flex items-center justify-between mb-1">
                <h2 class="text-lg font-semibold text-brand-ink">转账给朋友</h2>
                <button @click="closeModal" class="inline-flex h-8 w-8 items-center justify-center rounded-md text-brand-ink-3 transition-colors hover:bg-brand-surface-2 hover:text-brand-ink">
                  <X class="h-5 w-5" />
                </button>
              </div>
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink-2">对方账户</label>
                <input v-model="transferAccount" type="text" placeholder="手机号 / 用户名 / 邮箱"
                  class="h-11 w-full rounded-lg border border-brand-line bg-brand-surface-2 px-4 text-brand-ink placeholder:text-brand-ink-3 outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
              </div>
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink-2">转账金额</label>
                <div class="relative">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-lg font-medium text-brand-ink-2">¥</span>
                  <input v-model="transferAmount" type="number" min="0.01" step="0.01" placeholder="请输入转账金额"
                    class="h-12 w-full rounded-lg border border-brand-line bg-brand-surface-2 pl-9 pr-4 text-xl font-semibold text-brand-ink outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
                </div>
              </div>
              <div>
                <label class="mb-2 block text-sm font-medium text-brand-ink-2">备注（选填）</label>
                <input v-model="transferRemark" type="text" placeholder="请输入转账备注"
                  class="h-11 w-full rounded-lg border border-brand-line bg-brand-surface-2 px-4 text-brand-ink placeholder:text-brand-ink-3 outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20" />
              </div>
              <button @click="handleTransfer"
                class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-brand-primary px-4 text-sm font-semibold text-brand-primary-ink transition-colors hover:bg-brand-primary/90">
                <Check class="h-4 w-4" /> 确认转账
              </button>
            </div>

            <!-- 添加支付方式 -->
            <div v-else-if="activeModal === 'add-method'" class="p-5 md:p-6 space-y-4">
              <div class="flex items-center justify-between mb-1">
                <h2 class="text-lg font-semibold text-brand-ink">添加支付方式</h2>
                <button @click="closeModal" class="inline-flex h-8 w-8 items-center justify-center rounded-md text-brand-ink-3 transition-colors hover:bg-brand-surface-2 hover:text-brand-ink">
                  <X class="h-5 w-5" />
                </button>
              </div>
              <button v-for="(opt, idx) in [
                { icon: Landmark, color: 'text-brand-primary', name: '添加银行卡' },
                { icon: Wallet, color: 'text-brand-info', name: '添加支付宝' },
                { icon: Smartphone, color: 'text-brand-success', name: '添加微信支付' }
              ]" :key="idx" @click="alert(opt.name + ' - 请完成授权绑定'); closeModal()"
                class="flex w-full items-center gap-3 rounded-xl border border-brand-line bg-brand-surface-2 p-4 text-left transition-colors hover:border-brand-primary hover:bg-brand-primary/12">
                <component :is="opt.icon" :class="['h-6 w-6', opt.color]" />
                <span class="text-sm font-medium text-brand-ink">{{ opt.name }}</span>
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
.fade-enter-active, .fade-leave-active { transition: opacity 0.18s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
