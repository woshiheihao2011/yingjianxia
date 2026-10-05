# ============================================================
# Yingjianxia Platform - Local Database Init (PowerShell)
# Usage: .\scripts\init-db.ps1
# Prereq: Local MySQL running, root password 123456
# ============================================================

param(
    [string]$MySQLHost = "127.0.0.1",
    [string]$Port = "3306",
    [string]$User = "root",
    [string]$Password = "123456"
)

$SqlDir = Join-Path $PSScriptRoot "..\..\hardware-architecture-design\sql"
if (-not (Test-Path $SqlDir)) {
    Write-Host "SQL dir not found: $SqlDir" -ForegroundColor Red
    exit 1
}

# Database -> SQL file mapping
$DbMap = @(
    @{Db="db_user";        File="01_db_user.sql"},
    @{Db="db_product";     File="02_db_product.sql"},
    @{Db="db_inspection";  File="03_db_inspection.sql"},
    @{Db="db_order";       File="04_db_order.sql"},
    @{Db="db_escrow";      File="05_db_escrow.sql"},
    @{Db="db_logistics";   File="06_db_logistics.sql"},
    @{Db="db_aftersales";  File="07_db_aftersales.sql"},
    @{Db="db_message";     File="08_db_message.sql"},
    @{Db="db_support";     File="09_db_support.sql"},
    @{Db="db_evaluation";  File="10_db_evaluation.sql"},
    @{Db="db_risk";        File="11_db_risk.sql"},
    @{Db="db_audit";       File="12_db_audit.sql"}
)

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Yingjianxia - Database Init" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check mysql client
$mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysqlCmd) {
    Write-Host "FAIL: mysql client not in PATH" -ForegroundColor Red
    Write-Host "Add MySQL bin to PATH, e.g. C:\ProgramData\MySQL\MySQL Server 8.0\bin" -ForegroundColor Yellow
    exit 1
}

# 1. Create all databases
Write-Host "`n[1/2] Create databases..." -ForegroundColor Yellow

$databases = @(
    "db_user","db_product","db_inspection","db_order","db_escrow",
    "db_logistics","db_aftersales","db_message","db_support",
    "db_evaluation","db_audit","db_risk"
)

$createLines = $databases | ForEach-Object {
    "CREATE DATABASE IF NOT EXISTS $_ DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"
}
$createSql = $createLines -join "`n"

# Pipe SQL string to mysql via stdin (PowerShell does not support < redirect)
$errors = $createSql | & mysql --host=$MySQLHost --port=$Port --user=$User --password=$Password --default-character-set=utf8mb4 2>&1
$errFound = $false
$errors | ForEach-Object {
    if ($_ -is [System.Management.Automation.ErrorRecord]) {
        $msg = $_.Exception.Message
        if ($msg -match "ERROR|error") { Write-Host "  $msg" -ForegroundColor Red; $errFound=$true }
    } elseif ("$_" -match "ERROR") { Write-Host "  $_" -ForegroundColor Red; $errFound=$true }
}
if (-not $errFound) {
    Write-Host "  OK 12 databases created/verified" -ForegroundColor Green
}

# 2. Import table structure SQL
Write-Host "`n[2/2] Import table structure..." -ForegroundColor Yellow

foreach ($entry in $DbMap) {
    $sqlFile = Join-Path $SqlDir $entry.File
    if (Test-Path $sqlFile) {
        Write-Host "  Import $($entry.Db) <- $($entry.File)..." -NoNewline
        # Use .NET Process with binary-mode to preserve UTF-8 bytes through stdin
        # (PowerShell pipe re-encodes UTF-8 -> latin1, corrupting Chinese chars to '?')
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = "mysql"
        $psi.Arguments = "--host=$MySQLHost --port=$Port --user=$User --password=$Password --default-character-set=utf8mb4 --binary-mode $($entry.Db)"
        $psi.RedirectStandardInput = $true
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $psi.UseShellExecute = $false
        $proc = [System.Diagnostics.Process]::Start($psi)
        $content = [System.IO.File]::ReadAllText($sqlFile, [System.Text.Encoding]::UTF8)
        $proc.StandardInput.WriteLine($content)
        $proc.StandardInput.Close()
        $null = $proc.StandardOutput.ReadToEnd()
        $errOut = $proc.StandardError.ReadToEnd()
        $proc.WaitForExit(60000)
        $hasErr = $false
        if ($errOut -and $errOut -match "ERROR") {
            Write-Host " WARN:$errOut" -ForegroundColor Yellow -NoNewline
            $hasErr = $true
        }
        if (-not $hasErr) { Write-Host " OK" -ForegroundColor Green }
        else { Write-Host "" }
    } else {
        Write-Host "  SKIP $($entry.Db): $sqlFile not found" -ForegroundColor Yellow
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Database Init Done" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Databases:" -ForegroundColor Gray
Write-Host "  db_user, db_product, db_inspection, db_order" -ForegroundColor Gray
Write-Host "  db_escrow, db_logistics, db_aftersales" -ForegroundColor Gray
Write-Host "  db_message, db_support, db_evaluation" -ForegroundColor Gray
Write-Host "  db_audit, db_risk" -ForegroundColor Gray
Write-Host ""
