# ============================================================
# Yingjianxia - Start gateway-service (port 8080) for local testing
# Gateway 是 Spring Cloud Gateway（Netty），无 Knife4j，用 actuator 验证
# ============================================================

$env:REDIS_PWD = "123456"

$JarPath = "c:\Users\DELL\Desktop\trae\yingjianxia-backend\gateway-service\target\gateway-service-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $JarPath)) {
    Write-Host "FAIL: jar not found: $JarPath" -ForegroundColor Red
    Write-Host "Run: mvn -s settings-local.xml -pl gateway-service -am clean install -DskipTests" -ForegroundColor Yellow
    exit 1
}

$SentinelLogDir = "c:/Users/DELL/Desktop/trae/yingjianxia-backend/logs/sentinel"
if (-not (Test-Path $SentinelLogDir)) { New-Item -ItemType Directory -Path $SentinelLogDir -Force | Out-Null }

Write-Host "Starting gateway-service (port 8080)..." -ForegroundColor Cyan
Write-Host "  REDIS_PWD=$($env:REDIS_PWD)  Sentinel=disabled" -ForegroundColor Gray
Write-Host "  Gateway: http://127.0.0.1:8080/" -ForegroundColor Cyan
Write-Host "  Actuator: http://127.0.0.1:8080/actuator/health" -ForegroundColor Cyan
Write-Host ""

java "-Dcsp.sentinel.log.dir=$SentinelLogDir" -Xms256m -Xmx512m -jar $JarPath --spring.cloud.sentinel.enabled=false --spring.data.redis.password=$($env:REDIS_PWD) --redisson.single-server-config.password=$($env:REDIS_PWD)
