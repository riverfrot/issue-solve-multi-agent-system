package net.riverfrot.multiagent.kafka.service;

import net.riverfrot.multiagent.kafka.dto.ChatKafkaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka 채팅 통합 서비스 Virtual Thread
 * Producer와 Consumer를 연동하여 완전한 요청-응답 플로우 제공
 */
@Service
public class ChatKafkaService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatKafkaService.class);
    
    private final ChatKafkaProducerService producerService;
    private final ChatKafkaConsumerService consumerService;
    
    public ChatKafkaService(
            ChatKafkaProducerService producerService,
            ChatKafkaConsumerService consumerService
    ) {
        this.producerService = producerService;
        this.consumerService = consumerService;
    }
    
    /**
     * 채팅 요청 전송 및 응답 대기 (Virtual Thread)
     */
    public ChatKafkaResponse sendChatAndWaitResponse(String sessionId, String userId, String message) {
        String correlationId = null;
        try {
            log.info("Starting chat request flow - session: {}, user: {}", sessionId, userId);
            
            correlationId = producerService.sendChatRequest(sessionId, userId, message);
            
            log.info("Chat request sent successfully - correlationId: {}", correlationId);
            
            ChatKafkaResponse response = consumerService.waitForResponse(correlationId)
                    .get(30, TimeUnit.SECONDS); // 30초 타임아웃
            
            log.info("Chat response received successfully - correlationId: {}, isFinal: {}", 
                    response.correlationId(), response.isFinal());
            
            return response;
            
        } catch (TimeoutException e) {
            log.warn("Timeout waiting for response. correlationId: {}", correlationId);
            if (correlationId != null) {
                consumerService.cancelWaitForResponse(correlationId);
            }
            throw new RuntimeException("Chat request timeout - no response from AI agent", e);
        } catch (Exception e) {
            log.error("Error in chat request flow: {}", e.getMessage(), e);
            throw new RuntimeException("Chat service error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Multiagent 채팅 요청 전송 및 응답 대기 (Virtual Thread)
     */
    public ChatKafkaResponse sendChatWithMultiagent(String sessionId, String userId, String message, String agentType, boolean useMultiagent) {
        String correlationId = null;
        try {
            log.info("Starting multiagent chat request flow - session: {}, user: {}, agentType: {}, useMultiagent: {}", 
                    sessionId, userId, agentType, useMultiagent);
            
            correlationId = producerService.sendChatRequestWithMultiagent(sessionId, userId, message, agentType, useMultiagent);
            
            log.info("Multiagent chat request sent successfully - correlationId: {}", correlationId);
            
            ChatKafkaResponse response = consumerService.waitForResponse(correlationId)
                    .get(30, TimeUnit.SECONDS); // 30초 타임아웃
            
            log.info("Multiagent chat response received successfully - correlationId: {}, agentType: {}, isFinal: {}", 
                    response.correlationId(), response.agentType(), response.isFinal());
            
            return response;
            
        } catch (TimeoutException e) {
            log.warn("Timeout waiting for multiagent response. correlationId: {}", correlationId);
            if (correlationId != null) {
                consumerService.cancelWaitForResponse(correlationId);
            }
            throw new RuntimeException("Multiagent chat request timeout - no response from AI agent", e);
        } catch (Exception e) {
            log.error("Error in multiagent chat request flow: {}", e.getMessage(), e);
            throw new RuntimeException("Multiagent chat service error: " + e.getMessage(), e);
        }
    }
    
    /**
     * 서비스 상태 정보 조회
     */
    public java.util.Map<String, Object> getServiceStatus() {
        return java.util.Map.of(
                "service", "ChatKafkaService",
                "producer", producerService.getClass().getSimpleName(),
                "consumer", consumerService.getClass().getSimpleName(),
                "pendingResponses", consumerService.getPendingResponseCount(),
                "threadingModel", "VirtualThread",
                "blockingStyle", "Synchronous code on Virtual Thread"
        );
    }
}