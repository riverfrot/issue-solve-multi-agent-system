"""
Kafka Consumer for receiving chat requests from Spring Boot
"""
import json
import asyncio
import logging
from typing import Callable, Optional
from kafka import KafkaConsumer
from kafka.errors import KafkaError
from .models import ChatKafkaRequest, ChatKafkaResponse
from .producer import ChatKafkaProducer

logger = logging.getLogger(__name__)


class ChatKafkaConsumer:
    """채팅 요청을 받는 Kafka Consumer"""
    
    def __init__(
        self,
        bootstrap_servers: str = "localhost:9092",
        group_id: str = "python-ai-agent",
        request_topic: str = "chat-request",
        auto_offset_reset: str = "latest"
    ):
        self.bootstrap_servers = bootstrap_servers
        self.group_id = group_id
        self.request_topic = request_topic
        self.auto_offset_reset = auto_offset_reset
        self.consumer: Optional[KafkaConsumer] = None
        self.producer: Optional[ChatKafkaProducer] = None
        self.message_handler: Optional[Callable[[ChatKafkaRequest], str]] = None
        self.running = False
        
    def set_message_handler(self, handler: Callable[[ChatKafkaRequest], str]):
        """메시지 처리 핸들러 설정"""
        self.message_handler = handler
        
    def set_producer(self, producer: ChatKafkaProducer):
        """응답 전송용 Producer 설정"""
        self.producer = producer
    
    async def start(self):
        """Consumer 시작"""
        try:
            logger.info(f"Starting Kafka Consumer - Topic: {self.request_topic}, Group: {self.group_id}")
            
            self.consumer = KafkaConsumer(
                self.request_topic,
                bootstrap_servers=self.bootstrap_servers,
                group_id=self.group_id,
                auto_offset_reset=self.auto_offset_reset,
                enable_auto_commit=False,
                value_deserializer=lambda x: x.decode('utf-8') if x else None,
                consumer_timeout_ms=1000  # 1초 타임아웃으로 논블로킹 동작
            )
            
            self.running = True
            logger.info("Kafka Consumer started successfully")
            
            # 논블로킹 메시지 처리 루프
            await self._consume_messages()
            
        except Exception as e:
            logger.error(f"Failed to start Kafka Consumer: {e}")
            raise
    
    async def _consume_messages(self):
        """메시지 소비 루프"""
        logger.info("Starting message consumption loop")
        
        while self.running:
            try:
                # 논블로킹으로 메시지 폴링
                message_batch = self.consumer.poll(timeout_ms=1000, max_records=10)
                
                if message_batch:
                    for topic_partition, messages in message_batch.items():
                        for message in messages:
                            await self._process_message(message)
                            
                    # 메시지 처리 완료 후 커밋
                    self.consumer.commit()
                    
                # 다른 비동기 작업에게 제어권 양보
                await asyncio.sleep(0.1)
                
            except KafkaError as e:
                logger.error(f"Kafka error during message consumption: {e}")
                await asyncio.sleep(1)  # 에러 시 잠시 대기
                
            except Exception as e:
                logger.error(f"Unexpected error during message consumption: {e}")
                await asyncio.sleep(1)
    
    async def _process_message(self, message):
        """개별 메시지 처리"""
        try:
            logger.info(f"Received message: {message.value}")
            
            # JSON 파싱
            request_data = json.loads(message.value)
            chat_request = ChatKafkaRequest(**request_data)
            
            logger.info(f"Processing chat request - Correlation ID: {chat_request.correlation_id}")
            
            # 메시지 처리
            if self.message_handler:
                response_message = await self._handle_message_async(chat_request)
                
                # 응답 전송
                if self.producer:
                    response = ChatKafkaResponse.success(
                        correlation_id=chat_request.correlation_id,
                        session_id=chat_request.session_id,
                        message=response_message,
                        agent_type="GENERAL"
                    )
                    await self.producer.send_response(response)
                    logger.info(f"Response sent for correlation ID: {chat_request.correlation_id}")
                else:
                    logger.warning("No producer configured - response not sent")
            else:
                logger.warning("No message handler configured")
                
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON message: {e}")
            
        except Exception as e:
            logger.error(f"Failed to process message: {e}")
            
            # 에러 응답 전송 (가능한 경우)
            try:
                if hasattr(self, '_last_correlation_id') and self.producer:
                    error_response = ChatKafkaResponse.error(
                        correlation_id=getattr(self, '_last_correlation_id', 'unknown'),
                        session_id=getattr(self, '_last_session_id', 'unknown'),
                        error_message=str(e),
                        error_code="PROCESSING_ERROR"
                    )
                    await self.producer.send_response(error_response)
            except:
                pass  # 에러 응답 전송 실패는 무시
    
    async def _handle_message_async(self, chat_request: ChatKafkaRequest) -> str:
        """비동기로 메시지 처리"""
        # correlation_id와 session_id를 에러 처리용으로 저장
        self._last_correlation_id = chat_request.correlation_id
        self._last_session_id = chat_request.session_id
        
        if self.message_handler:
            # 동기 핸들러를 비동기로 실행
            if asyncio.iscoroutinefunction(self.message_handler):
                return await self.message_handler(chat_request)
            else:
                # CPU 집약적 작업은 별도 스레드에서 실행
                loop = asyncio.get_event_loop()
                return await loop.run_in_executor(None, self.message_handler, chat_request)
        else:
            return f"Echo: {chat_request.message} (processed by Python AI Agent)"
    
    async def stop(self):
        """Consumer 중지"""
        logger.info("Stopping Kafka Consumer")
        self.running = False
        
        if self.consumer:
            self.consumer.close()
            logger.info("Kafka Consumer stopped")
    
    def __del__(self):
        """소멸자 - 리소스 정리"""
        if hasattr(self, 'consumer') and self.consumer:
            try:
                self.consumer.close()
            except:
                pass