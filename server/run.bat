@echo off
echo =========================================
echo   SketchBlog Auto - Admin Server
echo =========================================
echo.

REM Check Python
python --version >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Python is not installed!
    echo Please install Python 3.9 or higher from https://www.python.org/
    pause
    exit /b 1
)

REM Create virtual environment if not exists
if not exist "venv" (
    echo [SETUP] Creating virtual environment...
    python -m venv venv
)

REM Activate virtual environment
call venv\Scripts\activate.bat

REM Install dependencies
echo [SETUP] Installing dependencies...
pip install -r requirements.txt -q

echo.
echo =========================================
echo   Server starting at http://localhost:8000
echo   Admin Panel: http://localhost:8000/
echo   Default login: admin / admin123
echo =========================================
echo.

REM Run server
python main.py
