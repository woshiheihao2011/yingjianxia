# ============================================================
# 硬件侠平台 - 全量启动脚本 (PowerShell)
# 功能: 编译 → 按依赖顺序启动 16 个服务
# 用法:
#   1. 先启动中间件: docker-compose -f docker-compose-env.yml up -d
#   2. 加载环境变量: . .\scripts\env.ps1
#   3. 执行本脚本: .\scripts\start-all.ps1
# ============================================================

param(
    [switch]$Build,      # -Build 先编译再启动
    [switch]$SkipGateway, # -SkipGateway 跳过网关
    [string]$Only = ""    # -Only user-service,order-service 只启动指定服务
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$LogDir = "$ProjectRoot\logs"
if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir | Out-Null }

# ---- 服务定义: 名称 | 端口 | 启动优先级(越小越先) ----
$Services = @(
    @{Name="gateway-service";       Port=8080; Priority=1; Group="网关"},
    @{Name="user-service";          Port=8081; Priority=2; Group="基础"},
    @{Name="evaluation-service";   Port=8092; Priority=2; Group="基础"},
    @{Name="product-service";       Port=8082; Priority=3; Group="商品"},
    @{Name="inspection-service";    Port=8083; Priority=3; Group="商品"},
    @{Name="audit-service";         Port=8095; Priority=3; Group="商品"},
    @{Name="order-service";         Port=8084; Priority=4; Group="交易"},
    @{Name="escrow-service";        Port=8085; Priority=4; Group="交易"},
    @{Name="payment-service";      Port=8086; Priority=4; Group="交易"},
    @{Name="marketing-service";     Port=8091; Priority=4; Group="交易"},
    @{Name="logistics-service";     Port=8087; Priority=5; Group="履约"},
    @{Name="aftersales-service";   Port=8088; Priority=5; Group="履约"},
    @{Name="message-service";       Port=8089; Priority=5; Group="履约"},
    @{Name="community-service";    Port=8090; Priority=6; Group="平台"},
    @{Name="support-service";       Port=8093; Priority=6; Group="平台"},
    @{Name="risk-service";          Port=8094; Priority=6; Group="平台"}
)

# ---- 筛选 ----
if ($Only) {
    $OnlyList = $Only.Split(",") | ForEach-Object { $_.Trim() }
    $Services = $Services | Where-Object { $OnlyList -contains $_.Name }
}
if ($SkipGateway) {
    $Services = $Services | Where-Object { $_.Name -ne "gateway-service" }
}

# 按优先级排序
$Services = $Services | Sort-Object Priority, Name

# ---- 编译 ----
if ($Build) {
    Write-Host "`n🔨 开始编译项目..." -ForegroundColor Cyan
    Push-Location $ProjectRoot
    mvn clean install -DskipTests -T 4 -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ 编译失败，请检查错误" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Write-Host "✅ 编译成功" -ForegroundColor Green
    Pop-Location
}

# ---- 启动服务 ----
Write-Host "`n🚀 开始启动 $($Services.Count) 个服务..." -ForegroundColor Cyan
Write-Host ("=" * 70) -ForegroundColor DarkGray

$StartedCount = 0
foreach ($svc in $Services) {
    $name = $svc.Name
    $port = $svc.Port
    $group = $svc.Group
    $jarPath = "$ProjectRoot\$name\target\$name-1.0.0-SNAPSHOT.jar"

    # 检查 jar 是否存在
    if (-not (Test-Path $jarPath)) {
        Write-Host "⚠️  [$name] jar 不存在，跳过 (请先执行 -Build 编译)" -ForegroundColor Yellow
        continue
    }

    # 检查端口是否已占用
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conn) {
        Write-Host "⚠️  [$name] 端口 $port 已被占用，跳过" -ForegroundColor Yellow
        continue
    }

    Write-Host "▶️  [$group] $name (端口 $port) 启动中..." -ForegroundColor Cyan

    $logFile = "$LogDir\$name.log"
    $jvmOpts = $env:JAVA_OPTS
    if (-not $jvmOpts) { $jvmOpts = "-Xms256m -Xmx512m" }

    Start-Process -FilePath "java" `
        -ArgumentList "$jvmOpts -jar `"$jarPath`"" `
        -WindowStyle Minimized `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError "$LogDir\$name.err.log"

    $StartedCount++
    Start-Sleep -Seconds 3  # 间隔启动，避免资源争抢
}

Write-Host ("=" * 70) -ForegroundColor DarkGray
Write-Host "✅ 已启动 $StartedCount / $($Services.Count) 个服务" -ForegroundColor Green
Write-Host "📄 日志目录: $LogDir" -ForegroundColor Gray
Write-Host ""
Write-Host "服务状态检查:" -ForegroundColor Cyan
Write-Host "  gateway-service:    http://localhost:8080" -ForegroundColor Gray
Write-Host "  user-service:       http://localhost:8081" -ForegroundColor Gray
Write-Host "  product-service:    http://localhost:8082" -ForegroundColor Gray
Write-Host "  inspection-service: http://localhost:8083" -ForegroundColor Gray
Write-Host "  order-service:      http://localhost:8084" -ForegroundColor Gray
Write-Host "  escrow-service:     http://localhost:8085" -ForegroundColor Gray
Write-Host "  payment-service:    http://localhost:8086" -ForegroundColor Gray
Write-Host "  logistics-service:  http://localhost:8087" -ForegroundColor Gray
Write-Host "  aftersales-service: http://localhost:8088" -ForegroundColor Gray
Write-Host "  message-service:    http://localhost:8089" -ForegroundColor Gray
Write-Host "  community-service:  http://localhost:8090" -ForegroundColor Gray
Write-Host "  marketing-service:  http://localhost:8091" -ForegroundColor Gray
Write-Host "  evaluation-service: http://localhost:8092" -ForegroundColor Gray
Write-Host "  support-service:    http://localhost:8093" -ForegroundColor Gray
Write-Host "  risk-service:       http://localhost:8094" -ForegroundColor Gray
Write-Host "  audit-service:      http://localhost:8095" -ForegroundColor Gray
Write-Host ""
Write-Host "Knife4j API 文档: http://localhost:<port>/doc.html" -ForegroundColor Yellow
Write-Host "Nacos 控制台:     http://localhost:8848/nacos (nacos/nacos)" -ForegroundColor Yellow
Write-Host "Sentinel 控制台:  http://localhost:8858 (sentinel/sentinel)" -ForegroundColor Yellow
Write-Host "RocketMQ 控制台:  http://localhost:19876" -ForegroundColor Yellow
