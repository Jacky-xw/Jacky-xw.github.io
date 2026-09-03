@echo off
setlocal
where gradle >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Gradle was not found in PATH.
  echo Open this project in Android Studio, or install Gradle and Android SDK first.
  pause
  exit /b 1
)
gradle :app:assembleDebug
if errorlevel 1 exit /b 1
echo.
echo APK: app\build\outputs\apk\debug\app-debug.apk
pause
