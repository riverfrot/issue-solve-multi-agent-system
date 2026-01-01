"""
Kafka Producer for sending chat responses to Spring Boot
"""
import json
import asyncio
import logging
from typing import Optional
from kafka import KafkaProducer
from kafka.errors import KafkaError
from .models import ChatKafkaResponse

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
        try:
            if not self.producer:
                raise RuntimeError("Producer not started")
            
            # Pydantic 모델을 dict로 변환
            response_dict = response.model_dump()
            
            # correlation_id를 key로 사용 (파티셔닝을 위해)
            key = response.correlation_id
            
            logger.info(f"Sending response - Correlation ID: {response.correlation_id}")
            logger.debug(f"Response data: {response_dict}")
            
            # 비동기로 메시지 전송
            future = self.producer.send(
                topic=self.response_topic,
                value=response_dict,
                key=key
            )
            
            # 전송 완료까지 대기
            record_metadata = await self._wait_for_send(future)
            
            logger.info(
                f"Response sent successfully - "
                f"Topic: {record_metadata.topic}, "
                f"Partition: {record_metadata.partition}, "
                f"Offset: {record_metadata.offset}"
            )
            
            return True
            
        except KafkaError as e:
            logger.error(f"Kafka error sending response: {e}")
            return False
            
        except Exception as e:
            logger.error(f"Unexpected error sending response: {e}")
            return False
    
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