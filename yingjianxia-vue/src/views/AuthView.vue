<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Eye, EyeOff, Lock, Smartphone, Mail, CheckCircle2, AlertCircle, Tag, ArrowLeft } from 'lucide-vue-next'
import * as userApi from '@/api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const mode = ref('login')
const tab = ref('password')
const form = ref({ phone: '', password: '', confirmPassword: '', smsCode: '', agree: true, inviteCode: '' })
const showPwd = ref(false)
const showPwd2 = ref(false)
const codeCountdown = ref(0)
const loading = ref(false)
const error = ref('')

const redirect = computed(() => route.query.redirect || '/')
const pageTitle = computed(() => ({ login: '登录', register: '创建账号', forgot: '找回密码' }[mode.value]))

function switchMode(m) { mode.value = m; error.value = '' }

async function sendCode() {
  if (!form.value.phone || !/^1[3-9]\d{9}$/.test(form.value.phone)) { error.value = '请输入有效的手机号'; return }
  try {
    await userStore.sendSmsCode(form.value.phone, mode.value === 'register' ? 'register' : 'login')
    codeCountdown.value = 60
    error.value = ''
    const t = setInterval(() => { codeCountdown.value--; if (codeCountdown.value <= 0) clearInterval(t) }, 1000)
  } catch (e) { error.value = e.message || '验证码发送失败' }
}

async function submit() {
  error.value = ''
  if (!form.value.phone || !/^1[3-9]\d{9}$/.test(form.value.phone)) { error.value = '请输入有效的手机号'; return }
  loading.value = true
  try {
    if (mode.value === 'register') {
      if (!form.value.smsCode || form.value.smsCode.length < 4) { error.value = '请输入验证码'; return }
      if (!form.value.password || form.value.password.length < 8) { error.value = '密码至少8位'; return }
      if (form.value.password !== form.value.confirmPassword) { error.value = '两次密码不一致'; return }
      if (!form.value.agree) { error.value = '请先同意用户协议和隐私政策'; return }
      await userStore.register({ ...form.value })
      mode.value = 'login'
    } else if (mode.value === 'forgot') {
      if (!form.value.smsCode) { error.value = '请输入验证码'; return }
      if (!form.value.password || form.value.password.length < 8) { error.value = '密码至少8位'; return }
      if (form.value.password !== form.value.confirmPassword) { error.value = '两次密码不一致'; return }
      await userApi.resetPasswordApi({ phone: form.value.phone, smsCode: form.value.smsCode, newPassword: form.value.password })
      mode.value = 'login'
    } else {
      if (tab.value === 'password') {
        if (!form.value.password) { error.value = '请输入密码'; return }
        await userStore.login({ phone: form.value.phone, password: form.value.password, type: 'password' })
      } else {
        if (!form.value.smsCode) { error.value = '请输入验证码'; return }
        await userStore.login({ phone: form.value.phone, smsCode: form.value.smsCode, type: 'sms' })
      }
      router.push(redirect.value)
    }
  } catch (e) { error.value = e.message || '操作失败，请重试' }
  finally { loading.value = false }
}

function oauthLogin() { router.push('/') }

onMounted(() => { document.title = `${pageTitle.value} - 硬件侠` })
</script>

<template>
  <div class="min-h-screen flex">
    <!-- 左侧宣传区 -->
    <div class="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-brand-primary/20 via-brand-surface to-brand-bg relative overflow-hidden">
      <div class="absolute inset-0 opacity-30 pointer-events-none" style="background-image: radial-gradient(circle at 30% 40%, #00D4AA 0%, transparent 50%), radial-gradient(circle at 70% 70%, #3B82F6 0%, transparent 40%);"></div>
      <div class="relative w-full flex flex-col justify-between p-14">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-xl bg-brand-primary/20 border border-brand-primary/40 flex items-center justify-center">
            <Tag class="w-6 h-6 text-brand-primary" />
          </div>
          <div>
            <div class="text-2xl font-bold text-brand-ink">硬件侠</div>
            <div class="text-[11px] text-brand-ink-3 uppercase tracking-[0.3em]">HARDWARE · XIA</div>
          </div>
        </div>
        <div class="space-y-6 max-w-md">
          <h2 class="text-4xl font-bold text-brand-ink leading-tight">专业电脑配件<br/>二手交易平台</h2>
          <p class="text-brand-ink-2 leading-relaxed">12+ 品类、AI 假货识别、官方验机报告、担保交易，全国 1800+ 验机服务点，让每一台二手硬件都能放心交易。</p>
          <div class="grid grid-cols-3 gap-3">
            <div class="p-4 rounded-xl bg-brand-surface/50 border border-brand-line/60"><div class="text-2xl font-bold text-brand-primary">120万+</div><div class="text-xs text-brand-ink-3 mt-1">累计玩家</div></div>
            <div class="p-4 rounded-xl bg-brand-surface/50 border border-brand-line/60"><div class="text-2xl font-bold text-brand-primary">98.7%</div><div class="text-xs text-brand-ink-3 mt-1">好评率</div></div>
            <div class="p-4 rounded-xl bg-brand-surface/50 border border-brand-line/60"><div class="text-2xl font-bold text-brand-primary">¥56亿</div><div class="text-xs text-brand-ink-3 mt-1">累计成交</div></div>
          </div>
          <div class="space-y-2 text-sm text-brand-ink-2">
            <div class="flex items-center gap-2"><CheckCircle2 class="w-4 h-4 text-brand-primary" />实名认证 + 信用分双体系</div>
            <div class="flex items-center gap-2"><CheckCircle2 class="w-4 h-4 text-brand-primary" />JWT 鉴权 + API 幂等键防重放</div>
            <div class="flex items-center gap-2"><CheckCircle2 class="w-4 h-4 text-brand-primary" />风控实时拦截欺诈商品</div>
          </div>
        </div>
        <div class="text-xs text-brand-ink-3">© 2025 硬件侠 Hardware Xia · 京ICP备2025000000号</div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="w-full lg:w-1/2 flex items-center justify-center p-6 md:p-12">
      <div class="w-full max-w-md">
        <button v-if="mode !== 'forgot'" @click="switchMode(mode === 'login' ? 'register' : 'login')"
          class="hidden lg:inline-flex items-center gap-1 text-xs text-brand-ink-3 hover:text-brand-primary mb-6">
          {{ mode === 'login' ? '还没有账号？立即注册' : '已有账号？返回登录' }}
        </button>

        <div class="lg:hidden flex items-center gap-2 mb-8">
          <button @click="router.back()" class="p-2 rounded-lg hover:bg-brand-surface-2 transition"><ArrowLeft class="w-5 h-5" /></button>
          <div class="text-xl font-bold text-brand-ink">{{ pageTitle }}硬件侠</div>
        </div>

        <h1 class="text-3xl font-bold text-brand-ink mb-2">{{ pageTitle }}</h1>
        <p class="text-sm text-brand-ink-3 mb-8">
          {{ mode === 'login' ? '欢迎回来，继续淘好货' : mode === 'register' ? '注册账号，立即享受平台保障' : '输入手机号重置密码' }}
        </p>

        <div v-if="mode === 'login'" class="flex gap-1 p-1 mb-6 rounded-lg bg-brand-surface-2">
          <button @click="tab='password'" :class="['flex-1 py-2 rounded-md text-sm font-medium transition', tab === 'password' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">密码登录</button>
          <button @click="tab='sms'" :class="['flex-1 py-2 rounded-md text-sm font-medium transition', tab === 'sms' ? 'bg-brand-primary text-brand-primary-ink' : 'text-brand-ink-2 hover:text-brand-primary']">短信登录</button>
        </div>

        <div v-if="error" class="mb-4 p-3 rounded-lg bg-brand-error-subtle text-brand-error text-xs flex items-start gap-2 border border-brand-error/20">
          <AlertCircle class="w-4 h-4 shrink-0 mt-0.5" />{{ error }}
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-xs font-medium text-brand-ink-2 mb-2">手机号</label>
            <div class="flex h-12 rounded-lg bg-brand-surface border border-brand-line focus-within:border-brand-primary transition">
              <span class="px-4 flex items-center border-r border-brand-line text-sm text-brand-ink-2 bg-brand-surface-2">+86</span>
              <input v-model="form.phone" type="tel" maxlength="11" placeholder="请输入11位手机号" class="flex-1 px-4 text-sm bg-transparent text-brand-ink placeholder:text-brand-ink-3 focus:outline-none" />
              <div class="pr-4 flex items-center text-brand-ink-3"><Smartphone class="w-4 h-4" /></div>
            </div>
          </div>

          <div v-if="tab === 'password' || mode !== 'login'">
            <label class="block text-xs font-medium text-brand-ink-2 mb-2">
              {{ mode === 'login' ? '密码' : (mode === 'forgot' ? '新密码' : '设置密码') }}
            </label>
            <div class="flex h-12 rounded-lg bg-brand-surface border border-brand-line focus-within:border-brand-primary transition">
              <div class="pl-4 flex items-center text-brand-ink-3"><Lock class="w-4 h-4" /></div>
              <input v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="8-20位，字母+数字组合" class="flex-1 px-4 text-sm bg-transparent text-brand-ink placeholder:text-brand-ink-3 focus:outline-none" />
              <button type="button" @click="showPwd = !showPwd" class="pr-4 flex items-center text-brand-ink-3 hover:text-brand-primary">
                <EyeOff v-if="showPwd" class="w-4 h-4" /><Eye v-else class="w-4 h-4" />
              </button>
            </div>
          </div>

          <div v-if="mode === 'register' || mode === 'forgot'">
            <label class="block text-xs font-medium text-brand-ink-2 mb-2">确认密码</label>
            <div class="flex h-12 rounded-lg bg-brand-surface border border-brand-line focus-within:border-brand-primary transition">
              <div class="pl-4 flex items-center text-brand-ink-3"><Lock class="w-4 h-4" /></div>
              <input v-model="form.confirmPassword" :type="showPwd2 ? 'text' : 'password'" placeholder="请再次输入密码" class="flex-1 px-4 text-sm bg-transparent text-brand-ink placeholder:text-brand-ink-3 focus:outline-none" />
              <button type="button" @click="showPwd2 = !showPwd2" class="pr-4 flex items-center text-brand-ink-3 hover:text-brand-primary">
                <EyeOff v-if="showPwd2" class="w-4 h-4" /><Eye v-else class="w-4 h-4" />
              </button>
            </div>
          </div>

          <div v-if="tab === 'sms' || mode !== 'login'">
            <label class="block text-xs font-medium text-brand-ink-2 mb-2">短信验证码</label>
            <div class="flex gap-2">
              <div class="flex-1 flex h-12 rounded-lg bg-brand-surface border border-brand-line focus-within:border-brand-primary transition">
                <div class="pl-4 flex items-center text-brand-ink-3"><Mail class="w-4 h-4" /></div>
                <input v-model="form.smsCode" type="text" maxlength="6" placeholder="请输入6位验证码" class="flex-1 px-4 text-sm bg-transparent text-brand-ink placeholder:text-brand-ink-3 focus:outline-none" />
              </div>
              <button @click="sendCode" :disabled="codeCountdown > 0 || loading"
                :class="['shrink-0 px-5 h-12 rounded-lg text-sm font-medium border transition', codeCountdown > 0 ? 'bg-brand-surface-2 border-brand-line text-brand-ink-3 cursor-not-allowed' : 'bg-brand-primary-subtle border-brand-primary/30 text-brand-primary hover:bg-brand-primary hover:text-brand-primary-ink']">
                {{ codeCountdown > 0 ? codeCountdown + 's' : '获取验证码' }}
              </button>
            </div>
          </div>

          <div v-if="mode === 'register'">
            <label class="block text-xs font-medium text-brand-ink-2 mb-2">邀请码（选填）</label>
            <div class="flex h-12 rounded-lg bg-brand-surface border border-brand-line focus-within:border-brand-primary transition">
              <div class="pl-4 flex items-center text-brand-ink-3"><Tag class="w-4 h-4" /></div>
              <input v-model="form.inviteCode" type="text" placeholder="填写邀请码可获得新手礼包" class="flex-1 px-4 text-sm bg-transparent text-brand-ink placeholder:text-brand-ink-3 focus:outline-none" />
            </div>
          </div>

          <div v-if="mode === 'login'" class="flex items-center justify-between text-xs">
            <label class="flex items-center gap-2 text-brand-ink-2 cursor-pointer select-none">
              <input type="checkbox" checked class="w-3.5 h-3.5 rounded border-brand-line bg-brand-surface accent-brand-primary" />记住我
            </label>
            <button @click="switchMode('forgot')" class="text-brand-primary hover:underline">忘记密码？</button>
          </div>
          <div v-if="mode === 'register'" class="text-xs">
            <label class="flex items-start gap-2 text-brand-ink-2 cursor-pointer select-none">
              <input type="checkbox" v-model="form.agree" class="mt-0.5 w-3.5 h-3.5 rounded border-brand-line bg-brand-surface accent-brand-primary" />
              <span>我已阅读并同意 <a class="text-brand-primary hover:underline">《用户协议》</a>、<a class="text-brand-primary hover:underline">《隐私政策》</a>、<a class="text-brand-primary hover:underline">《交易规则》</a></span>
            </label>
          </div>
          <div v-if="mode === 'forgot'" class="text-xs">
            <button @click="switchMode('login')" class="text-brand-primary hover:underline">← 返回登录</button>
          </div>

          <button @click="submit" :disabled="loading"
            class="w-full h-12 rounded-lg bg-brand-primary text-brand-primary-ink font-semibold hover:bg-brand-primary-hover disabled:opacity-60 transition flex items-center justify-center gap-2">
            <svg v-if="loading" class="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-opacity="0.25" stroke-width="3"/><path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-width="3" stroke-linecap="round"/></svg>
            {{ loading ? '提交中...' : pageTitle }}
          </button>

          <div class="lg:hidden flex items-center justify-center gap-2 text-xs text-brand-ink-3 pt-2">
            <span>{{ mode === 'login' ? '还没有账号？' : mode === 'register' ? '已有账号？' : '想起密码了？' }}</span>
            <button @click="switchMode(mode === 'login' ? 'register' : 'login')" class="text-brand-primary font-medium hover:underline">
              {{ mode === 'login' ? '立即注册' : mode === 'register' ? '立即登录' : '返回登录' }}
            </button>
          </div>

          <div class="relative my-6"><div class="absolute inset-0 flex items-center"><div class="w-full border-t border-brand-line"></div></div><div class="relative flex justify-center"><span class="px-3 text-xs text-brand-ink-3 bg-brand-bg">其他登录方式</span></div></div>

          <div class="grid grid-cols-3 gap-3">
            <button @click="oauthLogin" class="h-11 rounded-lg bg-brand-surface border border-brand-line hover:border-brand-primary/50 hover:bg-brand-surface-2 transition flex items-center justify-center gap-2 text-xs text-brand-ink-2">
              <div class="w-5 h-5 rounded-full bg-[#07C160] flex items-center justify-center text-white text-[10px] font-bold">微</div>微信
            </button>
            <button @click="oauthLogin" class="h-11 rounded-lg bg-brand-surface border border-brand-line hover:border-brand-primary/50 hover:bg-brand-surface-2 transition flex items-center justify-center gap-2 text-xs text-brand-ink-2">
              <div class="w-5 h-5 rounded-full bg-[#12B7F5] flex items-center justify-center text-white text-[10px] font-bold">Q</div>QQ
            </button>
            <button @click="oauthLogin" class="h-11 rounded-lg bg-brand-surface border border-brand-line hover:border-brand-primary/50 hover:bg-brand-surface-2 transition flex items-center justify-center gap-2 text-xs text-brand-ink-2">
              <div class="w-5 h-5 rounded-full bg-[#E6162D] flex items-center justify-center text-white text-[10px] font-bold">W</div>微博
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
