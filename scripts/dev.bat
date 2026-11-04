@echo off
setlocal EnableExtensions

rem Prefer an explicit Java 17 home, then the known local install, then JAVA_HOME.
set "JDK_HOME=%JAVA17_HOME%"
if not "%JDK_HOME%"=="" goto check

if exist "D:\ProgramFiles\DevTools\Java\jdk\bin\java.exe" set "JDK_HOME=D:\ProgramFiles\DevTools\Java\jdk"
if not "%JDK_HOME%"=="" goto check

set "JDK_HOME=%JAVA_HOME%"
if not "%JDK_HOME%"=="" goto check

echo [ERROR] JDK 17 not found.
echo         Install Temurin 17 or set JAVA17_HOME.
exit /b 1

:check
if not exist "%JDK_HOME%\bin\java.exe" goto bad_jdk

set "JAVA_HOME=%JDK_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [INFO] JAVA_HOME=%JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version

if /I "%~1"=="--check" goto end

where mvn >nul 2>nul
if errorlevel 1 goto no_mvn

call mvn spring-boot:run
goto end

:bad_jdk
echo [ERROR] Invalid JDK path: %JDK_HOME%
exit /b 1

:no_mvn
echo [ERROR] Maven not found in PATH.
exit /b 1

:end
endlocal
