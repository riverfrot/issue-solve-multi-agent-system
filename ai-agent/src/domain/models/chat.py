from pydantic import BaseModel, Field
from typing import Optional, List, Dict
from datetime import datetime
import uuid


class ChatMessage(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    session_id: str
    role: str  # "user" or "assistant"
    content: str
    agent_type: Optional[str] = None
    metadata: Dict = Field(default_factory=dict)
    timestamp: datetime = Field(default_factory=datetime.now)

    def validate_content(self):
        """기본적인 예외처리"""
        if not self.content or len(self.content.strip()) == 0:
            raise ValueError("Content cannot be empty")

        if len(self.content) > 10000:
            raise ValueError("Content too long (max 10000 chars)")

    def is_from_user(self) -> bool:
        return self.role == "user"

    def is_from_assistant(self) -> bool:
        return self.role == "assistant"


class ChatRequest(BaseModel):
    message: str
    session_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    stream: bool = False


class ChatResponse(BaseModel):
    message: str
    session_id: str
    agent_used: str
    thinking_process: List[str] = Field(default_factory=list)
    metadata: Dict = Field(default_factory=dict)


class Conversation(BaseModel):
    session_id: str
    messages: List[ChatMessage] = Field(default_factory=list)
    user_id: Optional[str] = None
    started_at: datetime = Field(default_factory=datetime.now)
    last_activity: datetime = Field(default_factory=datetime.now)

    def add_message(self, message: ChatMessage):
        message.validate_content()
        self.messages.append(message)
        self.last_activity = datetime.now()

    def get_recent_messages(self, limit: int = 10) -> List[ChatMessage]:
        return self.messages[-limit:]

    def is_active(self) -> bool:
        # 30분 이내 활동이 있으면 활성
        inactive_minutes = (datetime.now() - self.last_activity).seconds / 60
        return inactive_minutes < 30
