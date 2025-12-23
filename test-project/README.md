# 이미지 효과 테스트 프로젝트

블로그 이미지 자동 처리 기능을 테스트하기 위한 프로젝트입니다.

## 주요 기능

### 1. 프리미엄 카드 이미지
- 25레이어 소프트 그림자
- 40px 둥근 모서리
- 얇은 테두리 효과
- 고품질 렌더링

### 2. 2열 그리드 (양옆 배치)
- 2개의 이미지를 나란히 배치
- 자동 크롭 및 리사이즈

### 3. 2x2 그리드
- 4개의 이미지를 2x2로 배치

### 4. 동적 그리드
- 여러 이미지를 자동으로 2열 그리드로 변환

---

## 실행 방법

### 요구사항
- Java 17 이상
- Gradle (또는 Gradle Wrapper 사용)

### Mac/Linux
```bash
chmod +x run-test.sh
./run-test.sh
```

### Windows
```cmd
run-test.bat
```

### 직접 실행
```bash
# 빌드
gradle build

# 샘플 이미지로 테스트
gradle run

# 실제 이미지로 테스트
gradle run --args="image1.jpg image2.jpg"
```

---

## 사용 예시

### 1. 샘플 테스트 (이미지 없이)
```bash
./run-test.sh
```
→ 자동 생성된 샘플 이미지로 모든 효과 테스트

### 2. 단일 이미지 테스트
```bash
./run-test.sh photo.jpg
```
→ 프리미엄 카드 + 기본 카드 생성

### 3. 2열 그리드 테스트
```bash
./run-test.sh photo1.jpg photo2.jpg
```
→ 양옆 배치 이미지 생성

### 4. 2x2 그리드 테스트
```bash
./run-test.sh img1.jpg img2.jpg img3.jpg img4.jpg
```
→ 2x2 그리드 이미지 생성

---

## 출력 결과

테스트 실행 후 `output/` 폴더에 다음 파일들이 생성됩니다:

| 파일명 | 설명 |
|--------|------|
| `1_basic_card.png` | 기본 카드 이미지 |
| `2_premium_card.png` | 프리미엄 카드 이미지 (고급 그림자) |
| `3_two_column_grid.png` | 2열 그리드 (양옆 배치) |
| `4_two_by_two_grid.png` | 2x2 그리드 |
| `5_dynamic_grid_*.png` | 동적 그리드 |
| `6_comparison_*.png` | 기본 vs 프리미엄 비교 |

---

## 코드에서 사용하기

```java
import util.ImageUtil;
import java.awt.image.BufferedImage;

// 이미지 로드
BufferedImage img = ImageUtil.loadImage("photo.jpg");

// 프리미엄 카드 생성
BufferedImage premium = ImageUtil.createPremiumCardImage(img);
ImageUtil.saveImage(premium, "output.png");

// 2열 그리드 생성
BufferedImage grid = ImageUtil.createTwoColumnGrid(img1, img2);

// 2x2 그리드 생성
BufferedImage grid2x2 = ImageUtil.createTwoByTwoGrid(img1, img2, img3, img4);

// 여러 이미지 동적 그리드
List<BufferedImage> grids = ImageUtil.createMultipleGridImages(imageList);
```

---

## 효과 비교

### 기본 vs 프리미엄 그림자

| 항목 | 기본 | 프리미엄 |
|------|------|----------|
| 그림자 레이어 | 1개 | 25개 |
| 그림자 투명도 | 0.03 | 0.12 (비선형) |
| 모서리 반경 | 30px | 40px |
| 테두리 | 없음 | 1px |
| 렌더링 | 기본 | Bicubic |
