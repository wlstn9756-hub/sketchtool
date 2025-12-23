@echo off
chcp 65001 > nul
echo =========================================
echo   이미지 효과 테스트 빌드 및 실행
echo =========================================
echo.

REM Gradle 확인
where gradle >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Gradle Wrapper 사용...
    set GRADLE_CMD=gradlew.bat
) else (
    set GRADLE_CMD=gradle
)

REM 빌드
echo 🔨 프로젝트 빌드 중...
call %GRADLE_CMD% build -q

if %ERRORLEVEL% NEQ 0 (
    echo ❌ 빌드 실패
    pause
    exit /b 1
)

echo ✅ 빌드 성공!
echo.

REM 실행
echo 🚀 테스트 실행 중...
echo ----------------------------------------
call %GRADLE_CMD% run --args="%*" -q

echo.
echo =========================================
echo 📁 결과 파일: %CD%\output\
echo =========================================
dir /b output 2>nul || echo (output 폴더가 비어있습니다)
echo.
pause
