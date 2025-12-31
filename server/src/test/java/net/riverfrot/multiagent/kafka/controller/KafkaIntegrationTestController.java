package net.riverfrot.multiagent.kafka.controller;

import net.riverfrot.multiagent.kafka.service.ChatKafkaProducerService;
import net.riverfrot.multiagent.kafka.service.ChatKafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 통합 테스트용 컨트롤러
 * 실제 동작을 수동으로 검증하기 위한 REST API 제공
 * 
 * 테스트 시나리오:
 * 1. POST /kafka/test/send - 메시지 전송
 * 2. GET /kafka/test/status - Consumer 상태 확인
 * 3. POST /kafka/test/simulate-response - Python AI Agent 응답 시뮬레이션
 */
@RestController
@RequestMapping("/kafka/test")
public class KafkaIntegrationTestController {

    @Autowired
    private ChatKafkaProducerService producerService;

    @Autowired
    private ChatKafkaConsumerService consumerService;

    /**
     * 채팅 요청 전송 테스트 (Virtual Thread 동기 방식)
     */
    @PostMapping("/send")
    public Map<String, Object> sendTestMessage(@RequestBody Map<String, String> request) {
        long startTime = System.currentTimeMillis();
        
        try {
            String sessionId = request.getOrDefault("sessionId", "test-session");
            String userId = request.getOrDefault("userId", "test-user");  
            String message = request.getOrDefault("message", "Test message from Virtual Thread");

            // Virtual Thread에서 동기 방식으로 전송
            String correlationId = producerService.sendChatRequest(sessionId, userId, message);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            return Map.of(
                    "status", "success",
                    "correlationId", correlationId,
                    "sessionId", sessionId,
                    "message", message,
                    "sendDurationMs", duration,
                    "threadModel", "Virtual Thread (Synchronous)",
                    "timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            return Map.of(
                    "status", "error",
                    "error", e.getMessage(),
                    "sendDurationMs", duration,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * Producer → Consumer 전체 플로우 테스트
     */
    @PostMapping("/send-and-wait")
    public Map<String, Object> sendAndWaitForResponse(@RequestBody Map<String, String> request) {
        long startTime = System.currentTimeMillis();
        
        try {
            String sessionId = request.getOrDefault("sessionId", "test-session");
            String userId = request.getOrDefault("userId", "test-user");
            String message = request.getOrDefault("message", "E2E Test Message");
            int timeoutSeconds = Integer.parseInt(request.getOrDefault("timeoutSeconds", "10"));

            // 1. 메시지 전송
            String correlationId = producerService.sendChatRequest(sessionId, userId, message);
            
            // 2. 응답 대기 등록  
            CompletableFuture<net.riverfrot.multiagent.kafka.dto.ChatKafkaResponse> responseFuture = 
                    consumerService.waitForResponse(correlationId);
            
            // 3. 지정된 시간만큼 응답 대기
            var response = responseFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            
            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;

            if (response != null) {
                return Map.of(
                        "status", "success",
                        "correlationId", correlationId,
                        "response", Map.of(
                                "content", response.message(),
                                "isFinal", response.isFinal(),
                                "chunkIndex", response.chunkIndex(),
                                "sessionId", response.sessionId()
                        ),
                        "totalDurationMs", totalDuration,
                        "threadModel", "Virtual Thread E2E",
                        "timestamp", System.currentTimeMillis()
                );
            } else {
                return Map.of(
                        "status", "timeout",
                        "correlationId", correlationId,
                        "totalDurationMs", totalDuration,
                        "message", "No response received within timeout period",
                        "timestamp", System.currentTimeMillis()
                );
            }

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;

            return Map.of(
                    "status", "error",
                    "error", e.getMessage(),
                    "totalDurationMs", totalDuration,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * Consumer 상태 조회
     */
    @GetMapping("/status")
    public Map<String, Object> getConsumerStatus() {
        return Map.of(
                "service", "ChatKafkaConsumerService",
                "pendingResponseCount", consumerService.getPendingResponseCount(),
                "status", "active", 
                "threadModel", "Spring Managed Virtual Thread",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 성능 테스트용 - 다중 동시 요청
     */
    @PostMapping("/performance-test")
    public Map<String, Object> performanceTest(@RequestBody Map<String, Object> request) {
        int requestCount = (Integer) request.getOrDefault("requestCount", 10);
        long startTime = System.currentTimeMillis();
        
        try {
            java.util.List<CompletableFuture<String>> futures = new java.util.ArrayList<>();
            
            // 동시에 여러 요청 전송
            for (int i = 0; i < requestCount; i++) {
                final int requestId = i;
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    String sessionId = "perf-session-" + requestId;
                    String userId = "perf-user-" + requestId;
                    String message = "Performance test message #" + requestId;
                    
                    return producerService.sendChatRequest(sessionId, userId, message);
                });
                futures.add(future);
            }
            
            // 모든 요청 완료 대기
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );
            
            allOf.get(30, TimeUnit.SECONDS); // 30초 타임아웃
            
            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;
            double avgDuration = (double) totalDuration / requestCount;

            return Map.of(
                    "status", "success",
                    "requestCount", requestCount,
                    "totalDurationMs", totalDuration,
                    "avgDurationPerRequestMs", avgDuration,
                    "requestsPerSecond", requestCount * 1000.0 / totalDuration,
                    "threadModel", "Virtual Thread Concurrent",
                    "timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;

            return Map.of(
                    "status", "error",
                    "requestCount", requestCount,
                    "totalDurationMs", totalDuration,
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }

    /**
     * 메모리 사용량 및 스레드 정보 조회
     */
    @GetMapping("/system-info")
    public Map<String, Object> getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return Map.of(
                "memory", Map.of(
                        "maxMB", maxMemory / (1024 * 1024),
                        "totalMB", totalMemory / (1024 * 1024),
                        "usedMB", usedMemory / (1024 * 1024),
                        "freeMB", freeMemory / (1024 * 1024),
                        "usagePercent", (double) usedMemory / totalMemory * 100
                ),
                "threads", Map.of(
                        "activeCount", Thread.activeCount(),
                        "virtualThreadSupport", true,
                        "currentThread", Map.of(
                                "name", Thread.currentThread().getName(),
                                "isVirtual", Thread.currentThread().isVirtual()
                        )
                ),
                "kafka", Map.of(
                        "pendingResponses", consumerService.getPendingResponseCount()
                ),
                "timestamp", System.currentTimeMillis()
        );
    }
}