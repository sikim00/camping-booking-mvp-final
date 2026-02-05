@echo off
setlocal
set DIR=%~dp0
set WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo [ERROR] gradle-wrapper.jar not found: %WRAPPER_JAR%
  exit /b 1
)

REM Prefer JAVA_HOME if set, else use java from PATH
if defined JAVA_HOME (
  "%JAVA_HOME%\bin\java.exe" -jar "%WRAPPER_JAR%" %*
) else (
  java -jar "%WRAPPER_JAR%" %*
)
