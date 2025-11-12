from fastapi import APIRouter, Depends, HTTPException
from typing import List, Dict, Any

from app.domain.models.chat import ChatRequest, ChatResponse, ChatMessage
from app.service.chat_service import ChatService
from app.config.dependencies import get_chat_service
from app.utils.logger import logger


router = APIRouter(prefix="/api/chat", tags=["Multi-Agent Chat"])


@router.post("/", response_model=ChatResponse)
async def chat(
    request: ChatRequest, chat_service: ChatService = Depends(get_chat_service)
) -> ChatResponse:

    try:
        logger.info(f"Chat Request - Session: {request.session_id}")

        response = await chat_service.chat(request)

        logger.info(
            f"Chat Response - Success: {response.metadata.get('workflow_success', True)}"
        )

        return response

    except Exception as e:
        logger.error(f"LangGraph Chat Controller Error: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")


@router.get("/history/{session_id}", response_model=List[ChatMessage])
async def get_chat_history(
    session_id: str, chat_service: ChatService = Depends(get_chat_service)
) -> List[ChatMessage]:
    """세션 히스토리 조회"""
    try:
        history = chat_service.get_session_history(session_id)
        return history

    except Exception as e:
        logger.error(f"Get History Error: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to get history: {str(e)}")


@router.delete("/session/{session_id}")
async def clear_session(
    session_id: str, chat_service: ChatService = Depends(get_chat_service)
) -> Dict[str, Any]:
    """세션 삭제"""
    try:
        success = chat_service.clear_session(session_id)

        if success:
            return {"message": f"Session {session_id} cleared successfully"}
        else:
            return {"message": f"Session {session_id} not found"}

    except Exception as e:
        logger.error(f"Clear Session Error: {e}")
        raise HTTPException(
            status_code=500, detail=f"Failed to clear session: {str(e)}"
        )


@router.get("/sessions")
async def get_active_sessions(
    chat_service: ChatService = Depends(get_chat_service),
) -> Dict[str, Any]:
    """활성 세션 목록"""
    try:
        sessions = chat_service.get_active_sessions()

        return {"active_sessions": sessions, "total_count": len(sessions)}

    except Exception as e:
        logger.error(f"Get Sessions Error: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to get sessions: {str(e)}")


@router.get("/info")
async def get_service_info(
    chat_service: ChatService = Depends(get_chat_service),
) -> Dict[str, Any]:
    """LangGraph 서비스 정보"""
    try:
        info = chat_service.get_service_info()
        return info

    except Exception as e:
        logger.error(f"Get Service Info Error: {e}")
        raise HTTPException(
            status_code=500, detail=f"Failed to get service info: {str(e)}"
        )


@router.get("/health")
async def health_check(
    chat_service: ChatService = Depends(get_chat_service),
) -> Dict[str, Any]:
    """LangGraph 서비스 상태 확인"""
    try:
        health_status = await chat_service.health_check()

        # 상태에 따라 HTTP 상태 코드 결정
        if health_status.get("status") == "unhealthy":
            raise HTTPException(status_code=503, detail="Service unhealthy")

        return health_status

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Health Check Error: {e}")
        raise HTTPException(status_code=500, detail=f"Health check failed: {str(e)}")


@router.post("/workflow/test")
async def test_workflow(
    test_query: str = "안녕하세요",
    chat_service: ChatService = Depends(get_chat_service),
) -> Dict[str, Any]:
    """
    워크플로우 테스트 엔드포인트
    개발/디버깅용
    """
    try:
        # 테스트 요청 생성
        test_request = ChatRequest(
            message=test_query, session_id="workflow_test_session"
        )
        
        # 워크플로우 실행
        response = await chat_service.chat(test_request)

        return {
            "test_query": test_query,
            "workflow_response": response.message,
            "metadata": response.metadata,
            "success": True,
        }

    except Exception as e:
        logger.error(f"Workflow Test Error: {e}")
        return {"test_query": test_query, "error": str(e), "success": False}
