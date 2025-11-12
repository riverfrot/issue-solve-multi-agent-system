from typing import List, Dict, Optional, Any, Literal
from pydantic import BaseModel, Field
from enum import Enum
from .chat import ChatMessage
from .agent import AgentType


class GraphState(BaseModel):
    # 입력 정보
    query: str = Field(description="사용자 질문")
    session_id: str = Field(description="세션 ID")

    # 대화 히스토리
    history: List[ChatMessage] = Field(
        default_factory=list, description="대화 히스토리"
    )

    # 에이전트 라우팅 정보
    current_agent: Optional[AgentType] = Field(
        None, description="현재 처리 중인 에이전트"
    )
    agent_route: List[AgentType] = Field(
        default_factory=list, description="에이전트 실행 경로"
    )

    # 처리 결과
    response: str = Field(default="", description="최종 응답")
    intermediate_responses: Dict[str, str] = Field(
        default_factory=dict, description="중간 응답들"
    )

    # 메타데이터
    metadata: Dict[str, Any] = Field(default_factory=dict, description="메타데이터")
    reasoning: List[str] = Field(default_factory=list, description="추론 과정")

    # 에러 처리
    errors: List[str] = Field(default_factory=list, description="에러 로그")
    retry_count: int = Field(default=0, description="재시도 횟수")

    # 플래그
    requires_multi_agent: bool = Field(
        default=False, description="다중 에이전트 필요 여부"
    )
    is_complete: bool = Field(default=False, description="처리 완료 여부")
    needs_human_review: bool = Field(default=False, description="인간 검토 필요 여부")

    class Config:
        arbitrary_types_allowed = True


class AgentDecision(str, Enum):
    CONTINUE = "continue"
    ROUTE_TO_RAG = "route_to_rag"
    ROUTE_TO_CODE = "route_to_code"
    ROUTE_TO_SEARCH = "route_to_search"
    ROUTE_TO_GENERAL = "route_to_general"
    AGGREGATE = "aggregate"
    FINISH = "finish"
    ERROR = "error"


class NodeConfig(BaseModel):
    name: str
    agent_type: AgentType
    max_retries: int = 3
    timeout_seconds: int = 30
    fallback_agent: Optional[AgentType] = None
    requires_previous_context: bool = False


class WorkflowConfig(BaseModel):
    name: str
    description: str
    max_iterations: int = 10
    enable_parallel_execution: bool = False
    fallback_to_general: bool = True
    log_level: Literal["DEBUG", "INFO", "WARNING", "ERROR"] = "INFO"
