from typing import List, Dict
from domain.models.agent import AgentRequest, AgentResponse, AgentType
from infrastructure.llm.openai_client import OpenAIClient
from utils.logger import logger


class SearchAgent:
    """
    검색 서비스 - 웹 검색 및 정보 수집

    Tavily API를 통해 필요한 웹검색을 진행 예정
    """

    def __init__(self, llm_client: OpenAIClient):
        self.llm_client = llm_client
        # Tavily 클라이언트는 일단 생략 (API 키 문제)
        # self.tavily_client = TavilyClient()
        logger.info("Search Service initialized")

    def process(self, request: AgentRequest) -> AgentResponse:
        """
        검색 처리 로직
        """
        logger.info(f"Search Agent processing: {request.query}")

        try:
            # 현재는 Mock 데이터 사용 (Tavily API 키 미설정)
            search_results = self._get_mock_search_results(request.query)
            
            if not search_results:
                return AgentResponse(
                    content="검색 결과를 찾을 수 없습니다.",
                    agent_type=AgentType.SEARCH,
                    metadata={"results_count": 0}
                )

            summary = self._generate_summary(request.query, search_results)

            # 응답 포매팅
            response_content = f"""**검색 결과 요약**
{summary}

**상세 검색 결과**
{self._format_search_results(search_results)}"""

            return AgentResponse(
                content=response_content,
                agent_type=AgentType.SEARCH,
                metadata={
                    "results_count": len(search_results),
                    "search_query": request.query,
                    "sources": [result["url"] for result in search_results]
                }
            )
            
        except Exception as e:
            logger.error(f"Search Agent error: {e}")
            return AgentResponse(
                content=f"검색 중 오류가 발생했습니다: {str(e)}",
                agent_type=AgentType.SEARCH,
                metadata={"error": str(e)}
            )

    def _get_mock_search_results(self, query: str) -> List[Dict]:
        """Mock 검색 결과 데이터"""
        if "ai" in query.lower() or "인공지능" in query.lower():
            return [
                {
                    "title": "2024년 AI 기술 동향",
                    "url": "https://example.com/ai-trends-2024",
                    "content": "2024년 AI 기술은 생성형 AI의 발전과 함께 산업 전반에 걸쳐 혁신을 이끌고 있습니다. ChatGPT, Claude와 같은 대화형 AI의 성능이 크게 향상되었으며..."
                },
                {
                    "title": "생성형 AI 시장 전망",
                    "url": "https://example.com/generative-ai-market",
                    "content": "생성형 AI 시장은 2024년 현재 급속도로 성장하고 있으며, 특히 기업 영역에서의 도입이 활발해지고 있습니다. 문서 작성, 코드 생성, 이미지 및 영상 제작 등..."
                }
            ]
        elif "python" in query.lower() or "파이썬" in query.lower():
            return [
                {
                    "title": "Python 3.12 새로운 기능",
                    "url": "https://example.com/python-312-features",
                    "content": "Python 3.12는 성능 향상과 새로운 기능들을 제공합니다. f-string 개선, 타입 시스템 향상, 그리고 더 나은 에러 메시지 등이 주요 특징입니다..."
                }
            ]
        else:
            return [
                {
                    "title": "최신 기술 뉴스",
                    "url": "https://example.com/tech-news",
                    "content": "최신 기술 동향과 뉴스를 전해드립니다. 현재 기술 업계는 AI, 클라우드, 사이버보안 등 다양한 분야에서 혁신이 일어나고 있습니다..."
                }
            ]

    def _generate_summary(self, query: str, search_results: List[Dict]) -> str:
        """검색 결과 요약 생성"""
        # 검색 결과를 텍스트로 변환
        results_text = "\n\n".join([
            f"제목: {result['title']}\n"
            f"출처: {result['url']}\n"
            f"내용: {result['content']}"
            for result in search_results[:3]  # 상위 3개만 사용
        ])

        system_prompt = f"""당신은 웹 검색 결과를 분석하고 요약하는 전문가입니다.

사용자 질문: {query}

아래 검색 결과들을 분석하여 사용자 질문에 대한 포괄적이고 정확한 답변을 제공하세요.

검색 결과:
{results_text}

요약 시 다음을 포함하세요:
1. 핵심 정보 및 요점
2. 최신 동향이나 변화사항 (있다면)
3. 출처 신뢰성을 고려한 정보
4. 실용적인 인사이트나 조언

간결하지만 유용한 정보로 구성하여 200-400자 내로 작성하세요."""
        
        try:
            summary = self.llm_client.generate(
                prompt=f"위 검색 결과를 바탕으로 '{query}'에 대한 요약을 작성해주세요.",
                system_prompt=system_prompt,
                temperature=0.3
            )
            return summary
        except Exception as e:
            logger.error(f"Summary generation error: {e}")
            return "검색 결과를 요약하는 중 오류가 발생했습니다."

    def _format_search_results(self, search_results: List[Dict]) -> str:
        """
        검색 결과 포맷팅
        """
        formatted_results = []
        
        for i, result in enumerate(search_results, 1):
            formatted_result = f"""**{i}. {result['title']}**
출처: {result['url']}
내용: {result['content'][:200]}{'...' if len(result['content']) > 200 else ''}
"""
            formatted_results.append(formatted_result)
        
        return "\n".join(formatted_results)