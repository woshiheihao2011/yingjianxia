@echo off
REM ============================================================
REM 硬件侠平台 - Windows 批处理启动脚本
REM 用法: scripts\start-all.bat
REM ============================================================

cd /d "%~dp0\.."

echo.
echo ============================================================
echo   硬件侠平台 - 服务启动 (Windows Batch)
echo ============================================================
echo.

REM ---- 加载环境变量 ----
if exist "scripts\env.bat" (
    call scripts\env.bat
) else (
    echo [INFO] env.bat 不存在，使用默认配置
    set NACOS_SERVER=127.0.0.1:8848
    set MYSQL_HOST=127.0.0.1
    set MYSQL_PORT=3306
    set MYSQL_USER=root
    set MYSQL_PWD=123456
    set REDIS_HOST=127.0.0.1
    set REDIS_PORT=6379
    set REDIS_PWD=
)

if not exist "logs" mkdir "logs"

REM ---- 服务列表: 名称 端口 ----
set SERVICES=gateway-service:8080 user-service:8081 evaluation-service:8092 product-service:8082 inspection-service:8083 audit-service:8095 order-service:8084 escrow-service:8085 payment-service:8086 marketing-service:8091 logistics-service:8087 aftersales-service:8088 message-service:8089 community-service:8090 support-service:8093 risk-service:8094

for %%s in (%SERVICES%) do (
    for /f "tokens=1,2 delims=:" %%a in ("%%s") do (
        set SVCNAME=%%a
        set SVCPORT=%%b
        set JARPATH=%%a\target\%%a-1.0.0-SNAPSHOT.jar
        if exist "%%a\target\%%a-1.0.0-SNAPSHOT.jar" (
            echo [START] %%a (port %%b)
            start /min "%%a" java -Xms256m -Xmx512m -jar "%%a\target\%%a-1.0.0-SNAPSHOT.jar" > "logs\%%a.log" 2>&1
            timeout /t 3 /nobreak >nul
        ) else (
            echo [SKIP]  %%a - jar not found, run: mvn clean install -DskipTests
        )
    )
)

echo.
echo ============================================================
echo   启动完成！
echo   日志目录: logs\
echo   Nacos:   http://localhost:8848/nacos
echo   API文档: http://localhost:8080/doc.html
echo ============================================================
pause
