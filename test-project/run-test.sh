#!/bin/bash

echo "========================================="
echo "  이미지 효과 테스트 빌드 및 실행"
echo "========================================="
echo

# Gradle 확인
if ! command -v gradle &> /dev/null; then
    echo "⚠️  Gradle이 설치되어 있지 않습니다."
    echo "   ./gradlew 를 사용합니다..."

    # Gradle Wrapper 다운로드
    if [ ! -f "gradlew" ]; then
        echo "📦 Gradle Wrapper 생성 중..."
        gradle wrapper 2>/dev/null || {
            echo "❌ Gradle이 필요합니다. 설치 후 다시 시도해주세요."
            echo "   macOS: brew install gradle"
            echo "   Ubuntu: sudo apt install gradle"
            exit 1
        }
    fi
    GRADLE_CMD="./gradlew"
else
    GRADLE_CMD="gradle"
fi

# 빌드
echo "🔨 프로젝트 빌드 중..."
$GRADLE_CMD build -q

if [ $? -ne 0 ]; then
    echo "❌ 빌드 실패"
    exit 1
fi

echo "✅ 빌드 성공!"
echo

# 실행
echo "🚀 테스트 실행 중..."
echo "----------------------------------------"
$GRADLE_CMD run --args="$*" -q

echo
echo "========================================="
echo "📁 결과 파일: $(pwd)/output/"
echo "========================================="
ls -la output/ 2>/dev/null || echo "(output 폴더가 비어있습니다)"
