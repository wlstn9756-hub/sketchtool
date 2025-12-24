"""
네이버 로그인 모듈
Selenium을 사용하여 네이버에 로그인합니다.
"""
import time
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
    """네이버 로그인 클래스"""

    LOGIN_URL = "https://nid.naver.com/nidlogin.login"
    BLOG_URL = "https://blog.naver.com"

    def __init__(self, headless=False, proxy=None):
        self.driver = None
        self.headless = headless
        self.proxy = proxy
        self.logged_in = False

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

        # User-Agent 설정
        options.add_argument("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

        # 프록시 설정
        if self.proxy:
            options.add_argument(f"--proxy-server={self.proxy}")

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
            """
        })

        return self.driver

    def login(self, naver_id, naver_pw):
        """네이버 로그인"""
        if not self.driver:
            self.init_driver()

        print(f"[로그인] {naver_id} 계정으로 로그인 시도...")

        try:
            self.driver.get(self.LOGIN_URL)
            time.sleep(2)

            # 아이디 입력 (클립보드 방식 - 봇 감지 우회)
            id_input = WebDriverWait(self.driver, 10).until(
                EC.presence_of_element_located((By.ID, "id"))
            )

            # 클립보드를 이용한 입력 (키 입력 감지 우회)
            self._paste_text(id_input, naver_id)
            time.sleep(0.5)

            # 비밀번호 입력
            pw_input = self.driver.find_element(By.ID, "pw")
            self._paste_text(pw_input, naver_pw)
            time.sleep(0.5)

            # 로그인 버튼 클릭
            login_btn = self.driver.find_element(By.ID, "log.login")
            login_btn.click()

            time.sleep(3)

            # 로그인 성공 확인
            if self._check_login_success():
                print(f"[성공] {naver_id} 로그인 완료")
                self.logged_in = True
                return True
            else:
                # 캡차 또는 추가 인증 확인
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

    def _paste_text(self, element, text):
        """클립보드를 이용한 텍스트 입력"""
        try:
            if HAS_PYPERCLIP:
                # pyperclip으로 클립보드에 복사
                pyperclip.copy(text)
                element.click()
                time.sleep(0.2)
                # Ctrl+V로 붙여넣기
                element.send_keys(Keys.CONTROL, 'v')
            else:
                raise Exception("pyperclip not available")
        except:
            # 클립보드 실패 시 JavaScript로 입력
            element.click()
            time.sleep(0.2)
            self.driver.execute_script(
                "arguments[0].value = arguments[1];",
                element, text
            )
            # 입력 이벤트 발생시키기
            self.driver.execute_script(
                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                element
            )

    def _check_login_success(self):
        """로그인 성공 여부 확인"""
        try:
            # 로그인 후 URL 확인
            current_url = self.driver.current_url

            # 메인 페이지로 이동했는지 확인
            if "nid.naver.com" not in current_url:
                return True

            # 로그인 폼이 여전히 있는지 확인
            try:
                self.driver.find_element(By.ID, "id")
                return False  # 로그인 폼이 있으면 실패
            except NoSuchElementException:
                return True

        except:
            return False

    def _check_captcha(self):
        """캡차/추가인증 확인"""
        try:
            # 캡차 이미지 확인
            captcha = self.driver.find_element(By.ID, "captcha")
            return True
        except NoSuchElementException:
            pass

        try:
            # 2단계 인증 확인
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
