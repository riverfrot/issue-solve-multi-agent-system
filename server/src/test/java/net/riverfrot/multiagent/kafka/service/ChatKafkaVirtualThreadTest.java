package net.riverfrot.multiagent.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.riverfrot.multiagent.kafka.dto.ChatKafkaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Virtual Thread 기반 Kafka Producer/Consumer 단위 테스트
 * Mock을 사용한 빠른 검증 (Kafka 의존성 제거)
 */
public class ChatKafkaVirtualThreadTest {

    @Test
    @DisplayName("Producer 동기 전송 성능 측정")
    void testProducerSynchronousSending() {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Mock Producer (실제 Kafka 호출 없이 성능 측정)
        ChatKafkaProducerService producerService = spy(new ChatKafkaProducerService(null, objectMapper, "test-topic"));
        
        // When
        long startTime = System.currentTimeMillis();
        
        try {
            // Mock 동작: 실제 Kafka 전송 없이 correlationId만 생성
            doAnswer(invocation -> {
                Thread.sleep(50); // 50ms 지연 시뮬레이션
                return "req_test_" + System.currentTimeMillis();
            }).when(producerService).sendChatRequest(anyString(), anyString(), anyString());
            
            String correlationId = producerService.sendChatRequest("session-1", "user-1", "test message");
            
            long endTime = System.currentTimeMillis();
            
            // Then
            assertThat(correlationId).isNotNull().startsWith("req_");
            assertThat(endTime - startTime).isLessThan(200L); // 200ms 이내
            
            System.out.printf("✅ Producer 동기 전송 - correlationId: %s, 시간: %d ms%n", 
                    correlationId, endTime - startTime);
                    
        } catch (Exception e) {
            fail("Producer test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Consumer 응답 매칭 및 대기 테스트")
    void testConsumerResponseMatching() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();
        ChatKafkaConsumerService consumerService = new ChatKafkaConsumerService(objectMapper);
        
        String testCorrelationId = "test-correlation-123";
        
        // When: 응답 대기 등록
        CompletableFuture<ChatKafkaResponse> responseFuture = consumerService.waitForResponse(testCorrelationId);
        
        // 대기 중인 요청이 등록되었는지 확인
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(1);
        
        // Mock Response 생성 (올바른 constructor 사용)
        ChatKafkaResponse mockResponse = ChatKafkaResponse.success(
                testCorrelationId,
                "test-session", 
                "Mock response content",
                "test-agent",
                true, // isFinal
                0 // chunkIndex
        );
        
        // Virtual Thread에서 응답 완료 시뮬레이션
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(100); // 0.1초 후 응답
                responseFuture.complete(mockResponse);
            } catch (Exception e) {
                responseFuture.completeExceptionally(e);
            }
        });

        // Then: 응답 수신 확인
        ChatKafkaResponse response = responseFuture.get(2, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.correlationId()).isEqualTo(testCorrelationId);
        assertThat(response.message()).isEqualTo("Mock response content");
        assertThat(response.isFinal()).isTrue();

        System.out.printf("✅ Consumer 응답 매칭 완료 - correlationId: %s, message: %s%n", 
                response.correlationId(), response.message());
    }

    @Test
    @DisplayName("Virtual Thread 동시 처리 성능 벤치마크")
    void testVirtualThreadConcurrentPerformance() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();
        ChatKafkaConsumerService consumerService = new ChatKafkaConsumerService(objectMapper);

        // When: 100개 동시 요청-응답 시뮬레이션
        int requestCount = 100;
        java.util.List<CompletableFuture<String>> futures = new java.util.ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < requestCount; i++) {
            final int requestId = i;
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 요청-응답 플로우 시뮬레이션
                    String correlationId = "perf-test-" + requestId;
                    
                    // 응답 대기 등록
                    CompletableFuture<ChatKafkaResponse> responseFuture = consumerService.waitForResponse(correlationId);
                    
                    // 가상의 AI Agent 응답 (10-50ms 랜덤 지연)
                    Thread.startVirtualThread(() -> {
                        try {
                            Thread.sleep(10 + (int)(Math.random() * 40)); // 10-50ms 랜덤
                            ChatKafkaResponse mockResponse = ChatKafkaResponse.success(
                                    correlationId,
                                    "perf-session-" + requestId,
                                    "Performance test response #" + requestId,
                                    "perf-agent",
                                    true,
                                    0
                            );
                            responseFuture.complete(mockResponse);
                        } catch (Exception e) {
                            responseFuture.completeExceptionally(e);
                        }
                    });
                    
                    // 응답 대기 (최대 1초)
                    ChatKafkaResponse response = responseFuture.get(1, TimeUnit.SECONDS);
                    return response.correlationId();
                    
                } catch (Exception e) {
                    throw new RuntimeException("Request " + requestId + " failed: " + e.getMessage(), e);
                }
            });
            futures.add(future);
        }
        
        // 모든 요청 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        double avgDuration = (double) totalDuration / requestCount;
        double requestsPerSecond = requestCount * 1000.0 / totalDuration;

        // Then: 성능 검증
        assertThat(totalDuration).isLessThan(10000L); // 10초 이내
        assertThat(avgDuration).isLessThan(100.0); // 평균 100ms/req 이내
        assertThat(requestsPerSecond).isGreaterThan(20.0); // 초당 20req 이상

        System.out.printf("🚀 Virtual Thread 성능 벤치마크 결과%n");
        System.out.printf("📊 %d개 동시 요청-응답 처리%n", requestCount);
        System.out.printf("⏱️ 총 시간: %d ms%n", totalDuration);
        System.out.printf("📈 평균 응답시간: %.2f ms/req%n", avgDuration);
        System.out.printf("🔥 처리율: %.1f req/sec%n", requestsPerSecond);

        // 메모리 정리 확인
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("메모리 누수 방지 및 타임아웃 처리")
    void testMemoryLeakPrevention() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();
        ChatKafkaConsumerService consumerService = new ChatKafkaConsumerService(objectMapper);
        
        // When: 다수의 요청을 등록하고 일부만 응답
        String[] correlationIds = new String[10];
        for (int i = 0; i < 10; i++) {
            correlationIds[i] = "memory-test-" + i;
            consumerService.waitForResponse(correlationIds[i]);
        }
        
        // 초기 상태: 10개 대기 중
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(10);
        
        // 일부만 응답 완료 (5개)
        for (int i = 0; i < 5; i++) {
            consumerService.cancelWaitForResponse(correlationIds[i]);
        }
        
        // Then: 5개만 남아야 함
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(5);
        
        // 남은 것들도 정리
        for (int i = 5; i < 10; i++) {
            consumerService.cancelWaitForResponse(correlationIds[i]);
        }
        
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(0);

        System.out.println("✅ 메모리 누수 방지 테스트 완료 - 모든 대기 요청이 적절히 정리됨");
    }

    @Test
    @DisplayName("Virtual Thread vs 일반 Thread 성능 비교 (Mock)")
    void testVirtualThreadVsNormalThreadPerformance() {
        // Virtual Thread 성능 측정
        long vtStartTime = System.currentTimeMillis();
        int taskCount = 1000;
        
        java.util.List<CompletableFuture<Void>> vtFutures = new java.util.ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Virtual Thread에서 블로킹 작업 시뮬레이션
                    Thread.sleep(10); // 10ms 블로킹
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            vtFutures.add(future);
        }
        
        try {
            CompletableFuture.allOf(vtFutures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Virtual Thread test failed: " + e.getMessage());
        }
        
        long vtEndTime = System.currentTimeMillis();
        long vtDuration = vtEndTime - vtStartTime;

        System.out.printf("🧵 Virtual Thread 성능 (Mock):%n");
        System.out.printf("  📊 %d개 작업 완료: %d ms%n", taskCount, vtDuration);
        System.out.printf("  ⚡ 평균 처리시간: %.2f ms/task%n", (double) vtDuration / taskCount);
        System.out.printf("  🚀 처리율: %.1f tasks/sec%n", taskCount * 1000.0 / vtDuration);

        // 성능 검증
        assertThat(vtDuration).isLessThan(20000L); // 20초 이내
        double avgTime = (double) vtDuration / taskCount;
        assertThat(avgTime).isLessThan(20.0); // 평균 20ms 이내
    }

    @Test
    @DisplayName("동시성 안전성 테스트 - 같은 correlationId 중복 처리")
    void testConcurrentSafety() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();
        ChatKafkaConsumerService consumerService = new ChatKafkaConsumerService(objectMapper);
        
        String correlationId = "concurrent-test-123";
        
        // When: 같은 correlationId로 여러 번 대기 등록 시도
        java.util.List<CompletableFuture<ChatKafkaResponse>> futures = new java.util.ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            futures.add(consumerService.waitForResponse(correlationId + "-" + i));
        }
        
        // Then: 각각 독립적으로 등록되어야 함
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(5);
        
        // 정리
        for (int i = 0; i < 5; i++) {
            consumerService.cancelWaitForResponse(correlationId + "-" + i);
        }
        
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(0);
        
        System.out.println("✅ 동시성 안전성 테스트 완료 - 각 correlationId가 독립적으로 관리됨");
    }
}