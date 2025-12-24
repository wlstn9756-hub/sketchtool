"""
네이버 블로그 포스팅 모듈
Selenium을 사용하여 블로그에 글을 작성합니다.
"""
import time
import re
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import TimeoutException, NoSuchElementException
from selenium.webdriver.common.action_chains import ActionChains

class BlogPoster:
    """네이버 블로그 포스터"""

    WRITE_URL = "https://blog.naver.com/{blog_id}/postwrite"
    SMART_EDITOR_URL = "https://blog.naver.com/{blog_id}?Redirect=Write"

    def __init__(self, driver):
        self.driver = driver
        self.blog_id = None

    def get_blog_id(self):
        """현재 로그인된 계정의 블로그 ID 가져오기"""
        try:
            self.driver.get("https://blog.naver.com/MyBlog.naver")
            time.sleep(2)

            current_url = self.driver.current_url
            # URL에서 블로그 ID 추출
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
            write_url = self.SMART_EDITOR_URL.format(blog_id=self.blog_id)
            self.driver.get(write_url)
            time.sleep(3)

            # 스마트 에디터 로딩 대기
            self._wait_for_editor()

            # 제목 입력
            self._input_title(title)
            time.sleep(0.5)

            # 본문 입력
            self._input_body(body)
            time.sleep(0.5)

            # 이미지 삽입 (옵션)
            if images:
                self._insert_images(images)

            # 태그 입력
            if tags:
                self._input_tags(tags)

            time.sleep(1)

            # 발행
            post_url = self._publish_post()

            if post_url:
                print(f"[성공] 글 발행 완료: {post_url}")
                return post_url
            else:
                print("[경고] 글이 발행되었지만 URL을 가져오지 못했습니다")
                return "발행완료"

        except Exception as e:
            print(f"[오류] 글 작성 실패: {e}")
            import traceback
            traceback.print_exc()
            return None

    def _wait_for_editor(self):
        """에디터 로딩 대기"""
        try:
            # 새 스마트 에디터 (SE3)
            WebDriverWait(self.driver, 15).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, ".se-component-content, .se_editArea, #post-editor"))
            )
            time.sleep(1)
        except TimeoutException:
            print("[경고] 에디터 로딩 시간 초과, 계속 진행...")

    def _input_title(self, title):
        """제목 입력"""
        try:
            # 새 에디터의 제목 입력란
            title_selectors = [
                "textarea.se-ff-nanumgothic",  # SE 에디터
                ".se-title-text",
                "input.se-title-input",
                "#subject",  # 구 에디터
                ".title textarea",
            ]

            title_elem = None
            for selector in title_selectors:
                try:
                    title_elem = self.driver.find_element(By.CSS_SELECTOR, selector)
                    if title_elem:
                        break
                except NoSuchElementException:
                    continue

            if title_elem:
                title_elem.clear()
                title_elem.send_keys(title)
                print(f"[블로그] 제목 입력 완료")
            else:
                # 직접 contenteditable 영역 찾기
                self._input_to_contenteditable("제목", title)

        except Exception as e:
            print(f"[경고] 제목 입력 중 오류: {e}")

    def _input_body(self, body):
        """본문 입력"""
        try:
            # 본문 영역 찾기
            body_selectors = [
                ".se-component-content .se-text-paragraph",
                ".se-section-text .se-text-paragraph",
                "#post-area",
                ".se_editArea",
                "[contenteditable='true']"
            ]

            body_elem = None
            for selector in body_selectors:
                try:
                    elems = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    for elem in elems:
                        if elem.is_displayed():
                            body_elem = elem
                            break
                    if body_elem:
                        break
                except NoSuchElementException:
                    continue

            if body_elem:
                # 본문 클릭하여 포커스
                ActionChains(self.driver).move_to_element(body_elem).click().perform()
                time.sleep(0.3)

                # 줄바꿈 처리하여 입력
                paragraphs = body.split('\n')
                for i, para in enumerate(paragraphs):
                    if para.strip():
                        body_elem.send_keys(para)
                    if i < len(paragraphs) - 1:
                        body_elem.send_keys(Keys.ENTER)
                    time.sleep(0.1)

                print(f"[블로그] 본문 입력 완료 ({len(body)}자)")
            else:
                print("[경고] 본문 입력 영역을 찾을 수 없습니다")

        except Exception as e:
            print(f"[경고] 본문 입력 중 오류: {e}")

    def _input_to_contenteditable(self, field_name, text):
        """contenteditable 영역에 입력"""
        try:
            editables = self.driver.find_elements(By.CSS_SELECTOR, "[contenteditable='true']")
            for elem in editables:
                if elem.is_displayed():
                    elem.click()
                    elem.send_keys(text)
                    print(f"[블로그] {field_name} 입력 완료 (contenteditable)")
                    return True
        except Exception as e:
            print(f"[경고] contenteditable 입력 실패: {e}")
        return False

    def _insert_images(self, images):
        """이미지 삽입"""
        try:
            # 이미지 버튼 찾기
            img_btn_selectors = [
                ".se-toolbar-item-image",
                "button[data-name='image']",
                ".se-image-toolbar-button"
            ]

            for url in images[:5]:  # 최대 5개
                print(f"[블로그] 이미지 삽입: {url[:50]}...")
                # 이미지 URL로 삽입하는 로직 (추후 구현 필요)

        except Exception as e:
            print(f"[경고] 이미지 삽입 중 오류: {e}")

    def _input_tags(self, tags):
        """태그 입력"""
        try:
            # 태그 입력란 찾기
            tag_selectors = [
                ".se-tag-input input",
                "#tag",
                "input[placeholder*='태그']",
                ".tag_input input"
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
                # 태그 정리 (# 제거하고 콤마로 구분)
                clean_tags = tags.replace('#', '').strip()
                tag_list = [t.strip() for t in clean_tags.split() if t.strip()]

                for tag in tag_list[:10]:  # 최대 10개
                    tag_elem.send_keys(tag)
                    tag_elem.send_keys(Keys.ENTER)
                    time.sleep(0.2)

                print(f"[블로그] 태그 입력 완료 ({len(tag_list)}개)")

        except Exception as e:
            print(f"[경고] 태그 입력 중 오류: {e}")

    def _publish_post(self):
        """글 발행"""
        try:
            # 발행 버튼 찾기
            publish_selectors = [
                "button.se-publish-button",
                ".publish_btn",
                "#publish-btn",
                "button[data-name='publish']",
                ".se-toolbar-item-publish",
                "button.btn_publish"
            ]

            publish_btn = None
            for selector in publish_selectors:
                try:
                    publish_btn = self.driver.find_element(By.CSS_SELECTOR, selector)
                    if publish_btn and publish_btn.is_displayed():
                        break
                except NoSuchElementException:
                    continue

            # 버튼 텍스트로 찾기
            if not publish_btn:
                buttons = self.driver.find_elements(By.TAG_NAME, "button")
                for btn in buttons:
                    if "발행" in btn.text or "등록" in btn.text:
                        publish_btn = btn
                        break

            if publish_btn:
                publish_btn.click()
                time.sleep(2)

                # 추가 확인 팝업 처리
                try:
                    confirm_btn = self.driver.find_element(By.CSS_SELECTOR, ".se-popup-button-confirm, .btn_ok")
                    confirm_btn.click()
                    time.sleep(2)
                except:
                    pass

                # 발행 후 URL 가져오기
                time.sleep(3)
                current_url = self.driver.current_url

                if "postwrite" not in current_url and self.blog_id in current_url:
                    return current_url

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
