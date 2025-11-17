class RAGAgent:
    """
    RAG agent (Retrieval Augmented Generation)

    추후 해당 agent는 MCP tool을 이용해 기존에 구축된
    https://github.com/riverfrot/advanced-rag-system
    repo를 tool로써 가져올 예정
    """

    def __init__(self, llm_client: OpenAIClient):
        self.llm_client = llm_client
        logger.info("RAG Agent initialized")

    def process(self, request: AgentRequest) -> AgentResponse:
        """
        RAG 처리 로직
        """
        logger.info(f"RAG Agent processing: {request.query}")

        return self.llm_client.generate(request.query)
