#!/bin/bash

echo "========================================="
echo "  SketchBlog Auto - Admin Server"
echo "========================================="
echo

# Check Python
if ! command -v python3 &> /dev/null; then
    echo "[ERROR] Python3 is not installed!"
    echo "Please install Python 3.9 or higher"
    exit 1
fi

# Create virtual environment if not exists
if [ ! -d "venv" ]; then
    echo "[SETUP] Creating virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
source venv/bin/activate

# Install dependencies
echo "[SETUP] Installing dependencies..."
pip install -r requirements.txt -q

echo
echo "========================================="
echo "  Server starting at http://localhost:8000"
echo "  Admin Panel: http://localhost:8000/"
echo "  Default login: admin / admin123"
echo "========================================="
echo

# Run server
python main.py
