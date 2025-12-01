from typing import Dict, List
from domain.models.agent import AgentRequest, AgentResponse, AgentType
from infrastructure.llm.openai_client import OpenAIClient
from utils.logger import logger


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

        documents = self._get_mock_documents(request.query)
        
        context = self._build_context(documents)

        system_prompt = f"""당신은 내부 문서 검색 전문가입니다.

        아래 문서들을 참조하여 사용자의 질문에 정확하고 도움이 되는 답변을 제공하세요.

        {context}

        답변 시 다음을 포함하세요:
        - 문서에서 찾은 구체적인 정보
        - 실용적인 사용 예시
        - 관련 문서 섹션 참조"""


        try:
            response_content = self.llm_client.generate(
                prompt=request.query,
                system_prompt=system_prompt,
                temperature=0.0
            )

            return AgentResponse(
                content=response_content,
                agent_type=AgentType.RAG,
                metadata={
                    "documents_found": len(documents),
                    "search_query": request.query,
                    "context_length": len(context)
                }
            )

        except Exception as e:
            logger.error(f"RAG Agent error: {e}")
            return AgentResponse(
                content=f"죄송합니다. 문서 검색 중 오류가 발생했습니다: {str(e)}",
                agent_type=AgentType.RAG,
                metadata={"error": str(e)}
            )

    def _get_mock_documents(self, query: str) -> List[Dict]:
        """
        Mock 문서 데이터 (실제로는 벡터 DB에서 검색)
        """
        if "api" in query.lower():
            return [
                {
                    "title": "API 인증 가이드",
                    "content": "API 사용을 위해서는 API 키가 필요합니다. 헤더에 'Authorization: Bearer {api_key}' 형식으로 포함하세요.",
                    "section": "authentication",
                    "relevance": 0.95
                },
                {
                    "title": "API 엔드포인트 목록",
                    "content": "주요 엔드포인트: POST /api/chat/message (채팅), GET /api/health (헬스체크)",
                    "section": "endpoints",
                    "relevance": 0.90
                }
            ]
        elif "설정" in query.lower() or "config" in query.lower():
            return [
                {
                    "title": "환경 설정 가이드",
                    "content": ".env 파일을 생성하고 OPENAI_API_KEY를 설정하세요. 선택적으로 TAVILY_API_KEY도 설정할 수 있습니다.",
                    "section": "configuration",
                    "relevance": 0.92
                }
            ]
        else:
            return [
                {
                    "title": "시스템 개요",
                    "content": "이 시스템은 Vue.js + FastAPI 기반의 멀티 에이전트 챗봇입니다. Supervisor가 사용자 의도를 분석하여 적절한 에이전트로 라우팅합니다.",
                    "section": "overview",
                    "relevance": 0.85
                },
                {
                    "title": "에이전트 종류",
                    "content": "4가지 전문 에이전트: RAG(문서검색), Code(코드실행), Search(웹검색), General(일반대화)",
                    "section": "agents",
                    "relevance": 0.80
                }
            ]
        
    def _build_context(self, documents: List[Dict]) -> str:
        """
        문서들로부터 컨텍스트 구성
        """
        if not documents:
            return "관련 문서를 찾을 수 없습니다."
        
        context_parts = []
        for i, doc in enumerate(documents, 1):
            context_parts.append(
                f"## 문서 {i}: {doc['title']}\n"
                f"**섹션**: {doc['section']}\n"
                f"**내용**: {doc['content']}\n"
                f"**관련도**: {doc['relevance']:.2f}\n"
            )
        
        return "\n".join(context_parts)