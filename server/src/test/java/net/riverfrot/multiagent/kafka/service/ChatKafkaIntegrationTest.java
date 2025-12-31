package net.riverfrot.multiagent.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.riverfrot.multiagent.kafka.dto.ChatKafkaRequest;
import net.riverfrot.multiagent.kafka.dto.ChatKafkaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * Kafka Producer/Consumer Virtual Thread 통합 테스트
 * 실제 Embedded Kafka를 사용한 E2E 검증
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"chat-request", "chat-response"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.consumer.group-id=test-group",
        "multiagent.kafka.topics.chat-request=chat-request",
        "multiagent.kafka.topics.chat-response=chat-response",
        "spring.threads.virtual.enabled=true" // Virtual Thread 활성화
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChatKafkaIntegrationTest {

    @Autowired
    private ChatKafkaProducerService producerService;

    @Autowired
    private ChatKafkaConsumerService consumerService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Virtual Thread 기반 Kafka Producer 동기 전송 테스트")
    @Timeout(10) // 10초 제한
    void testProducerSynchronousSending() {
        // Given
        String sessionId = "test-session-123";
        String userId = "user-456";
        String message = "Hello Virtual Thread!";

        // When - Virtual Thread에서 동기 방식으로 전송
        long startTime = System.currentTimeMillis();
        String correlationId = producerService.sendChatRequest(sessionId, userId, message);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(correlationId)
                .isNotNull()
                .startsWith("req_")
                .contains("_");

        long sendDuration = endTime - startTime;
        System.out.printf("🚀 동기 전송 완료 시간: %d ms%n", sendDuration);
        
        // Kafka 전송은 보통 100ms 이내여야 함
        assertThat(sendDuration).isLessThan(5000L);
    }

    @Test
    @DisplayName("Producer → Consumer 전체 흐름 테스트 (Virtual Thread)")
    @Timeout(15) // 15초 제한
    void testCompleteProducerConsumerFlow() throws Exception {
        // Given
        String sessionId = "test-session-789";
        String userId = "user-999";
        String message = "Virtual Thread E2E Test";

        // 응답 시뮬레이션을 위한 준비
        CountDownLatch responseLatch = new CountDownLatch(1);
        AtomicReference<String> receivedCorrelationId = new AtomicReference<>();

        // When 1: Producer가 요청 전송 (동기 방식)
        String correlationId = producerService.sendChatRequest(sessionId, userId, message);
        System.out.printf("📤 요청 전송 완료 - correlationId: %s%n", correlationId);

        // When 2: Consumer에 응답 대기 등록
        CompletableFuture<ChatKafkaResponse> responseFuture = consumerService.waitForResponse(correlationId);
        
        // When 3: Python AI Agent 응답 시뮬레이션 (별도 스레드에서)
        Thread.startVirtualThread(() -> {
            try {
                // 1초 후 응답 시뮬레이션
                Thread.sleep(1000);
                
                ChatKafkaResponse mockResponse = ChatKafkaResponse.create(
                        correlationId,
                        sessionId,
                        "Virtual Thread로 처리된 응답입니다!",
                        true, // isFinal
                        0, // chunkIndex
                        System.currentTimeMillis()
                );
                
                String responseJson = objectMapper.writeValueAsString(mockResponse);
                kafkaTemplate.send("chat-response", sessionId, responseJson);
                
                receivedCorrelationId.set(correlationId);
                responseLatch.countDown();
                
                System.out.printf("📥 응답 시뮬레이션 완료 - correlationId: %s%n", correlationId);
                
            } catch (Exception e) {
                System.err.printf("❌ 응답 시뮬레이션 실패: %s%n", e.getMessage());
            }
        });

        // Then: 응답 수신 검증
        ChatKafkaResponse response = responseFuture.get(10, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.correlationId()).isEqualTo(correlationId);
        assertThat(response.sessionId()).isEqualTo(sessionId);
        assertThat(response.content()).isEqualTo("Virtual Thread로 처리된 응답입니다!");
        assertThat(response.isFinal()).isTrue();

        // 전체 플로우 완료 확인
        boolean completed = responseLatch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(receivedCorrelationId.get()).isEqualTo(correlationId);

        System.out.printf("✅ 전체 플로우 완료 - 응답: %s%n", response.content());
    }

    @Test
    @DisplayName("동시 다중 요청 처리 성능 테스트 (Virtual Thread)")
    @Timeout(20) // 20초 제한  
    void testConcurrentMultipleRequests() throws Exception {
        // Given
        int requestCount = 100; // 100개 동시 요청
        CountDownLatch completionLatch = new CountDownLatch(requestCount);
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        long startTime = System.currentTimeMillis();

        // When: 100개 요청을 Virtual Thread로 동시 전송
        for (int i = 0; i < requestCount; i++) {
            final int requestId = i;
            
            Thread.startVirtualThread(() -> {
                try {
                    String sessionId = "concurrent-session-" + requestId;
                    String userId = "user-" + requestId;
                    String message = "Concurrent request #" + requestId;

                    String correlationId = producerService.sendChatRequest(sessionId, userId, message);
                    
                    // correlation ID 검증
                    if (correlationId == null || !correlationId.startsWith("req_")) {
                        throw new IllegalStateException("Invalid correlationId: " + correlationId);
                    }

                    completionLatch.countDown();
                    
                } catch (Exception e) {
                    errorRef.set(e);
                    completionLatch.countDown();
                }
            });
        }

        // Then: 모든 요청이 완료될 때까지 대기
        boolean allCompleted = completionLatch.await(15, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        // 결과 검증
        assertThat(allCompleted).isTrue();
        assertThat(errorRef.get()).isNull();

        double avgTimePerRequest = (double) totalDuration / requestCount;
        System.out.printf("🏃‍♂️ %d개 동시 요청 완료%n", requestCount);
        System.out.printf("⏱️ 총 소요시간: %d ms%n", totalDuration);
        System.out.printf("📊 평균 요청당 시간: %.2f ms%n", avgTimePerRequest);

        // 성능 기준: 100개 요청이 15초 내, 평균 150ms/req 이내
        assertThat(totalDuration).isLessThan(15000L);
        assertThat(avgTimePerRequest).isLessThan(150.0);
    }

    @Test
    @DisplayName("메모리 누수 방지 테스트 - 타임아웃된 요청 정리")
    @Timeout(10)
    void testMemoryLeakPrevention() throws Exception {
        // Given
        String correlationId = "test-correlation-timeout";
        
        // When: 응답 대기 등록 (타임아웃 설정: 1초)
        CompletableFuture<ChatKafkaResponse> future = consumerService.waitForResponse(correlationId);
        
        // 초기 상태 확인
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(1);
        
        // 2초 대기 (타임아웃 후)
        Thread.sleep(2000);
        
        // Then: 타임아웃으로 자동 정리되었는지 확인
        assertThat(future.isCompletedExceptionally()).isTrue();
        
        // 잠시 대기 후 메모리 정리 확인
        Thread.sleep(1000);
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(0);
        
        System.out.println("✅ 메모리 누수 방지 테스트 통과 - 타임아웃된 요청이 자동 정리됨");
    }
}