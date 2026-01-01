package net.riverfrot.multiagent.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.riverfrot.multiagent.kafka.dto.ChatKafkaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kafka Producer 서비스 테스트
 * 실제 Kafka 환경에서 메시지 전송 검증
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 3,
        topics = {"chat-request", "chat-response", "system-events"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
)
@DirtiesContext
@ActiveProfiles("test")
@DisplayName("Kafka Producer 서비스 테스트")
class ChatKafkaProducerServiceTest {

    @Autowired
    private ChatKafkaProducerService producerService;

    @Autowired  
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("채팅 요청 Kafka 전송 - 성공")
    void sendChatRequest_Success() throws Exception {
        // Given
        String sessionId = "test-session-123";
        String userId = "test-user-456"; 
        String message = "안녕하세요, 테스트 메시지입니다.";

        // When
        CompletableFuture<String> future = producerService.sendChatRequest(sessionId, userId, message);
        String correlationId = future.get(5, TimeUnit.SECONDS);

        // Then
        assertNotNull(correlationId);
        assertTrue(correlationId.startsWith("req_"));
        assertTrue(correlationId.contains("_"));
    }

    @Test
    @DisplayName("여러 채팅 요청 동시 전송 - 성공")
    void sendMultipleChatRequests_Success() throws Exception {
        // Given
        int requestCount = 10;
        String sessionId = "test-session-multi";
        String userId = "test-user-multi";

        // When
        CompletableFuture<String>[] futures = new CompletableFuture[requestCount];
        
        for (int i = 0; i < requestCount; i++) {
            String message = "테스트 메시지 " + (i + 1);
            futures[i] = producerService.sendChatRequest(sessionId, userId, message);
        }

        // Then
        for (int i = 0; i < requestCount; i++) {
            String correlationId = futures[i].get(5, TimeUnit.SECONDS);
            assertNotNull(correlationId);
            assertTrue(correlationId.startsWith("req_"));
        }
    }

    @Test
    @DisplayName("Correlation ID 고유성 검증")
    void correlationId_UniqueGeneration() throws Exception {
        // Given
        String sessionId = "test-session-unique";
        String userId = "test-user-unique";
        String message = "고유성 테스트 메시지";
        
        // When
        CompletableFuture<String> future1 = producerService.sendChatRequest(sessionId, userId, message);
        Thread.sleep(1); // 최소 시간 차이 보장
        CompletableFuture<String> future2 = producerService.sendChatRequest(sessionId, userId, message);
        
        String correlationId1 = future1.get(5, TimeUnit.SECONDS);
        String correlationId2 = future2.get(5, TimeUnit.SECONDS);

        // Then
        assertNotEquals(correlationId1, correlationId2);
    }

    @Test
    @DisplayName("JSON 직렬화 검증") 
    void jsonSerialization_Validation() throws Exception {
        // Given
        String sessionId = "test-session-json";
        String userId = "test-user-json";
        String message = "JSON 직렬화 테스트";
        
        // When
        ChatKafkaRequest request = ChatKafkaRequest.create(
            "test-correlation-id", sessionId, userId, message
        );
        
        String json = objectMapper.writeValueAsString(request);
        ChatKafkaRequest deserializedRequest = objectMapper.readValue(json, ChatKafkaRequest.class);

        // Then
        assertEquals(request.correlationId(), deserializedRequest.correlationId());
        assertEquals(request.sessionId(), deserializedRequest.sessionId());
        assertEquals(request.userId(), deserializedRequest.userId());
        assertEquals(request.message(), deserializedRequest.message());
    }
}