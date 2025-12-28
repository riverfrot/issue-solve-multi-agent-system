package net.riverfrot.multiagent.kafka.controller;

import net.riverfrot.multiagent.kafka.service.ChatKafkaProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/kafka-test")
public class KafkaTestController {
    
    private final ChatKafkaProducerService producerService;
    
    public KafkaTestController(ChatKafkaProducerService producerService) {
        this.producerService = producerService;
    }
    
    @GetMapping("/send-message")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendTestMessage(
            @RequestParam String message,
            @RequestParam(defaultValue = "test-session") String sessionId,
            @RequestParam(defaultValue = "test-user") String userId
    ) {
        
        return producerService.sendChatRequest(sessionId, userId, message)
                .handle((correlationId, throwable) -> {
                    if (throwable != null) {
                        return ResponseEntity.status(500).body(Map.of(
                                "success", false,
                                "error", throwable.getMessage(),
                                "timestamp", System.currentTimeMillis()
                        ));
                    }
                    
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "correlationId", correlationId,
                            "sessionId", sessionId,
                            "userId", userId,
                            "message", message,
                            "timestamp", System.currentTimeMillis()
                    ));
                });
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> kafkaHealth() {
        return ResponseEntity.ok(Map.of(
                "kafka", "available",
                "producer", "ready",
                "timestamp", System.currentTimeMillis()
        ));
    }
}