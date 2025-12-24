"""
네이버 로그인 모듈 (개선된 버전)
Selenium을 사용하여 네이버에 로그인합니다.
- 쿠키 저장/로드 지원
- 로그인 상태 유지
- IP 보안 설정 자동 해제
"""
import time
import os
import json
import pickle
from pathlib import Path

try:
    import pyperclip
    HAS_PYPERCLIP = True
except ImportError:
    HAS_PYPERCLIP = False

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from webdriver_manager.chrome import ChromeDriverManager


class NaverLogin:
    """네이버 로그인 클래스 (쿠키 기반 로그인 지원)"""

    LOGIN_URL = "https://nid.naver.com/nidlogin.login"
    BLOG_URL = "https://blog.naver.com"
    COOKIES_DIR = Path(__file__).parent / "cookies"

    def __init__(self, headless=False, proxy=None):
        self.driver = None
        self.headless = headless
        self.proxy = proxy
        self.logged_in = False
        self.current_account = None

        # 쿠키 디렉토리 생성
        self.COOKIES_DIR.mkdir(exist_ok=True)

    def init_driver(self):
        """Chrome 드라이버 초기화"""
        options = Options()

        if self.headless:
            options.add_argument("--headless=new")

        # 기본 옵션
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--disable-gpu")
        options.add_argument("--window-size=1920,1080")
        options.add_argument("--disable-blink-features=AutomationControlled")
        options.add_experimental_option("excludeSwitches", ["enable-automation"])
        options.add_experimental_option("useAutomationExtension", False)

        # 알림 및 팝업 비활성화
        options.add_argument("--disable-notifications")
        options.add_argument("--disable-popup-blocking")
        options.add_argument("--disable-infobars")

        # User-Agent 설정
        options.add_argument("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

        # 클립보드 권한
        prefs = {
            "profile.default_content_setting_values.clipboard": 1,
            "profile.default_content_setting_values.notifications": 2,
        }
        options.add_experimental_option("prefs", prefs)

        # 프록시 설정
        if self.proxy:
            options.add_argument(f"--proxy-server={self.proxy}")
            print(f"[프록시] 설정됨: {self.proxy}")

        # ChromeDriver 자동 설치 및 실행
        try:
            service = Service(ChromeDriverManager().install())
            self.driver = webdriver.Chrome(service=service, options=options)
        except Exception as e:
            print(f"[오류] ChromeDriver 초기화 실패: {e}")
            raise

        # 자동화 감지 우회
        self.driver.execute_cdp_cmd("Page.addScriptToEvaluateOnNewDocument", {
            "source": """
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined
                });
                window.navigator.chrome = { runtime: {} };
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['ko-KR', 'ko']
                });
            """
        })

        return self.driver

    def _get_cookie_file(self, naver_id):
        """계정별 쿠키 파일 경로"""
        return self.COOKIES_DIR / f"{naver_id}_cookies.pkl"

    def _save_cookies(self, naver_id):
        """쿠키 저장"""
        try:
            cookie_file = self._get_cookie_file(naver_id)
            cookies = self.driver.get_cookies()
            with open(cookie_file, 'wb') as f:
                pickle.dump(cookies, f)
            print(f"[쿠키] {naver_id} 쿠키 저장 완료")
        except Exception as e:
            print(f"[경고] 쿠키 저장 실패: {e}")

    def _load_cookies(self, naver_id):
        """쿠키 로드"""
        try:
            cookie_file = self._get_cookie_file(naver_id)
            if cookie_file.exists():
                with open(cookie_file, 'rb') as f:
                    cookies = pickle.load(f)
                return cookies
        except Exception as e:
            print(f"[경고] 쿠키 로드 실패: {e}")
        return None

    def _try_cookie_login(self, naver_id):
        """쿠키를 사용한 로그인 시도"""
        cookies = self._load_cookies(naver_id)
        if not cookies:
            return False

        try:
            # 먼저 네이버 도메인 접속 (쿠키 설정을 위해)
            self.driver.get("https://www.naver.com")
            time.sleep(1)

            # 쿠키 설정
            for cookie in cookies:
                try:
                    # domain 검증
                    if 'naver.com' in cookie.get('domain', ''):
                        self.driver.add_cookie(cookie)
                except Exception:
                    pass

            # 로그인 확인을 위해 페이지 새로고침
            self.driver.get("https://www.naver.com")
            time.sleep(2)

            # 로그인 상태 확인
            if self._check_login_status():
                print(f"[쿠키] {naver_id} 쿠키 로그인 성공")
                return True
            else:
                print(f"[쿠키] {naver_id} 쿠키 만료됨")
                return False

        except Exception as e:
            print(f"[쿠키] 로그인 시도 실패: {e}")
            return False

    def _check_login_status(self):
        """로그인 상태 확인"""
        try:
            # 로그인 버튼이 있는지 확인
            login_btn = self.driver.find_elements(By.CSS_SELECTOR, ".MyView-module__btn_login___HpHMW, .link_login")
            if login_btn:
                return False

            # 내 정보 영역이 있는지 확인
            my_area = self.driver.find_elements(By.CSS_SELECTOR, ".MyView-module__my_area___piNJP, .area_my")
            if my_area:
                return True

            return False
        except:
            return False

    def login(self, naver_id, naver_pw):
        """네이버 로그인 (쿠키 우선)"""
        if not self.driver:
            self.init_driver()

        self.current_account = naver_id

        # 1. 쿠키 로그인 시도
        print(f"[로그인] {naver_id} 쿠키 로그인 시도...")
        if self._try_cookie_login(naver_id):
            self.logged_in = True
            return True

        # 2. 일반 로그인
        print(f"[로그인] {naver_id} 일반 로그인 시도...")

        try:
            self.driver.get(self.LOGIN_URL)
            time.sleep(2)

            # 아이디 입력
            id_input = WebDriverWait(self.driver, 10).until(
                EC.presence_of_element_located((By.ID, "id"))
            )
            self._paste_text(id_input, naver_id)
            time.sleep(0.5)

            # 비밀번호 입력
            pw_input = self.driver.find_element(By.ID, "pw")
            self._paste_text(pw_input, naver_pw)
            time.sleep(0.5)

            # 로그인 상태 유지 체크
            try:
                keep_login = self.driver.find_element(By.ID, "keep")
                if not keep_login.is_selected():
                    keep_login.click()
                    print("[로그인] 로그인 상태 유지 체크")
            except:
                pass

            # 로그인 버튼 클릭
            login_btn = self.driver.find_element(By.ID, "log.login")
            login_btn.click()

            time.sleep(3)

            # 추가 인증 처리
            self._handle_additional_auth()

            # 로그인 성공 확인
            if self._check_login_success():
                print(f"[성공] {naver_id} 로그인 완료")
                self.logged_in = True

                # 쿠키 저장
                self._save_cookies(naver_id)

                # IP 보안 설정 해제
                self._disable_ip_security()

                return True
            else:
                if self._check_captcha():
                    print("[경고] 캡차 또는 추가 인증이 필요합니다")
                    return False
                print("[실패] 로그인 실패 - 아이디/비밀번호를 확인하세요")
                return False

        except TimeoutException:
            print("[오류] 로그인 페이지 로딩 시간 초과")
            return False
        except Exception as e:
            print(f"[오류] 로그인 중 오류 발생: {e}")
            return False

    def _handle_additional_auth(self):
        """추가 인증 처리 (새 기기 등록 등)"""
        try:
            # 새 기기 등록 팝업 처리
            time.sleep(1)

            # "등록 안함" 또는 "나중에" 버튼 찾기
            skip_buttons = [
                "a.btn_cancel",
                "button.btn_type2",
                "[data-action='cancel']",
            ]

            for selector in skip_buttons:
                try:
                    btn = self.driver.find_element(By.CSS_SELECTOR, selector)
                    if btn.is_displayed():
                        btn.click()
                        time.sleep(1)
                        break
                except:
                    pass

        except Exception:
            pass

    def _disable_ip_security(self):
        """IP 보안 설정 해제 (자동화를 위해)"""
        try:
            # 보안 설정 페이지 이동
            self.driver.get("https://nid.naver.com/user2/help/myInfo?m=viewOverseasSecuritySetting")
            time.sleep(2)

            # IP 보안 해제 옵션 찾기
            security_off = self.driver.find_elements(By.CSS_SELECTOR, "input[value='off'], #off_btn")
            for elem in security_off:
                try:
                    if not elem.is_selected():
                        elem.click()
                        time.sleep(1)
                        print("[보안] IP 보안 해제됨")
                except:
                    pass

        except Exception:
            pass  # 보안 설정 페이지 접근 실패는 무시

    def _paste_text(self, element, text):
        """클립보드를 이용한 텍스트 입력"""
        try:
            if HAS_PYPERCLIP:
                pyperclip.copy(text)
                element.click()
                time.sleep(0.2)
                element.send_keys(Keys.CONTROL, 'v')
            else:
                raise Exception("pyperclip not available")
        except:
            # JavaScript로 입력
            element.click()
            time.sleep(0.2)
            self.driver.execute_script(
                "arguments[0].value = arguments[1];",
                element, text
            )
            self.driver.execute_script(
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                element
            )

    def _check_login_success(self):
        """로그인 성공 여부 확인"""
        try:
            current_url = self.driver.current_url

            if "nid.naver.com" not in current_url:
                return True

            try:
                self.driver.find_element(By.ID, "id")
                return False
            except NoSuchElementException:
                return True

        except:
            return False

    def _check_captcha(self):
        """캡차/추가인증 확인"""
        try:
            captcha = self.driver.find_element(By.ID, "captcha")
            return True
        except NoSuchElementException:
            pass

        try:
            otp = self.driver.find_element(By.ID, "otp")
            return True
        except NoSuchElementException:
            pass

        return False

    def go_to_blog(self):
        """블로그 페이지로 이동"""
        if not self.logged_in:
            print("[오류] 먼저 로그인이 필요합니다")
            return False

        try:
            self.driver.get(self.BLOG_URL)
            time.sleep(2)
            return True
        except Exception as e:
            print(f"[오류] 블로그 이동 실패: {e}")
            return False

    def close(self):
        """드라이버 종료"""
        if self.driver:
            try:
                self.driver.quit()
            except:
                pass
            self.driver = None
            self.logged_in = False


if __name__ == "__main__":
    # 테스트
    print("네이버 로그인 테스트")
    naver = NaverLogin(headless=False)

    test_id = input("네이버 아이디: ")
    test_pw = input("네이버 비밀번호: ")

    if naver.login(test_id, test_pw):
        print("로그인 성공!")
        naver.go_to_blog()
        input("Enter를 누르면 종료...")
    else:
        print("로그인 실패!")

    naver.close()
