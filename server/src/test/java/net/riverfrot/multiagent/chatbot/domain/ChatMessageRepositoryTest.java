package net.riverfrot.multiagent.chatbot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("채팅 메시지 Repository 및 ChatHistory 도메인 테스트")
class ChatMessageRepositoryTest {
    
    @Autowired
    private ChatMessageRepository repository;
    
    @Test
    @DisplayName("메시지 저장 - 필수 필드 누락 시 예외 발생")
    void save_shouldThrowExceptionForInvalidMessage() {
        // Given - content가 빈 메시지 (ChatMessage 내부 검증 로직 트리거)
        
        // When & Then
        assertThrows(
            IllegalArgumentException.class,
            () -> ChatMessage.builder()
                    .sessionId("test-session")
                    .role("user")
                    .content("") // 빈 content로 검증 실패 유도
                    .build()
        );
    }
    
    @Test
    @DisplayName("채팅 기록 조회 - 존재하지 않는 세션ID로 조회 시 빈 목록 반환")
    void findBySessionIdOrderByTimestamp_shouldReturnEmptyListForNonExistentSession() {
        // Given - 존재하지 않는 세션ID
        String nonExistentSessionId = "non-existent-session";
        
        // When
        List<ChatMessage> messages = repository.findBySessionIdOrderByTimestamp(nonExistentSessionId);
        
        // Then - 빈 목록 반환 (예외 발생하지 않음)
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }
    
    @Test
    @DisplayName("채팅 기록 조회 - 시간순 정렬 검증")
    void findBySessionIdOrderByTimestamp_shouldReturnMessagesInTimeOrder() {
        // Given - 동일 세션의 메시지들을 역순으로 저장
        String sessionId = "test-session";
        LocalDateTime baseTime = LocalDateTime.now();
        
        ChatMessage laterMessage = ChatMessage.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content("Later message")
                .agentType(AgentType.GENERAL)
                .timestamp(baseTime.plusMinutes(1))
                .build();
                
        ChatMessage earlierMessage = ChatMessage.builder()
                .sessionId(sessionId)
                .role("user")
                .content("Earlier message")
                .timestamp(baseTime)
                .build();
        
        // 나중 메시지를 먼저 저장
        repository.save(laterMessage);
        repository.save(earlierMessage);
        
        // When
        List<ChatMessage> messages = repository.findBySessionIdOrderByTimestamp(sessionId);
        
        // Then - 시간순으로 정렬되어야 함 (이전 메시지가 먼저)
        assertEquals(2, messages.size());
        assertEquals("Earlier message", messages.get(0).getContent());
        assertEquals("Later message", messages.get(1).getContent());
        assertTrue(messages.get(0).getTimestamp().isBefore(messages.get(1).getTimestamp()));
    }
    
    @Test
    @DisplayName("ChatMessage 도메인 로직 - 과도하게 긴 메시지는 예외 발생")
    void chatMessage_shouldRejectTooLongContent() {
        // Given - 10000자를 초과하는 메시지
        String tooLongContent = "a".repeat(10001);
        
        // When & Then
        assertThrows(
            IllegalArgumentException.class,
            () -> ChatMessage.builder()
                    .sessionId("test-session")
                    .role("user")
                    .content(tooLongContent)
                    .build(),
            "Content too long (max 10000 chars)"
        );
    }
}