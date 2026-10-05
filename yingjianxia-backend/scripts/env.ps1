# ============================================================
# 硬件侠平台 - 环境变量模板
# 复制为 env.ps1 并修改实际值后执行: . .\env.ps1
# ============================================================

# ---- 中间件地址 ----
$env:NACOS_SERVER = "127.0.0.1:8848"
$env:NACOS_NAMESPACE = "public"

$env:MYSQL_HOST = "127.0.0.1"
$env:MYSQL_PORT = "3306"
$env:MYSQL_USER = "root"
$env:MYSQL_PWD = "123456"

$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"
$env:REDIS_PWD = "123456"

$env:SENTINEL_DASHBOARD = "127.0.0.1:8858"

$env:ROCKETMQ_NAMESRV = "127.0.0.1:9876"

# ---- JWT 密钥（生产环境务必更换）----
$env:JWT_SECRET = "yingjianxia-jwt-secret-please-change-in-production"

# ---- 内部服务密钥 ----
$env:INTERNAL_SECRET = "yjx-internal-secret-please-change-in-production"

# ---- JVM 参数 ----
$env:JAVA_OPTS = "-Xms256m -Xmx512m -Xmn128m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"

Write-Host "✅ 环境变量已加载" -ForegroundColor Green
