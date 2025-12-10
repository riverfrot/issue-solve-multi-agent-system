package net.riverfrot.multiagent.chatbot.dto;

import net.riverfrot.multiagent.chatbot.domain.AgentType;
import net.riverfrot.multiagent.chatbot.domain.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChatMessageResponse DTO 변환 테스트")
class ChatMessageResponseTest {

    @Test
    @DisplayName("ChatMessage → ChatMessageResponse 변환 - 사용자 메시지")
    void from_shouldConvertUserMessage() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        ChatMessage userMessage = ChatMessage.builder()
                .sessionId("test-session")
                .role("user")
                .content("Hello, how are you?")
                .timestamp(timestamp)
                .build();

        // When
        ChatMessageResponse response = ChatMessageResponse.from(userMessage);

        // Then
        assertNotNull(response);
        assertEquals(userMessage.getId(), response.id());
        assertEquals("test-session", response.sessionId());
        assertEquals("Hello, how are you?", response.content());
        assertEquals("user", response.role());
        assertNull(response.agentType()); // 사용자 메시지는 agentType이 null
        assertEquals(timestamp, response.timestamp());
    }

    @Test
    @DisplayName("ChatMessage → ChatMessageResponse 변환 - AI 어시스턴트 메시지")
    void from_shouldConvertAssistantMessage() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        ChatMessage assistantMessage = ChatMessage.builder()
                .sessionId("test-session")
                .role("assistant")
                .content("I'm doing well, thank you!")
                .agentType(AgentType.SUPERVISOR)
                .timestamp(timestamp)
                .build();

        // When
        ChatMessageResponse response = ChatMessageResponse.from(assistantMessage);

        // Then
        assertNotNull(response);
        assertEquals(assistantMessage.getId(), response.id());
        assertEquals("test-session", response.sessionId());
        assertEquals("I'm doing well, thank you!", response.content());
        assertEquals("assistant", response.role());
        assertEquals("supervisor", response.agentType()); // agentType 코드 변환
        assertEquals(timestamp, response.timestamp());
    }

    @Test
    @DisplayName("ChatMessageResponse 변환 - agentType이 null인 경우 null 유지")
    void from_shouldHandleNullAgentType() {
        // Given - agentType이 null인 메시지
        ChatMessage messageWithoutAgent = ChatMessage.builder()
                .sessionId("test-session")
                .role("user")
                .content("Test message")
                .build();

        // When
        ChatMessageResponse response = ChatMessageResponse.from(messageWithoutAgent);

        // Then - agentType이 null로 변환되어야 함
        assertNull(response.agentType());
        assertEquals("user", response.role());
        assertEquals("Test message", response.content());
    }
}