from app.domain.models.agent import AgentRequest, AgentResponse, AgentType
from app.infrastructure.llm.openai_client import OpenAIClient
from app.utils.logger import logger


class GeneralAgent:
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

        try:
            # 응답 컨텍스트에 에러가 있는지 먼저 확인
            is_error_fallback = request.context and request.context.get("error", False)

            if is_error_fallback:
                response_content = await self._handle_error_fallback(request.query)
            else:
                respone_content = response_content = await self._handle_general_conversation(request) 
            
            return AgentResponse(
                content=response_content,
                agent_type=AgentType.GENERAL.value,
                metadata={
                    "conversation_type": "error_fallback" if is_error_fallback else "general",
                    "query_length": len(request.query)
                }
            )
        
        except Exception as e:
            logger.error(f"General Agent error: {e}")
            return AgentResponnse(
                content="응답을 생성하는중 오류가 발생했습니다.,
                agent_type=AgentType.GENERAL.value,
                metadata={"error": str(e)}
            )

    async def _handle_general_conversation(self, request: AgentRequest) -> str:
        """
        일반 대화 처리
        """
        # 시스템 정보나 도움말 요청인지 확인
        query_lower = request.query.lower()
        
        if any(keyword in query_lower for keyword in ["도움", "help", "사용법", "기능"]):
            return await self._get_help_message()
        
        if any(keyword in query_lower for keyword in ["안녕", "hello", "hi", "처음"]):
            return await self._get_greeting_message()
        
        # 일반 대화
        system_prompt = """당신은 도움이 되고 친근한 AI 어시스턴트입니다.

멀티 에이전트 챗봇 시스템의 General Agent로서 다음과 같은 역할을 수행합니다:
- 일반적인 질문과 대화
- 친근하고 자연스러운 응답
- 다른 전문 에이전트들에 대한 안내
- 시스템 사용법 설명

응답할 때:
- 자연스럽고 친근한 톤 사용
- 구체적이고 도움이 되는 정보 제공
- 필요시 다른 에이전트 기능 안내
- 한국어로 응답"""
        
        return await self.llm_client.generate(
            prompt=request.query,
            system_prompt=system_prompt,
            temperature=0.7
        )
    
    async def _handle_error_fallback(self, query: str) -> str:
        """
        에러 폴백 처리
        """
        return f"""죄송합니다. 요청을 처리하는 중 문제가 발생했습니다.

다음을 시도해보세요:
1. 질문을 다시 표현해보세요
2. 더 구체적으로 질문해보세요
3. 다른 에이전트 기능을 시도해보세요

**사용 가능한 기능:**
- **문서 검색**: "API 사용법을 찾아줘"
- **코드 실행**: "1부터 100까지 합을 계산해줘"
- **웹 검색**: "최신 AI 뉴스를 검색해줘"
- **일반 대화**: "AI에 대해 설명해줘"

궁금한 점이 있으시면 언제든 질문해주세요!"""
    
    async def _get_help_message(self) -> str:
        """
        도움말 메시지
        """
        return """🤖 **멀티 에이전트 챗봇 시스템** 사용 가이드

이 시스템은 4가지 전문 에이전트가 협력하여 다양한 요청을 처리합니다:

## **Supervisor Agent**
- 사용자 의도 분석 및 적절한 에이전트 자동 선택

## **RAG Agent (문서 검색)**
- 내부 문서 및 API 가이드 검색
- **예시**: "API 사용법을 찾아줘", "인증 방법을 알려줘"

## **Code Agent (코드 실행)**
- Python 코드 생성 및 안전한 실행
- **예시**: "피보나치 수열 계산해줘", "1부터 100까지 합을 구해줘"

## **Search Agent (웹 검색)**
- 최신 정보 및 뉴스 검색
- **예시**: "최신 AI 뉴스 검색해줘", "Python 3.12 새로운 기능"

## **General Agent (일반 대화)**
- 일반적인 질의응답 및 대화
- **예시**: "AI에 대해 설명해줘", "안녕하세요"

---

**팁**: 자연스럽게 질문하시면 Supervisor가 자동으로 적절한 에이전트를 선택합니다!"""
    
    async def _get_greeting_message(self) -> str:
        """
        인사 메시지
        """
        return """안녕하세요! 👋 

멀티 에이전트 챗봇 시스템에 오신 것을 환영합니다!

저는 4개의 전문 에이전트가 협력하는 AI 시스템입니다:
- 문서 검색 
- 코드 실행
- 웹 검색  
- 일반 대화

무엇을 도와드릴까요? 자연스럽게 질문하시면 가장 적합한 에이전트가 답변드리겠습니다!

**빠른 시작 예시:**
- "API 사용법을 알려줘"
- "파이썬으로 계산해줘"
- "최신 기술 뉴스 검색"
- "AI에 대해 설명해줘"

**도움이 필요하시면 "도움말"이라고 입력하세요!**"""