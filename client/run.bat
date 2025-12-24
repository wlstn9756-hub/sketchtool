@echo off
chcp 65001 > nul
title 블로그 자동화 클라이언트

echo.
echo ====================================
echo   블로그 자동화 클라이언트
echo ====================================
echo.

REM Python 확인
where python >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [오류] Python이 설치되어 있지 않습니다.
    echo        https://www.python.org/downloads/ 에서 Python을 설치하세요.
    pause
    exit /b 1
)

REM 패키지 설치 확인
echo [확인] 필요한 패키지 설치 중...
python -m pip install -r requirements.txt -q

echo.
echo [시작] 클라이언트를 실행합니다...
echo.

python client.py %*

echo.
pause
