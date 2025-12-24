"""
네이버 블로그 포스팅 모듈 (완전 재작성)
스마트에디터 One 대응 - 2024년 버전
"""
import time
import re
import os
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from selenium.webdriver.common.action_chains import ActionChains


class BlogPoster:
    """네이버 블로그 포스터 (스마트에디터 One)"""

    def __init__(self, driver):
        self.driver = driver
        self.blog_id = None
        self.in_iframe = False

    def get_blog_id(self):
        """블로그 ID 가져오기"""
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
        """블로그 글 작성 - 메인 함수"""
        if not self.blog_id:
            self.get_blog_id()

        if not self.blog_id:
            print("[오류] 블로그 ID를 찾을 수 없습니다")
            return None

        print(f"[블로그] 글 작성 시작 - {title[:30]}...")

        try:
            # 1. 글쓰기 페이지로 이동
            write_url = f"https://blog.naver.com/{self.blog_id}?Redirect=Write"
            self.driver.get(write_url)
            time.sleep(3)

            # 2. 에디터 프레임 찾기 및 진입
            if not self._enter_editor_frame():
                print("[오류] 에디터 프레임 진입 실패")
                return None

            # 3. 도움말/팝업 닫기
            self._close_help_popups()
            time.sleep(0.5)

            # 4. 제목 입력
            if not self._write_title(title):
                print("[오류] 제목 입력 실패")
                return None

            # 5. 본문 영역으로 이동
            time.sleep(0.5)

            # 6. 이미지 업로드 (있으면)
            if images:
                for img_path in images:
                    if img_path and os.path.exists(img_path):
                        self._upload_image(img_path)

            # 7. 본문 입력
            if not self._write_body(body):
                print("[오류] 본문 입력 실패")
                return None

            # 8. 태그 입력
            if tags:
                self._write_tags(tags)

            time.sleep(1)

            # 9. 프레임 나가기 (발행 버튼은 메인 페이지에 있음)
            self._exit_frame()

            # 10. 도움말 다시 닫기
            self._close_help_popups()

            # 11. 발행
            post_url = self._publish()

            if post_url:
                print(f"[성공] 글 발행 완료: {post_url}")
                return post_url
            else:
                print("[실패] 발행 실패")
                return None

        except Exception as e:
            print(f"[오류] 글 작성 실패: {e}")
            import traceback
            traceback.print_exc()
            return None

    def _enter_editor_frame(self):
        """에디터 iframe 찾아서 진입"""
        try:
            # 먼저 메인 컨텐츠에서 에디터 찾기
            self.driver.switch_to.default_content()
            time.sleep(1)

            # mainFrame 찾기
            try:
                main_frame = WebDriverWait(self.driver, 10).until(
                    EC.presence_of_element_located((By.ID, "mainFrame"))
                )
                self.driver.switch_to.frame(main_frame)
                print("[블로그] mainFrame 진입")
                time.sleep(1)
            except:
                print("[블로그] mainFrame 없음, 직접 찾기 시도")

            # se-frame 또는 에디터 iframe 찾기
            iframes = self.driver.find_elements(By.TAG_NAME, "iframe")
            for iframe in iframes:
                try:
                    iframe_id = iframe.get_attribute("id") or ""
                    iframe_name = iframe.get_attribute("name") or ""
                    iframe_class = iframe.get_attribute("class") or ""

                    if any(x in iframe_id.lower() or x in iframe_name.lower() or x in iframe_class.lower()
                           for x in ["editor", "se-", "write"]):
                        self.driver.switch_to.frame(iframe)
                        print(f"[블로그] 에디터 iframe 진입: {iframe_id or iframe_name}")
                        self.in_iframe = True
                        time.sleep(1)
                        return True
                except:
                    continue

            # 에디터 영역 확인 (iframe 없이 직접)
            try:
                editor = self.driver.find_element(By.CSS_SELECTOR, ".se-component")
                if editor:
                    print("[블로그] 에디터 직접 발견 (iframe 없음)")
                    self.in_iframe = False
                    return True
            except:
                pass

            print("[블로그] 에디터를 찾을 수 없습니다")
            return False

        except Exception as e:
            print(f"[오류] 에디터 프레임 진입 실패: {e}")
            return False

    def _exit_frame(self):
        """iframe에서 나가기"""
        try:
            self.driver.switch_to.default_content()
            self.in_iframe = False
            time.sleep(0.5)
        except:
            pass

    def _close_help_popups(self):
        """도움말/튜토리얼 팝업 닫기"""
        try:
            # 여러 종류의 닫기 버튼 시도
            close_selectors = [
                ".se-help-panel-close-button",
                ".se-help-close",
                "button[class*='close']",
                ".se-popup-close",
                ".tooltip-close",
                "[class*='help'] [class*='close']",
                ".se-help-panel button",
                "button.close",
                ".modal-close",
            ]

            for selector in close_selectors:
                try:
                    buttons = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    for btn in buttons:
                        if btn.is_displayed():
                            btn.click()
                            print(f"[블로그] 팝업 닫기: {selector}")
                            time.sleep(0.3)
                except:
                    continue

            # ESC 키로 팝업 닫기 시도
            try:
                ActionChains(self.driver).send_keys(Keys.ESCAPE).perform()
                time.sleep(0.3)
            except:
                pass

            # 도움말 패널 숨기기 (JavaScript)
            try:
                self.driver.execute_script("""
                    var helpElements = document.querySelectorAll('[class*="help"], [class*="tooltip"], [class*="guide"]');
                    helpElements.forEach(function(el) {
                        el.style.display = 'none';
                    });
                """)
            except:
                pass

        except Exception as e:
            print(f"[블로그] 팝업 닫기 중 오류 (무시): {e}")

    def _write_title(self, title):
        """제목 입력"""
        try:
            # 제목 영역 찾기
            title_selectors = [
                ".se-documentTitle .se-text-paragraph",
                ".se-documentTitle",
                ".se-module-text.se-is-title .se-text-paragraph",
                "[class*='title'] [contenteditable='true']",
                ".se-placeholder.__se_placeholder",
            ]

            title_elem = None
            for selector in title_selectors:
                try:
                    elems = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    for elem in elems:
                        if elem.is_displayed():
                            title_elem = elem
                            break
                    if title_elem:
                        break
                except:
                    continue

            if not title_elem:
                # contenteditable 찾기
                editables = self.driver.find_elements(By.CSS_SELECTOR, "[contenteditable='true']")
                if editables:
                    title_elem = editables[0]

            if title_elem:
                # 클릭하여 포커스
                try:
                    title_elem.click()
                except:
                    ActionChains(self.driver).move_to_element(title_elem).click().perform()

                time.sleep(0.3)

                # 기존 내용 지우고 입력
                ActionChains(self.driver).key_down(Keys.CONTROL).send_keys('a').key_up(Keys.CONTROL).perform()
                time.sleep(0.1)

                # 직접 타이핑
                for char in title:
                    ActionChains(self.driver).send_keys(char).perform()
                    time.sleep(0.02)

                print(f"[블로그] 제목 입력 완료: {title[:30]}...")
                return True

            print("[경고] 제목 영역을 찾을 수 없습니다")
            return False

        except Exception as e:
            print(f"[오류] 제목 입력 실패: {e}")
            return False

    def _write_body(self, body):
        """본문 입력"""
        try:
            # TAB으로 본문 영역으로 이동
            ActionChains(self.driver).send_keys(Keys.TAB).perform()
            time.sleep(0.5)

            # 본문 영역 클릭 시도
            body_selectors = [
                ".se-component.se-text:not(.se-documentTitle) .se-text-paragraph",
                ".se-section.se-section-text .se-text-paragraph",
                ".se-component.se-text .se-text-paragraph span",
            ]

            for selector in body_selectors:
                try:
                    elems = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    for elem in elems:
                        if elem.is_displayed():
                            try:
                                parent = elem.find_element(By.XPATH, "./ancestor::*[contains(@class, 'se-component')]")
                                if 'documentTitle' not in parent.get_attribute('class'):
                                    elem.click()
                                    time.sleep(0.3)
                                    break
                            except:
                                elem.click()
                                time.sleep(0.3)
                                break
                except:
                    continue

            # 본문 입력 - 한 글자씩
            lines = body.split('\n')
            for i, line in enumerate(lines):
                line = line.strip()
                if line:
                    for char in line:
                        ActionChains(self.driver).send_keys(char).perform()
                        time.sleep(0.01)

                if i < len(lines) - 1:
                    ActionChains(self.driver).send_keys(Keys.ENTER).perform()
                    time.sleep(0.05)

            print(f"[블로그] 본문 입력 완료 ({len(body)}자)")
            return True

        except Exception as e:
            print(f"[오류] 본문 입력 실패: {e}")
            # 대체: 직접 send_keys
            try:
                ActionChains(self.driver).send_keys(body).perform()
                print("[블로그] 본문 입력 완료 (대체방법)")
                return True
            except:
                return False

    def _upload_image(self, image_path):
        """이미지 업로드"""
        try:
            print(f"[블로그] 이미지 업로드: {image_path}")

            # 파일 input 찾기
            file_inputs = self.driver.find_elements(By.CSS_SELECTOR, "input[type='file']")
            if file_inputs:
                file_inputs[0].send_keys(image_path)
                print("[블로그] 이미지 업로드 완료")
                time.sleep(3)
                return True

            print("[경고] 파일 입력란을 찾을 수 없습니다")
            return False

        except Exception as e:
            print(f"[오류] 이미지 업로드 실패: {e}")
            return False

    def _write_tags(self, tags):
        """태그 입력"""
        try:
            # 프레임 나갔다가 태그 영역 찾기
            self._exit_frame()
            time.sleep(0.5)

            tag_selectors = [
                "input.se-tag-input__input",
                ".se-tag-input input",
                "input[placeholder*='태그']",
                "[class*='tag'] input",
            ]

            tag_elem = None
            for selector in tag_selectors:
                try:
                    tag_elem = self.driver.find_element(By.CSS_SELECTOR, selector)
                    if tag_elem and tag_elem.is_displayed():
                        break
                except:
                    continue

            if tag_elem:
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
            print(f"[경고] 태그 입력 실패: {e}")

    def _publish(self):
        """발행 버튼 클릭"""
        try:
            # 메인 프레임으로 이동
            self.driver.switch_to.default_content()
            time.sleep(0.5)

            # 도움말 다시 닫기
            self._close_help_popups()
            time.sleep(0.5)

            # 발행 버튼 찾기
            publish_btn = None
            publish_selectors = [
                "button.publish_btn__m9KHH",
                "button.publish_btn__Y5Sxd",
                "button[class*='publish_btn']",
                "button[class*='publish']",
            ]

            for selector in publish_selectors:
                try:
                    publish_btn = self.driver.find_element(By.CSS_SELECTOR, selector)
                    if publish_btn and publish_btn.is_displayed():
                        break
                except:
                    continue

            # 텍스트로 찾기
            if not publish_btn:
                buttons = self.driver.find_elements(By.TAG_NAME, "button")
                for btn in buttons:
                    try:
                        if "발행" in btn.text and btn.is_displayed():
                            publish_btn = btn
                            break
                    except:
                        continue

            if publish_btn:
                print("[블로그] 발행 버튼 클릭...")

                # 스크롤해서 버튼 보이게
                self.driver.execute_script("arguments[0].scrollIntoView(true);", publish_btn)
                time.sleep(0.3)

                # JavaScript로 클릭 (오버레이 우회)
                try:
                    self.driver.execute_script("arguments[0].click();", publish_btn)
                except:
                    publish_btn.click()

                time.sleep(3)

                # 발행 확인 팝업 처리
                self._handle_publish_confirm()

                time.sleep(3)

                # 결과 URL 가져오기
                current_url = self.driver.current_url
                if self.blog_id in current_url and "postwrite" not in current_url.lower():
                    return current_url

                return "발행완료"

            print("[경고] 발행 버튼을 찾을 수 없습니다")
            return None

        except Exception as e:
            print(f"[오류] 발행 실패: {e}")
            return None

    def _handle_publish_confirm(self):
        """발행 확인 팝업 처리"""
        try:
            confirm_selectors = [
                "button.confirm_btn__Dv9sp",
                "button[class*='confirm']",
                ".se-popup-button-confirm",
                "button.ok",
            ]

            for selector in confirm_selectors:
                try:
                    btn = self.driver.find_element(By.CSS_SELECTOR, selector)
                    if btn.is_displayed():
                        self.driver.execute_script("arguments[0].click();", btn)
                        print("[블로그] 발행 확인")
                        time.sleep(1)
                        return
                except:
                    continue

            # 텍스트로 찾기
            buttons = self.driver.find_elements(By.TAG_NAME, "button")
            for btn in buttons:
                try:
                    if btn.is_displayed() and btn.text in ["확인", "발행", "등록"]:
                        self.driver.execute_script("arguments[0].click();", btn)
                        print("[블로그] 발행 확인 (텍스트)")
                        time.sleep(1)
                        return
                except:
                    continue

        except Exception as e:
            print(f"[경고] 발행 확인 처리 중 오류: {e}")


if __name__ == "__main__":
    print("블로그 포스터 모듈")
    print("client.py를 통해 실행하세요.")
