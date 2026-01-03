"""
Kafka Producer for sending chat responses to Spring Boot
"""
import json
import asyncio
import logging
from typing import Optional, Dict, Any
from kafka import KafkaProducer
from kafka.errors import KafkaError
from opentelemetry import trace
from opentelemetry.trace.propagation.tracecontext import TraceContextTextMapPropagator
from .models import ChatKafkaResponse
from ..config.otel_config import get_tracer

logger = logging.getLogger(__name__)


class ChatKafkaProducer:
    """채팅 응답을 보내는 Kafka Producer"""
    
    def __init__(
        self,
        bootstrap_servers: str = "localhost:9092",
        response_topic: str = "chat-response"
    ):
        self.bootstrap_servers = bootstrap_servers
        self.response_topic = response_topic
        self.producer: Optional[KafkaProducer] = None
        
        # OpenTelemetry components
        self.tracer = get_tracer(__name__)
        self.propagator = TraceContextTextMapPropagator()
        
    async def start(self):
        """Producer 시작"""
        try:
            logger.info(f"Starting Kafka Producer - Topic: {self.response_topic}")
            
            self.producer = KafkaProducer(
                bootstrap_servers=self.bootstrap_servers,
                value_serializer=lambda x: json.dumps(x, ensure_ascii=False).encode('utf-8'),
                key_serializer=lambda x: x.encode('utf-8') if x else None,
                acks='all',  # 모든 복제본에서 확인
                retries=3,
                batch_size=16384,
                linger_ms=1,
                buffer_memory=33554432,
                enable_idempotence=True,
                max_in_flight_requests_per_connection=1  # idempotence 사용시 1로 설정 필요
            )
            
            logger.info("Kafka Producer started successfully")
            
        except Exception as e:
            logger.error(f"Failed to start Kafka Producer: {e}")
            raise
    
    async def send_response(self, response: ChatKafkaResponse) -> bool:
        """채팅 응답 전송"""
        # Start a new span for message sending
        with self.tracer.start_as_current_span(
            "kafka.producer.send_response",
            attributes={
                "messaging.system": "kafka",
                "messaging.destination": self.response_topic,
                "messaging.operation": "send",
                "messaging.correlation_id": response.correlation_id,
                "chat.session_id": response.session_id,
                "chat.agent_type": response.agent_type,
            }
        ) as span:
            try:
                if not self.producer:
                    raise RuntimeError("Producer not started")
                
                # Pydantic 모델을 dict로 변환
                response_dict = response.model_dump()
                
                # correlation_id를 key로 사용 (파티셔닝을 위해)
                key = response.correlation_id
                
                logger.info(f"Sending response - Correlation ID: {response.correlation_id}")
                logger.debug(f"Response data: {response_dict}")
                
                # Inject trace context into headers
                headers = self._create_headers_with_trace_context()
                
                # 비동기로 메시지 전송
                future = self.producer.send(
                    topic=self.response_topic,
                    value=response_dict,
                    key=key,
                    headers=headers
                )
                
                # 전송 완료까지 대기
                record_metadata = await self._wait_for_send(future)
                
                # Add success attributes
                span.set_attribute("messaging.kafka.partition", record_metadata.partition)
                span.set_attribute("messaging.kafka.offset", record_metadata.offset)
                span.set_attribute("chat.processing.status", "sent")
                
                logger.info(
                    f"Response sent successfully - "
                    f"Topic: {record_metadata.topic}, "
                    f"Partition: {record_metadata.partition}, "
                    f"Offset: {record_metadata.offset}"
                )
                
                return True
            
            except KafkaError as e:
                logger.error(f"Kafka error sending response: {e}")
                span.record_exception(e)
                span.set_attribute("chat.processing.status", "kafka_error")
                return False
                
            except Exception as e:
                logger.error(f"Unexpected error sending response: {e}")
                span.record_exception(e)
                span.set_attribute("chat.processing.status", "error")
                return False
    
    def _create_headers_with_trace_context(self) -> list[tuple[str, bytes]]:
        """Create Kafka headers with injected trace context."""
        headers_dict: Dict[str, str] = {}
        
        # Inject current trace context into headers
        self.propagator.inject(headers_dict)
        
        # Convert to Kafka headers format
        return [(key.encode('utf-8'), value.encode('utf-8')) 
                for key, value in headers_dict.items()]
    
    async def _wait_for_send(self, future):
        """비동기로 전송 완료 대기"""
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(None, future.get, 10)  # 10초 타임아웃
    
    async def send_streaming_response(
        self,
        correlation_id: str,
        session_id: str,
        message_chunks: list[str],
        agent_type: str = "GENERAL"
    ) -> bool:
        """스트리밍 응답 전송 (여러 청크로 나누어 전송)"""
        try:
            total_chunks = len(message_chunks)
            
            for i, chunk in enumerate(message_chunks):
                is_final = (i == total_chunks - 1)
                
                response = ChatKafkaResponse.success(
                    correlation_id=correlation_id,
                    session_id=session_id,
                    message=chunk,
                    agent_type=agent_type,
                    is_final=is_final,
                    chunk_index=i
                )
                
                success = await self.send_response(response)
                if not success:
                    logger.error(f"Failed to send chunk {i} for correlation ID: {correlation_id}")
                    return False
                
                # 청크 간 짧은 지연 (스트리밍 효과)
                if not is_final:
                    await asyncio.sleep(0.1)
            
            logger.info(f"Streaming response completed - Total chunks: {total_chunks}")
            return True
            
        except Exception as e:
            logger.error(f"Error sending streaming response: {e}")
            return False
    
    async def send_error_response(
        self,
        correlation_id: str,
        session_id: str,
        error_message: str,
        error_code: str = "INTERNAL_ERROR"
    ) -> bool:
        """에러 응답 전송"""
        try:
            error_response = ChatKafkaResponse.error(
                correlation_id=correlation_id,
                session_id=session_id,
                error_message=error_message,
                error_code=error_code
            )
            
            return await self.send_response(error_response)
            
        except Exception as e:
            logger.error(f"Error sending error response: {e}")
            return False
    
    async def stop(self):
        """Producer 중지"""
        logger.info("Stopping Kafka Producer")
        
        if self.producer:
            # 대기 중인 모든 메시지 전송 완료 대기
            self.producer.flush(timeout=10)
            self.producer.close()
            logger.info("Kafka Producer stopped")
    
    def __del__(self):
        """소멸자 - 리소스 정리"""
        if hasattr(self, 'producer') and self.producer:
            try:
                self.producer.close()
            except:
                pass