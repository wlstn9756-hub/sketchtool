#!/bin/bash

echo ""
echo "===================================="
echo "  블로그 자동화 클라이언트"
echo "===================================="
echo ""

# Python 확인
if ! command -v python3 &> /dev/null; then
    echo "[오류] Python3이 설치되어 있지 않습니다."
    exit 1
fi

# 패키지 설치
echo "[확인] 필요한 패키지 설치 중..."
pip3 install -r requirements.txt -q

echo ""
echo "[시작] 클라이언트를 실행합니다..."
echo ""

python3 client.py "$@"
