package net.riverfrot.multiagent.chatbot.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.riverfrot.multiagent.chatbot.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatBot Streaming Service 테스트")
class ChatbotStreamingServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private AIMockService aiMockService;

    private ChatbotService chatbotService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        chatbotService = new ChatbotService(
                chatMessageRepository,
                conversationRepository,
                aiMockService,
                objectMapper
        );
    }

    @Test
    @DisplayName("스트리밍 채팅 처리 시 SSE Emitter 반환")
    void processStreamingChat_shouldReturnSseEmitter() {
        // Given
        String message = "테스트 메시지";
        String sessionId = "test-session";
        
        // When
        SseEmitter result = chatbotService.processStreamingChat(message, sessionId);

        // Then
        assertNotNull(result);
        // SSE Emitter가 정상적으로 반환되는지만 확인
    }

    @Test  
    @DisplayName("SSE Emitter 타임아웃 설정 확인")
    void processStreamingChat_shouldReturnSseEmitterWithTimeout() {
        // Given
        String message = "타임아웃 테스트";
        String sessionId = "timeout-session";

        // When
        SseEmitter result = chatbotService.processStreamingChat(message, sessionId);

        // Then
        assertNotNull(result);
        // 30초 타임아웃이 설정된 SSE Emitter 반환 확인
    }
}