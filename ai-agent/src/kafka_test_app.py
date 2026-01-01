"""
Kafka 연동 테스트 애플리케이션
Spring Boot와 Python 간의 Kafka 통신을 테스트
"""
import asyncio
import logging
import signal
import sys
from typing import Optional
from kafka_integration.models import ChatKafkaRequest
from kafka_integration.service import ChatKafkaService

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger(__name__)


class KafkaTestApp:
    """Kafka 테스트 애플리케이션"""
    
    def __init__(self):
        self.kafka_service: Optional[ChatKafkaService] = None
        self.running = True
        
    def setup_signal_handlers(self):
        """시그널 핸들러 설정"""
        def signal_handler(signum, frame):
            logger.info(f"Received signal {signum}, shutting down...")
            self.running = False
        
        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)
    
    async def message_handler(self, chat_request: ChatKafkaRequest) -> str:
        """커스텀 메시지 핸들러 - 멀티에이전트 지원"""
        logger.info(f"=== Processing Chat Request ===")
        logger.info(f"Correlation ID: {chat_request.correlation_id}")
        logger.info(f"Session ID: {chat_request.session_id}")
        logger.info(f"User ID: {chat_request.user_id}")
        logger.info(f"Message: {chat_request.message}")
        logger.info(f"Timestamp: {chat_request.timestamp}")
        logger.info(f"Response Type: {chat_request.metadata.response_type}")
        
        # 멀티에이전트 시스템 사용 여부 확인
        use_multiagent = getattr(chat_request.metadata, 'use_multiagent', False)
        agent_type = getattr(chat_request.metadata, 'agent_type', 'GENERAL')
        
        if use_multiagent:
            logger.info(f"🤖 Using MultiAgent System - Requested Agent: {agent_type}")
            
            # 멀티에이전트 시스템으로 요청 처리
            from kafka_integration.multiagent import get_multiagent_system
            multiagent = get_multiagent_system()
            
            response, used_agent_type = await multiagent.process_request(
                chat_request.message, 
                agent_type
            )
            
            logger.info(f"✅ MultiAgent Response - Used Agent: {used_agent_type}")
            return response
            
        else:
            logger.info("🔧 Using Simple AI Handler")
            # 기존의 간단한 AI 시뮬레이션 (하위 호환성)
            await asyncio.sleep(0.5)  # AI 처리 시간 시뮬레이션
            
            if "안녕" in chat_request.message or "hello" in chat_request.message.lower():
                return f"안녕하세요! Python AI Agent입니다. '{chat_request.message}'라고 말씀하셨군요!"
            elif "날씨" in chat_request.message:
                return "죄송합니다. 현재 날씨 정보 기능은 구현되지 않았습니다. 하지만 좋은 하루 보내세요!"
            elif "도움" in chat_request.message or "help" in chat_request.message.lower():
                return "Python AI Agent가 도움을 드리겠습니다. 무엇을 도와드릴까요?"
            else:
                return f"Python AI Agent에서 처리됨: {chat_request.message} [세션: {chat_request.session_id}]"
    
    async def start(self):
        """애플리케이션 시작"""
        try:
            logger.info("🚀 Starting Kafka Test Application")
            
            # Kafka 서비스 초기화
            self.kafka_service = ChatKafkaService(
                bootstrap_servers="localhost:9092",
                consumer_group_id="python-ai-agent",
                request_topic="chat-request",
                response_topic="chat-response"
            )
            
            # Kafka 서비스 시작 (커스텀 메시지 핸들러와 함께)
            await self.kafka_service.start(message_handler=self.message_handler)
            
            logger.info("✅ Kafka Test Application started successfully")
            logger.info("📝 Waiting for messages from Spring Boot...")
            logger.info("🛑 Press Ctrl+C to stop")
            
            # 상태 체크
            health = await self.kafka_service.health_check()
            logger.info(f"📊 Service Status: {health}")
            
            # 메인 루프
            await self._main_loop()
            
        except Exception as e:
            logger.error(f"❌ Failed to start application: {e}")
            raise
    
    async def _main_loop(self):
        """메인 실행 루프"""
        try:
            # 서비스가 실행 중일 때까지 대기
            while self.running and self.kafka_service and self.kafka_service.running:
                await asyncio.sleep(1)
                
                # 주기적으로 상태 체크 (30초마다)
                if hasattr(self, '_last_health_check'):
                    if asyncio.get_event_loop().time() - self._last_health_check > 30:
                        await self._periodic_health_check()
                else:
                    self._last_health_check = asyncio.get_event_loop().time()
                    
        except KeyboardInterrupt:
            logger.info("🛑 Received keyboard interrupt")
        except Exception as e:
            logger.error(f"❌ Error in main loop: {e}")
        finally:
            await self.stop()
    
    async def _periodic_health_check(self):
        """주기적 상태 체크"""
        if self.kafka_service:
            health = await self.kafka_service.health_check()
            logger.info(f"💓 Health Check: {health['kafka_service']} - "
                       f"Producer: {health['producer']}, Consumer: {health['consumer']}")
        self._last_health_check = asyncio.get_event_loop().time()
    
    async def stop(self):
        """애플리케이션 종료"""
        logger.info("🛑 Stopping Kafka Test Application")
        self.running = False
        
        if self.kafka_service:
            await self.kafka_service.stop()
            
        logger.info("✅ Kafka Test Application stopped")


async def main():
    """메인 함수"""
    app = KafkaTestApp()
    app.setup_signal_handlers()
    
    try:
        await app.start()
    except KeyboardInterrupt:
        logger.info("🛑 Application interrupted by user")
    except Exception as e:
        logger.error(f"❌ Application failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    # Python 3.11+ 호환성을 위한 이벤트 루프 설정
    try:
        # Windows에서의 이벤트 루프 정책 설정
        if sys.platform.startswith('win'):
            asyncio.set_event_loop_policy(asyncio.WindowsProactorEventLoopPolicy())
    except AttributeError:
        pass
    
    # 애플리케이션 실행
    asyncio.run(main())