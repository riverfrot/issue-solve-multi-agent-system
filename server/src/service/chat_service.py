from typing import List, Dict, Any
from datetime import datetime
from domain.models.chat import ChatRequest, ChatResponse, ChatMessage
from service.graph.workflow import MultiAgentWorkflow
from infrastructure.llm.openai_client import OpenAIClient
from utils.logger import logger
import time
import uuid


class ChatService:
    """
    LangGraph 기반 멀티 에이전트 채팅 서비스

    Features:
    - LangGraph 워크플로우 기반 에이전트 조율
    - 상태 기반 대화 관리
    - 자동 에이전트 라우팅
    - 다중 에이전트 응답 통합
    """

    def __init__(
        self,
        openai_client: OpenAIClient,
    ):
        # LangGraph 워크플로우 초기화
        self.workflow = MultiAgentWorkflow(
            openai_client=openai_client,
        )

        # 세션 관리 (메모리 기반, 추후 Redis/DB로 확장 가능)
        self.sessions: Dict[str, List[ChatMessage]] = {}

        logger.info("Multi-Agent Chat Service initialized with LangGraph")

    async def chat(self, request: ChatRequest) -> ChatResponse:
        """
        채팅 요청 처리 - LangGraph 워크플로우 실행
        """
        start_time = time.time()

        # 세션 ID 생성 (없으면)
        session_id = request.session_id or str(uuid.uuid4())

        # 세션 히스토리 가져오기
        history = self.sessions.get(session_id, [])

        logger.info(f"Processing chat request - Session: {session_id}")
        logger.info(f"Query: {request.message}")

        try:
            # LangGraph 워크플로우 실행
            result = await self.workflow.execute(
                query=request.message, session_id=session_id, history=history
            )

            # 응답 생성
            response_message = result["response"]
            metadata = result["metadata"]

            # 대화 히스토리 업데이트
            self._update_session_history(
                session_id=session_id,
                user_message=request.message,
                assistant_response=response_message,
            )

            # 처리 시간 계산
            total_time = (time.time() - start_time) * 1000

            # 응답 객체 생성
            chat_response = ChatResponse(
                message=response_message,
                session_id=session_id,
                agent_used=(
                    result.get("agent_route", ["general"])[-1]
                    if result.get("agent_route")
                    else "general"
                ),
                thinking_process=result.get("reasoning", []),
                metadata={
                    **metadata,
                    "total_latency_ms": total_time,
                    "agent_route": result.get("agent_route", []),
                    "langgraph_enabled": True,
                    "workflow_success": result.get("success", True),
                },
            )

            logger.info(f"Chat response completed in {total_time:.2f}ms")
            return chat_response

        except Exception as e:
            logger.error(f"Chat service error: {e}")

            # 에러 응답
            error_response = ChatResponse(
                message=f"죄송합니다. 처리 중 오류가 발생했습니다: {str(e)}",
                session_id=session_id,
                agent_used="error_handler",
                thinking_process=[f"Error occurred: {str(e)}"],
                metadata={
                    "error": str(e),
                    "total_latency_ms": (time.time() - start_time) * 1000,
                    "langgraph_enabled": True,
                    "workflow_success": False,
                },
            )

            return error_response

    def _update_session_history(
        self, session_id: str, user_message: str, assistant_response: str
    ):
        """세션 히스토리 업데이트"""
        if session_id not in self.sessions:
            self.sessions[session_id] = []

        # 사용자 메시지 추가
        self.sessions[session_id].append(
            ChatMessage(
                session_id=session_id,
                role="user",
                content=user_message,
                timestamp=datetime.now(),
            )
        )

        # 어시스턴트 응답 추가
        self.sessions[session_id].append(
            ChatMessage(
                session_id=session_id,
                role="assistant",
                content=assistant_response,
                timestamp=datetime.now(),
            )
        )

        # 히스토리 길이 제한 (최근 20개 메시지만 보관)
        if len(self.sessions[session_id]) > 20:
            self.sessions[session_id] = self.sessions[session_id][-20:]

    def get_session_history(self, session_id: str) -> List[ChatMessage]:
        """세션 히스토리 조회"""
        return self.sessions.get(session_id, [])

    def clear_session(self, session_id: str) -> bool:
        """세션 히스토리 삭제"""
        if session_id in self.sessions:
            del self.sessions[session_id]
            logger.info(f"🗑️ Cleared session: {session_id}")
            return True
        return False

    def get_active_sessions(self) -> List[str]:
        """활성 세션 목록"""
        return list(self.sessions.keys())

    def get_service_info(self) -> Dict[str, Any]:
        """서비스 정보"""
        workflow_info = self.workflow.get_workflow_info()

        return {
            "service_name": "Multi-Agent Chat Service",
            "version": "2.0.0",
            "description": "Multi-agent chatbot powered by LangGraph",
            "active_sessions": len(self.sessions),
            "workflow_info": workflow_info,
            "capabilities": [
                "Multi-agent coordination with LangGraph",
                "Automatic intent analysis and routing",
                "Document retrieval and QA",
                "Code generation and execution",
                "Web search and information gathering",
                "Session-based conversation management",
                "Response aggregation from multiple agents",
                "Error handling with fallback mechanisms",
            ],
            "features": [
                "StateGraph-based workflow orchestration",
                "Conditional agent routing",
                "Conversation history management",
                "Performance monitoring and tracing",
                "Retry mechanisms with graceful degradation",
            ],
        }

    async def health_check(self) -> Dict[str, Any]:
        """서비스 상태 확인"""
        try:
            # 간단한 테스트 쿼리로 워크플로우 동작 확인
            test_result = await self.workflow.execute(
                query="health check", session_id="health_check_session"
            )

            return {
                "status": "healthy",
                "langgraph_workflow": "operational",
                "active_sessions": len(self.sessions),
                "test_execution": test_result.get("success", False),
                "timestamp": time.time(),
            }

        except Exception as e:
            logger.error(f"Health check failed: {e}")
            return {"status": "unhealthy", "error": str(e), "timestamp": time.time()}
