"""
네이버 블로그 포스팅 모듈 (개선된 버전)
Selenium을 사용하여 블로그에 글을 작성합니다.
스마트에디터 One 대응
"""
import time
import re
import os
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException, StaleElementReferenceException
from selenium.webdriver.common.action_chains import ActionChains

class BlogPoster:
    """네이버 블로그 포스터 (스마트에디터 One 대응)"""

    WRITE_URL = "https://blog.naver.com/{blog_id}/postwrite"
    EDITOR_URL = "https://blog.naver.com/{blog_id}?Redirect=Write"

    def __init__(self, driver):
        self.driver = driver
        self.blog_id = None

    def get_blog_id(self):
        """현재 로그인된 계정의 블로그 ID 가져오기"""
        try:
            self.driver.get("https://blog.naver.com/MyBlog.naver")
            time.sleep(2)

            current_url = self.driver.current_url
            match = re.search(r'blog\.naver\.com/([^/?]+)', current_url)
            if match:
                self.blog_id = match.group(1)
                print(f"[블로그] 블로그 ID: {self.blog_id}")
                return self.blog_id

        except Exception as e:
            print(f"[오류] 블로그 ID 가져오기 실패: {e}")

        return None

    def write_post(self, title, body, tags=None, category=None, images=None):
        """블로그 글 작성"""
        if not self.blog_id:
            self.get_blog_id()

        if not self.blog_id:
            print("[오류] 블로그 ID를 찾을 수 없습니다")
            return None

        print(f"[블로그] 글 작성 시작 - {title[:30]}...")

        try:
            # 글쓰기 페이지로 이동
            write_url = self.EDITOR_URL.format(blog_id=self.blog_id)
            self.driver.get(write_url)
            time.sleep(4)

            # 스마트 에디터 로딩 대기
            if not self._wait_for_smart_editor():
                print("[오류] 에디터 로딩 실패")
                return None

            # 제목 입력
            if not self._input_title_smart_editor(title):
                print("[오류] 제목 입력 실패")
                return None
            time.sleep(0.5)

            # 이미지 업로드 (본문 전에)
            if images:
                for img_path in images:
                    if img_path and os.path.exists(img_path):
                        self._upload_image_smart_editor(img_path)
                        time.sleep(1)

            # 본문 입력
            if not self._input_body_smart_editor(body):
                print("[오류] 본문 입력 실패")
                return None
            time.sleep(0.5)

            # 태그 입력
            if tags:
                self._input_tags_smart_editor(tags)

            time.sleep(1)

            # 발행
            post_url = self._publish_smart_editor()

            if post_url:
                print(f"[성공] 글 발행 완료: {post_url}")
                return post_url
            else:
                print("[경고] 발행 실패")
                return None

        except Exception as e:
            print(f"[오류] 글 작성 실패: {e}")
            import traceback
            traceback.print_exc()
            return None

    def _wait_for_smart_editor(self):
        """스마트 에디터 로딩 대기"""
        try:
            # 스마트에디터 One의 제목 영역 대기
            WebDriverWait(self.driver, 20).until(
                EC.presence_of_element_located((By.CSS_SELECTOR,
                    ".se-documentTitle, .se-component.se-documentTitle, #post-area"))
            )
            time.sleep(2)
            print("[블로그] 에디터 로딩 완료")
            return True
        except TimeoutException:
            # iframe 내부인지 확인
            try:
                iframes = self.driver.find_elements(By.TAG_NAME, "iframe")
                for iframe in iframes:
                    try:
                        self.driver.switch_to.frame(iframe)
                        if self.driver.find_elements(By.CSS_SELECTOR, ".se-documentTitle"):
                            print("[블로그] iframe 내에서 에디터 발견")
                            return True
                        self.driver.switch_to.default_content()
                    except:
                        self.driver.switch_to.default_content()
            except:
                pass
            print("[경고] 에디터 로딩 시간 초과")
            return False

    def _input_title_smart_editor(self, title):
        """스마트 에디터 제목 입력"""
        try:
            # 제목 입력 영역 찾기 (스마트에디터 One)
            title_selectors = [
                ".se-documentTitle .se-text-paragraph",
                ".se-documentTitle span[data-placeholder]",
                ".se-component.se-documentTitle .se-text-paragraph span",
                "span.se-ff-nanumgothic.se-fs32",
                ".se-module-title .se-text-paragraph",
            ]

            title_elem = None
            for selector in title_selectors:
                try:
                    elements = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    for elem in elements:
                        if elem.is_displayed():
                            title_elem = elem
                            break
                    if title_elem:
                        break
                except:
                    continue

            if title_elem:
                # 클릭하여 포커스
                ActionChains(self.driver).move_to_element(title_elem).click().perform()
                time.sleep(0.3)

                # JavaScript로 텍스트 입력
                self.driver.execute_script("""
                    var elem = arguments[0];
                    elem.textContent = arguments[1];
                    elem.dispatchEvent(new Event('input', { bubbles: true }));
                    elem.dispatchEvent(new Event('change', { bubbles: true }));
                """, title_elem, title)

                print(f"[블로그] 제목 입력 완료: {title[:30]}...")
                return True

            # 대체 방법: contenteditable 영역 찾기
            editables = self.driver.find_elements(By.CSS_SELECTOR, "[contenteditable='true']")
            for elem in editables:
                if elem.is_displayed():
                    # 첫 번째 editable이 제목
                    ActionChains(self.driver).move_to_element(elem).click().perform()
                    time.sleep(0.2)
                    elem.send_keys(title)
                    print(f"[블로그] 제목 입력 완료 (대체): {title[:30]}...")
                    return True

            print("[경고] 제목 입력 영역을 찾을 수 없습니다")
            return False

        except Exception as e:
            print(f"[오류] 제목 입력 중 오류: {e}")
            return False

    def _input_body_smart_editor(self, body):
        """스마트 에디터 본문 입력"""
        try:
            # 본문 영역 찾기
            body_selectors = [
                ".se-component.se-text .se-text-paragraph",
                ".se-section-text .se-text-paragraph",
                ".se-component:not(.se-documentTitle) .se-text-paragraph span",
                ".se-main-container .se-text-paragraph",
            ]

            body_elem = None

            # 먼저 본문 영역 클릭 (제목 다음으로 이동)
            time.sleep(0.5)
            ActionChains(self.driver).send_keys(Keys.TAB).perform()
            time.sleep(0.3)

            for selector in body_selectors:
                try:
                    elements = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    for elem in elements:
                        if elem.is_displayed():
                            # 제목 영역이 아닌지 확인
                            parent = elem.find_element(By.XPATH, "./ancestor::*[contains(@class, 'se-component')]")
                            if 'se-documentTitle' not in parent.get_attribute('class'):
                                body_elem = elem
                                break
                    if body_elem:
                        break
                except:
                    continue

            if body_elem:
                ActionChains(self.driver).move_to_element(body_elem).click().perform()
                time.sleep(0.3)

            # 본문 입력 - 줄바꿈 처리
            paragraphs = body.split('\n')
            for i, para in enumerate(paragraphs):
                para = para.strip()
                if para:
                    # JavaScript로 현재 위치에 텍스트 입력
                    self.driver.execute_script("""
                        document.execCommand('insertText', false, arguments[0]);
                    """, para)
                if i < len(paragraphs) - 1:
                    ActionChains(self.driver).send_keys(Keys.ENTER).perform()
                time.sleep(0.05)

            print(f"[블로그] 본문 입력 완료 ({len(body)}자)")
            return True

        except Exception as e:
            print(f"[오류] 본문 입력 중 오류: {e}")
            # 대체 방법
            try:
                ActionChains(self.driver).send_keys(body).perform()
                print(f"[블로그] 본문 입력 완료 (대체방법)")
                return True
            except:
                return False

    def _upload_image_smart_editor(self, image_path):
        """스마트 에디터 이미지 업로드"""
        try:
            print(f"[블로그] 이미지 업로드 중: {image_path}")

            # 이미지 버튼 찾기
            image_btn_selectors = [
                "button.se-image-toolbar-button",
                "button[data-name='image']",
                ".se-toolbar-item-image button",
                ".se-toolbar button[title*='사진']",
                ".se-toolbar button[title*='이미지']",
            ]

            image_btn = None
            for selector in image_btn_selectors:
                try:
                    image_btn = self.driver.find_element(By.CSS_SELECTOR, selector)
                    if image_btn and image_btn.is_displayed():
                        break
                except:
                    continue

            # 아이콘으로 찾기
            if not image_btn:
                buttons = self.driver.find_elements(By.CSS_SELECTOR, ".se-toolbar button")
                for btn in buttons:
                    try:
                        # SVG 아이콘이나 클래스로 식별
                        if "image" in btn.get_attribute("class").lower():
                            image_btn = btn
                            break
                    except:
                        continue

            if image_btn:
                image_btn.click()
                time.sleep(1)

            # 파일 input 찾기
            file_input = None
            file_input_selectors = [
                "input[type='file']",
                "input[accept*='image']",
            ]

            for selector in file_input_selectors:
                try:
                    inputs = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    for inp in inputs:
                        file_input = inp
                        break
                    if file_input:
                        break
                except:
                    continue

            if file_input:
                # 파일 업로드
                file_input.send_keys(image_path)
                print("[블로그] 이미지 파일 전송 완료")
                time.sleep(3)  # 업로드 대기
                return True
            else:
                print("[경고] 파일 입력란을 찾을 수 없습니다")
                return False

        except Exception as e:
            print(f"[경고] 이미지 업로드 중 오류: {e}")
            return False

    def _input_tags_smart_editor(self, tags):
        """스마트 에디터 태그 입력"""
        try:
            # 태그 입력란 찾기
            tag_selectors = [
                "input.se-tag-input__input",
                ".se-tag-input input",
                "input[placeholder*='태그']",
                ".tag_input input",
            ]

            tag_elem = None
            for selector in tag_selectors:
                try:
                    tag_elem = self.driver.find_element(By.CSS_SELECTOR, selector)
                    if tag_elem and tag_elem.is_displayed():
                        break
                except NoSuchElementException:
                    continue

            if tag_elem:
                # 태그 정리
                clean_tags = tags.replace('#', '').strip()
                tag_list = [t.strip() for t in clean_tags.split() if t.strip()]

                for tag in tag_list[:10]:
                    tag_elem.send_keys(tag)
                    tag_elem.send_keys(Keys.ENTER)
                    time.sleep(0.2)

                print(f"[블로그] 태그 입력 완료 ({len(tag_list)}개)")
            else:
                print("[경고] 태그 입력란을 찾을 수 없습니다")

        except Exception as e:
            print(f"[경고] 태그 입력 중 오류: {e}")

    def _publish_smart_editor(self):
        """스마트 에디터 발행"""
        try:
            # 발행 버튼 찾기
            publish_selectors = [
                "button.publish_btn__Y5Sxd",
                "button[class*='publish']",
                ".publish_btn",
                "button.se-publish-btn",
                "#publish-btn",
            ]

            publish_btn = None
            for selector in publish_selectors:
                try:
                    publish_btn = self.driver.find_element(By.CSS_SELECTOR, selector)
                    if publish_btn and publish_btn.is_displayed():
                        break
                except NoSuchElementException:
                    continue

            # 텍스트로 버튼 찾기
            if not publish_btn:
                buttons = self.driver.find_elements(By.TAG_NAME, "button")
                for btn in buttons:
                    try:
                        if "발행" in btn.text:
                            publish_btn = btn
                            break
                    except:
                        continue

            if publish_btn:
                print("[블로그] 발행 버튼 클릭...")
                publish_btn.click()
                time.sleep(3)

                # 추가 확인 팝업 처리
                try:
                    confirm_selectors = [
                        ".confirm_btn",
                        "button.confirm",
                        ".se-popup-button-confirm",
                        "button[class*='confirm']",
                    ]
                    for selector in confirm_selectors:
                        try:
                            confirm_btn = self.driver.find_element(By.CSS_SELECTOR, selector)
                            if confirm_btn.is_displayed():
                                confirm_btn.click()
                                time.sleep(2)
                                break
                        except:
                            continue
                except:
                    pass

                # 발행 후 URL 가져오기
                time.sleep(3)
                current_url = self.driver.current_url

                if "postwrite" not in current_url.lower() and self.blog_id in current_url:
                    return current_url

                # URL에서 포스트 번호 추출 시도
                match = re.search(r'/(\d+)$', current_url)
                if match:
                    return f"https://blog.naver.com/{self.blog_id}/{match.group(1)}"

                return "발행완료"

            else:
                print("[경고] 발행 버튼을 찾을 수 없습니다")
                return None

        except Exception as e:
            print(f"[오류] 발행 중 오류: {e}")
            return None


if __name__ == "__main__":
    print("블로그 포스터 테스트")
    print("이 모듈은 직접 실행할 수 없습니다.")
    print("client.py를 통해 실행하세요.")
