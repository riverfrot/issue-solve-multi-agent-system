from fastapi import APIRouter, Depends
from datetime import datetime
from service.chat_service import ChatService
from config.dependencies import get_chat_service


router = APIRouter(prefix="/api", tags=["health"])


@router.get("/health")
async def health_check(chat_service: ChatService = Depends(get_chat_service)):
    try:
        # 서비스 상태 확인
        service_status = await chat_service.health_check()

        return {
            "status": "healthy",
            "timestamp": datetime.now().isoformat(),
            "version": "1.0.0",
            "services": service_status,
        }
    except Exception as e:
        return {
            "status": "unhealthy",
            "timestamp": datetime.now().isoformat(),
            "error": str(e),
        }
