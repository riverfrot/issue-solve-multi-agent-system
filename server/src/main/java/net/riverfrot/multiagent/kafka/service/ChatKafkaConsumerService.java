package net.riverfrot.multiagent.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.riverfrot.multiagent.kafka.dto.ChatKafkaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka Consumer 서비스 (Virtual Thread 활용)
 * Python AI Agent로부터 chat-response 토픽의 응답을 처리
 * Correlation ID 기반 비동기 응답 매칭 시스템
 * Spring 프레임워크가 자동으로 Virtual Thread를 관리하도록 변경
 */
@Service
public class ChatKafkaConsumerService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatKafkaConsumerService.class);
    
    private final ObjectMapper objectMapper;
    
    // 응답 대기용 Map (Latch 역할)
    private final ConcurrentHashMap<String, CompletableFuture<ChatKafkaResponse>> pendingResponses
            = new ConcurrentHashMap<>();
    
    public ChatKafkaConsumerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Kafka Listener
     * spring.threads.virtual.enabled=true 설정 시, 프레임워크가 알아서 Virtual Thread에서 실행합니다.
     * 수동으로 Thread.startVirtualThread를 할 필요가 없습니다.
     */
    @KafkaListener(
            topics = "${multiagent.kafka.topics.chat-response}",
            groupId = "${spring.kafka.consumer.group-id}"
            // containerFactory 설정은 application.properties에서 전역 설정 추천
    )
    public void consumeChatResponse(
            @Payload String jsonPayload,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Kafka message received - Topic: {}, Partition: {}, Offset: {}, Key: {}", 
                    topic, partition, offset, messageKey);
            
            // 바로 로직 실행 (이미 Virtual Thread 환경임)
            processMessage(jsonPayload, messageKey, partition);
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing Kafka response: {}", e.getMessage(), e);
            // 에러가 나도 offset은 커밋하여 루프 방지 (필요 시 DLT 처리)
            acknowledgment.acknowledge(); 
        }
    }
    
    private void processMessage(String jsonPayload, String messageKey, int partition) throws Exception {
        ChatKafkaResponse response = objectMapper.readValue(jsonPayload, ChatKafkaResponse.class);
        String correlationId = response.correlationId();

        log.debug("Processing response - correlationId: {}, isFinal: {}", correlationId, response.isFinal());

        CompletableFuture<ChatKafkaResponse> pendingFuture = pendingResponses.get(correlationId);

        if (pendingFuture != null) {
            if (response.isFinal()) {
                // 최종 응답: 대기 중인 Service 스레드에게 값을 넘겨줌
                pendingFuture.complete(response);
                pendingResponses.remove(correlationId);
                log.info("Completed request for correlationId: {}", correlationId);
            } else {
                // Streaming chunk 처리 (TODO: SSE 연결)
                log.debug("Streaming chunk for: {}", correlationId);
            }
        } else {
            log.warn("No pending waiter for correlationId: {} (timeout or duplicate)", correlationId);
        }
    }
    
    /**
     * 특정 correlation ID에 대한 응답 대기 등록
     * Producer에서 요청 전송 후 이 메서드를 호출하여 응답 대기
     */
    public CompletableFuture<ChatKafkaResponse> waitForResponse(String correlationId) {
        CompletableFuture<ChatKafkaResponse> future = new CompletableFuture<>();
        pendingResponses.put(correlationId, future);
        
        // 메모리 누수 방지를 위해 30초 후 맵에서 자동 제거하는 안전장치만 추가 (타임아웃은 Service에서 처리)
        future.orTimeout(35, java.util.concurrent.TimeUnit.SECONDS)
              .exceptionally(throwable -> {
                  pendingResponses.remove(correlationId);
                  return null;
              });

        return future;
    }
    
    /**
     * 대기 중인 응답 요청 취소
     */
    public void cancelWaitForResponse(String correlationId) {
        pendingResponses.remove(correlationId);
    }
    
    /**
     * 현재 대기 중인 요청 수 조회 (모니터링용)
     */
    public int getPendingResponseCount() {
        return pendingResponses.size();
    }
}