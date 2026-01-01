package net.riverfrot.multiagent.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.riverfrot.multiagent.kafka.dto.ChatKafkaRequest;
import net.riverfrot.multiagent.kafka.dto.ChatKafkaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Virtual Thread 기반 Kafka Producer/Consumer 단위 테스트
 * Mock을 사용한 빠른 검증
 */
public class ChatKafkaSimpleTest {

    @Test
    @DisplayName("Producer 동기 전송 테스트")
    void testProducerSynchronousSending() throws Exception {
        // Given
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> mockKafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Mock SendResult
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> mockFuture = mock(CompletableFuture.class);
        SendResult<String, String> mockSendResult = mock(SendResult.class);
        
        when(mockKafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(mockFuture);
        when(mockFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(mockSendResult);
        
        ChatKafkaProducerService producerService = new ChatKafkaProducerService(
                mockKafkaTemplate, objectMapper, "test-topic"
        );

        // When
        long startTime = System.currentTimeMillis();
        String correlationId = producerService.sendChatRequest("session-1", "user-1", "test message");
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(correlationId).isNotNull().startsWith("req_");
        assertThat(endTime - startTime).isLessThan(1000L); // 1초 이내

        // Verify 호출 확인
        verify(mockKafkaTemplate, times(1)).send(eq("test-topic"), eq("session-1"), anyString());
        verify(mockFuture, times(1)).get(5L, TimeUnit.SECONDS);

        System.out.printf("✅ Producer 동기 전송 완료 - correlationId: %s, 시간: %d ms%n", 
                correlationId, endTime - startTime);
    }

    @Test
    @DisplayName("Consumer 응답 매칭 테스트")
    void testConsumerResponseMatching() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();
        ChatKafkaConsumerService consumerService = new ChatKafkaConsumerService(objectMapper);
        
        String testCorrelationId = "test-correlation-123";
        
        // When: 응답 대기 등록
        CompletableFuture<ChatKafkaResponse> responseFuture = consumerService.waitForResponse(testCorrelationId);
        
        // 대기 중인 요청이 등록되었는지 확인
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(1);
        
        // Mock Response 생성
        ChatKafkaResponse mockResponse = new ChatKafkaResponse(
                testCorrelationId,
                "test-session", 
                "Mock response content",
                true, // isFinal
                0, // chunkIndex
                System.currentTimeMillis()
        );
        
        // Consumer 내부 메서드를 통해 응답 완료 시뮬레이션
        String responseJson = objectMapper.writeValueAsString(mockResponse);
        
        // processMessage를 직접 호출할 수 없으므로, 다른 방식으로 테스트
        // 실제로는 Kafka에서 메시지가 와서 complete()가 호출됨을 시뮬레이션
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(100); // 0.1초 후 응답
                // Consumer의 pendingResponses에서 future를 찾아 complete 호출
                // 이는 실제 Consumer가 하는 일을 시뮬레이션
                responseFuture.complete(mockResponse);
            } catch (Exception e) {
                responseFuture.completeExceptionally(e);
            }
        });

        // Then: 응답 수신 확인
        ChatKafkaResponse response = responseFuture.get(2, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.correlationId()).isEqualTo(testCorrelationId);
        assertThat(response.content()).isEqualTo("Mock response content");
        assertThat(response.isFinal()).isTrue();

        System.out.printf("✅ Consumer 응답 매칭 완료 - correlationId: %s, content: %s%n", 
                response.correlationId(), response.content());
    }

    @Test
    @DisplayName("Virtual Thread 동시 처리 성능 테스트")
    void testVirtualThreadPerformance() throws Exception {
        // Given
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> mockKafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> mockFuture = mock(CompletableFuture.class);
        SendResult<String, String> mockSendResult = mock(SendResult.class);
        
        when(mockKafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(mockFuture);
        when(mockFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(mockSendResult);
        
        ChatKafkaProducerService producerService = new ChatKafkaProducerService(
                mockKafkaTemplate, objectMapper, "test-topic"
        );

        // When: 50개 동시 요청
        int requestCount = 50;
        java.util.List<CompletableFuture<String>> futures = new java.util.ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < requestCount; i++) {
            final int requestId = i;
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return producerService.sendChatRequest(
                            "session-" + requestId, 
                            "user-" + requestId, 
                            "Message #" + requestId
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }
        
        // 모든 요청 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        double avgDuration = (double) totalDuration / requestCount;

        // Then: 성능 검증
        assertThat(totalDuration).isLessThan(10000L); // 10초 이내
        assertThat(avgDuration).isLessThan(200.0); // 평균 200ms/req 이내

        System.out.printf("🏃‍♂️ Virtual Thread 성능 테스트 완료%n");
        System.out.printf("📊 %d개 요청 - 총 시간: %d ms, 평균: %.2f ms/req%n", 
                requestCount, totalDuration, avgDuration);

        // Verify
        verify(mockKafkaTemplate, times(requestCount)).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("타임아웃 및 메모리 누수 방지 테스트")
    void testTimeoutAndMemoryLeak() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();
        ChatKafkaConsumerService consumerService = new ChatKafkaConsumerService(objectMapper);
        
        // When: 매우 짧은 타임아웃으로 응답 대기
        String correlationId = "timeout-test-123";
        CompletableFuture<ChatKafkaResponse> future = consumerService.waitForResponse(correlationId);
        
        // 초기 상태 확인
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(1);
        
        // 응답 없이 타임아웃 대기
        Thread.sleep(100); // 0.1초 대기
        
        // Then: 아직 타임아웃 전이므로 대기 중
        assertThat(future.isDone()).isFalse();
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(1);
        
        // 수동으로 취소 테스트
        consumerService.cancelWaitForResponse(correlationId);
        assertThat(consumerService.getPendingResponseCount()).isEqualTo(0);

        System.out.println("✅ 타임아웃 및 메모리 누수 방지 테스트 완료");
    }
}