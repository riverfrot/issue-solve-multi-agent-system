from functools import lru_cache
from infrastructure.llm.openai_client import OpenAIClient
from service.chat_service import ChatService
from config.settings import settings


@lru_cache()
def get_openai_client() -> OpenAIClient:
    """OpenAI 클라이언트 의존성"""
    return OpenAIClient(api_key=settings.openai_api_key)


@lru_cache()
def get_chat_service() -> ChatService:
    """채팅 서비스 의존성"""
    openai_client = get_openai_client()
    return ChatService(openai_client=openai_client)