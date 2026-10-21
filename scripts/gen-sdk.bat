@echo off
rem 由 api\openapi.json 生成并编译 Java SDK
setlocal
call mvn -f "%~dp0..\sdk-java\pom.xml" clean package
if errorlevel 1 (
    echo SDK 构建失败
    exit /b 1
)
echo SDK 已生成: sdk-java\target\openapi-sdk-java-1.0.0.jar
endlocal
