class GeneralService:
    """
    일반 서비스 - 일반 대화 및 질의응답
    """

    def __init__(self, llm_client: OpenAIClient):
        self.llm_client = llm_client

    def process(self, request: AgentRequest) -> AgentResponse:
        """
        일반 대화 처리 로직
        """
        logger.info(f"General Service Processing: {request.query}")

        return self.llm_client.generate(request.query)
