# ============================================================
# Yingjianxia - Start escrow-service (port 8085) for local testing
# Aligns REDIS_PWD with local Redis password (123456)
# ============================================================

$env:REDIS_PWD = "123456"
$env:MYSQL_PWD = "123456"

$JarPath = "c:\Users\DELL\Desktop\trae\yingjianxia-backend\escrow-service\target\escrow-service-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $JarPath)) {
    Write-Host "FAIL: jar not found: $JarPath" -ForegroundColor Red
    Write-Host "Run: mvn -s settings-local.xml -pl escrow-service -am clean install -DskipTests" -ForegroundColor Yellow
    exit 1
}

$SentinelLogDir = "c:/Users/DELL/Desktop/trae/yingjianxia-backend/logs/sentinel"
if (-not (Test-Path $SentinelLogDir)) { New-Item -ItemType Directory -Path $SentinelLogDir -Force | Out-Null }

Write-Host "Starting escrow-service (port 8085)..." -ForegroundColor Cyan
Write-Host "  REDIS_PWD=$($env:REDIS_PWD)  MYSQL_PWD=$($env:MYSQL_PWD)  Sentinel=disabled" -ForegroundColor Gray
Write-Host "  Knife4j: http://127.0.0.1:8085/doc.html" -ForegroundColor Cyan
Write-Host ""

java "-Dcsp.sentinel.log.dir=$SentinelLogDir" -Xms256m -Xmx512m -jar $JarPath --spring.cloud.sentinel.enabled=false
