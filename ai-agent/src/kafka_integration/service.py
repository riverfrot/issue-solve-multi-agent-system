"""
Kafka 통합 서비스
Consumer와 Producer를 관리하고 메시지 처리를 조정
"""
import asyncio
import logging
from typing import Callable, Optional
from .consumer import ChatKafkaConsumer
from .producer import ChatKafkaProducer
from .models import ChatKafkaRequest

logger = logging.getLogger(__name__)


class ChatKafkaService:
    """Kafka 통합 서비스"""
    
    def __init__(
        self,
        bootstrap_servers: str = "localhost:9092",
        consumer_group_id: str = "python-ai-agent",
        request_topic: str = "chat-request",
        response_topic: str = "chat-response"
    ):
        self.bootstrap_servers = bootstrap_servers
        self.consumer_group_id = consumer_group_id
        self.request_topic = request_topic
        self.response_topic = response_topic
        
        # Kafka 컴포넌트
        self.consumer: Optional[ChatKafkaConsumer] = None
        self.producer: Optional[ChatKafkaProducer] = None
        
        # 서비스 상태
        self.running = False
        
    async def start(self, message_handler: Optional[Callable[[ChatKafkaRequest], str]] = None):
        """Kafka 서비스 시작"""
        try:
            logger.info("Starting Kafka Service")
            
            # Producer 시작
            self.producer = ChatKafkaProducer(
                bootstrap_servers=self.bootstrap_servers,
                response_topic=self.response_topic
            )
            await self.producer.start()
            
            # Consumer 시작
            self.consumer = ChatKafkaConsumer(
                bootstrap_servers=self.bootstrap_servers,
                group_id=self.consumer_group_id,
                request_topic=self.request_topic
            )
            
            # Producer를 Consumer에 설정
            self.consumer.set_producer(self.producer)
            
            # 메시지 핸들러 설정 (제공된 경우)
            if message_handler:
                self.consumer.set_message_handler(message_handler)
            else:
                # 기본 핸들러 설정
                self.consumer.set_message_handler(self._default_message_handler)
            
            # Consumer 시작 (백그라운드에서 실행)
            self.consumer_task = asyncio.create_task(self.consumer.start())
            
            self.running = True
            logger.info("Kafka Service started successfully")
            
        except Exception as e:
            logger.error(f"Failed to start Kafka Service: {e}")
            await self.stop()
            raise
    
    def _default_message_handler(self, chat_request: ChatKafkaRequest) -> str:
        """기본 메시지 핸들러 - 단순 에코"""
        logger.info(f"Processing message with default handler: {chat_request.message}")
        return f"Echo from Python AI Agent: {chat_request.message}"
    
    async def send_response(self, response_message: str, correlation_id: str, session_id: str) -> bool:
        """직접 응답 전송"""
        if not self.producer:
            logger.error("Producer not available")
            return False
        
        from .models import ChatKafkaResponse
        response = ChatKafkaResponse.success(
            correlation_id=correlation_id,
            session_id=session_id,
            message=response_message
        )
        
        return await self.producer.send_response(response)
    
    async def send_streaming_response(
        self,
        message_chunks: list[str],
        correlation_id: str,
        session_id: str,
        agent_type: str = "GENERAL"
    ) -> bool:
        """스트리밍 응답 전송"""
        if not self.producer:
            logger.error("Producer not available")
            return False
        
        return await self.producer.send_streaming_response(
            correlation_id=correlation_id,
            session_id=session_id,
            message_chunks=message_chunks,
            agent_type=agent_type
        )
    
    async def send_error(
        self,
        error_message: str,
        correlation_id: str,
        session_id: str,
        error_code: str = "INTERNAL_ERROR"
    ) -> bool:
        """에러 응답 전송"""
        if not self.producer:
            logger.error("Producer not available")
            return False
        
        return await self.producer.send_error_response(
            correlation_id=correlation_id,
            session_id=session_id,
            error_message=error_message,
            error_code=error_code
        )
    
    async def health_check(self) -> dict:
        """서비스 상태 확인"""
        return {
            "kafka_service": "running" if self.running else "stopped",
            "producer": "connected" if self.producer else "disconnected",
            "consumer": "connected" if self.consumer else "disconnected",
            "bootstrap_servers": self.bootstrap_servers,
            "request_topic": self.request_topic,
            "response_topic": self.response_topic,
            "consumer_group": self.consumer_group_id
        }
    
    async def stop(self):
        """Kafka 서비스 중지"""
        logger.info("Stopping Kafka Service")
        self.running = False
        
        # Consumer 중지
        if hasattr(self, 'consumer_task'):
            self.consumer_task.cancel()
            try:
                await self.consumer_task
            except asyncio.CancelledError:
                pass
        
        if self.consumer:
            await self.consumer.stop()
        
        # Producer 중지
        if self.producer:
            await self.producer.stop()
        
        logger.info("Kafka Service stopped")
    
    async def wait_for_shutdown(self):
        """종료 대기"""
        if hasattr(self, 'consumer_task'):
            try:
                await self.consumer_task
            except asyncio.CancelledError:
                logger.info("Consumer task was cancelled")
            except Exception as e:
                logger.error(f"Consumer task ended with error: {e}")


# 전역 Kafka 서비스 인스턴스
kafka_service: Optional[ChatKafkaService] = None


async def get_kafka_service() -> ChatKafkaService:
    """Kafka 서비스 인스턴스 반환"""
    global kafka_service
    if not kafka_service:
        kafka_service = ChatKafkaService()
    return kafka_service


async def start_kafka_service(message_handler: Optional[Callable[[ChatKafkaRequest], str]] = None):
    """Kafka 서비스 시작"""
    service = await get_kafka_service()
    await service.start(message_handler)
    return service


async def stop_kafka_service():
    """Kafka 서비스 중지"""
    global kafka_service
    if kafka_service:
        await kafka_service.stop()
        kafka_service = None