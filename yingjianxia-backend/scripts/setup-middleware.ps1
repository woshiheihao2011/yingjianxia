# ============================================================
# Yingjianxia Platform - Local Middleware One-Click Setup (Redis + Nacos)
# For: Windows without Docker
# Usage: Run in PowerShell (as Admin to also start MySQL)
#        .\scripts\setup-middleware.ps1
# Output: C:\Users\DELL\Desktop\trae\middleware\
#           ├── redis\        (Redis 5.0.14.1 Windows)
#           └── nacos\        (Nacos 2.3.0)
# ============================================================

$ErrorActionPreference = "Continue"
$ProgressPreference = "SilentlyContinue"   # speed up Invoke-WebRequest

$MiddlewareDir = "C:\Users\DELL\Desktop\trae\middleware"
if (-not (Test-Path $MiddlewareDir)) { New-Item -ItemType Directory -Path $MiddlewareDir -Force | Out-Null }

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Yingjianxia - Local Middleware Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ---------- Multi-mirror download function ----------
function Download-File {
    param([string[]]$Urls, [string]$OutFile, [string]$Label)
    if (Test-Path $OutFile) {
        $size = (Get-Item $OutFile).Length
        if ($size -gt 100KB) {
            Write-Host "  [$Label] already exists, skip download ($([math]::Round($size/1MB,2)) MB)" -ForegroundColor DarkGray
            return $true
        }
    }
    foreach ($url in $Urls) {
        Write-Host "  [$Label] trying: $url" -ForegroundColor Gray
        try {
            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            Invoke-WebRequest -Uri $url -OutFile $OutFile -UseBasicParsing -TimeoutSec 180
            $sw.Stop()
            if ((Test-Path $OutFile) -and ((Get-Item $OutFile).Length -gt 100KB)) {
                $mb = [math]::Round((Get-Item $OutFile).Length/1MB,2)
                Write-Host "  [$Label] OK  ${mb}MB in $([math]::Round($sw.Elapsed.TotalSeconds,1))s" -ForegroundColor Green
                return $true
            }
        } catch {
            $msg = $_.Exception.Message.Split("`n")[0]
            Write-Host "  [$Label] FAIL: $msg" -ForegroundColor Yellow
        }
    }
    return $false
}

# ============================================================
# 1. Start MySQL service (needs admin)
# ============================================================
Write-Host "`n[1/5] Check MySQL service..." -ForegroundColor Yellow
$svc = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue
if ($svc) {
    if ($svc.Status -ne "Running") {
        try {
            Start-Service -Name "MySQL80" -ErrorAction Stop
            Start-Sleep -Seconds 2
            Write-Host "  OK MySQL80 service started" -ForegroundColor Green
        } catch {
            Write-Host "  FAIL start (needs admin): $_" -ForegroundColor Red
            Write-Host "  Run in [Admin PowerShell]: Start-Service MySQL80" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  OK MySQL80 already running" -ForegroundColor Green
    }
} else {
    Write-Host "  WARN MySQL80 service not found, ensure MySQL is installed" -ForegroundColor Yellow
}

# ============================================================
# 2. Redis download + extract
# ============================================================
Write-Host "`n[2/5] Prepare Redis 5.0.14.1 (Windows)..." -ForegroundColor Yellow
$RedisDir = Join-Path $MiddlewareDir "redis"
$RedisZip = Join-Path $MiddlewareDir "redis.zip"

$needRedis = $true
if (Test-Path "$RedisDir\redis-server.exe") {
    Write-Host "  Redis already extracted, skip" -ForegroundColor DarkGray
    $needRedis = $false
}

if ($needRedis) {
    $redisUrls = @(
        "https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip",
        "https://ghproxy.com/https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip",
        "https://mirror.ghproxy.com/https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip",
        "https://gh-proxy.com/https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip"
    )
    if (-not (Download-File -Urls $redisUrls -OutFile $RedisZip -Label "Redis")) {
        Write-Host "  FAIL Redis download, please manually download to: $RedisZip" -ForegroundColor Red
    } else {
        if (Test-Path $RedisDir) { Remove-Item $RedisDir -Recurse -Force -ErrorAction SilentlyContinue }
        Expand-Archive -Path $RedisZip -DestinationPath $RedisDir -Force
        Write-Host "  OK Redis extracted: $RedisDir" -ForegroundColor Green
    }
}

# ============================================================
# 3. Nacos download + extract
# ============================================================
Write-Host "`n[3/5] Prepare Nacos 2.3.0..." -ForegroundColor Yellow
$NacosRoot = Join-Path $MiddlewareDir "nacos"
$NacosZip = Join-Path $MiddlewareDir "nacos.zip"

$needNacos = $true
if (Test-Path "$NacosRoot\bin\startup.cmd") {
    Write-Host "  Nacos already extracted, skip" -ForegroundColor DarkGray
    $needNacos = $false
}

if ($needNacos) {
    $nacosUrls = @(
        "https://mirror.ghproxy.com/https://github.com/alibaba/nacos/releases/download/2.3.0/nacos-server-2.3.0.zip",
        "https://ghproxy.com/https://github.com/alibaba/nacos/releases/download/2.3.0/nacos-server-2.3.0.zip",
        "https://gh-proxy.com/https://github.com/alibaba/nacos/releases/download/2.3.0/nacos-server-2.3.0.zip",
        "https://github.com/alibaba/nacos/releases/download/2.3.0/nacos-server-2.3.0.zip"
    )
    if (-not (Download-File -Urls $nacosUrls -OutFile $NacosZip -Label "Nacos")) {
        Write-Host "  FAIL Nacos download, please manually download to: $NacosZip" -ForegroundColor Red
    } else {
        $tmpExtract = Join-Path $MiddlewareDir "nacos-tmp"
        if (Test-Path $tmpExtract) { Remove-Item $tmpExtract -Recurse -Force -ErrorAction SilentlyContinue }
        Expand-Archive -Path $NacosZip -DestinationPath $tmpExtract -Force
        # nacos zip top-level is nacos/ folder, move up one level
        if (Test-Path "$tmpExtract\nacos") {
            if (Test-Path $NacosRoot) { Remove-Item $NacosRoot -Recurse -Force -ErrorAction SilentlyContinue }
            Move-Item "$tmpExtract\nacos" $NacosRoot -Force
            Remove-Item $tmpExtract -Recurse -Force -ErrorAction SilentlyContinue
        } else {
            if (-not (Test-Path $NacosRoot)) { New-Item -ItemType Directory -Path $NacosRoot | Out-Null }
            Copy-Item "$tmpExtract\*" $NacosRoot -Recurse -Force
            Remove-Item $tmpExtract -Recurse -Force -ErrorAction SilentlyContinue
        }
        Write-Host "  OK Nacos extracted: $NacosRoot" -ForegroundColor Green
    }
}

# ============================================================
# 4. Start Redis
# ============================================================
Write-Host "`n[4/5] Start Redis (port 6379)..." -ForegroundColor Yellow

$redisPortInUse = (Test-NetConnection -ComputerName 127.0.0.1 -Port 6379 -WarningAction SilentlyContinue).TcpTestSucceeded
if ($redisPortInUse) {
    Write-Host "  OK Redis port 6379 already listening, skip" -ForegroundColor Green
} elseif (Test-Path "$RedisDir\redis-server.exe") {
    Get-Process -Name "redis-server" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    $logDir = Join-Path $MiddlewareDir "logs"
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "$RedisDir\redis-server.exe"
    $psi.Arguments = "--port 6379 --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru"
    $psi.WorkingDirectory = $RedisDir
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true
    $p = New-Object System.Diagnostics.Process
    $p.StartInfo = $psi
    [void]$p.Start()
    try { $p.PriorityClass = "BelowNormal" } catch {}
    $ok = $false
    for ($i=0; $i -lt 15; $i++) {
        Start-Sleep -Seconds 1
        if ((Test-NetConnection -ComputerName 127.0.0.1 -Port 6379 -WarningAction SilentlyContinue).TcpTestSucceeded) { $ok=$true; break }
    }
    if ($ok) {
        Write-Host "  OK Redis started (PID $($p.Id))" -ForegroundColor Green
    } else {
        Write-Host "  WARN Redis port not ready after 15s" -ForegroundColor Yellow
    }
} else {
    Write-Host "  FAIL redis-server.exe not found" -ForegroundColor Red
}

# ============================================================
# 5. Start Nacos (standalone)
# ============================================================
Write-Host "`n[5/5] Start Nacos 2.3.0 standalone (port 8848)..." -ForegroundColor Yellow

$nacosPortInUse = (Test-NetConnection -ComputerName 127.0.0.1 -Port 8848 -WarningAction SilentlyContinue).TcpTestSucceeded
if ($nacosPortInUse) {
    Write-Host "  OK Nacos port 8848 already listening, skip" -ForegroundColor Green
} elseif (Test-Path "$NacosRoot\bin\startup.cmd") {
    $logDir = Join-Path $MiddlewareDir "logs"
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }
    $nacosLog = Join-Path $logDir "nacos.log"

    $cmdStr = "cd /d `"$NacosRoot\bin`" && startup.cmd -m standalone > `"$nacosLog`" 2>&1"
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "cmd.exe"
    $psi.Arguments = "/c $cmdStr"
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $p = New-Object System.Diagnostics.Process
    $p.StartInfo = $psi
    [void]$p.Start()
    Write-Host "  Nacos starting (may take 30-60s)..." -ForegroundColor Gray
    $ok = $false
    for ($i=0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 2
        if ((Test-NetConnection -ComputerName 127.0.0.1 -Port 8848 -WarningAction SilentlyContinue).TcpTestSucceeded) {
            try {
                $resp = Invoke-WebRequest -Uri "http://127.0.0.1:8848/nacos/" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
                if ($resp.StatusCode -eq 200) { $ok=$true; break }
            } catch { <# not ready yet #> }
        }
    }
    if ($ok) {
        Write-Host "  OK Nacos started" -ForegroundColor Green
    } else {
        Write-Host "  WARN Nacos start timeout, check: $nacosLog" -ForegroundColor Yellow
        Write-Host "  Or run manually: cd $NacosRoot\bin; .\startup.cmd -m standalone" -ForegroundColor Gray
    }
} else {
    Write-Host "  FAIL startup.cmd not found, cannot start Nacos" -ForegroundColor Red
}

# ============================================================
# Summary
# ============================================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Middleware Status" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

function Check-Port {
    param($port, $name)
    $up = (Test-NetConnection -ComputerName 127.0.0.1 -Port $port -WarningAction SilentlyContinue).TcpTestSucceeded
    if ($up) { Write-Host "  [OK]   $name : 127.0.0.1:$port" -ForegroundColor Green }
    else     { Write-Host "  [DOWN] $name : 127.0.0.1:$port" -ForegroundColor Red }
}
Check-Port 3306 "MySQL "
Check-Port 6379 "Redis "
Check-Port 8848 "Nacos "

Write-Host "`nMiddleware dir: $MiddlewareDir" -ForegroundColor Gray
Write-Host "Logs dir      : $MiddlewareDir\logs" -ForegroundColor Gray
Write-Host "`nNacos console : http://127.0.0.1:8848/nacos  (user/pass: nacos/nacos)" -ForegroundColor Cyan
Write-Host ""
