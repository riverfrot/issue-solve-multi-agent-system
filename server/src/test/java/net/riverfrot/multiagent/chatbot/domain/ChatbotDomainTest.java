package net.riverfrot.multiagent.chatbot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("챗봇 도메인 테스트")
class ChatbotDomainTest {

    @Test
    @DisplayName("존재하지 않는 에이전트 코드는 예외를 발생시켜야 한다")
    void agentType_shouldThrowExceptionForInvalidCode() {
        // given: 존재하지 않는 에이전트 코드
        String invalidCode = "invalid_agent";
        
        // when & then: 예외가 발생해야 함
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> AgentType.fromCode(invalidCode)
        );
        
        assertTrue(exception.getMessage().contains("Unknown agent type"));
    }

    @Test
    @DisplayName("빈 내용으로 메시지 생성 시 예외가 발생해야 한다")
    void chatMessage_shouldRejectEmptyContent() {
        // given: 빈 내용
        String sessionId = "test-session";
        String emptyContent = "   "; // 공백만 있는 경우
        
        // when & then: 예외가 발생해야 함
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ChatMessage.createUserMessage(sessionId, emptyContent)
        );
        
        assertEquals("Content cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("다른 세션 ID의 메시지 추가 시 예외가 발생해야 한다")
    void conversation_shouldRejectMessageFromDifferentSession() {
        // given: 특정 세션의 대화와 다른 세션의 메시지
        Conversation conversation = Conversation.withSessionId("session-A", "user-123");
        ChatMessage wrongSessionMessage = ChatMessage.createUserMessage("session-B", "잘못된 세션 메시지");
        
        // when & then: 예외가 발생해야 함
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> conversation.addMessage(wrongSessionMessage)
        );
        
        assertTrue(exception.getMessage().contains("Session ID mismatch"));
    }
}