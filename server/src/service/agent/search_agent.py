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

        return self.llm_client.generate(request.query)
