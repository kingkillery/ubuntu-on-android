@echo off
echo Building Ubuntu on Android...
setlocal

REM Set Java path (no quotes in JAVA_HOME)
set JAVA_HOME=C:\Progra~1\Android\Android~1\jbr

REM Add Java to PATH
set PATH=%JAVA_HOME%\bin;%PATH%

REM Change to project directory
cd /d "%~dp0"

REM Build and install
echo.
echo Building debug APK...
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b 1
)

echo.
echo Build successful! Installing to device...
adb install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   Installation successful!
    echo ========================================
    echo.
    echo Check your tablet for "Ubuntu on Android" app
) else (
    echo Installation failed. Make sure device is connected via USB debugging.
)

endlocal
pause
