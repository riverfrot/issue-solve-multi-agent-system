from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from controller.health_controller import router as health_router
from controller.chat_controller import router as chat_router
from config.settings import settings
from config.otel_config import otel_config
from utils.logger import logger


def create_app() -> FastAPI:
    """
    FastAPI 애플리케이션 팩토리
    """
    # FastAPI 앱 생성
    app = FastAPI(
        title=settings.app_name,
        version=settings.app_version,
        description=settings.app_description,
        debug=settings.debug,
    )
    
    # OpenTelemetry 설정 (FastAPI instrumentation 포함)
    otel_config.configure_all(app)

    # CORS 설정
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # 라우터 등록
    app.include_router(health_router)
    app.include_router(chat_router)  # LangGraph 기반 멀티 에이전트 채팅 API

    # 시작 이벤트
    @app.on_event("startup")
    async def startup_event():
        logger.info(f"🚀 Starting {settings.app_name} v{settings.app_version}")
        logger.info(f"🌐 Server: http://{settings.host}:{settings.port}")
        logger.info(f"📚 API Docs: http://{settings.host}:{settings.port}/docs")
        logger.info(f"📊 OpenTelemetry: {otel_config.otel_endpoint}")

        # 의존성 검증
        # validate_dependencies()  # 추후 구현 필요

        logger.info("✅ Application started successfully with OpenTelemetry tracing")

    # 종료 이벤트
    @app.on_event("shutdown")
    async def shutdown_event():
        logger.info("🛑 Shutting down application")

    # 루트 엔드포인트
    @app.get("/")
    async def root():
        return {
            "message": f"Welcome to {settings.app_name}",
            "version": settings.app_version,
            "description": "Multi-agent chatbot powered by LangGraph",
            "docs": "/docs",
            "health": "/api/health",
            "chat": "/api/chat",
            "features": [
                "LangGraph workflow orchestration",
                "Multi-agent coordination",
                "Automatic intent analysis",
                "Document retrieval & QA",
                "Code generation & execution",
                "Web search & information gathering",
            ],
        }

    return app


# 앱 인스턴스 생성
app = create_app()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app", host=settings.host, port=settings.port, reload=settings.debug
    )
