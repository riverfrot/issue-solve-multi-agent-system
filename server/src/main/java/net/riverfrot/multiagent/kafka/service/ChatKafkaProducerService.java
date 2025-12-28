package net.riverfrot.multiagent.kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.riverfrot.multiagent.kafka.dto.ChatKafkaRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Spring Boot에서 Python AI Agent로 채팅 요청을 Kafka를 통해 전송하는 서비스
 * correlation ID 기반 비동기 요청 처리
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
     * 채팅 요청을 Kafka로 비동기 전송
     * @return correlation ID for tracking async response
     */
    public CompletableFuture<String> sendChatRequest(String sessionId, String userId, String message) {
        String correlationId = generateCorrelationId();
        
        try {
            ChatKafkaRequest request = ChatKafkaRequest.create(correlationId, sessionId, userId, message);
            String jsonPayload = objectMapper.writeValueAsString(request);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    chatRequestTopic, 
                    sessionId, // 파티션 키: 세션별로 순서 보장
                    jsonPayload
            );
            
            return future.handle((result, throwable) -> {
                if (throwable != null) {
                    throw new RuntimeException("Failed to send chat request to Kafka", throwable);
                }
                return correlationId;
            });
            
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(
                    new RuntimeException("Failed to serialize chat request", e)
            );
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