@echo off
REM 硬件侠平台 - Windows 环境变量模板
REM 复制为 env.bat 并修改实际值

set NACOS_SERVER=127.0.0.1:8848
set NACOS_NAMESPACE=public

set MYSQL_HOST=127.0.0.1
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PWD=123456

set REDIS_HOST=127.0.0.1
set REDIS_PORT=6379
set REDIS_PWD=

set SENTINEL_DASHBOARD=127.0.0.1:8858

set ROCKETMQ_NAMESRV=127.0.0.1:9876

set JWT_SECRET=yingjianxia-jwt-secret-please-change-in-production
set INTERNAL_SECRET=yjx-internal-secret-please-change-in-production

set JAVA_OPTS=-Xms256m -Xmx512m -Xmn128m
