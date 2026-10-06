@echo off
setlocal

where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)

echo Gradle is not installed on PATH.
echo Open the project in Android Studio and use its configured Gradle 8.13 distribution,
echo or install Gradle 8.13 and run this script again.
exit /b 1
