"""
블로그 자동화 클라이언트 (Python 버전)
서버에서 작업을 받아 네이버 블로그에 자동 포스팅합니다.
"""
import sys
import time
import json
import requests
from datetime import datetime

from config import CONFIG, save_config
from naver_login import NaverLogin
from gpt_generator import GPTGenerator
from blog_poster import BlogPoster
from thumbnail_generator import ThumbnailGenerator


class BlogAutoClient:
    """블로그 자동화 클라이언트"""

    def __init__(self):
        self.server_url = CONFIG["server_url"]
        self.hw_code = CONFIG["hw_code"]
        self.pc_name = CONFIG["pc_name"]
        self.gpt_api_key = None
        self.running = False

    def print_banner(self):
        """시작 배너 출력"""
        print("=" * 60)
        print("  블로그 자동화 클라이언트 (Python Edition)")
        print("=" * 60)
        print(f"  서버: {self.server_url}")
        print(f"  PC: {self.pc_name} ({self.hw_code})")
        print(f"  시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 60)
        print()

    def check_server(self):
        """서버 연결 확인"""
        try:
            response = requests.get(f"{self.server_url}/api/health", timeout=10)
            if response.status_code == 200:
                print("[서버] 연결 성공")
                return True
        except requests.exceptions.ConnectionError:
            print(f"[오류] 서버에 연결할 수 없습니다: {self.server_url}")
        except Exception as e:
            print(f"[오류] 서버 확인 실패: {e}")
        return False

    def register_pc(self):
        """PC 등록"""
        try:
            response = requests.post(
                f"{self.server_url}/api/pc/register",
                json={
                    "hw_code": self.hw_code,
                    "pc_name": self.pc_name
                },
                timeout=10
            )

            if response.status_code == 200:
                data = response.json()
                if data.get("success"):
                    print(f"[PC] 등록 완료: {self.pc_name}")
                    return True
                else:
                    print(f"[PC] 등록 실패: {data.get('error', '알 수 없는 오류')}")

        except Exception as e:
            print(f"[오류] PC 등록 실패: {e}")

        return False

    def get_settings(self):
        """서버에서 설정 가져오기"""
        try:
            response = requests.get(
                f"{self.server_url}/api/settings",
                timeout=10
            )

            if response.status_code == 200:
                settings = response.json()
                self.gpt_api_key = settings.get("chatgpt_api_key")
                return settings

        except Exception as e:
            print(f"[오류] 설정 가져오기 실패: {e}")

        return {}

    def get_task(self):
        """서버에서 작업 가져오기"""
        try:
            response = requests.get(
                f"{self.server_url}/api/task/get",
                params={"hw_code": self.hw_code},
                timeout=10
            )

            if response.status_code == 200:
                data = response.json()
                if data.get("task"):
                    return data["task"]

        except Exception as e:
            print(f"[오류] 작업 가져오기 실패: {e}")

        return None

    def update_task_status(self, task_id, status, result_url=None, error_msg=None):
        """작업 상태 업데이트"""
        try:
            payload = {
                "task_id": task_id,
                "status": status,
                "hw_code": self.hw_code
            }

            if result_url:
                payload["result_url"] = result_url
            if error_msg:
                payload["error_message"] = error_msg

            response = requests.post(
                f"{self.server_url}/api/task/update",
                json=payload,
                timeout=10
            )

            return response.status_code == 200

        except Exception as e:
            print(f"[오류] 상태 업데이트 실패: {e}")
            return False

    def process_task(self, task):
        """작업 처리"""
        task_id = task.get("id")
        distribution = task.get("distribution", {})
        account = task.get("account", {})

        print(f"\n{'='*50}")
        print(f"[작업] ID: {task_id}")
        print(f"[작업] 업체: {distribution.get('place_name')}")
        print(f"[작업] 키워드: {distribution.get('main_keyword')}")
        print(f"[작업] 계정: {account.get('naver_id')}")
        print(f"{'='*50}\n")

        # 상태를 진행중으로 업데이트
        self.update_task_status(task_id, "in_progress")

        naver = None
        try:
            # 1. GPT로 콘텐츠 생성
            print("[단계 1/4] GPT 콘텐츠 생성 중...")

            if self.gpt_api_key:
                generator = GPTGenerator(self.gpt_api_key)
                content = generator.generate_blog_content(
                    place_name=distribution.get("place_name"),
                    address=distribution.get("place_address"),
                    category=distribution.get("distribution_category"),
                    keyword=distribution.get("main_keyword"),
                    forbidden_words=distribution.get("forbidden_words"),
                    custom_prompt=distribution.get("prompt_template"),
                    bottom_tags=distribution.get("bottom_tags")
                )
            else:
                # API 키 없으면 간단한 콘텐츠 생성
                print("[경고] GPT API 키가 없어 기본 콘텐츠를 사용합니다")
                generator = GPTGenerator("")
                content = generator.generate_simple_content(
                    distribution.get("main_keyword"),
                    distribution.get("place_name")
                )

            if not content:
                raise Exception("콘텐츠 생성 실패")

            print(f"[콘텐츠] 제목: {content['title']}")

            # 1.5 이미지 처리
            images = []
            image_option = distribution.get("image_option", "none")
            print(f"[이미지] 옵션: {image_option}")

            if image_option == "text_image":
                # 텍스트 이미지 생성
                print("[이미지] 텍스트 이미지 생성 중...")
                thumb_gen = ThumbnailGenerator()
                thumb_path = thumb_gen.generate_thumbnail(content["title"])
                if thumb_path:
                    images.append(thumb_path)
                    print(f"[이미지] 썸네일 생성 완료: {thumb_path}")

            elif image_option == "custom" and distribution.get("image_urls"):
                # 직접 입력된 이미지 URL
                urls = distribution.get("image_urls", "")
                if urls:
                    images = [u.strip() for u in urls.split("\n") if u.strip()]
                    print(f"[이미지] 커스텀 이미지 {len(images)}개")

            # 2. 네이버 로그인
            print("\n[단계 2/4] 네이버 로그인 중...")

            naver = NaverLogin(
                headless=CONFIG.get("chrome_headless", False),
                proxy=account.get("proxy_ip")
            )

            if not naver.login(account.get("naver_id"), account.get("naver_pw")):
                raise Exception("네이버 로그인 실패")

            # 3. 블로그 포스팅
            print("\n[단계 3/4] 블로그 글 작성 중...")

            poster = BlogPoster(naver.driver)

            result_url = poster.write_post(
                title=content["title"],
                body=content["body"],
                tags=content.get("tags"),
                images=images if images else None
            )

            if not result_url:
                raise Exception("블로그 포스팅 실패")

            # 4. 완료 처리
            print("\n[단계 4/4] 작업 완료 처리...")

            self.update_task_status(task_id, "completed", result_url=result_url)
            print(f"\n[완료] 포스팅 성공: {result_url}")

            return True

        except Exception as e:
            error_msg = str(e)
            print(f"\n[실패] 작업 실패: {error_msg}")
            self.update_task_status(task_id, "fail", error_msg=error_msg)
            return False

        finally:
            # 브라우저 정리
            if naver:
                time.sleep(2)
                naver.close()

    def run(self):
        """메인 실행 루프"""
        self.print_banner()

        # 서버 연결 확인
        if not self.check_server():
            print("\n[!] 서버에 연결할 수 없습니다.")
            print(f"    서버가 실행 중인지 확인하세요: {self.server_url}")
            return

        # PC 등록
        self.register_pc()

        # 설정 가져오기
        self.get_settings()

        print("\n[시작] 작업 대기 중... (Ctrl+C로 종료)")
        print("-" * 50)

        self.running = True
        task_interval = CONFIG.get("task_interval", 60)

        while self.running:
            try:
                # 작업 가져오기
                task = self.get_task()

                if task:
                    self.process_task(task)
                    print("\n[대기] 다음 작업 확인까지 10초...")
                    time.sleep(10)
                else:
                    # 작업 없으면 대기
                    print(f"[대기] 작업 없음. {task_interval}초 후 다시 확인...", end="\r")
                    time.sleep(task_interval)

            except KeyboardInterrupt:
                print("\n\n[종료] 사용자 요청으로 종료합니다.")
                self.running = False

            except Exception as e:
                print(f"\n[오류] 예외 발생: {e}")
                time.sleep(30)

    def run_single_test(self, naver_id, naver_pw, keyword, place_name):
        """단일 테스트 실행"""
        self.print_banner()
        print("[테스트 모드] 단일 포스팅 테스트")
        print("-" * 50)

        # 설정 가져오기
        self.get_settings()

        test_task = {
            "id": 0,
            "distribution": {
                "place_name": place_name,
                "place_address": "테스트 주소",
                "distribution_category": "맛집",
                "main_keyword": keyword,
                "forbidden_words": "",
                "prompt_template": None,
                "bottom_tags": f"#{keyword}"
            },
            "account": {
                "naver_id": naver_id,
                "naver_pw": naver_pw,
                "proxy_ip": None
            }
        }

        self.process_task(test_task)


def setup_wizard():
    """초기 설정 마법사"""
    print("\n" + "=" * 50)
    print("  블로그 자동화 클라이언트 - 초기 설정")
    print("=" * 50 + "\n")

    # 서버 URL 설정
    print("1. 서버 URL 설정")
    server_url = input(f"   서버 URL [{CONFIG['server_url']}]: ").strip()
    if server_url:
        CONFIG["server_url"] = server_url

    # PC 이름 설정
    print("\n2. PC 이름 설정")
    pc_name = input(f"   PC 이름 [{CONFIG['pc_name']}]: ").strip()
    if pc_name:
        CONFIG["pc_name"] = pc_name

    # 헤드리스 모드
    print("\n3. Chrome 헤드리스 모드")
    print("   (백그라운드 실행, 브라우저 창 안 보임)")
    headless = input("   헤드리스 모드 사용? [y/N]: ").strip().lower()
    CONFIG["chrome_headless"] = headless == 'y'

    # 설정 저장
    save_config(CONFIG)
    print("\n[설정 완료] 설정이 저장되었습니다.")
    print(f"   설정 파일: client_config.json")


def main():
    """메인 함수"""
    if len(sys.argv) > 1:
        cmd = sys.argv[1].lower()

        if cmd == "setup":
            setup_wizard()
            return

        elif cmd == "test":
            # 테스트 모드
            client = BlogAutoClient()

            print("\n[테스트 모드]")
            naver_id = input("네이버 아이디: ").strip()
            naver_pw = input("네이버 비밀번호: ").strip()
            keyword = input("키워드: ").strip() or "맛집추천"
            place_name = input("업체명: ").strip() or "테스트 업체"

            client.run_single_test(naver_id, naver_pw, keyword, place_name)
            return

        elif cmd == "help":
            print("\n사용법:")
            print("  python client.py        - 클라이언트 실행")
            print("  python client.py setup  - 초기 설정")
            print("  python client.py test   - 단일 테스트")
            print("  python client.py help   - 도움말")
            return

    # 일반 실행
    client = BlogAutoClient()
    client.run()


if __name__ == "__main__":
    main()
