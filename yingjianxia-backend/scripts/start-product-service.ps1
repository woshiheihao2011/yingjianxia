# ============================================================
# Yingjianxia - Start product-service (port 8082) for local testing
# Aligns REDIS_PWD with local Redis password (123456)
# ============================================================

# Align with local Redis password
$env:REDIS_PWD = "123456"
$env:MYSQL_PWD = "123456"

$JarPath = "c:\Users\DELL\Desktop\trae\yingjianxia-backend\product-service\target\product-service-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $JarPath)) {
    Write-Host "FAIL: jar not found: $JarPath" -ForegroundColor Red
    Write-Host "Run: mvn -s settings-local.xml -pl product-service -am clean install -DskipTests" -ForegroundColor Yellow
    exit 1
}

# Sentinel log dir (avoid DateFileLogHandler NPE); disable Sentinel for local test
$SentinelLogDir = "c:/Users/DELL/Desktop/trae/yingjianxia-backend/logs/sentinel"
if (-not (Test-Path $SentinelLogDir)) { New-Item -ItemType Directory -Path $SentinelLogDir -Force | Out-Null }

# XXL-Job executor log path
$XxlLogPath = "c:/Users/DELL/Desktop/trae/yingjianxia-backend/logs/xxl-job/product-service"
if (-not (Test-Path $XxlLogPath)) { New-Item -ItemType Directory -Path $XxlLogPath -Force | Out-Null }

Write-Host "Starting product-service (port 8082)..." -ForegroundColor Cyan
Write-Host "  REDIS_PWD=$($env:REDIS_PWD)  MYSQL_PWD=$($env:MYSQL_PWD)  Sentinel=disabled" -ForegroundColor Gray
Write-Host "  Knife4j: http://127.0.0.1:8082/doc.html" -ForegroundColor Cyan
Write-Host ""

java "-Dcsp.sentinel.log.dir=$SentinelLogDir" -Xms256m -Xmx512m -jar $JarPath --spring.cloud.sentinel.enabled=false
