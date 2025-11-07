"""
Tavily Search API Client
"""
from typing import List, Dict, Optional
import aiohttp
import os
from app.utils.logger import logger


class TavilyClient:
    """
    Tavily 검색 클라이언트
    
    책임:
    - Tavily API 호출만 담당
    """
    
    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or os.getenv("TAVILY_API_KEY")
        self.base_url = "https://api.tavily.com"
    
    async def search(
        self,
        query: str,
        max_results: int = 5,
        search_depth: str = "basic"
    ) -> List[Dict]:
        """웹 검색"""
        if not self.api_key:
            logger.warning("Tavily API key not provided, using mock data")
            return self._get_mock_results(query)
        
        try:
            async with aiohttp.ClientSession() as session:
                payload = {
                    "api_key": self.api_key,
                    "query": query,
                    "search_depth": search_depth,
                    "max_results": max_results,
                    "include_answer": True,
                    "include_images": False
                }
                
                async with session.post(
                    f"{self.base_url}/search",
                    json=payload
                ) as response:
                    if response.status == 200:
                        data = await response.json()
                        return self._format_results(data.get("results", []))
                    else:
                        logger.error(f"Tavily API error: {response.status}")
                        return self._get_mock_results(query)
        except Exception as e:
            logger.error(f"Tavily search error: {e}")
            return self._get_mock_results(query)
    
    def _format_results(self, results: List[Dict]) -> List[Dict]:
        """검색 결과 포맷팅"""
        formatted = []
        for result in results:
            formatted.append({
                "title": result.get("title", ""),
                "url": result.get("url", ""),
                "content": result.get("content", ""),
                "score": result.get("score", 0.0),
                "published_date": result.get("published_date", "")
            })
        return formatted
    
    def _get_mock_results(self, query: str) -> List[Dict]:
        """Mock 검색 결과 (API 키가 없을 때)"""
        return [
            {
                "title": f"Search Results for: {query}",
                "url": "https://example.com/search",
                "content": f"여기는 '{query}'에 대한 검색 결과입니다. 실제 Tavily API를 사용하려면 TAVILY_API_KEY를 설정하세요.",
                "score": 0.9,
                "published_date": "2024-01-01"
            },
            {
                "title": "Related Information",
                "url": "https://example.com/related",
                "content": f"'{query}' 관련 추가 정보입니다. 이는 데모용 Mock 데이터입니다.",
                "score": 0.8,
                "published_date": "2024-01-01"
            }
        ]