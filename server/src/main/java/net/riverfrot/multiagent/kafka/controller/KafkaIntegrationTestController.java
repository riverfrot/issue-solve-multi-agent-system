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
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "correlationId", response.correlationId(),
                    "sessionId", response.sessionId(),
                    "message", response.message(),
                    "isFinal", response.isFinal(),
                    "chunkIndex", response.chunkIndex(),
                    "timestamp", System.currentTimeMillis(),
                    "note", "Python AI Agent로부터 성공적으로 응답을 받았습니다",
                    "threadingModel", "VirtualThread - Synchronous Style"
            ));
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("timeout")) {
                return ResponseEntity.status(408).body(Map.of(
                        "success", false,
                        "error", "Timeout - no response from Python AI Agent",
                        "timeout", "30 seconds",
                        "timestamp", System.currentTimeMillis(),
                        "note", "Python AI Agent에서 응답을 받지 못했습니다. Consumer 구현이 필요합니다.",
                        "threadingModel", "VirtualThread - Synchronous Style"
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", e.getMessage(),
                        "correlationId", "unknown",
                        "timestamp", System.currentTimeMillis(),
                        "note", "Python AI Agent가 아직 구현되지 않았거나 Kafka 연결에 문제가 있습니다",
                        "threadingModel", "VirtualThread - Synchronous Style"
                ));
            }
        }
    }
    
    /**
     * Kafka 서비스 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getKafkaServiceStatus() {
        Map<String, Object> status = chatKafkaService.getServiceStatus();
        
        return ResponseEntity.ok(Map.of(
                "kafkaIntegration", status,
                "timestamp", System.currentTimeMillis(),
                "readyForPythonAgent", true
        ));
    }
    
    @PostMapping("/simulate-response")
    public ResponseEntity<Map<String, Object>> simulateAIAgentResponse(
            @RequestParam String correlationId,
            @RequestParam(defaultValue = "테스트 응답입니다") String responseMessage,
            @RequestParam(defaultValue = "test-session") String sessionId
    ) {
        
        // TODO: Python AI Agent 구현 후에는 이 메서드를 제거하고
        // 실제 Python Consumer/Producer로 대체

        return ResponseEntity.ok(Map.of(
                "message", "시뮬레이션 엔드포인트입니다",
                "note", "Python AI Agent 구현 후에는 실제 Kafka 응답으로 대체됩니다",
                "correlationId", correlationId,
                "responseMessage", responseMessage,
                "sessionId", sessionId,
                "timestamp", System.currentTimeMillis()
        ));
    }
}