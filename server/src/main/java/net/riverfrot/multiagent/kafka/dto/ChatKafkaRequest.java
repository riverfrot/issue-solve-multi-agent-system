package net.riverfrot.multiagent.kafka.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Kafka를 통해 Python AI Agent로 전송되는 채팅 요청 메시지
 * correlation ID를 통한 비동기 요청-응답 매칭
 */
public record ChatKafkaRequest(
        @JsonProperty("correlation_id")
        String correlationId,
        
        @JsonProperty("session_id") 
        String sessionId,
        
        @JsonProperty("user_id")
        String userId,
        
        @JsonProperty("message")
        String message,
        
        @JsonProperty("timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime timestamp,
        
        @JsonProperty("metadata")
        ChatRequestMetadata metadata
) {
    
    public static ChatKafkaRequest create(String correlationId, String sessionId, String userId, String message) {
        return new ChatKafkaRequest(
                correlationId,
                sessionId, 
                userId,
                message,
                LocalDateTime.now(),
                new ChatRequestMetadata("streaming", 30000L)
        );
    }
    
    public record ChatRequestMetadata(
            @JsonProperty("response_type")
            String responseType, // "streaming" | "batch"
            
            @JsonProperty("timeout_ms") 
            Long timeoutMs
    ) {}
}