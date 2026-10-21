@echo off
rem 用 SDK 对运行中的服务做冒烟测试：查询围栏、上传订单（幂等）、上传位置
setlocal
set BASE_URL=%~1
if "%BASE_URL%"=="" set BASE_URL=http://localhost:8080
call mvn -f "%~dp0..\sdk-java\pom.xml" test -Dsbike.smoke=true -Dsbike.baseUrl=%BASE_URL%
if errorlevel 1 (
    echo 冒烟测试失败
    exit /b 1
)
echo 冒烟测试通过
endlocal
