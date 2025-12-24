"""
네이버 플레이스 크롤러
플레이스 URL 또는 PID에서 업체 정보를 추출합니다.
"""
import httpx
import re
import json
from typing import Optional, Dict, Any
from urllib.parse import urlparse, unquote

class PlaceCrawler:
    """네이버 플레이스 정보 크롤러"""

    # 더 완전한 브라우저 헤더
    HEADERS = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
        "Accept-Encoding": "gzip, deflate, br",
        "Cache-Control": "no-cache",
        "Pragma": "no-cache",
        "Sec-Ch-Ua": '"Google Chrome";v="131", "Chromium";v="131", "Not_A Brand";v="24"',
        "Sec-Ch-Ua-Mobile": "?0",
        "Sec-Ch-Ua-Platform": '"Windows"',
        "Sec-Fetch-Dest": "document",
        "Sec-Fetch-Mode": "navigate",
        "Sec-Fetch-Site": "none",
        "Sec-Fetch-User": "?1",
        "Upgrade-Insecure-Requests": "1",
    }

    @staticmethod
    def extract_place_id(url_or_pid: str) -> Optional[str]:
        """URL 또는 PID에서 플레이스 ID 추출"""
        url_or_pid = url_or_pid.strip()

        # 1. 숫자만 입력된 경우 (PID 직접 입력)
        if url_or_pid.isdigit():
            return url_or_pid

        # 2. URL에서 PID 추출
        patterns = [
            r'/place/(\d+)',
            r'/restaurant/(\d+)',
            r'/cafe/(\d+)',
            r'/hospital/(\d+)',
            r'/beauty/(\d+)',
            r'/accommodation/(\d+)',
            r'/entry/place/(\d+)',
            r'/home\?.*?placePath=%2Fplace%2F(\d+)',
            r'[?&]placePath=[^&]*%2F(\d+)',
            r'/(\d{8,12})(?:/|$|\?)',  # 8-12자리 숫자 (PID 형식)
        ]

        for pattern in patterns:
            match = re.search(pattern, url_or_pid)
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
    async def fetch_place_info(url_or_pid: str) -> Dict[str, Any]:
        """플레이스 URL 또는 PID에서 정보 크롤링"""
        result = {
            "success": False,
            "place_id": None,
            "name": "",
            "address": "",
            "category": "",
            "phone": "",
            "url": url_or_pid,
            "error": None
        }

        url_or_pid = url_or_pid.strip()

        try:
            # 단축 URL 처리 (naver.me)
            if "naver.me" in url_or_pid:
                url_or_pid = await PlaceCrawler.resolve_short_url(url_or_pid)
                result["url"] = url_or_pid

            # 모바일 URL 변환
            if "m.map.naver.com" in url_or_pid or "m.place.naver.com" in url_or_pid:
                # 모바일 URL도 처리
                pass

            place_id = PlaceCrawler.extract_place_id(url_or_pid)

            if not place_id:
                result["error"] = "플레이스 ID를 찾을 수 없습니다. URL 또는 PID 번호를 확인해주세요."
                return result

            result["place_id"] = place_id
            print(f"[크롤러] 플레이스 ID: {place_id}")

            async with httpx.AsyncClient(timeout=15) as client:
                # 방법 1: 네이버 플레이스 API v2 시도
                info = await PlaceCrawler._fetch_from_api_v2(client, place_id)
                if info and info.get("name"):
                    result.update(info)
                    result["success"] = True
                    return result

                # 방법 2: GraphQL API 시도
                info = await PlaceCrawler._fetch_from_graphql(client, place_id)
                if info and info.get("name"):
                    result.update(info)
                    result["success"] = True
                    return result

                # 방법 3: 모바일 페이지 크롤링
                info = await PlaceCrawler._fetch_from_mobile_page(client, place_id)
                if info and info.get("name"):
                    result.update(info)
                    result["success"] = True
                    return result

                # 방법 4: PC 페이지 크롤링
                info = await PlaceCrawler._fetch_from_pc_page(client, place_id)
                if info and info.get("name"):
                    result.update(info)
                    result["success"] = True
                    return result

            if not result["name"]:
                result["error"] = "업체 정보를 찾을 수 없습니다. URL 또는 PID를 확인해주세요."

        except httpx.TimeoutException:
            result["error"] = "요청 시간이 초과되었습니다. 다시 시도해주세요."
        except httpx.RequestError as e:
            result["error"] = f"네트워크 오류: {str(e)}"
        except Exception as e:
            result["error"] = f"오류 발생: {str(e)}"
            import traceback
            traceback.print_exc()

        return result

    @staticmethod
    async def _fetch_from_api_v2(client: httpx.AsyncClient, place_id: str) -> Optional[Dict]:
        """네이버 플레이스 API v2에서 정보 가져오기"""
        try:
            api_url = f"https://map.naver.com/p/api/place/summary/{place_id}"
            headers = {
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Accept": "application/json, text/plain, */*",
                "Accept-Language": "ko-KR,ko;q=0.9",
                "Referer": f"https://map.naver.com/p/entry/place/{place_id}",
                "Origin": "https://map.naver.com",
            }

            response = await client.get(api_url, headers=headers)
            if response.status_code == 200:
                data = response.json()
                if data:
                    return {
                        "name": data.get("name", ""),
                        "address": data.get("roadAddress") or data.get("address", ""),
                        "category": data.get("category", ""),
                        "phone": data.get("phone") or data.get("tel", ""),
                    }
        except Exception as e:
            print(f"[크롤러] API v2 실패: {e}")
        return None

    @staticmethod
    async def _fetch_from_graphql(client: httpx.AsyncClient, place_id: str) -> Optional[Dict]:
        """GraphQL API에서 정보 가져오기"""
        try:
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
                        }
                    }
                """
            }

            headers = {
                **PlaceCrawler.HEADERS,
                "Content-Type": "application/json",
                "Origin": "https://map.naver.com",
            }

            response = await client.post(api_url, json=graphql_query, headers=headers)
            if response.status_code == 200:
                data = response.json()
                if "data" in data and data["data"].get("placeDetail"):
                    place = data["data"]["placeDetail"]
                    return {
                        "name": place.get("name", ""),
                        "address": place.get("road") or place.get("address", ""),
                        "category": place.get("category", ""),
                        "phone": place.get("phone", ""),
                    }
        except Exception as e:
            print(f"[크롤러] GraphQL 실패: {e}")
        return None

    @staticmethod
    async def _fetch_from_mobile_page(client: httpx.AsyncClient, place_id: str) -> Optional[Dict]:
        """모바일 페이지에서 정보 크롤링"""
        try:
            mobile_url = f"https://m.place.naver.com/place/{place_id}/home"
            response = await client.get(mobile_url, headers=PlaceCrawler.HEADERS)
            html = response.text

            result = {"name": "", "address": "", "category": "", "phone": ""}

            # __NEXT_DATA__ JSON 추출
            next_data_match = re.search(
                r'<script id="__NEXT_DATA__" type="application/json">(.*?)</script>',
                html, re.DOTALL
            )

            if next_data_match:
                try:
                    next_data = json.loads(next_data_match.group(1))
                    page_props = next_data.get("props", {}).get("pageProps", {})
                    initial_state = page_props.get("initialState", {})

                    # 여러 위치에서 데이터 찾기
                    place_data = None
                    for path in ["place.detailPlaceHome", "place.home", "place.detail"]:
                        parts = path.split(".")
                        data = initial_state
                        for part in parts:
                            data = data.get(part, {})
                        if data and data.get("name"):
                            place_data = data
                            break

                    if place_data:
                        result["name"] = place_data.get("name", "")
                        result["address"] = place_data.get("roadAddress") or place_data.get("address", "")
                        result["category"] = place_data.get("category", "")
                        result["phone"] = place_data.get("phone") or place_data.get("tel", "")

                        if result["name"]:
                            return result
                except json.JSONDecodeError:
                    pass

            # 메타 태그에서 추출
            og_title = re.search(r'<meta property="og:title" content="([^"]+)"', html)
            if og_title:
                title = og_title.group(1)
                result["name"] = title.split(" : ")[0].split(" - ")[0].strip()

            og_desc = re.search(r'<meta property="og:description" content="([^"]+)"', html)
            if og_desc:
                desc = og_desc.group(1)
                if any(x in desc for x in ["시 ", "군 ", "구 ", "동 ", "로 ", "길 "]):
                    result["address"] = desc.split(",")[0].strip()

            if result["name"]:
                return result

        except Exception as e:
            print(f"[크롤러] 모바일 페이지 실패: {e}")
        return None

    @staticmethod
    async def _fetch_from_pc_page(client: httpx.AsyncClient, place_id: str) -> Optional[Dict]:
        """PC 페이지에서 정보 크롤링"""
        try:
            pc_url = f"https://map.naver.com/p/entry/place/{place_id}"
            response = await client.get(pc_url, headers=PlaceCrawler.HEADERS, follow_redirects=True)
            html = response.text

            result = {"name": "", "address": "", "category": "", "phone": ""}

            # 타이틀 태그에서 추출
            title_match = re.search(r'<title>([^<]+)</title>', html)
            if title_match:
                title = title_match.group(1)
                result["name"] = title.split(" : ")[0].split(" - ")[0].strip()

            # JSON-LD에서 추출
            jsonld_match = re.search(r'<script type="application/ld\+json">(.*?)</script>', html, re.DOTALL)
            if jsonld_match:
                try:
                    jsonld = json.loads(jsonld_match.group(1))
                    if isinstance(jsonld, dict):
                        result["name"] = jsonld.get("name", result["name"])
                        addr = jsonld.get("address", {})
                        if isinstance(addr, dict):
                            result["address"] = addr.get("streetAddress", "")
                        result["phone"] = jsonld.get("telephone", "")
                except:
                    pass

            if result["name"]:
                return result

        except Exception as e:
            print(f"[크롤러] PC 페이지 실패: {e}")
        return None

    @staticmethod
    def validate_url(url_or_pid: str) -> Dict[str, Any]:
        """URL 또는 PID 유효성 검사"""
        result = {"valid": False, "url_type": None, "error": None}
        url_or_pid = url_or_pid.strip()

        try:
            # PID만 입력된 경우
            if url_or_pid.isdigit():
                if len(url_or_pid) >= 8:
                    result["valid"] = True
                    result["url_type"] = "pid_only"
                    return result
                else:
                    result["error"] = "PID는 최소 8자리 이상이어야 합니다."
                    return result

            parsed = urlparse(url_or_pid)

            valid_domains = [
                "map.naver.com",
                "m.place.naver.com",
                "place.naver.com",
                "naver.me",
                "m.map.naver.com"
            ]

            if parsed.netloc in valid_domains or any(d in parsed.netloc for d in valid_domains):
                result["valid"] = True
                if "m.map.naver.com" in parsed.netloc or "m.place.naver.com" in parsed.netloc:
                    result["url_type"] = "naver_mobile"
                elif "map.naver.com" in parsed.netloc:
                    result["url_type"] = "naver_map"
                elif "place.naver.com" in parsed.netloc:
                    result["url_type"] = "naver_place"
                elif "naver.me" in parsed.netloc:
                    result["url_type"] = "naver_short"
                else:
                    result["url_type"] = "naver_other"
            else:
                result["error"] = "지원하지 않는 URL 형식입니다. 네이버 플레이스/지도 URL 또는 PID를 입력하세요."

        except Exception as e:
            result["error"] = f"URL 파싱 오류: {str(e)}"

        return result


if __name__ == "__main__":
    import asyncio

    async def test():
        # 테스트
        test_cases = [
            "1839653593",  # PID만
            "https://m.place.naver.com/place/1839653593/home",  # 모바일
            "https://map.naver.com/p/entry/place/1839653593",  # PC
        ]

        for test_input in test_cases:
            print(f"\n{'='*50}")
            print(f"Testing: {test_input}")
            print(f"{'='*50}")

            # 유효성 검사
            validation = PlaceCrawler.validate_url(test_input)
            print(f"Validation: {validation}")

            if validation["valid"]:
                result = await PlaceCrawler.fetch_place_info(test_input)
                print(f"Result: {json.dumps(result, ensure_ascii=False, indent=2)}")

    asyncio.run(test())
