@echo off
echo =========================================
echo   Image Effect Test - Build and Run
echo =========================================
echo.

REM Check Gradle
where gradle >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Using Gradle Wrapper...
    set GRADLE_CMD=gradlew.bat
) else (
    set GRADLE_CMD=gradle
)

REM Build
echo [BUILD] Building project...
call %GRADLE_CMD% build -q

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

echo [OK] Build successful!
echo.

REM Run
echo [RUN] Running test...
echo ----------------------------------------
call %GRADLE_CMD% run --args="%*" -q

echo.
echo =========================================
echo [OUTPUT] Result files: %CD%\output\
echo =========================================
dir /b output 2>nul || echo (output folder is empty)
echo.
pause
