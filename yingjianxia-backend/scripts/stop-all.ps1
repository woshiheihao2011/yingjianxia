# ============================================================
# 硬件侠平台 - 停止所有服务脚本 (PowerShell)
# 用法: .\scripts\stop-all.ps1
# ============================================================

$Services = @(
    "gateway-service", "user-service", "evaluation-service",
    "product-service", "inspection-service", "audit-service",
    "order-service", "escrow-service", "payment-service", "marketing-service",
    "logistics-service", "aftersales-service", "message-service",
    "community-service", "support-service", "risk-service"
)

$Ports = @{
    "gateway-service"=8080; "user-service"=8081; "evaluation-service"=8092
    "product-service"=8082; "inspection-service"=8083; "audit-service"=8095
    "order-service"=8084; "escrow-service"=8085; "payment-service"=8086
    "marketing-service"=8091; "logistics-service"=8087; "aftersales-service"=8088
    "message-service"=8089; "community-service"=8090; "support-service"=8093
    "risk-service"=8094
}

Write-Host "`n🛑 停止所有硬件侠服务..." -ForegroundColor Cyan

$StoppedCount = 0
foreach ($name in $Services) {
    $port = $Ports[$name]
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conn) {
        $pid = $conn[0].OwningProcess
        try {
            Stop-Process -Id $pid -Force -ErrorAction Stop
            Write-Host "  ✅ $name (PID: $pid, 端口: $port) 已停止" -ForegroundColor Green
            $StoppedCount++
        } catch {
            Write-Host "  ⚠️  $name (PID: $pid) 停止失败: $_" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ⏭️  $name (端口: $port) 未运行" -ForegroundColor DarkGray
    }
}

Write-Host "`n已停止 $StoppedCount 个服务" -ForegroundColor Green
