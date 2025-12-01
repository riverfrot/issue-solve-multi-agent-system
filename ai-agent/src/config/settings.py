"""
Application Settings
"""

from pydantic_settings import BaseSettings
from pydantic import ConfigDict
from typing import Optional
import os
from pathlib import Path


class Settings(BaseSettings):
    """
    애플리케이션 설정
    """

    # API Keys
    openai_api_key: str
    tavily_api_key: Optional[str] = None

    # Server Configuration
    host: str = "0.0.0.0"
    port: int = 8000
    debug: bool = False

    # CORS
    cors_origins: list = ["http://localhost:3000", "http://127.0.0.1:3000"]

    # Logging
    log_level: str = "INFO"

    # Application Info
    app_name: str = "Multi-Agent Chatbot"
    app_version: str = "1.0.0"
    app_description: str = "Vue.js + FastAPI 기반 멀티 에이전트 챗봇 시스템"

    # OpenAI Settings
    openai_model: str = "gpt-4"
    openai_temperature: float = 0.7
    openai_max_tokens: int = 2000

    # Agent Settings
    max_message_length: int = 10000
    session_timeout_minutes: int = 30

    model_config = ConfigDict(
        env_file=str(Path(__file__).parent.parent.parent / ".env"),
        env_file_encoding="utf-8",
        extra="ignore"
    )


# 전역 설정 인스턴스
settings = Settings()
