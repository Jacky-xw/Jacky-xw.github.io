@echo off
setlocal
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo 未检测到本机 Gradle。请安装 Gradle 8.11.1 后重新运行，或在 Android Studio 中打开此项目。
exit /b 1
