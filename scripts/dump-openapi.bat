@echo off
rem 从运行中的服务导出 OpenAPI 文档，作为 SDK 生成输入
setlocal
set BASE_URL=%~1
if "%BASE_URL%"=="" set BASE_URL=http://localhost:8080
curl -s -o "%~dp0..\api\openapi.json" "%BASE_URL%/v3/api-docs"
if errorlevel 1 (
    echo 导出失败，请确认服务已启动: %BASE_URL%
    exit /b 1
)
echo 已更新 api\openapi.json
endlocal
