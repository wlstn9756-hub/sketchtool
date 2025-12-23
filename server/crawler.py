"""
네이버 플레이스 크롤러
플레이스 URL에서 업체 정보를 추출합니다.
"""
import httpx
import re
import json
from typing import Optional, Dict, Any
from urllib.parse import urlparse, parse_qs

class PlaceCrawler:
    """네이버 플레이스 정보 크롤러"""

    HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
    }

    @staticmethod
    def extract_place_id(url: str) -> Optional[str]:
        """URL에서 플레이스 ID 추출"""
        # 다양한 URL 형식 처리
        # https://map.naver.com/p/search/맛집/place/12345678
        # https://map.naver.com/v5/search/맛집/place/12345678
        # https://naver.me/xxxxx (단축 URL)
        # https://m.place.naver.com/restaurant/12345678
        # https://place.naver.com/restaurant/12345678

        patterns = [
            r'/place/(\d+)',
            r'/restaurant/(\d+)',
            r'/cafe/(\d+)',
            r'/hospital/(\d+)',
            r'/beauty/(\d+)',
            r'/accommodation/(\d+)',
        ]

        for pattern in patterns:
            match = re.search(pattern, url)
            if match:
                return match.group(1)

        return None

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
            place_id = PlaceCrawler.extract_place_id(url)

            if not place_id:
                # 단축 URL인 경우 리다이렉트 따라가기
                async with httpx.AsyncClient(follow_redirects=True) as client:
                    response = await client.get(url, headers=PlaceCrawler.HEADERS, timeout=10)
                    final_url = str(response.url)
                    place_id = PlaceCrawler.extract_place_id(final_url)

            if not place_id:
                result["error"] = "플레이스 ID를 찾을 수 없습니다"
                return result

            result["place_id"] = place_id

            # 네이버 플레이스 API 호출 (비공식)
            api_url = f"https://map.naver.com/p/api/search/allSearch?query={place_id}&type=all"

            # 또는 직접 페이지 크롤링
            place_url = f"https://m.place.naver.com/place/{place_id}"

            async with httpx.AsyncClient() as client:
                response = await client.get(place_url, headers=PlaceCrawler.HEADERS, timeout=10)
                html = response.text

                # JSON-LD 데이터 추출 시도
                json_ld_match = re.search(r'<script type="application/ld\+json">(.*?)</script>', html, re.DOTALL)
                if json_ld_match:
                    try:
                        json_data = json.loads(json_ld_match.group(1))
                        if isinstance(json_data, dict):
                            result["name"] = json_data.get("name", "")
                            if "address" in json_data:
                                addr = json_data["address"]
                                if isinstance(addr, dict):
                                    result["address"] = addr.get("streetAddress", "")
                                else:
                                    result["address"] = str(addr)
                            result["phone"] = json_data.get("telephone", "")
                    except json.JSONDecodeError:
                        pass

                # __NEXT_DATA__ 에서 추출 시도
                next_data_match = re.search(r'<script id="__NEXT_DATA__" type="application/json">(.*?)</script>', html, re.DOTALL)
                if next_data_match:
                    try:
                        next_data = json.loads(next_data_match.group(1))
                        # props에서 업체 정보 추출
                        props = next_data.get("props", {}).get("pageProps", {})
                        if "initialState" in props:
                            state = props["initialState"]
                            if "place" in state:
                                place_data = state["place"].get("place", {})
                                result["name"] = place_data.get("name", result["name"])
                                result["address"] = place_data.get("roadAddress", result["address"]) or place_data.get("address", "")
                                result["category"] = place_data.get("category", "")
                                result["phone"] = place_data.get("phone", result["phone"])
                    except json.JSONDecodeError:
                        pass

                # 메타 태그에서 추출 시도
                if not result["name"]:
                    og_title = re.search(r'<meta property="og:title" content="([^"]+)"', html)
                    if og_title:
                        result["name"] = og_title.group(1).split(" : ")[0].strip()

                if not result["address"]:
                    # 주소 패턴 찾기
                    addr_match = re.search(r'주소["\s:]+([^"<]+)', html)
                    if addr_match:
                        result["address"] = addr_match.group(1).strip()

            if result["name"]:
                result["success"] = True
            else:
                result["error"] = "업체 정보를 추출할 수 없습니다"

        except httpx.TimeoutException:
            result["error"] = "요청 시간이 초과되었습니다"
        except httpx.RequestError as e:
            result["error"] = f"요청 오류: {str(e)}"
        except Exception as e:
            result["error"] = f"크롤링 오류: {str(e)}"

        return result

    @staticmethod
    def validate_url(url: str) -> Dict[str, Any]:
        """URL 유효성 검사"""
        result = {"valid": False, "url_type": None, "error": None}

        try:
            parsed = urlparse(url)

            # 지원하는 도메인 확인
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


# 동기 버전 (테스트용)
def fetch_place_info_sync(url: str) -> Dict[str, Any]:
    """동기 버전 크롤러"""
    import asyncio
    return asyncio.run(PlaceCrawler.fetch_place_info(url))


if __name__ == "__main__":
    # 테스트
    import asyncio

    test_urls = [
        "https://map.naver.com/p/search/맛집/place/12345678",
    ]

    async def test():
        for url in test_urls:
            print(f"\nTesting: {url}")
            result = await PlaceCrawler.fetch_place_info(url)
            print(f"Result: {result}")

    asyncio.run(test())
