package net.riverfrot.multiagent.kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.riverfrot.multiagent.kafka.dto.ChatKafkaRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Spring Boot에서 Python AI Agent로 채팅 요청을 Kafka를 통해 전송하는 서비스 (Virtual Thread 활용)
 * correlation ID 기반 동기 처리로 변경 (Virtual Thread 친화적)
 * CompletableFuture 체이닝 대신 직접 블로킹으로 가독성 향상
 */
@Service
public class ChatKafkaProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String chatRequestTopic;
    
    public ChatKafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${multiagent.kafka.topics.chat-request:chat-request}") String chatRequestTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.chatRequestTopic = chatRequestTopic;
    }
    
    /**
     * 채팅 요청을 Kafka로 전송 (Virtual Thread 친화적 - 동기 방식)
     * CompletableFuture를 리턴하지 않고, 전송 완료 후 ID를 직접 리턴
     * @return correlation ID for tracking response
     */
    public String sendChatRequest(String sessionId, String userId, String message) {
        String correlationId = generateCorrelationId();
        
        try {
            ChatKafkaRequest request = ChatKafkaRequest.create(correlationId, sessionId, userId, message);
            String jsonPayload = objectMapper.writeValueAsString(request);
            
            // Virtual Thread에서는 여기서 블로킹(.get)해도 비용이 거의 0에 가깝습니다.
            // 복잡한 Future 체이닝 대신 그냥 기다립니다.
            kafkaTemplate.send(chatRequestTopic, sessionId, jsonPayload)
                    .get(5, TimeUnit.SECONDS); // Kafka 전송 자체는 빠르므로 5초면 충분
            
            return correlationId;
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize chat request", e);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to send chat request to Kafka", e);
        }
    }
    
    /**
     * Multiagent 채팅 요청을 Kafka로 전송 (Virtual Thread 친화적 - 동기 방식)
     * 에이전트 타입 및 멀티에이전트 사용 여부 지원
     * @return correlation ID for tracking response
     */
    public String sendChatRequestWithMultiagent(String sessionId, String userId, String message, String agentType, boolean useMultiagent) {
        String correlationId = generateCorrelationId();
        
        try {
            ChatKafkaRequest request = ChatKafkaRequest.createWithMultiagent(
                correlationId, sessionId, userId, message, agentType, useMultiagent);
            String jsonPayload = objectMapper.writeValueAsString(request);
            
            // Virtual Thread에서는 여기서 블로킹(.get)해도 비용이 거의 0에 가깝습니다.
            kafkaTemplate.send(chatRequestTopic, sessionId, jsonPayload)
                    .get(5, TimeUnit.SECONDS); // Kafka 전송 자체는 빠르므로 5초면 충분
            
            return correlationId;
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize multiagent chat request", e);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to send multiagent chat request to Kafka", e);
        }
    }
    
    /**
     * 고유한 correlation ID 생성
     * 형식: sessionId-timestamp-uuid
     */
    private String generateCorrelationId() {
        return String.format("req_%s_%d", 
                UUID.randomUUID().toString().substring(0, 8),
                System.currentTimeMillis()
        );
    }
}