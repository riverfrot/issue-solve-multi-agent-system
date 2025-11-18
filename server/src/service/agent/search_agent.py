class SearchAgent:
    """
    검색 서비스 - 웹 검색 및 정보 수집

    Tavily API를 통해 필요한 웹검색을 진행 예정
    """

    def __init__(self, tavily_client:TavilyClient, llm_client: OpenAIClient):
        self.llm_client = llm_client
        self.tavily_client = tavily_client
        logger.info("Search Service initialized")

    def process(self, request: AgentRequest) -> AgentResponse:
        """
        검색 처리 로직
        """
        logger.info(f"RAG Agent processing: {request.query}")

        try:
            search_results = await self.tavily_client.search(
                query=request.query,
                max_results=5
            )
            
            if not search_results:
                return AgentResponse(
                    conntent="검색 결과를 찾을 수 없습니다.",
                    agent_type=AgentType.SEARCH.value,
                    metadata={"results_count": 0}
                )

            summary = await self._generate_summary(request.query, search_results)

            # 응답 포맷팅
            response_content = f"""**검색 결과 요약**
            {summary}
            **상세 검색 결과**
            {self._format_search_results(search_results)}"""

        return AgentResponse(
                content=response_content,
                agent_type=AgentType.SEARCH.value,
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
                agent_type=AgentType.SEARCH.value,
                metadata={"error": str(e)}
            )

    async def _generate_summary(self, query:str, search_results: list) -> str:
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
            summary = await self.llm_client.generate(
                prompt=f"위 검색 결과를 바탕으로 '{query}'에 대한 요약을 작성해주세요.",
                system_prompt=system_prompt,
                temperature=0.3
            )
            return summary
        except Exception as e:
            logger.error(f"Summary generation error: {e}")
            return "검색 결과를 요약하는 중 오류가 발생했습니다."

    def _format_search_results(self, search_results: list) -> str:
        """
        검색 결과 포맷팅
        """
        formatted_results = []
        
        for i, result in enumerate(search_results, 1):
            formatted_result = f"""**{i}. {result['title']}**
            출처: {result['url']}
            내용: {result['content'][:200]}{'...' if len(result['content']) > 200 else ''}
            """

            if result.get('published_date'):
                formatted_result += f"발행일: {result['published_date']}\n"
            
            formatted_results.append(formatted_result)
        
        return "\n".join(formatted_results)