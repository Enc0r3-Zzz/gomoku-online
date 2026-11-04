@echo off
setlocal

if "%JAVA_HOME%"=="" (
    if exist "D:\ProgramFiles\DevTools\Java\jdk\bin\java.exe" (
        set "JAVA_HOME=D:\ProgramFiles\DevTools\Java\jdk"
    )
)

if "%JAVA_HOME%"=="" (
    echo [ERROR] JAVA_HOME 未设置，且未找到 JDK 17。
    echo         请安装 Temurin 17 并设置 JAVA_HOME。
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [INFO] JAVA_HOME=%JAVA_HOME%
call mvn spring-boot:run
endlocal
