package net.riverfrot.multiagent.kafka.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Python AI Agent에서 Kafka를 통해 전송되는 채팅 응답 메시지
 * correlation ID를 통한 비동기 요청-응답 매칭
 */
public record ChatKafkaResponse(
        @JsonProperty("correlation_id")
        String correlationId,
        
        @JsonProperty("session_id")
        String sessionId,
        
        @JsonProperty("message") 
        String message,
        
        @JsonProperty("agent_type")
        String agentType,
        
        @JsonProperty("is_final")
        boolean isFinal, // 스트리밍의 마지막 청크 여부
        
        @JsonProperty("chunk_index")
        Integer chunkIndex, // 청크 순서 (스트리밍용)
        
        @JsonProperty("timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime timestamp,
        
        @JsonProperty("metadata")
        Map<String, Object> metadata,
        
        @JsonProperty("error")
        ErrorInfo error
) {
    
    public static ChatKafkaResponse success(String correlationId, String sessionId, 
                                          String message, String agentType, boolean isFinal, Integer chunkIndex) {
        return new ChatKafkaResponse(
                correlationId,
                sessionId,
                message,
                agentType,
                isFinal,
                chunkIndex,
                LocalDateTime.now(),
                Map.of("success", true),
                null
        );
    }
    
    public static ChatKafkaResponse error(String correlationId, String sessionId, String errorMessage, String errorCode) {
        return new ChatKafkaResponse(
                correlationId,
                sessionId,
                null,
                null,
                true,
                null,
                LocalDateTime.now(),
                Map.of("success", false),
                new ErrorInfo(errorCode, errorMessage)
        );
    }
    
    public record ErrorInfo(
            @JsonProperty("code")
            String code,
            
            @JsonProperty("message") 
            String message
    ) {}
}