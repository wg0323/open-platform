@echo off
rem 固定用 JDK 8 执行 Maven：系统 JAVA_HOME 若指向 JDK 23+，javac 默认不再运行注解处理器，Lombok 会静默失效
setlocal
if not defined SBK_JDK8_HOME set "SBK_JDK8_HOME=C:\Program Files\jdk"
set "JAVA_HOME=%SBK_JDK8_HOME%"
call mvn -f "%~dp0..\pom.xml" %*
if errorlevel 1 (
    echo 构建失败，请确认 %SBK_JDK8_HOME% 是 JDK 8
    exit /b 1
)
echo 构建完成
endlocal
