const http = require('http')
const { execSync } = require('child_process')

const BASE = 'http://localhost:8081/api/v1'
const REDIS_CLI = 'C:\\Users\\DELL\\Desktop\\trae\\middleware\\redis\\redis-cli.exe'

function httpReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const u = new URL(BASE + path)
    const headers = { 'Content-Type': 'application/json; charset=utf-8', 'Content-Length': Buffer.byteLength(data || '') }
    if (token) headers['Authorization'] = 'Bearer ' + token
    if (['POST','PUT','DELETE'].includes(method)) headers['X-Idempotency-Key'] = 'idem_' + Date.now() + '_' + Math.random().toString(36).slice(2)
    const req = http.request({ hostname: u.hostname, port: u.port, path: u.pathname + u.search, method, headers }, res => {
      let chunks = ''
      res.on('data', c => chunks += c)
      res.on('end', () => { try { resolve({ status: res.statusCode, body: JSON.parse(chunks) }) } catch { resolve({ status: res.statusCode, raw: chunks }) } })
    })
    req.on('error', reject)
    if (data) req.write(data)
    req.end()
  })
}

function redisCmd(...args) {
  const out = execSync(`"${REDIS_CLI}" --no-auth-warning -a 123456 -n 0 ${args.join(' ')}`, { encoding: 'utf8' })
  const lines = out.trim().split('\n')
  return lines[lines.length - 1].trim()
}

;(async () => {
  const phone = '138' + String(Math.floor(10000000 + Math.random() * 90000000))
  console.log('phone:', phone)

  const s1 = await httpReq('POST', '/auth/sms/send', { phone, scene: 'register' })
  console.log('[1] send sms:', JSON.stringify(s1.body))

  await new Promise(r => setTimeout(r, 500))
  const code = await redisCmd('GET', `yjx:user:sms:${phone}:register`)
  console.log('[2] redis code:', JSON.stringify(code), 'len:', code.length)

  const s2 = await httpReq('POST', '/auth/register/phone', { phone, smsCode: code, password: 'Abcd1234', confirmPassword: 'Abcd1234', nickname: 'testuser' })
  console.log('[3] register:', JSON.stringify(s2.body).substring(0, 400))

  if (s2.body.code === 0) {
    const token = s2.body.data.accessToken
    console.log('[4] accessToken len:', token.length)

    const s3 = await httpReq('GET', '/user/profile', null, token)
    console.log('[5] user/profile:', JSON.stringify(s3.body).substring(0, 300))
  }

  process.exit(0)
})().catch(e => { console.error(e); process.exit(1) })
