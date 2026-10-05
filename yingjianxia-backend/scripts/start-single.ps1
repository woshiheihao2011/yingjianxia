# ============================================================
# 硬件侠平台 - 单服务启动脚本 (PowerShell)
# 用法: .\scripts\start-single.ps1 -Service order-service
# ============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$Service
)

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$LogDir = "$ProjectRoot\logs"
if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir | Out-Null }

$JarPath = "$ProjectRoot\$Service\target\$Service-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $JarPath)) {
    Write-Host "❌ jar 不存在: $JarPath" -ForegroundColor Red
    Write-Host "   请先编译: mvn -pl $Service -am clean install -DskipTests" -ForegroundColor Yellow
    exit 1
}

Write-Host "🚀 启动 $Service ..." -ForegroundColor Cyan
Start-Process -FilePath "java" `
    -ArgumentList "-Xms256m -Xmx512m -jar `"$JarPath`"" `
    -WindowStyle Minimized `
    -RedirectStandardOutput "$LogDir\$Service.log" `
    -RedirectStandardError "$LogDir\$Service.err.log"

Write-Host "✅ $Service 已启动，日志: $LogDir\$Service.log" -ForegroundColor Green
