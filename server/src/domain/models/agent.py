from pydantic import BaseModel
from enum import Enum
from typing import List, Optional, Dict

class AgentType(str, Enum):
    "에이전트 타입"
    SUPERVISOR = "supervisor"
    RAG = "rag"
    CODE = "code"
    SEARCH = "search"
    GENERAL = "general"
    
    def get_emoji(self) -> str:
        return {
            self.SUPERVISOR: "🎯",
            self.RAG: "📚",
            self.SEARCH: "🔍",
            self.GENERAL: "💬"

    
    def get_description(serf)-> str:
        descriptions = {
            self.SUPERVISOR: "사용자 의도 분석 및 적절한 에이전트 선택",
            self.RAG: "내부 기본 문서탐색",
            self.SEARCH: "외부 인터넷 검색",
            self.GENERAL: "일반 대화 및 질의응답"
        }
        return descriptions.get(self, "알 수 없는 에이전트")
    

class Classification(BaseModel):
    """분류 결과값 이값은 supervisor에서 사용되는 값"""
    query_type: str 
    agent_types: List[str] # ["rag", "search", "general"]
    required_capabilities: List[str]
    priority: int # 1-5
    estimated_complexity: str # "low", "medium", "high"
    needs_multi_agent: bool
    reasoning: str
    

class AgentRequest(BaseModel):
    """에이전트 요청시 모델"""
    query: str
    context: Optional[Dict] = None
    session_id: Optional[str] = None

class AgentResponse(BaseModel):
    """에이전트 응답시 사용되는 모델"""
    content: str
    agent_type: AgentType
    metadata: Dict = {}s