"""
GPT 콘텐츠 생성기
OpenAI API를 사용하여 블로그 글을 생성합니다.
"""
import re
from openai import OpenAI

class GPTGenerator:
    """GPT 기반 블로그 콘텐츠 생성기"""

    DEFAULT_PROMPT = """당신은 한국 블로그 글을 작성하는 전문 작가입니다.
다음 조건에 맞게 블로그 글을 작성해주세요:

- 업체명: {place_name}
- 주소: {address}
- 카테고리: {category}
- 대표 키워드: {keyword}

작성 규칙:
1. 제목에 반드시 "{keyword}" 키워드를 포함하세요
2. 자연스럽고 친근한 말투로 작성하세요
3. 실제 방문한 것처럼 생생하게 묘사하세요
4. 장점을 부각하되 과장하지 마세요
5. 글자 수는 800~1200자 정도로 작성하세요
6. {forbidden_words}

응답 형식:
[제목]
(제목 내용)

[본문]
(본문 내용)

[태그]
(해시태그들, 공백으로 구분)
"""

    def __init__(self, api_key):
        self.client = OpenAI(api_key=api_key)
        self.model = "gpt-4o-mini"  # 비용 효율적인 모델

    def generate_blog_content(self, place_name, address, category, keyword,
                               forbidden_words=None, custom_prompt=None, bottom_tags=None):
        """블로그 글 생성"""
        # 금지어 처리
        forbidden_text = ""
        if forbidden_words:
            words = [w.strip() for w in forbidden_words.split(",") if w.strip()]
            if words:
                forbidden_text = f"다음 단어는 절대 사용하지 마세요: {', '.join(words)}"

        # 프롬프트 준비
        prompt = custom_prompt if custom_prompt else self.DEFAULT_PROMPT

        # 변수 치환
        prompt = prompt.format(
            place_name=place_name or "업체",
            address=address or "주소 미상",
            category=category or "일반",
            keyword=keyword or place_name,
            forbidden_words=forbidden_text
        )

        try:
            print(f"[GPT] 콘텐츠 생성 중... (키워드: {keyword})")

            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "당신은 한국 블로그 글을 작성하는 전문 작가입니다."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.8,
                max_tokens=2000
            )

            content = response.choices[0].message.content
            result = self._parse_response(content, keyword, bottom_tags)

            print(f"[GPT] 콘텐츠 생성 완료 - 제목: {result['title'][:30]}...")
            return result

        except Exception as e:
            print(f"[오류] GPT 생성 실패: {e}")
            return None

    def _parse_response(self, content, keyword, bottom_tags=None):
        """GPT 응답 파싱"""
        result = {
            "title": "",
            "body": "",
            "tags": ""
        }

        # 제목 추출
        title_match = re.search(r'\[제목\]\s*\n(.+?)(?=\n\[|$)', content, re.DOTALL)
        if title_match:
            result["title"] = title_match.group(1).strip()
        else:
            # 첫 줄을 제목으로
            lines = content.strip().split('\n')
            result["title"] = lines[0].strip()

        # 제목에 키워드가 없으면 추가
        if keyword and keyword not in result["title"]:
            result["title"] = f"{keyword} - {result['title']}"

        # 본문 추출
        body_match = re.search(r'\[본문\]\s*\n(.+?)(?=\n\[태그\]|$)', content, re.DOTALL)
        if body_match:
            result["body"] = body_match.group(1).strip()
        else:
            # 제목 이후 내용을 본문으로
            lines = content.strip().split('\n')
            result["body"] = '\n'.join(lines[1:]).strip()

        # 태그 추출
        tags_match = re.search(r'\[태그\]\s*\n(.+?)$', content, re.DOTALL)
        if tags_match:
            result["tags"] = tags_match.group(1).strip()

        # 하단 태그 추가
        if bottom_tags:
            if result["tags"]:
                result["tags"] += " " + bottom_tags
            else:
                result["tags"] = bottom_tags

        return result

    def generate_simple_content(self, keyword, place_name):
        """간단한 콘텐츠 생성 (API 키 없을 때 폴백)"""
        title = f"{keyword} 추천 - {place_name} 방문 후기"
        body = f"""안녕하세요! 오늘은 {place_name}에 다녀왔어요.

{keyword}로 유명한 이곳은 정말 기대 이상이었습니다.

위치도 좋고, 분위기도 좋아서 다음에 또 방문하고 싶네요.

{place_name} 추천드립니다!
"""
        tags = f"#{keyword} #{place_name.replace(' ', '')}"

        return {
            "title": title,
            "body": body,
            "tags": tags
        }


if __name__ == "__main__":
    # 테스트
    api_key = input("OpenAI API Key: ")
    generator = GPTGenerator(api_key)

    result = generator.generate_blog_content(
        place_name="카페 테스트",
        address="서울시 강남구",
        category="카페",
        keyword="강남카페",
        forbidden_words="광고, 협찬"
    )

    if result:
        print("\n=== 생성된 콘텐츠 ===")
        print(f"제목: {result['title']}")
        print(f"\n본문:\n{result['body']}")
        print(f"\n태그: {result['tags']}")
