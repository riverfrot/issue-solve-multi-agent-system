package net.riverfrot.multiagent.kafka.controller;

import net.riverfrot.multiagent.kafka.dto.ChatKafkaResponse;
import net.riverfrot.multiagent.kafka.service.ChatKafkaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Kafka 통합 테스트 컨트롤러
 * Producer + Consumer를 연동한 완전한 비동기 요청-응답 플로우 테스트
 */
@RestController
@RequestMapping("/kafka-integration-test")
public class KafkaIntegrationTestController {
    
    private final ChatKafkaService chatKafkaService;
    
    public KafkaIntegrationTestController(ChatKafkaService chatKafkaService) {
        this.chatKafkaService = chatKafkaService;
    }
    
    @GetMapping("/chat")
    public ResponseEntity<Map<String, Object>> testChatFlow(
            @RequestParam String message,
            @RequestParam(defaultValue = "test-session") String sessionId,
            @RequestParam(defaultValue = "test-user") String userId
    ) {
        
        try {
            ChatKafkaResponse response = chatKafkaService.sendChatAndWaitResponse(sessionId, userId, message);
            
            var responseMap = new java.util.HashMap<String, Object>();
            responseMap.put("success", true);
            responseMap.put("correlationId", response.correlationId());
            responseMap.put("sessionId", response.sessionId());
            responseMap.put("message", response.message());
            responseMap.put("isFinal", response.isFinal());
            responseMap.put("chunkIndex", response.chunkIndex());
            responseMap.put("timestamp", System.currentTimeMillis());
            responseMap.put("note", "Python AI Agent로부터 성공적으로 응답을 받았습니다");
            responseMap.put("threadingModel", "VirtualThread - Synchronous Style");
            return ResponseEntity.ok(responseMap);
            
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("timeout")) {
                var errorMap = new java.util.HashMap<String, Object>();
                errorMap.put("success", false);
                errorMap.put("error", "Timeout - no response from Python AI Agent");
                errorMap.put("timeout", "30 seconds");
                errorMap.put("timestamp", System.currentTimeMillis());
                errorMap.put("note", "Python AI Agent에서 응답을 받지 못했습니다. Consumer 구현이 필요합니다.");
                errorMap.put("threadingModel", "VirtualThread - Synchronous Style");
                return ResponseEntity.status(408).body(errorMap);
            } else {
                var errorMap = new java.util.HashMap<String, Object>();
                errorMap.put("success", false);
                errorMap.put("error", errorMessage != null ? errorMessage : "Unknown error occurred");
                errorMap.put("correlationId", "unknown");
                errorMap.put("timestamp", System.currentTimeMillis());
                errorMap.put("note", "Python AI Agent가 아직 구현되지 않았거나 Kafka 연결에 문제가 있습니다");
                errorMap.put("threadingModel", "VirtualThread - Synchronous Style");
                return ResponseEntity.status(500).body(errorMap);
            }
        }
    }
    
    /**
     * Kafka 서비스 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getKafkaServiceStatus() {
        Map<String, Object> status = chatKafkaService.getServiceStatus();
        
        var statusMap = new java.util.HashMap<String, Object>();
        statusMap.put("kafkaIntegration", status);
        statusMap.put("timestamp", System.currentTimeMillis());
        statusMap.put("readyForPythonAgent", true);
        return ResponseEntity.ok(statusMap);
    }
    
    /**
     * Multiagent 채팅 플로우 테스트 (선택적 에이전트 타입 지원)
     */
    @GetMapping("/multiagent-chat")
    public ResponseEntity<Map<String, Object>> testMultiagentChatFlow(
            @RequestParam String message,
            @RequestParam(defaultValue = "test-session") String sessionId,
            @RequestParam(defaultValue = "test-user") String userId,
            @RequestParam(defaultValue = "GENERAL") String agentType,
            @RequestParam(defaultValue = "true") boolean useMultiagent
    ) {
        
        try {
            ChatKafkaResponse response = chatKafkaService.sendChatWithMultiagent(
                sessionId, userId, message, agentType, useMultiagent);
            
            var responseMap = new java.util.HashMap<String, Object>();
            responseMap.put("success", true);
            responseMap.put("correlationId", response.correlationId());
            responseMap.put("sessionId", response.sessionId());
            responseMap.put("message", response.message());
            responseMap.put("agentType", response.agentType());
            responseMap.put("isFinal", response.isFinal());
            responseMap.put("chunkIndex", response.chunkIndex());
            responseMap.put("timestamp", System.currentTimeMillis());
            responseMap.put("note", "Multiagent 시스템에서 성공적으로 응답을 받았습니다");
            responseMap.put("threadingModel", "VirtualThread - Synchronous Style");
            responseMap.put("multiagentEnabled", useMultiagent);
            
            return ResponseEntity.ok(responseMap);
            
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("timeout")) {
                var timeoutMap = new java.util.HashMap<String, Object>();
                timeoutMap.put("success", false);
                timeoutMap.put("error", "Timeout - no response from Python Multiagent System");
                timeoutMap.put("timeout", "30 seconds");
                timeoutMap.put("timestamp", System.currentTimeMillis());
                timeoutMap.put("note", "Python Multiagent System에서 응답을 받지 못했습니다.");
                timeoutMap.put("threadingModel", "VirtualThread - Synchronous Style");
                timeoutMap.put("multiagentEnabled", useMultiagent);
                return ResponseEntity.status(408).body(timeoutMap);
            } else {
                var multiErrorMap = new java.util.HashMap<String, Object>();
                multiErrorMap.put("success", false);
                multiErrorMap.put("error", errorMessage != null ? errorMessage : "Unknown error occurred");
                multiErrorMap.put("correlationId", "unknown");
                multiErrorMap.put("timestamp", System.currentTimeMillis());
                multiErrorMap.put("note", "Multiagent System 연결에 문제가 있습니다");
                multiErrorMap.put("threadingModel", "VirtualThread - Synchronous Style");
                multiErrorMap.put("multiagentEnabled", useMultiagent);
                return ResponseEntity.status(500).body(multiErrorMap);
            }
        }
    }

    @PostMapping("/simulate-response")
    public ResponseEntity<Map<String, Object>> simulateAIAgentResponse(
            @RequestParam String correlationId,
            @RequestParam(defaultValue = "테스트 응답입니다") String responseMessage,
            @RequestParam(defaultValue = "test-session") String sessionId
    ) {
        
        // TODO: Python AI Agent 구현 후에는 이 메서드를 제거하고
        // 실제 Python Consumer/Producer로 대체

        var simulateMap = new java.util.HashMap<String, Object>();
        simulateMap.put("message", "시뮬레이션 엔드포인트입니다");
        simulateMap.put("note", "Python AI Agent 구현 후에는 실제 Kafka 응답으로 대체됩니다");
        simulateMap.put("correlationId", correlationId);
        simulateMap.put("responseMessage", responseMessage);
        simulateMap.put("sessionId", sessionId);
        simulateMap.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(simulateMap);
    }
}