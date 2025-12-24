"""
텍스트 이미지 생성 모듈
배경 이미지에 텍스트를 오버레이하여 썸네일 생성
ThumbnailGenerator.java 기반 Python 구현
"""
import os
import random
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont


class ThumbnailGenerator:
    """텍스트 이미지 생성기"""

    # 기본 폴더 경로
    BASE_DIR = Path(__file__).parent
    BACKGROUND_DIR = BASE_DIR / "images" / "background"
    THUMBNAIL_DIR = BASE_DIR / "images" / "thumbnail"

    # 기본 폰트 목록 (시스템에서 사용 가능한 폰트)
    DEFAULT_FONTS = [
        "malgun.ttf",  # Windows 맑은 고딕
        "NanumGothicBold.ttf",  # 나눔고딕
        "AppleGothic.ttf",  # macOS
        "NotoSansKR-Bold.otf",  # Noto Sans
        "/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf",  # Linux
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",  # Linux fallback
    ]

    def __init__(self):
        # 디렉토리 생성
        self.BACKGROUND_DIR.mkdir(parents=True, exist_ok=True)
        self.THUMBNAIL_DIR.mkdir(parents=True, exist_ok=True)

        # 기본 배경 이미지 생성 (없을 경우)
        self._ensure_default_backgrounds()

    def _ensure_default_backgrounds(self):
        """기본 배경 이미지가 없으면 생성"""
        if not any(self.BACKGROUND_DIR.glob("*.png")) and not any(self.BACKGROUND_DIR.glob("*.jpg")):
            print("[썸네일] 기본 배경 이미지 생성 중...")
            # 단색 배경 이미지 생성
            colors = [
                (255, 245, 238),  # Seashell
                (240, 248, 255),  # Alice Blue
                (245, 255, 250),  # Mint Cream
                (255, 250, 240),  # Floral White
                (248, 248, 255),  # Ghost White
                (255, 228, 225),  # Misty Rose
                (230, 230, 250),  # Lavender
                (240, 255, 240),  # Honeydew
            ]
            for i, color in enumerate(colors):
                img = Image.new("RGB", (1000, 1000), color)
                img.save(self.BACKGROUND_DIR / f"bg_{i+1}.png")
            print(f"[썸네일] {len(colors)}개 기본 배경 생성 완료")

    def _get_font(self, size: int):
        """사용 가능한 폰트 찾기"""
        for font_name in self.DEFAULT_FONTS:
            try:
                return ImageFont.truetype(font_name, size)
            except (IOError, OSError):
                continue
        # 모든 폰트 실패 시 기본 폰트 사용
        return ImageFont.load_default()

    def _get_random_background(self) -> Path:
        """랜덤 배경 이미지 선택"""
        image_patterns = ["*.png", "*.jpg", "*.jpeg", "*.PNG", "*.JPG", "*.JPEG"]
        all_images = []
        for pattern in image_patterns:
            all_images.extend(self.BACKGROUND_DIR.glob(pattern))

        if not all_images:
            raise FileNotFoundError(f"배경 이미지가 없습니다: {self.BACKGROUND_DIR}")

        return random.choice(all_images)

    def _wrap_text(self, text: str, font, max_width: int, draw: ImageDraw.Draw) -> list:
        """텍스트 줄바꿈"""
        words = text.split()
        lines = []
        current_line = ""

        for word in words:
            test_line = f"{current_line} {word}".strip() if current_line else word
            bbox = draw.textbbox((0, 0), test_line, font=font)
            width = bbox[2] - bbox[0]

            if width <= max_width:
                current_line = test_line
            else:
                if current_line:
                    lines.append(current_line)
                current_line = word

        if current_line:
            lines.append(current_line)

        # 한글의 경우 글자 단위로 줄바꿈 (띄어쓰기가 없는 경우)
        if not lines or (len(lines) == 1 and len(lines[0]) > 20):
            lines = []
            current_line = ""
            for char in text:
                test_line = current_line + char
                bbox = draw.textbbox((0, 0), test_line, font=font)
                width = bbox[2] - bbox[0]

                if width <= max_width:
                    current_line = test_line
                else:
                    if current_line:
                        lines.append(current_line)
                    current_line = char

            if current_line:
                lines.append(current_line)

        return lines

    def generate_thumbnail(self, title: str, output_name: str = "result_thumbnail.png") -> str:
        """
        텍스트 오버레이 썸네일 생성

        Args:
            title: 표시할 텍스트
            output_name: 출력 파일명

        Returns:
            생성된 이미지 경로
        """
        target_size = 800

        try:
            # 1. 랜덤 배경 이미지 선택
            bg_path = self._get_random_background()
            print(f"[썸네일] 배경 이미지: {bg_path.name}")

            original = Image.open(bg_path)

            # 2. 정사각형으로 크롭 및 리사이즈
            width, height = original.size
            min_dim = min(width, height)

            # 중앙 크롭
            left = (width - min_dim) // 2
            top = (height - min_dim) // 2
            cropped = original.crop((left, top, left + min_dim, top + min_dim))

            # 타겟 사이즈로 리사이즈
            if cropped.size[0] != target_size:
                cropped = cropped.resize((target_size, target_size), Image.Resampling.LANCZOS)

            # RGB로 변환 (PNG 투명도 문제 방지)
            if cropped.mode != "RGB":
                cropped = cropped.convert("RGB")

            result = cropped.copy()
            draw = ImageDraw.Draw(result)

            # 3. 반투명 흰색 박스 그리기
            box_size = int(target_size * 0.9)
            box_x = (target_size - box_size) // 2
            box_y = (target_size - box_size) // 2

            # 반투명 흰색 오버레이
            overlay = Image.new("RGBA", (target_size, target_size), (0, 0, 0, 0))
            overlay_draw = ImageDraw.Draw(overlay)
            overlay_draw.rounded_rectangle(
                [box_x, box_y, box_x + box_size, box_y + box_size],
                radius=20,
                fill=(255, 255, 255, 200)
            )
            result = Image.alpha_composite(result.convert("RGBA"), overlay).convert("RGB")
            draw = ImageDraw.Draw(result)

            # 4. 텍스트 그리기
            font_size = 60
            font = self._get_font(font_size)

            # 텍스트 줄바꿈
            max_text_width = box_size - 40
            lines = self._wrap_text(title, font, max_text_width, draw)

            # 줄 높이 및 간격
            line_height = font_size + 10
            line_spacing = 20
            total_text_height = len(lines) * line_height + (len(lines) - 1) * line_spacing

            # 시작 Y 좌표 (박스 중앙)
            text_y = box_y + (box_size - total_text_height) // 2

            for line in lines:
                bbox = draw.textbbox((0, 0), line, font=font)
                text_width = bbox[2] - bbox[0]
                text_x = box_x + (box_size - text_width) // 2
                draw.text((text_x, text_y), line, font=font, fill=(0, 0, 0))
                text_y += line_height + line_spacing

            # 5. 저장
            output_path = self.THUMBNAIL_DIR / output_name
            result.save(output_path, "PNG")
            print(f"[썸네일] 저장 완료: {output_path}")

            return str(output_path)

        except Exception as e:
            print(f"[오류] 썸네일 생성 실패: {e}")
            import traceback
            traceback.print_exc()
            return None

    def add_background_image(self, image_path: str) -> bool:
        """배경 이미지 추가"""
        try:
            src = Path(image_path)
            if not src.exists():
                print(f"[오류] 파일이 존재하지 않습니다: {image_path}")
                return False

            dest = self.BACKGROUND_DIR / src.name
            import shutil
            shutil.copy2(src, dest)
            print(f"[썸네일] 배경 이미지 추가: {src.name}")
            return True
        except Exception as e:
            print(f"[오류] 배경 이미지 추가 실패: {e}")
            return False


if __name__ == "__main__":
    # 테스트
    generator = ThumbnailGenerator()

    test_title = "강남역 맛집 추천 베스트 10"
    result = generator.generate_thumbnail(test_title)

    if result:
        print(f"\n생성된 이미지: {result}")
        # 이미지 열기 (Windows)
        if os.name == "nt":
            os.startfile(result)
    else:
        print("썸네일 생성 실패")
