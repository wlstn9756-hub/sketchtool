"""
네이버 플레이스 크롤러
플레이스 URL에서 업체 정보를 추출합니다.
"""
import httpx
import re
import json
from typing import Optional, Dict, Any
from urllib.parse import urlparse, unquote

class PlaceCrawler:
    """네이버 플레이스 정보 크롤러"""

    HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "application/json, text/plain, */*",
        "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
        "Referer": "https://map.naver.com/",
    }

    @staticmethod
    def extract_place_id(url: str) -> Optional[str]:
        """URL에서 플레이스 ID 추출"""
        patterns = [
            r'/place/(\d+)',
            r'/restaurant/(\d+)',
            r'/cafe/(\d+)',
            r'/hospital/(\d+)',
            r'/beauty/(\d+)',
            r'/accommodation/(\d+)',
            r'/entry/place/(\d+)',
        ]

        for pattern in patterns:
            match = re.search(pattern, url)
            if match:
                return match.group(1)

        return None

    @staticmethod
    async def resolve_short_url(url: str) -> str:
        """단축 URL을 원본 URL로 변환"""
        try:
            async with httpx.AsyncClient(follow_redirects=True, timeout=10) as client:
                response = await client.head(url, headers=PlaceCrawler.HEADERS)
                return str(response.url)
        except:
            return url

    @staticmethod
    async def fetch_place_info(url: str) -> Dict[str, Any]:
        """플레이스 URL에서 정보 크롤링"""
        result = {
            "success": False,
            "place_id": None,
            "name": "",
            "address": "",
            "category": "",
            "phone": "",
            "url": url,
            "error": None
        }

        try:
            # 단축 URL 처리
            if "naver.me" in url:
                url = await PlaceCrawler.resolve_short_url(url)
                result["url"] = url

            place_id = PlaceCrawler.extract_place_id(url)

            if not place_id:
                result["error"] = "플레이스 ID를 찾을 수 없습니다. URL을 확인해주세요."
                return result

            result["place_id"] = place_id

            # 네이버 플레이스 GraphQL API 사용
            api_url = "https://pcmap-api.place.naver.com/graphql"

            graphql_query = {
                "operationName": "getPlaceDetailBasic",
                "variables": {"id": place_id},
                "query": """
                    query getPlaceDetailBasic($id: String!) {
                        placeDetail(id: $id) {
                            id
                            name
                            category
                            road
                            address
                            phone
                            businessHours
                        }
                    }
                """
            }

            headers = {
                **PlaceCrawler.HEADERS,
                "Content-Type": "application/json",
                "Origin": "https://map.naver.com",
            }

            async with httpx.AsyncClient(timeout=15) as client:
                # GraphQL API 시도
                try:
                    response = await client.post(api_url, json=graphql_query, headers=headers)
                    if response.status_code == 200:
                        data = response.json()
                        if "data" in data and data["data"].get("placeDetail"):
                            place = data["data"]["placeDetail"]
                            result["name"] = place.get("name", "")
                            result["address"] = place.get("road") or place.get("address", "")
                            result["category"] = place.get("category", "")
                            result["phone"] = place.get("phone", "")
                            if result["name"]:
                                result["success"] = True
                                return result
                except Exception:
                    pass

                # 대체 방법: 모바일 페이지 크롤링
                mobile_url = f"https://m.place.naver.com/place/{place_id}/home"
                response = await client.get(mobile_url, headers=PlaceCrawler.HEADERS)
                html = response.text

                # __NEXT_DATA__ JSON 추출
                next_data_match = re.search(
                    r'<script id="__NEXT_DATA__" type="application/json">(.*?)</script>',
                    html, re.DOTALL
                )

                if next_data_match:
                    try:
                        next_data = json.loads(next_data_match.group(1))
                        page_props = next_data.get("props", {}).get("pageProps", {})

                        # initialState에서 찾기
                        initial_state = page_props.get("initialState", {})
                        place_data = initial_state.get("place", {}).get("detailPlaceHome", {})

                        if not place_data:
                            place_data = initial_state.get("place", {}).get("home", {})

                        if place_data:
                            result["name"] = place_data.get("name", "")
                            result["address"] = place_data.get("roadAddress") or place_data.get("address", "")
                            result["category"] = place_data.get("category", "")
                            result["phone"] = place_data.get("phone") or place_data.get("tel", "")
                    except json.JSONDecodeError:
                        pass

                # 메타 태그에서 추출
                if not result["name"]:
                    og_title = re.search(r'<meta property="og:title" content="([^"]+)"', html)
                    if og_title:
                        title = og_title.group(1)
                        # "업체명 : 네이버 지도" 형식에서 업체명만 추출
                        result["name"] = title.split(" : ")[0].split(" - ")[0].strip()

                if not result["address"]:
                    og_desc = re.search(r'<meta property="og:description" content="([^"]+)"', html)
                    if og_desc:
                        desc = og_desc.group(1)
                        # 주소가 포함되어 있을 수 있음
                        if any(x in desc for x in ["시 ", "군 ", "구 ", "동 ", "로 ", "길 "]):
                            result["address"] = desc.split(",")[0].strip()

                # 타이틀 태그에서 추출
                if not result["name"]:
                    title_match = re.search(r'<title>([^<]+)</title>', html)
                    if title_match:
                        title = title_match.group(1)
                        result["name"] = title.split(" : ")[0].split(" - ")[0].strip()

            if result["name"]:
                result["success"] = True
            else:
                result["error"] = "업체 정보를 찾을 수 없습니다. URL을 확인해주세요."

        except httpx.TimeoutException:
            result["error"] = "요청 시간이 초과되었습니다. 다시 시도해주세요."
        except httpx.RequestError as e:
            result["error"] = f"네트워크 오류: {str(e)}"
        except Exception as e:
            result["error"] = f"오류 발생: {str(e)}"

        return result

    @staticmethod
    def validate_url(url: str) -> Dict[str, Any]:
        """URL 유효성 검사"""
        result = {"valid": False, "url_type": None, "error": None}

        try:
            parsed = urlparse(url)

            valid_domains = [
                "map.naver.com",
                "m.place.naver.com",
                "place.naver.com",
                "naver.me",
                "m.map.naver.com"
            ]

            if parsed.netloc in valid_domains or any(d in parsed.netloc for d in valid_domains):
                result["valid"] = True
                if "map.naver.com" in parsed.netloc:
                    result["url_type"] = "naver_map"
                elif "place.naver.com" in parsed.netloc:
                    result["url_type"] = "naver_place"
                elif "naver.me" in parsed.netloc:
                    result["url_type"] = "naver_short"
                else:
                    result["url_type"] = "naver_other"
            else:
                result["error"] = "지원하지 않는 URL 형식입니다. 네이버 플레이스/지도 URL을 입력하세요."

        except Exception as e:
            result["error"] = f"URL 파싱 오류: {str(e)}"

        return result


if __name__ == "__main__":
    import asyncio

    async def test():
        # 테스트 URL (실제 URL로 교체하세요)
        test_url = "https://map.naver.com/p/entry/place/1234567890"
        print(f"Testing: {test_url}")
        result = await PlaceCrawler.fetch_place_info(test_url)
        print(f"Result: {json.dumps(result, ensure_ascii=False, indent=2)}")

    asyncio.run(test())
