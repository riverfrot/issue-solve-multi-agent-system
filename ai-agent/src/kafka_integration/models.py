"""
Kafka 메시지 데이터 모델
Spring Boot와 동기화된 메시지 포맷
"""
from datetime import datetime
from typing import Dict, Optional, Any
from pydantic import BaseModel, Field


class ChatRequestMetadata(BaseModel):
    """채팅 요청 메타데이터"""
    response_type: str = Field(description="응답 타입: streaming | batch")
    timeout_ms: int = Field(description="타임아웃 밀리초")
    agent_type: Optional[str] = Field("GENERAL", description="요청할 AI 에이전트 타입: GENERAL | CODE | RAG | SEARCH | SUPERVISOR")
    use_multiagent: bool = Field(False, description="멀티에이전트 시스템 사용 여부")


class ChatKafkaRequest(BaseModel):
    """Spring Boot에서 받는 채팅 요청 메시지"""
    correlation_id: str = Field(description="요청-응답 매칭을 위한 상관관계 ID")
    session_id: str = Field(description="채팅 세션 ID")
    user_id: str = Field(description="사용자 ID")
    message: str = Field(description="사용자 메시지")
    timestamp: str = Field(description="요청 시간")
    metadata: ChatRequestMetadata = Field(description="요청 메타데이터")


class ErrorInfo(BaseModel):
    """에러 정보"""
    code: str = Field(description="에러 코드")
    message: str = Field(description="에러 메시지")


class ChatKafkaResponse(BaseModel):
    """Spring Boot로 보내는 채팅 응답 메시지"""
    correlation_id: str = Field(description="요청-응답 매칭을 위한 상관관계 ID")
    session_id: str = Field(description="채팅 세션 ID")
    message: Optional[str] = Field(None, description="AI 응답 메시지")
    agent_type: Optional[str] = Field(None, description="응답한 에이전트 타입")
    is_final: bool = Field(description="스트리밍의 마지막 청크 여부")
    chunk_index: Optional[int] = Field(None, description="청크 순서 (스트리밍용)")
    timestamp: str = Field(description="응답 시간")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="응답 메타데이터")
    error: Optional[ErrorInfo] = Field(None, description="에러 정보")

    @classmethod
    def success(
        cls,
        correlation_id: str,
        session_id: str,
        message: str,
        agent_type: str = "GENERAL",
        is_final: bool = True,
        chunk_index: Optional[int] = None
    ) -> "ChatKafkaResponse":
        """성공 응답 생성"""
        return cls(
            correlation_id=correlation_id,
            session_id=session_id,
            message=message,
            agent_type=agent_type,
            is_final=is_final,
            chunk_index=chunk_index,
            timestamp=datetime.now().strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3],
            metadata={"success": True}
        )

    @classmethod
    def error(
        cls,
        correlation_id: str,
        session_id: str,
        error_message: str,
        error_code: str = "INTERNAL_ERROR"
    ) -> "ChatKafkaResponse":
        """에러 응답 생성"""
        return cls(
            correlation_id=correlation_id,
            session_id=session_id,
            is_final=True,
            timestamp=datetime.now().strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3],
            metadata={"success": False},
            error=ErrorInfo(code=error_code, message=error_message)
        )